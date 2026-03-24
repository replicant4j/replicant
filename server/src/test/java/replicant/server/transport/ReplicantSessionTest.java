package replicant.server.transport;

import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.json.Json;
import javax.json.JsonObject;
import javax.websocket.CloseReason;
import javax.websocket.RemoteEndpoint;
import javax.websocket.Session;
import replicant.server.ValueUtil;
import org.testng.annotations.Test;
import replicant.server.Change;
import replicant.server.ChangeSet;
import replicant.server.ChannelAction;
import replicant.server.ChannelAddress;
import replicant.server.EntityMessage;
import replicant.server.MessageTestUtil;
import replicant.shared.Messages;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class ReplicantSessionTest
{
  @SuppressWarnings( "ConstantConditions" )
  @Test
  public void basicOperation()
  {
    final var webSocketSession = mock( Session.class );
    final var sessionId = ValueUtil.randomString();
    when( webSocketSession.getId() ).thenReturn( sessionId );
    final var session = new ReplicantSession( webSocketSession );
    session.getLock().lock();

    assertEquals( session.getId(), sessionId );

    assertEquals( getSubscriptions( session ).size(), 0 );

    final var cd1 = new ChannelAddress( 1, null );

    assertNull( session.findSubscriptionEntry( cd1 ) );
    assertFalse( session.isSubscriptionEntryPresent( cd1 ) );

    try
    {
      session.getSubscriptionEntry( cd1 );
      fail( "Expected to be unable to get non existent entry" );
    }
    catch ( final IllegalStateException ise )
    {
      assertEquals( ise.getMessage(), "Unable to locate subscription entry for 1" );
    }

    final var entry = session.createSubscriptionEntry( cd1 );

    assertEquals( entry.address(), cd1 );
    assertEquals( getSubscriptions( session ).size(), 1 );
    assertEquals( session.findSubscriptionEntry( cd1 ), entry );
    assertEquals( session.getSubscriptionEntry( cd1 ), entry );
    assertTrue( getSubscriptions( session ).containsKey( cd1 ) );
    assertTrue( getSubscriptions( session ).containsValue( entry ) );

    assertTrue( session.deleteSubscriptionEntry( entry ) );
    assertFalse( session.deleteSubscriptionEntry( entry ) );

    assertNull( session.findSubscriptionEntry( cd1 ) );
    assertFalse( session.isSubscriptionEntryPresent( cd1 ) );
    assertEquals( getSubscriptions( session ).size(), 0 );
  }

  @Test
  public void subscriptionIndexesByChannelAndRoot()
  {
    final var session = new ReplicantSession( mock( Session.class ) );
    session.getLock().lock();
    try
    {
      final var typeA = new ChannelAddress( 1, null );
      final var typeB = new ChannelAddress( 1, null, "fi" );
      final var instA = new ChannelAddress( 1, 5 );
      final var instB = new ChannelAddress( 1, 5, "fi2" );
      final var other = new ChannelAddress( 2, null );

      session.createSubscriptionEntry( typeA );
      session.createSubscriptionEntry( typeB );
      session.createSubscriptionEntry( instA );
      session.createSubscriptionEntry( instB );
      session.createSubscriptionEntry( other );

      assertEquals( session.findSubscriptionEntries( 1, null ).size(), 2 );
      assertEquals( session.findSubscriptionEntries( 1, 5 ).size(), 2 );
      assertEquals( session.findSubscriptionEntries( 2, null ).size(), 1 );
      assertTrue( session.findSubscriptionEntries( 9, null ).isEmpty() );
    }
    finally
    {
      session.getLock().unlock();
    }
  }

  @Test
  public void requiresLockForSubscriptionAccess()
  {
    final var session = new ReplicantSession( mock( Session.class ) );

    final var exception =
      expectThrows( IllegalStateException.class, () -> session.findSubscriptionEntry( new ChannelAddress( 1 ) ) );
    assertEquals( exception.getMessage(), "Expected session to be locked by the current thread" );
  }

  @Test
  public void deleteSubscriptionEntry_updatesIndexes()
  {
    final var session = new ReplicantSession( mock( Session.class ) );
    session.getLock().lock();
    try
    {
      final var entryA = session.createSubscriptionEntry( new ChannelAddress( 1, 5 ) );
      final var entryB = session.createSubscriptionEntry( new ChannelAddress( 1, 5, "fi" ) );

      assertEquals( session.findSubscriptionEntries( 1, 5 ).size(), 2 );

      assertTrue( session.deleteSubscriptionEntry( entryA ) );
      assertEquals( session.findSubscriptionEntries( 1, 5 ).size(), 1 );

      assertTrue( session.deleteSubscriptionEntry( entryB ) );
      assertTrue( session.findSubscriptionEntries( 1, 5 ).isEmpty() );
    }
    finally
    {
      session.getLock().unlock();
    }
  }

  @Test
  public void setETags_resetsAndRemovesNulls()
  {
    final var session = new ReplicantSession( mock( Session.class ) );
    session.getLock().lock();
    try
    {
      final var address1 = new ChannelAddress( 1, null );
      final var address2 = new ChannelAddress( 2, 5 );

      session.setETag( address1, "v1" );
      assertEquals( session.getETag( address1 ), "v1" );

      final var eTags = new HashMap<ChannelAddress, String>();
      eTags.put( address1, null );
      eTags.put( address2, "v2" );
      session.setETags( eTags );

      assertNull( session.getETag( address1 ) );
      assertEquals( session.getETag( address2 ), "v2" );
    }
    finally
    {
      session.getLock().unlock();
    }
  }

  @Test
  public void queuePacket_prioritizesSubscriptionPackets()
  {
    final var session = new ReplicantSession( mock( Session.class ) );
    final var normal = new Packet( false, null, null, null, Collections.emptyList(), new ChangeSet() );
    final var sub1 = new Packet( true, null, null, null, Collections.emptyList(), new ChangeSet() );
    final var sub2 = new Packet( true, null, null, null, Collections.emptyList(), new ChangeSet() );

    session.queuePacket( normal );
    session.queuePacket( sub1 );
    session.queuePacket( sub2 );

    assertSame( session.popPendingPacket(), sub1 );
    assertSame( session.popPendingPacket(), sub2 );
    assertSame( session.popPendingPacket(), normal );
    assertNull( session.popPendingPacket() );
  }

  @Test
  public void sendPacket_emitsChangeSet()
    throws IOException
  {
    final var webSocketSession = mock( Session.class );
    final var remote = mock( RemoteEndpoint.Basic.class );
    when( webSocketSession.isOpen() ).thenReturn( true );
    when( webSocketSession.getBasicRemote() ).thenReturn( remote );

    final var session = new ReplicantSession( webSocketSession );
    session.getLock().lock();
    try
    {
      final var message = MessageTestUtil.createMessage( 1, 2, 0, "r1", "r2", "a1", "a2" );
      final var change = new Change( message, new ChannelAddress( 5, null ) );
      final var changeSet = new ChangeSet();
      changeSet.merge( change );

      session.sendPacket( 7, null, null, changeSet );

      final var captor = org.mockito.ArgumentCaptor.forClass( String.class );
      verify( remote ).sendText( captor.capture() );

      final var payload = Json.createReader( new StringReader( captor.getValue() ) ).readObject();
      assertEquals( payload.getString( Messages.Common.TYPE ), Messages.S2C_Type.UPDATE );
      assertEquals( payload.getInt( Messages.Common.REQUEST_ID ), 7 );
      assertEquals( payload.getJsonArray( Messages.Update.CHANGES ).size(), 1 );
    }
    finally
    {
      session.getLock().unlock();
    }
  }

  @Test
  public void sendPacket_requiresLock()
  {
    final var session = new ReplicantSession( mock( Session.class ) );
    final var changeSet = new ChangeSet();

    final var exception =
      expectThrows( IllegalStateException.class, () -> session.sendPacket( null, null, null, changeSet ) );
    assertEquals( exception.getMessage(), "Expected session to be locked by the current thread" );
  }

  @Test
  public void closeDueToInterrupt_closesSession()
    throws IOException
  {
    final var webSocketSession = mock( Session.class );
    when( webSocketSession.isOpen() ).thenReturn( true );
    final var session = new ReplicantSession( webSocketSession );

    session.closeDueToInterrupt();

    final var captor = org.mockito.ArgumentCaptor.forClass( CloseReason.class );
    verify( webSocketSession ).close( captor.capture() );
    assertEquals( captor.getValue().getReasonPhrase(), "Action interrupted" );
  }

  @Test
  public void close_noopWhenAlreadyClosed()
    throws IOException
  {
    final var webSocketSession = mock( Session.class );
    when( webSocketSession.isOpen() ).thenReturn( false );
    final var session = new ReplicantSession( webSocketSession );

    session.close();

    verify( webSocketSession, never() ).close();
  }

  @Test
  public void pingTransport_sendsPingWhenOpen()
    throws IOException
  {
    final var webSocketSession = mock( Session.class );
    final var remote = mock( RemoteEndpoint.Basic.class );
    when( webSocketSession.isOpen() ).thenReturn( true );
    when( webSocketSession.getBasicRemote() ).thenReturn( remote );

    final var session = new ReplicantSession( webSocketSession );

    session.pingTransport();

    verify( remote ).sendPing( null );
  }

  @Test
  public void pingTransport_noopWhenClosed()
  {
    final var webSocketSession = mock( Session.class );
    when( webSocketSession.isOpen() ).thenReturn( false );

    final var session = new ReplicantSession( webSocketSession );

    session.pingTransport();

    verify( webSocketSession, never() ).getBasicRemote();
  }

  @Test
  public void cacheKeys()
  {
    final var session = new ReplicantSession( mock( Session.class ) );
    session.getLock().lock();
    try
    {
      final var cd1 = new ChannelAddress( 1, null );

      assertNull( session.getETag( cd1 ) );

      session.setETag( cd1, "X" );

      assertEquals( session.getETag( cd1 ), "X" );
    }
    finally
    {
      session.getLock().unlock();
    }
  }

  @Test
  public void recordSubscription_addsEntryAndAction()
  {
    final var session = new ReplicantSession( mock( Session.class ) );
    final var changeSet = new ChangeSet();
    final var address = new ChannelAddress( 1, 2 );

    session.getLock().lock();
    try
    {
      session.recordSubscription( changeSet, address, Map.of( "k", "v" ), true );

      final var entry = session.getSubscriptionEntry( address );
      assertTrue( entry.isExplicitlySubscribed() );
      assertEquals( entry.getFilter(), Map.of( "k", "v" ) );
    }
    finally
    {
      session.getLock().unlock();
    }

    assertEquals( changeSet.getChannelActions().size(), 1 );
    final var action = changeSet.getChannelActions().get( 0 );
    assertEquals( action.address(), address );
    assertEquals( action.action(), ChannelAction.Action.ADD );
    assertNotNull( action.filter() );
    assertEquals( action.filter().getString( "k" ), "v" );
  }

  @Test
  public void recordSubscription_updatesEntryAndCanPromoteExplicitSubscribe()
  {
    final var session = new ReplicantSession( mock( Session.class ) );
    final var changeSet = new ChangeSet();
    final var address = new ChannelAddress( 1, 2 );

    session.getLock().lock();
    try
    {
      final var entry = session.createSubscriptionEntry( address );
      entry.setFilter( Map.of( "old", "value" ) );
      assertFalse( entry.isExplicitlySubscribed() );

      session.recordSubscription( changeSet, address, Map.of( "k", "v" ), true );

      assertTrue( entry.isExplicitlySubscribed() );
      assertEquals( entry.getFilter(), Map.of( "k", "v" ) );
    }
    finally
    {
      session.getLock().unlock();
    }

    assertEquals( changeSet.getChannelActions().size(), 1 );
    assertEquals( changeSet.getChannelActions().get( 0 ).action(), ChannelAction.Action.UPDATE );
  }

  @Test
  public void recordSubscriptions_recordsEachAddress()
  {
    final var session = new ReplicantSession( mock( Session.class ) );
    final var changeSet = new ChangeSet();
    final var addresses = List.of( new ChannelAddress( 1, 2 ), new ChannelAddress( 1, 3 ) );

    session.getLock().lock();
    try
    {
      session.recordSubscriptions( changeSet, addresses, null, false );

      assertEquals( session.findSubscriptionEntries( 1, 2 ).size(), 1 );
      assertEquals( session.findSubscriptionEntries( 1, 3 ).size(), 1 );
    }
    finally
    {
      session.getLock().unlock();
    }

    assertEquals( changeSet.getChannelActions().size(), 2 );
    assertEquals( changeSet.getChannelActions().get( 0 ).action(), ChannelAction.Action.ADD );
    assertEquals( changeSet.getChannelActions().get( 1 ).action(), ChannelAction.Action.ADD );
  }

  @Test
  public void recordSubscription_rejectsPartialAddress()
  {
    final var session = new ReplicantSession( mock( Session.class ) );
    final var changeSet = new ChangeSet();

    session.getLock().lock();
    try
    {
      expectThrows( AssertionError.class,
                    () -> session.recordSubscription( changeSet, ChannelAddress.partial( 1, 2 ), null, false ) );
    }
    finally
    {
      session.getLock().unlock();
    }
  }

  @Test
  public void recordGraphScopedGraphLink_linksEntries()
  {
    final var session = new ReplicantSession( mock( Session.class ) );
    final var source = new ChannelAddress( 1, 2 );
    final var target = new ChannelAddress( 2 );

    session.getLock().lock();
    try
    {
      session.createSubscriptionEntry( source );
      session.createSubscriptionEntry( target );

      session.recordGraphScopedGraphLink( source, target );

      assertTrue( session.getSubscriptionEntry( source ).getOutwardSubscriptions().contains( target ) );
      assertTrue( session.getSubscriptionEntry( target ).getInwardSubscriptions().contains( source ) );
    }
    finally
    {
      session.getLock().unlock();
    }
  }

  @Test
  public void recordGraphScopedGraphLink_rejectsPartialAddress()
  {
    final var session = new ReplicantSession( mock( Session.class ) );

    session.getLock().lock();
    try
    {
      session.createSubscriptionEntry( new ChannelAddress( 1, 2, "fi" ) );
      session.createSubscriptionEntry( new ChannelAddress( 2 ) );

      expectThrows( AssertionError.class,
                    () -> session.recordGraphScopedGraphLink( ChannelAddress.partial( 1, 2 ), new ChannelAddress( 2 ) ) );
    }
    finally
    {
      session.getLock().unlock();
    }
  }

  @Test
  public void recordGraphScopedGraphLink_rejectsInstanceGraphTarget()
  {
    final var session = new ReplicantSession( mock( Session.class ) );

    session.getLock().lock();
    try
    {
      session.createSubscriptionEntry( new ChannelAddress( 1, 2 ) );
      session.createSubscriptionEntry( new ChannelAddress( 2, 3 ) );

      expectThrows( AssertionError.class,
                    () -> session.recordGraphScopedGraphLink( new ChannelAddress( 1, 2 ), new ChannelAddress( 2, 3 ) ) );
    }
    finally
    {
      session.getLock().unlock();
    }
  }

  @Test
  public void entityOwnedGraphLinks_requireLastOwnerBeforeDelink()
  {
    final var session = new ReplicantSession( mock( Session.class ) );
    final var changeSet = new ChangeSet();
    final var source = new ChannelAddress( 1, 2 );
    final var target = new ChannelAddress( 2, 3 );

    session.getLock().lock();
    try
    {
      final var sourceEntry = session.createSubscriptionEntry( source );
      final var targetEntry = session.createSubscriptionEntry( target );
      targetEntry.setExplicitlySubscribed( true );

      session.recordGraphLink( sourceEntry, targetEntry, LinkOwner.entity( 7, 11 ) );
      session.recordGraphLink( sourceEntry, targetEntry, LinkOwner.entity( 7, 12 ) );

      assertTrue( sourceEntry.getOutwardSubscriptions().contains( target ) );
      assertEquals( sourceEntry.getOwnedOutwardSubscriptions( LinkOwner.entity( 7, 11 ) ), Set.of( target ) );
      assertEquals( sourceEntry.getOwnedOutwardSubscriptions( LinkOwner.entity( 7, 12 ) ), Set.of( target ) );

      session.delinkDownstreamSubscription( sourceEntry, LinkOwner.entity( 7, 11 ), target, changeSet );

      assertTrue( sourceEntry.getOutwardSubscriptions().contains( target ) );
      assertTrue( targetEntry.getInwardSubscriptions().contains( source ) );
      assertNotNull( session.findSubscriptionEntry( target ) );

      session.delinkDownstreamSubscription( sourceEntry, LinkOwner.entity( 7, 12 ), target, changeSet );

      assertTrue( sourceEntry.getOutwardSubscriptions().isEmpty() );
      assertTrue( targetEntry.getInwardSubscriptions().isEmpty() );
      assertNotNull( session.findSubscriptionEntry( target ) );
    }
    finally
    {
      session.getLock().unlock();
    }

    assertTrue( changeSet.getChannelActions().isEmpty() );
  }

  @Test
  public void getFilterAndSetFilter_roundTrip()
  {
    final var session = new ReplicantSession( mock( Session.class ) );
    final var address = new ChannelAddress( 1, 2 );

    session.getLock().lock();
    try
    {
      session.createSubscriptionEntry( address );
      session.setFilter( address, Map.of( "k", "v" ) );
      assertEquals( session.getFilter( address ), Map.of( "k", "v" ) );

      session.setFilter( address, null );
      assertNull( session.getFilter( address ) );
    }
    finally
    {
      session.getLock().unlock();
    }
  }

  @Test
  public void unsubscribe_removesExistingAndIgnoresMissing()
  {
    final var session = new ReplicantSession( mock( Session.class ) );
    final var changeSet = new ChangeSet();
    final var address = new ChannelAddress( 1, 2 );

    session.getLock().lock();
    try
    {
      final var entry = session.createSubscriptionEntry( address );
      entry.setExplicitlySubscribed( true );

      session.bulkUnsubscribe( Collections.singletonList( address ), changeSet );
      assertNull( session.findSubscriptionEntry( address ) );

      session.bulkUnsubscribe( Collections.singletonList( address ), changeSet );
    }
    finally
    {
      session.getLock().unlock();
    }

    assertEquals( changeSet.getChannelActions().size(), 1 );
    assertEquals( changeSet.getChannelActions().get( 0 ).action(), ChannelAction.Action.REMOVE );
  }

  @Test
  public void bulkUnsubscribe_removesEachSubscribedAddress()
  {
    final var session = new ReplicantSession( mock( Session.class ) );
    final var changeSet = new ChangeSet();
    final var address1 = new ChannelAddress( 1, 1 );
    final var address2 = new ChannelAddress( 1, 2 );

    session.getLock().lock();
    try
    {
      final var entry1 = session.createSubscriptionEntry( address1 );
      final var entry2 = session.createSubscriptionEntry( address2 );
      entry1.setExplicitlySubscribed( true );
      entry2.setExplicitlySubscribed( true );

      session.bulkUnsubscribe( List.of( address1, address2, new ChannelAddress( 1, 3 ) ), changeSet );

      assertNull( session.findSubscriptionEntry( address1 ) );
      assertNull( session.findSubscriptionEntry( address2 ) );
    }
    finally
    {
      session.getLock().unlock();
    }

    assertEquals( changeSet.getChannelActions().size(), 2 );
    assertEquals( changeSet.getChannelActions().get( 0 ).action(), ChannelAction.Action.REMOVE );
    assertEquals( changeSet.getChannelActions().get( 1 ).action(), ChannelAction.Action.REMOVE );
  }

  @Test
  public void bulkUnsubscribe_rejectsPartialAddress()
  {
    final var session = new ReplicantSession( mock( Session.class ) );
    final var changeSet = new ChangeSet();

    session.getLock().lock();
    try
    {
      expectThrows( AssertionError.class,
                    () -> session.bulkUnsubscribe( Collections.singletonList( ChannelAddress.partial( 1, 2 ) ),
                                                   changeSet ) );
    }
    finally
    {
      session.getLock().unlock();
    }
  }

  @Test
  public void performUnsubscribe_onlyRemovesWhenEntryCanUnsubscribe()
  {
    final var session = new ReplicantSession( mock( Session.class ) );
    final var changeSet = new ChangeSet();
    final var address = new ChannelAddress( 1, 2 );

    session.getLock().lock();
    try
    {
      final var entry = session.createSubscriptionEntry( address );
      entry.setExplicitlySubscribed( true );

      session.performUnsubscribe( entry, false, false, changeSet );
      assertNotNull( session.findSubscriptionEntry( address ) );

      session.performUnsubscribe( entry, true, false, changeSet );
      assertNull( session.findSubscriptionEntry( address ) );
    }
    finally
    {
      session.getLock().unlock();
    }

    assertEquals( changeSet.getChannelActions().size(), 1 );
    assertEquals( changeSet.getChannelActions().get( 0 ).action(), ChannelAction.Action.REMOVE );
  }

  @Test
  public void performUnsubscribe_deleteUsesDeleteAction()
  {
    final var session = new ReplicantSession( mock( Session.class ) );
    final var changeSet = new ChangeSet();
    final var address = new ChannelAddress( 1, 2 );

    session.getLock().lock();
    try
    {
      final var entry = session.createSubscriptionEntry( address );
      entry.setExplicitlySubscribed( true );

      session.performUnsubscribe( entry, true, true, changeSet );
      assertNull( session.findSubscriptionEntry( address ) );
    }
    finally
    {
      session.getLock().unlock();
    }

    assertEquals( changeSet.getChannelActions().size(), 1 );
    assertEquals( changeSet.getChannelActions().get( 0 ).action(), ChannelAction.Action.DELETE );
  }

  @Test
  public void performUnsubscribe_cascadesDownstreamSubscriptions()
  {
    final var session = new ReplicantSession( mock( Session.class ) );
    final var changeSet = new ChangeSet();
    final var a = new ChannelAddress( 1, 1 );
    final var b = new ChannelAddress( 2 );
    final var c = new ChannelAddress( 3 );

    session.getLock().lock();
    try
    {
      final var entryA = session.createSubscriptionEntry( a );
      session.createSubscriptionEntry( b );
      session.createSubscriptionEntry( c );
      session.recordGraphScopedGraphLink( a, b );
      session.recordGraphScopedGraphLink( b, c );

      session.performUnsubscribe( entryA, false, false, changeSet );

      assertNull( session.findSubscriptionEntry( a ) );
      assertNull( session.findSubscriptionEntry( b ) );
      assertNull( session.findSubscriptionEntry( c ) );
    }
    finally
    {
      session.getLock().unlock();
    }

    assertEquals( changeSet.getChannelActions().size(), 3 );
    final var actions =
      changeSet.getChannelActions().stream().map( ChannelAction::address ).collect( java.util.stream.Collectors.toSet() );
    assertEquals( actions, Set.of( a, b, c ) );
  }

  @Test
  public void delinkDownstreamSubscription_keepsExplicitDownstream()
  {
    final var session = new ReplicantSession( mock( Session.class ) );
    final var changeSet = new ChangeSet();
    final var upstream = new ChannelAddress( 1, 1 );
    final var downstream = new ChannelAddress( 2 );

    session.getLock().lock();
    try
    {
      session.createSubscriptionEntry( upstream );
      final var downstreamEntry = session.createSubscriptionEntry( downstream );
      downstreamEntry.setExplicitlySubscribed( true );
      session.recordGraphScopedGraphLink( upstream, downstream );

      session.delinkDownstreamSubscription( upstream, downstream, changeSet );

      assertTrue( session.getSubscriptionEntry( upstream ).getOutwardSubscriptions().isEmpty() );
      assertTrue( session.getSubscriptionEntry( downstream ).getInwardSubscriptions().isEmpty() );
      assertNotNull( session.findSubscriptionEntry( downstream ) );
    }
    finally
    {
      session.getLock().unlock();
    }

    assertTrue( changeSet.getChannelActions().isEmpty() );
  }

  @Test
  public void delinkDownstreamSubscription_rejectsPartialAddress()
  {
    final var session = new ReplicantSession( mock( Session.class ) );
    final var changeSet = new ChangeSet();

    session.getLock().lock();
    try
    {
      session.createSubscriptionEntry( new ChannelAddress( 1, 1, "fi" ) );
      session.createSubscriptionEntry( new ChannelAddress( 1, 2 ) );

      expectThrows( AssertionError.class,
                    () -> session.delinkDownstreamSubscription( ChannelAddress.partial( 1, 1 ),
                                                                new ChannelAddress( 1, 2 ),
                                                                changeSet ) );
    }
    finally
    {
      session.getLock().unlock();
    }
  }

  @SuppressWarnings( "DataFlowIssue" )
  @Nonnull
  private Map<ChannelAddress, SubscriptionEntry> getSubscriptions( final ReplicantSession session )
  {
    return getField( session, "_subscriptions" );
  }

  @SuppressWarnings( { "SameParameterValue", "unchecked" } )
  @Nullable
  private <T> T getField( @Nonnull final ReplicantSession session, @Nonnull final String fieldName )
  {
    try
    {
      final var field = ReplicantSession.class.getDeclaredField( fieldName );
      field.setAccessible( true );
      return (T) field.get( session );
    }
    catch ( final Throwable t )
    {
      throw new AssertionError( t );
    }
  }
}
