package replicant.server.transport;

import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.json.Json;
import javax.json.JsonObject;
import javax.websocket.CloseReason;
import javax.websocket.RemoteEndpoint;
import javax.websocket.Session;
import org.realityforge.guiceyloops.shared.ValueUtil;
import org.testng.annotations.Test;
import replicant.server.Change;
import replicant.server.ChangeSet;
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
    final Session webSocketSession = mock( Session.class );
    final String sessionId = ValueUtil.randomString();
    when( webSocketSession.getId() ).thenReturn( sessionId );
    final ReplicantSession session = new ReplicantSession( webSocketSession );
    session.getLock().lock();

    assertEquals( session.getId(), sessionId );

    assertEquals( getSubscriptions( session ).size(), 0 );

    final ChannelAddress cd1 = new ChannelAddress( 1, null );

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

    final SubscriptionEntry entry = session.createSubscriptionEntry( cd1 );

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
    final ReplicantSession session = new ReplicantSession( mock( Session.class ) );
    session.getLock().lock();
    try
    {
      final ChannelAddress typeA = new ChannelAddress( 1, null );
      final ChannelAddress typeB = new ChannelAddress( 1, null, "fi" );
      final ChannelAddress instA = new ChannelAddress( 1, 5 );
      final ChannelAddress instB = new ChannelAddress( 1, 5, "fi2" );
      final ChannelAddress other = new ChannelAddress( 2, null );

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
  public void deleteSubscriptionEntry_updatesIndexes()
  {
    final ReplicantSession session = new ReplicantSession( mock( Session.class ) );
    session.getLock().lock();
    try
    {
      final SubscriptionEntry entryA = session.createSubscriptionEntry( new ChannelAddress( 1, 5 ) );
      final SubscriptionEntry entryB = session.createSubscriptionEntry( new ChannelAddress( 1, 5, "fi" ) );

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
    final ReplicantSession session = new ReplicantSession( mock( Session.class ) );
    session.getLock().lock();
    try
    {
      final ChannelAddress address1 = new ChannelAddress( 1, null );
      final ChannelAddress address2 = new ChannelAddress( 2, 5 );

      session.setETag( address1, "v1" );
      assertEquals( session.getETag( address1 ), "v1" );

      final Map<ChannelAddress, String> eTags = new HashMap<>();
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
    final ReplicantSession session = new ReplicantSession( mock( Session.class ) );
    final Packet normal = new Packet( false, null, null, null, Collections.emptyList(), new ChangeSet() );
    final Packet sub1 = new Packet( true, null, null, null, Collections.emptyList(), new ChangeSet() );
    final Packet sub2 = new Packet( true, null, null, null, Collections.emptyList(), new ChangeSet() );

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
    final Session webSocketSession = mock( Session.class );
    final RemoteEndpoint.Basic remote = mock( RemoteEndpoint.Basic.class );
    when( webSocketSession.isOpen() ).thenReturn( true );
    when( webSocketSession.getBasicRemote() ).thenReturn( remote );

    final ReplicantSession session = new ReplicantSession( webSocketSession );
    session.getLock().lock();
    try
    {
      final EntityMessage message = MessageTestUtil.createMessage( 1, 2, 0, "r1", "r2", "a1", "a2" );
      final Change change = new Change( message, new ChannelAddress( 5, null ) );
      final ChangeSet changeSet = new ChangeSet();
      changeSet.merge( change );

      session.sendPacket( 7, null, null, changeSet );

      final var captor = org.mockito.ArgumentCaptor.forClass( String.class );
      verify( remote ).sendText( captor.capture() );

      final JsonObject payload = Json.createReader( new StringReader( captor.getValue() ) ).readObject();
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
    final ReplicantSession session = new ReplicantSession( mock( Session.class ) );
    final ChangeSet changeSet = new ChangeSet();

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class, () -> session.sendPacket( null, null, null, changeSet ) );
    assertEquals( exception.getMessage(), "Expected session to be locked by the current thread" );
  }

  @Test
  public void closeDueToInterrupt_closesSession()
    throws IOException
  {
    final Session webSocketSession = mock( Session.class );
    when( webSocketSession.isOpen() ).thenReturn( true );
    final ReplicantSession session = new ReplicantSession( webSocketSession );

    session.closeDueToInterrupt();

    final var captor = org.mockito.ArgumentCaptor.forClass( CloseReason.class );
    verify( webSocketSession ).close( captor.capture() );
    assertEquals( captor.getValue().getReasonPhrase(), "Action interrupted" );
  }

  @Test
  public void close_noopWhenAlreadyClosed()
    throws IOException
  {
    final Session webSocketSession = mock( Session.class );
    when( webSocketSession.isOpen() ).thenReturn( false );
    final ReplicantSession session = new ReplicantSession( webSocketSession );

    session.close();

    verify( webSocketSession, never() ).close();
  }

  @Test
  public void pingTransport_sendsPingWhenOpen()
    throws IOException
  {
    final Session webSocketSession = mock( Session.class );
    final RemoteEndpoint.Basic remote = mock( RemoteEndpoint.Basic.class );
    when( webSocketSession.isOpen() ).thenReturn( true );
    when( webSocketSession.getBasicRemote() ).thenReturn( remote );

    final ReplicantSession session = new ReplicantSession( webSocketSession );

    session.pingTransport();

    verify( remote ).sendPing( null );
  }

  @Test
  public void pingTransport_noopWhenClosed()
  {
    final Session webSocketSession = mock( Session.class );
    when( webSocketSession.isOpen() ).thenReturn( false );

    final ReplicantSession session = new ReplicantSession( webSocketSession );

    session.pingTransport();

    verify( webSocketSession, never() ).getBasicRemote();
  }

  @Test
  public void cacheKeys()
  {
    final ReplicantSession session = new ReplicantSession( mock( Session.class ) );
    session.getLock().lock();
    try
    {
      final ChannelAddress cd1 = new ChannelAddress( 1, null );

      assertNull( session.getETag( cd1 ) );

      session.setETag( cd1, "X" );

      assertEquals( session.getETag( cd1 ), "X" );
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
      final Field field = ReplicantSession.class.getDeclaredField( fieldName );
      field.setAccessible( true );
      return (T) field.get( session );
    }
    catch ( final Throwable t )
    {
      throw new AssertionError( t );
    }
  }
}
