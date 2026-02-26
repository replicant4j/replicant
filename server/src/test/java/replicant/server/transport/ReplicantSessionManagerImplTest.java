package replicant.server.transport;

import java.io.Serializable;
import java.lang.reflect.Field;
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
import org.realityforge.guiceyloops.server.TestInitialContextFactory;
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
    TestInitialContextFactory.reset();
  }

  @Test
  public void createChannelLinkEntry_staticInstanced_usesTargetInstanceId()
    throws Exception
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
    when( webSocketSession.getId() ).thenReturn( "session-1" );
    when( webSocketSession.isOpen() ).thenReturn( true );
    final var session = new ReplicantSession( webSocketSession );

    final ChannelAddress sourceAddress = new ChannelAddress( 0 );
    final ChannelAddress targetAddress = new ChannelAddress( 1, 7, "fi-7" );
    final ChannelLink link = new ChannelLink( sourceAddress, targetAddress, null );

    session.getLock().lock();
    try
    {
      final var sourceEntry = session.createSubscriptionEntry( sourceAddress );
      sourceEntry.setFilter( "source-filter" );

      final var routingKeys = new HashMap<String, Serializable>();
      final var attributes = new HashMap<String, Serializable>();
      final var message = new EntityMessage( 1, 1, 0L, routingKeys, attributes, null );

      final Method method =
        ReplicantSessionManagerImpl.class
          .getDeclaredMethod( "createChannelLinkEntryIfRequired",
                              EntityMessage.class,
                              ReplicantSession.class,
                              ChannelLink.class );
      method.setAccessible( true );
      final ChannelLinkEntry entry =
        (ChannelLinkEntry) method.invoke( manager, message, session, link );

      assertNotNull( entry );
      assertEquals( entry.target(), targetAddress );
      assertEquals( entry.filter(), Map.of( "k", "v" ) );
      assertEquals( context.getShouldFollowCalls().size(), 1 );
      final var call = context.getShouldFollowCalls().get( 0 );
      assertEquals( call.source(), sourceAddress );
      assertEquals( call.sourceFilter(), "source-filter" );
      assertEquals( call.target(), targetAddress );
      assertEquals( call.targetFilter(), Map.of( "k", "v" ) );
    }
    finally
    {
      session.getLock().unlock();
    }
  }

  @Test
  public void createChannelLinkEntry_filteredGraphShouldNotFollowWhenContextRejects()
    throws Exception
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
                           ChannelMetaData.FilterType.STATIC,
                           json -> json,
                           ChannelMetaData.CacheType.NONE,
                           true );
    final var schema = new SchemaMetaData( "Test", sourceChannel, targetChannel );

    final var context = new TestSessionContext( schema );
    context.setShouldFollowLinks( false );
    final var manager = createManager( context, mock( ReplicantMessageBroker.class ) );

    final Session webSocketSession = mock( Session.class );
    when( webSocketSession.getId() ).thenReturn( "session-1" );
    when( webSocketSession.isOpen() ).thenReturn( true );
    final var session = new ReplicantSession( webSocketSession );

    final ChannelAddress sourceAddress = new ChannelAddress( 0 );
    final ChannelAddress targetAddress = new ChannelAddress( 1, 7 );
    final ChannelLink link = new ChannelLink( sourceAddress, targetAddress, null );

    session.getLock().lock();
    try
    {
      final var sourceEntry = session.createSubscriptionEntry( sourceAddress );
      sourceEntry.setFilter( "source-filter" );

      final var message = new EntityMessage( 1, 1, 0L, new HashMap<>(), new HashMap<>(), null );

      final Method method =
        ReplicantSessionManagerImpl.class
          .getDeclaredMethod( "createChannelLinkEntryIfRequired",
                              EntityMessage.class,
                              ReplicantSession.class,
                              ChannelLink.class );
      method.setAccessible( true );
      final ChannelLinkEntry entry =
        (ChannelLinkEntry) method.invoke( manager, message, session, link );

      assertNull( entry );
      assertEquals( context.getShouldFollowCalls().size(), 1 );
    }
    finally
    {
      session.getLock().unlock();
    }
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
    final var link = new ChannelLink( sourceAddress, targetAddress, null );

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
  public void bulkUnsubscribe_removesSubscriptionsViaSessionLogic()
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

    manager.bulkUnsubscribe( session, 99, List.of( address1, address2, address3 ) );

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
                           2,
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
    final var targetAddress = new ChannelAddress( 1, 20 );

    final var routingKeys = new HashMap<String, Serializable>();
    routingKeys.put( "Source", new ArrayList<>( List.of( 10 ) ) );
    final var deleteMessage = new EntityMessage( 10,
                                                 1,
                                                 0,
                                                 routingKeys,
                                                 null,
                                                 Set.of( new ChannelLink( sourceAddress, targetAddress, null ) ) );
    final var changeSet = new ChangeSet();
    final var packet = new Packet( false, null, null, null, List.of( deleteMessage ), changeSet );

    session.getLock().lock();
    try
    {
      final var sourceEntry = session.createSubscriptionEntry( sourceAddress );
      sourceEntry.setExplicitlySubscribed( true );
      session.createSubscriptionEntry( targetAddress );
      session.recordGraphLink( sourceAddress, targetAddress );

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
    private final List<ShouldFollowCall> _shouldFollowCalls = new ArrayList<>();
    private boolean _shouldFollowLinks = true;

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
    public void bulkCollectDataForSubscribe( @Nullable final ReplicantSession session,
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
    public void bulkCollectDataForSubscriptionUpdate( @Nonnull final ReplicantSession session,
                                                      @Nonnull final List<ChannelAddress> addresses,
                                                      @Nullable final Object originalFilter,
                                                      @Nullable final Object filter,
                                                      @Nonnull final ChangeSet changeSet )
    {
    }

    @Nullable
    @Override
    public EntityMessage filterEntityMessage( @Nonnull final ReplicantSession session,
                                              @Nonnull final ChannelAddress address,
                                              @Nonnull final EntityMessage message )
    {
      return null;
    }

    @Override
    public boolean shouldFollowLink( @Nonnull final ChannelAddress source,
                                     final Object sourceFilter,
                                     @Nonnull final ChannelAddress target,
                                     @Nullable final Object targetFilter )
    {
      _shouldFollowCalls.add( new ShouldFollowCall( source, sourceFilter, target, targetFilter ) );
      return _shouldFollowLinks;
    }

    @Nonnull
    List<BulkCollectCall> getBulkCollectCalls()
    {
      return _bulkCollectCalls;
    }

    @Nonnull
    List<ShouldFollowCall> getShouldFollowCalls()
    {
      return _shouldFollowCalls;
    }

    void setShouldFollowLinks( final boolean shouldFollowLinks )
    {
      _shouldFollowLinks = shouldFollowLinks;
    }
  }

  private record BulkCollectCall(@Nonnull List<ChannelAddress> addresses,
                                 @Nullable Object filter,
                                 boolean isExplicitSubscribe)
  {
  }

  private record ShouldFollowCall(@Nonnull ChannelAddress source,
                                  @Nullable Object sourceFilter,
                                  @Nonnull ChannelAddress target,
                                  @Nullable Object targetFilter)
  {
  }
}
