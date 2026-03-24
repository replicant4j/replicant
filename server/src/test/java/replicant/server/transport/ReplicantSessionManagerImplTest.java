package replicant.server.transport;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.json.JsonObject;
import javax.websocket.RemoteEndpoint;
import javax.websocket.Session;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import replicant.server.ChangeSet;
import replicant.server.ChannelAction;
import replicant.server.ChannelAddress;
import replicant.server.ChannelLink;
import replicant.server.EntityMessage;
import replicant.server.ServerConstants;
import replicant.server.ee.EntityMessageCacheUtil;
import replicant.server.ee.RegistryUtil;
import replicant.server.ee.TransactionSynchronizationRegistryUtil;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class ReplicantSessionManagerImplTest
{
  @BeforeMethod
  public void setup()
  {
    RegistryUtil.bind();
  }

  @AfterMethod
  public void clearContext()
  {
    RegistryUtil.unbind();
  }

  @Test
  public void sendChangeMessage_staticInstancedLinkFollow_usesTargetInstanceId()
  {
    final var sourceChannel =
      new ChannelMetaData( 0,
                           "Source",
                           null,
                           ChannelMetaData.FilterType.NONE,
                           null,
                           ChannelMetaData.CacheType.NONE,
                           true );
    final var targetChannel =
      new ChannelMetaData( 1,
                           "Target",
                           1,
                           ChannelMetaData.FilterType.STATIC_INSTANCED,
                           json -> json,
                           ChannelMetaData.CacheType.NONE,
                           true );
    final var schema = new SchemaMetaData( "Test", sourceChannel, targetChannel );

    final var context = new TestSessionContext( schema );
    final var manager = new ReplicantSessionManagerImpl();
    setField( manager, "_context", context );
    setField( manager, "_broker", mock( ReplicantMessageBroker.class ) );
    setField( manager, "_registry", TransactionSynchronizationRegistryUtil.lookup() );

    final Session webSocketSession = mock( Session.class );
    final RemoteEndpoint.Basic remote = mock( RemoteEndpoint.Basic.class );
    when( webSocketSession.getId() ).thenReturn( "session-1" );
    when( webSocketSession.isOpen() ).thenReturn( true );
    when( webSocketSession.getBasicRemote() ).thenReturn( remote );

    final var session = new ReplicantSession( webSocketSession );

    final var sourceAddress = new ChannelAddress( 0 );
    final var targetAddress = new ChannelAddress( 1, 7, "fi-7" );
    final var link = new ChannelLink( sourceAddress, targetAddress, null, true );

    final var routingKeys = new HashMap<String, Serializable>();
    routingKeys.put( "Source", "present" );
    final var attributes = new HashMap<String, Serializable>();
    attributes.put( "ID", 1 );
    final var message = new EntityMessage( 1, 1, 0L, routingKeys, attributes, Set.of( link ) );

    final var packet = new Packet( false, null, null, null, List.of( message ), new ChangeSet() );

    session.getLock().lock();
    try
    {
      final var sourceEntry = session.createSubscriptionEntry( sourceAddress );
      sourceEntry.setFilter( "source-filter" );

      manager.sendChangeMessage( session, packet );

      final var targetEntry = session.findSubscriptionEntry( targetAddress );
      assertNotNull( targetEntry );
      assertEquals( targetEntry.getFilter(), Map.of( "k", "v" ) );
      assertTrue( sourceEntry.getOutwardSubscriptions().contains( targetAddress ) );
      assertTrue( targetEntry.getInwardSubscriptions().contains( sourceAddress ) );
    }
    finally
    {
      session.getLock().unlock();
    }

    final var collectCalls = context.getBulkCollectCalls();
    assertEquals( collectCalls.size(), 1 );
    final var call = collectCalls.get( 0 );
    assertEquals( call.addresses(), List.of( targetAddress ) );
    assertEquals( call.filter(), Map.of( "k", "v" ) );
    assertFalse( call.isExplicitSubscribe() );
  }

  @Test
  public void sendChangeMessage_deleteRemovesOnlyDeletedEntityOwnershipForSharedTarget()
  {
    final var sourceChannel =
      new ChannelMetaData( 0,
                           "Source",
                           1,
                           ChannelMetaData.FilterType.NONE,
                           null,
                           ChannelMetaData.CacheType.NONE,
                           true );
    final var targetChannel =
      new ChannelMetaData( 1,
                           "Target",
                           null,
                           ChannelMetaData.FilterType.NONE,
                           null,
                           ChannelMetaData.CacheType.NONE,
                           true );
    final var schema = new SchemaMetaData( "Test", sourceChannel, targetChannel );
    final var context = createManagerContext( schema );
    final var manager = createManager( context, mock( ReplicantMessageBroker.class ) );

    final Session webSocketSession = mock( Session.class );
    final RemoteEndpoint.Basic remote = mock( RemoteEndpoint.Basic.class );
    when( webSocketSession.getId() ).thenReturn( "session-1" );
    when( webSocketSession.isOpen() ).thenReturn( true );
    when( webSocketSession.getBasicRemote() ).thenReturn( remote );
    final var session = new ReplicantSession( webSocketSession );

    final var sourceAddress = new ChannelAddress( 0, 10 );
    final var targetAddress = new ChannelAddress( 1 );
    final var link = new ChannelLink( sourceAddress, targetAddress, null, false );
    final var routingKeys = new HashMap<String, Serializable>();
    routingKeys.put( "Source", new ArrayList<>( List.of( 10 ) ) );
    final var attributesA = new HashMap<String, Serializable>();
    final var attributesB = new HashMap<String, Serializable>();
    attributesA.put( "ID", 100 );
    attributesB.put( "ID", 101 );

    final var updateA = new EntityMessage( 100, 2, 0L, routingKeys, attributesA, Set.of( link ) );
    final var updateB = new EntityMessage( 101, 2, 0L, routingKeys, attributesB, Set.of( link ) );
    final var deleteA = new EntityMessage( 100, 2, 1L, routingKeys, null, null );

    session.getLock().lock();
    try
    {
      final var sourceEntry = session.createSubscriptionEntry( sourceAddress );
      sourceEntry.setExplicitlySubscribed( true );

      manager.sendChangeMessage( session,
                                 new Packet( false, null, null, null, List.of( updateA, updateB ), new ChangeSet() ) );

      assertNotNull( session.findSubscriptionEntry( targetAddress ) );
      assertEquals( sourceEntry.getOwnedOutwardSubscriptions( LinkOwner.entity( 2, 100 ) ), Set.of( targetAddress ) );
      assertEquals( sourceEntry.getOwnedOutwardSubscriptions( LinkOwner.entity( 2, 101 ) ), Set.of( targetAddress ) );

      manager.sendChangeMessage( session, new Packet( false, null, null, null, List.of( deleteA ), new ChangeSet() ) );

      final var targetEntry = session.findSubscriptionEntry( targetAddress );
      assertNotNull( targetEntry );
      assertTrue( sourceEntry.getOutwardSubscriptions().contains( targetAddress ) );
      assertTrue( targetEntry.getInwardSubscriptions().contains( sourceAddress ) );
      assertTrue( sourceEntry.getOwnedOutwardSubscriptions( LinkOwner.entity( 2, 100 ) ).isEmpty() );
      assertEquals( sourceEntry.getOwnedOutwardSubscriptions( LinkOwner.entity( 2, 101 ) ), Set.of( targetAddress ) );
    }
    finally
    {
      session.getLock().unlock();
    }
  }

  @Test
  public void invalidateSession_removesAndClosesExistingSession()
    throws Exception
  {
    final var schema =
      new SchemaMetaData( "Test",
                          new ChannelMetaData( 0,
                                               "Source",
                                               null,
                                               ChannelMetaData.FilterType.NONE,
                                               null,
                                               ChannelMetaData.CacheType.NONE,
                                               true ) );
    final var context = new TestSessionContext( schema );
    final var manager = createManager( context, mock( ReplicantMessageBroker.class ) );

    final Session webSocketSession = mock( Session.class );
    when( webSocketSession.getId() ).thenReturn( "session-1" );
    when( webSocketSession.isOpen() ).thenReturn( true );

    final var session = manager.createSession( webSocketSession );
    assertNotNull( manager.getSession( "session-1" ) );

    manager.invalidateSession( session );

    assertNull( manager.getSession( "session-1" ) );
    verify( webSocketSession ).close();
  }

  @Test
  public void invalidateSession_ignoresUnknownSession()
    throws Exception
  {
    final var schema =
      new SchemaMetaData( "Test",
                          new ChannelMetaData( 0,
                                               "Source",
                                               null,
                                               ChannelMetaData.FilterType.NONE,
                                               null,
                                               ChannelMetaData.CacheType.NONE,
                                               true ) );
    final var context = new TestSessionContext( schema );
    final var manager = createManager( context, mock( ReplicantMessageBroker.class ) );

    final Session webSocketSession = mock( Session.class );
    when( webSocketSession.getId() ).thenReturn( "unknown-session" );

    final var session = new ReplicantSession( webSocketSession );
    manager.invalidateSession( session );

    verify( webSocketSession, never() ).close();
  }

  @Test
  public void unsubscribe_removesSubscriptionsViaSessionLogic()
  {
    final var channel =
      new ChannelMetaData( 0,
                           "Channel",
                           1,
                           ChannelMetaData.FilterType.NONE,
                           null,
                           ChannelMetaData.CacheType.NONE,
                           true );
    final var schema = new SchemaMetaData( "Test", channel );
    final var context = new TestSessionContext( schema );
    final var manager = createManager( context, mock( ReplicantMessageBroker.class ) );

    final Session webSocketSession = mock( Session.class );
    when( webSocketSession.getId() ).thenReturn( "session-1" );
    when( webSocketSession.isOpen() ).thenReturn( true );
    final var session = new ReplicantSession( webSocketSession );

    final var address1 = new ChannelAddress( 0, 1 );
    final var address2 = new ChannelAddress( 0, 2 );
    final var address3 = new ChannelAddress( 0, 3 );

    session.getLock().lock();
    try
    {
      final var entry1 = session.createSubscriptionEntry( address1 );
      final var entry2 = session.createSubscriptionEntry( address2 );
      entry1.setExplicitlySubscribed( true );
      entry2.setExplicitlySubscribed( true );
    }
    finally
    {
      session.getLock().unlock();
    }

    final var registry = TransactionSynchronizationRegistryUtil.lookup();
    registry.putResource( ServerConstants.REPLICATION_INVOCATION_KEY, null );

    manager.unsubscribe( session, 99, List.of( address1, address2, address3 ) );

    session.getLock().lock();
    try
    {
      assertNull( session.findSubscriptionEntry( address1 ) );
      assertNull( session.findSubscriptionEntry( address2 ) );
    }
    finally
    {
      session.getLock().unlock();
    }

    final var sessionChanges = EntityMessageCacheUtil.lookupSessionChanges();
    assertNotNull( sessionChanges );
    assertEquals( sessionChanges.getChannelActions().size(), 2 );
    assertEquals( sessionChanges.getChannelActions().get( 0 ).action(), ChannelAction.Action.REMOVE );
    assertEquals( sessionChanges.getChannelActions().get( 1 ).action(), ChannelAction.Action.REMOVE );
  }

  @Test
  public void sendChangeMessage_deleteRootUnsubscribesRootAndDownstream()
  {
    final var sourceChannel =
      new ChannelMetaData( 0,
                           "Source",
                           1,
                           ChannelMetaData.FilterType.NONE,
                           null,
                           ChannelMetaData.CacheType.NONE,
                           true );
    final var targetChannel =
      new ChannelMetaData( 1,
                           "Target",
                           null,
                           ChannelMetaData.FilterType.NONE,
                           null,
                           ChannelMetaData.CacheType.NONE,
                           true );
    final var schema = new SchemaMetaData( "Test", sourceChannel, targetChannel );
    final var context = new TestSessionContext( schema );
    final var manager = createManager( context, mock( ReplicantMessageBroker.class ) );

    final Session webSocketSession = mock( Session.class );
    final RemoteEndpoint.Basic remote = mock( RemoteEndpoint.Basic.class );
    when( webSocketSession.getId() ).thenReturn( "session-1" );
    when( webSocketSession.isOpen() ).thenReturn( true );
    when( webSocketSession.getBasicRemote() ).thenReturn( remote );
    final var session = new ReplicantSession( webSocketSession );

    final var sourceAddress = new ChannelAddress( 0, 10 );
    final var targetAddress = new ChannelAddress( 1 );

    final var routingKeys = new HashMap<String, Serializable>();
    routingKeys.put( "Source", new ArrayList<>( List.of( 10 ) ) );
    final var deleteMessage = new EntityMessage( 10,
                                                 1,
                                                 0,
                                                 routingKeys,
                                                 null,
                                                 null );
    final var changeSet = new ChangeSet();
    final var packet = new Packet( false, null, null, null, List.of( deleteMessage ), changeSet );

    session.getLock().lock();
    try
    {
      final var sourceEntry = session.createSubscriptionEntry( sourceAddress );
      sourceEntry.setExplicitlySubscribed( true );
      session.createSubscriptionEntry( targetAddress );
      session.recordGraphScopedGraphLink( sourceAddress, targetAddress );

      manager.sendChangeMessage( session, packet );

      assertNull( session.findSubscriptionEntry( sourceAddress ) );
      assertNull( session.findSubscriptionEntry( targetAddress ) );
    }
    finally
    {
      session.getLock().unlock();
    }

    assertEquals( changeSet.getChannelActions().size(), 2 );
    final var actionByAddress =
      changeSet
        .getChannelActions()
        .stream()
        .collect( java.util.stream.Collectors.toMap( ChannelAction::address, ChannelAction::action ) );
    assertEquals( actionByAddress.get( sourceAddress ), ChannelAction.Action.DELETE );
    assertEquals( actionByAddress.get( targetAddress ), ChannelAction.Action.REMOVE );
  }

  @Test
  public void sendChangeMessage_deleteWithInstancedSubscriptions_unsubscribesConcreteTargetsWithoutMessageLinks()
  {
    final var sourceChannel =
      new ChannelMetaData( 0,
                           "Source",
                           1,
                           ChannelMetaData.FilterType.STATIC_INSTANCED,
                           json -> json,
                           ChannelMetaData.CacheType.NONE,
                           true );
    final var targetChannel =
      new ChannelMetaData( 1,
                           "Target",
                           null,
                           ChannelMetaData.FilterType.STATIC_INSTANCED,
                           json -> json,
                           ChannelMetaData.CacheType.NONE,
                           true );
    final var schema = new SchemaMetaData( "Test", sourceChannel, targetChannel );
    final var context = new TestSessionContext( schema );
    final var manager = createManager( context, mock( ReplicantMessageBroker.class ) );

    final Session webSocketSession = mock( Session.class );
    final RemoteEndpoint.Basic remote = mock( RemoteEndpoint.Basic.class );
    when( webSocketSession.getId() ).thenReturn( "session-1" );
    when( webSocketSession.isOpen() ).thenReturn( true );
    when( webSocketSession.getBasicRemote() ).thenReturn( remote );
    final var session = new ReplicantSession( webSocketSession );

    final var sourceAddressA = new ChannelAddress( 0, 10, "fi-a" );
    final var sourceAddressB = new ChannelAddress( 0, 10, "fi-b" );
    final var targetAddressA = new ChannelAddress( 1, null, "fi-a" );
    final var targetAddressB = new ChannelAddress( 1, null, "fi-b" );
    final var routingKeys = new HashMap<String, Serializable>();
    routingKeys.put( "Source", new ArrayList<>( List.of( 10 ) ) );
    final var deleteMessage = new EntityMessage( 10,
                                                 1,
                                                 0,
                                                 routingKeys,
                                                 null,
                                                 null );
    final var changeSet = new ChangeSet();
    final var packet = new Packet( false, null, null, null, List.of( deleteMessage ), changeSet );

    session.getLock().lock();
    try
    {
      final var sourceEntryA = session.createSubscriptionEntry( sourceAddressA );
      final var sourceEntryB = session.createSubscriptionEntry( sourceAddressB );
      sourceEntryA.setExplicitlySubscribed( true );
      sourceEntryB.setExplicitlySubscribed( true );
      session.createSubscriptionEntry( targetAddressA );
      session.createSubscriptionEntry( targetAddressB );
      session.recordGraphScopedGraphLink( sourceAddressA, targetAddressA );
      session.recordGraphScopedGraphLink( sourceAddressB, targetAddressB );

      manager.sendChangeMessage( session, packet );

      assertNull( session.findSubscriptionEntry( sourceAddressA ) );
      assertNull( session.findSubscriptionEntry( sourceAddressB ) );
      assertNull( session.findSubscriptionEntry( targetAddressA ) );
      assertNull( session.findSubscriptionEntry( targetAddressB ) );
    }
    finally
    {
      session.getLock().unlock();
    }
  }

  @Test
  public void tryGetCacheEntry_rejectsPartialAddresses()
    throws Exception
  {
    final var channel =
      new ChannelMetaData( 0,
                           "Source",
                           null,
                           ChannelMetaData.FilterType.NONE,
                           null,
                           ChannelMetaData.CacheType.INTERNAL,
                           true );
    final var schema = new SchemaMetaData( "Test", channel );
    final var context = new TestSessionContext( schema );
    final var manager = createManager( context, mock( ReplicantMessageBroker.class ) );

    final Method method =
      ReplicantSessionManagerImpl.class.getDeclaredMethod( "tryGetCacheEntry", ChannelAddress.class );
    method.setAccessible( true );

    final var exception =
      expectThrows( InvocationTargetException.class,
                    () -> method.invoke( manager, ChannelAddress.partial( 0 ) ) );
    assertTrue( exception.getCause() instanceof AssertionError );
  }

  @Nonnull
  private ReplicantSessionManagerImpl createManager( @Nonnull final TestSessionContext context,
                                                     @Nonnull final ReplicantMessageBroker broker )
  {
    final var manager = new ReplicantSessionManagerImpl();
    setField( manager, "_context", context );
    setField( manager, "_broker", broker );
    setField( manager, "_registry", TransactionSynchronizationRegistryUtil.lookup() );
    return manager;
  }

  @Nonnull
  private TestSessionContext createManagerContext( @Nonnull final SchemaMetaData schema )
  {
    return new TestSessionContext( schema );
  }

  private void setField( @Nonnull final Object target, @Nonnull final String name, @Nullable final Object value )
  {
    try
    {
      final Field field = ReplicantSessionManagerImpl.class.getDeclaredField( name );
      field.setAccessible( true );
      field.set( target, value );
    }
    catch ( final Exception e )
    {
      throw new AssertionError( e );
    }
  }

  private static final class TestSessionContext
    implements ReplicantSessionContext
  {
    @Nonnull
    private final SchemaMetaData _schema;
    @Nonnull
    private final List<BulkCollectCall> _bulkCollectCalls = new ArrayList<>();
    @Nonnull
    private final List<DeriveTargetFilterInstanceIdCall> _deriveTargetFilterInstanceIdCalls = new ArrayList<>();

    private TestSessionContext( @Nonnull final SchemaMetaData schema )
    {
      _schema = schema;
    }

    @Nonnull
    @Override
    public SchemaMetaData getSchemaMetaData()
    {
      return _schema;
    }

    @Override
    public boolean isAuthorized( @Nonnull final ReplicantSession session )
    {
      return true;
    }

    @Override
    public void preSubscribe( @Nonnull final ReplicantSession session,
                              @Nonnull final ChannelAddress address,
                              @Nullable final Object filter )
    {
    }

    @Nonnull
    @Override
    public Object deriveTargetFilter( @Nonnull final EntityMessage entityMessage,
                                      @Nonnull final ChannelAddress source,
                                      @Nullable final Object sourceFilter,
                                      @Nonnull final ChannelAddress target )
    {
      return Map.of( "k", "v" );
    }

    @Nonnull
    @Override
    public String deriveTargetFilterInstanceId( @Nonnull final EntityMessage entityMessage,
                                                @Nonnull final ChannelAddress source,
                                                @Nullable final Object sourceFilter,
                                                @Nonnull final ChannelAddress target,
                                                @Nullable final Object targetFilter )
    {
      final var sourceInstanceId = source.filterInstanceId();
      return null == sourceInstanceId ? "fi-7" : sourceInstanceId;
    }

    @Override
    public boolean flushOpenEntityManager()
    {
      return false;
    }

    @Override
    public void execCommand( @Nonnull final ReplicantSession session,
                             @Nonnull final String command,
                             final int requestId,
                             @Nullable final JsonObject payload )
    {
    }

    @Override
    public void collectChannelData( @Nullable final ReplicantSession session,
                                    @Nonnull final List<ChannelAddress> addresses,
                                    @Nullable final Object filter,
                                    @Nonnull final ChangeSet changeSet,
                                    final boolean isExplicitSubscribe )
    {
      _bulkCollectCalls.add( new BulkCollectCall( addresses, filter, isExplicitSubscribe ) );
      if ( null != session )
      {
        for ( final var address : addresses )
        {
          final var existing = session.findSubscriptionEntry( address );
          final var entry = null == existing ? session.createSubscriptionEntry( address ) : existing;
          entry.setFilter( filter );
          changeSet.mergeAction( address,
                                 null == existing ? ChannelAction.Action.ADD : ChannelAction.Action.UPDATE,
                                 filter );
        }
      }
    }

    @Override
    public void collectChannelDataForFilterChange( @Nonnull final ReplicantSession session,
                                                   @Nonnull final List<ChannelAddress> addresses,
                                                   @Nullable final Object originalFilter,
                                                   @Nullable final Object newFilter,
                                                   @Nonnull final ChangeSet changeSet )
    {
    }

    @Nonnull
    @Override
    public EntityMessage filterEntityMessage( @Nonnull final ReplicantSession session,
                                              @Nonnull final ChannelAddress address,
                                              @Nonnull final EntityMessage message )
    {
      return message;
    }

    @Override
    public boolean shouldFollowLink( @Nonnull final ChannelAddress source,
                                     final Object sourceFilter,
                                     @Nonnull final ChannelAddress target,
                                     @Nullable final Object targetFilter )
    {
      return true;
    }

    @Nonnull
    List<BulkCollectCall> getBulkCollectCalls()
    {
      return _bulkCollectCalls;
    }
  }

  private record BulkCollectCall(@Nonnull List<ChannelAddress> addresses,
                                 @Nullable Object filter,
                                 boolean isExplicitSubscribe)
  {
  }

  private record DeriveTargetFilterInstanceIdCall(@Nonnull EntityMessage entityMessage,
                                                  @Nonnull ChannelAddress source,
                                                  @Nullable Object sourceFilter,
                                                  @Nonnull ChannelAddress target,
                                                  @Nullable Object targetFilter)
  {
  }
}
