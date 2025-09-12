package replicant.server.transport;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.websocket.RemoteEndpoint;
import javax.websocket.Session;
import org.realityforge.guiceyloops.server.AssertUtil;
import org.realityforge.guiceyloops.server.TestInitialContextFactory;
import org.realityforge.guiceyloops.shared.ValueUtil;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import replicant.server.Change;
import replicant.server.ChangeSet;
import replicant.server.ChannelAction;
import replicant.server.ChannelAddress;
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
  public void setupTransactionContext()
  {
    RegistryUtil.bind();
  }

  @AfterMethod
  public void clearContext()
  {
    TestInitialContextFactory.reset();
  }

  @Test
  public void basicWorkflow()
  {
    final TestReplicantSessionManager sm = new TestReplicantSessionManager();
    assertEquals( sm.getSessionIDs().size(), 0 );
    assertNull( sm.getSession( "MySessionID" ) );
    final ReplicantSession session = createSession( sm );
    assertNotNull( session );
    assertNotNull( session.getId() );
    assertEquals( sm.getSessionIDs().size(), 1 );

    // The next line should update the last accessed time too!
    assertEquals( sm.getSession( session.getId() ), session );

    assertTrue( sm.invalidateSession( session ) );
    assertEquals( sm.getSessionIDs().size(), 0 );
    assertFalse( sm.invalidateSession( session ) );
    assertNull( sm.getSession( session.getId() ) );
  }

  @Test
  public void removeClosedSessions_openWebSocketSession()
  {
    final Session webSocketSession = mock( Session.class );
    final String id = ValueUtil.randomString();
    when( webSocketSession.getId() ).thenReturn( id );
    when( webSocketSession.isOpen() ).thenReturn( true );

    final ReplicantSessionManagerImpl sm = new TestReplicantSessionManager();
    final ReplicantSession session = sm.createSession( webSocketSession );

    assertEquals( sm.getSession( session.getId() ), session );
    sm.removeClosedSessions();
    assertEquals( sm.getSession( session.getId() ), session );
  }

  @Test
  public void removeClosedSessions_closedWebSocketSession()
  {
    final Session webSocketSession = mock( Session.class );
    final String id = ValueUtil.randomString();
    when( webSocketSession.getId() ).thenReturn( id );
    when( webSocketSession.isOpen() ).thenReturn( false );

    final ReplicantSessionManagerImpl sm = new TestReplicantSessionManager();
    final ReplicantSession session = sm.createSession( webSocketSession );

    assertEquals( sm.getSession( session.getId() ), session );
    sm.removeClosedSessions();
    assertNull( sm.getSession( session.getId() ) );
  }

  @Test
  public void locking()
    throws Exception
  {
    final TestReplicantSessionManager sm = new TestReplicantSessionManager();
    final ReadWriteLock lock = sm.getLock();

    // Variable used to pass data back from threads
    final ReplicantSession[] sessions = new ReplicantSession[ 2 ];

    lock.readLock().lock();

    // Make sure createSession can not complete if something has a read lock
    final CyclicBarrier end = go( () -> sessions[ 0 ] = createSession( sm ) );

    assertNull( sessions[ 0 ] );
    lock.readLock().unlock();
    end.await();
    assertNotNull( sessions[ 0 ] );

    lock.writeLock().lock();

    // Make sure getSession can acquire a read lock
    final CyclicBarrier end2 = go( () -> sessions[ 1 ] = sm.getSession( sessions[ 0 ].getId() ) );

    assertNull( sessions[ 1 ] );
    lock.writeLock().unlock();
    end2.await();
    assertNotNull( sessions[ 1 ] );

    lock.readLock().lock();

    final Boolean[] invalidated = new Boolean[ 1 ];
    // Make sure createSession can not complete if something has a read lock
    final CyclicBarrier end3 = go( () -> invalidated[ 0 ] = sm.invalidateSession( sessions[ 0 ] ) );

    assertNull( invalidated[ 0 ] );
    lock.readLock().unlock();
    end3.await();
    assertEquals( invalidated[ 0 ], Boolean.TRUE );
  }

  @FunctionalInterface
  public interface Action
  {
    void run()
      throws Exception;
  }

  private CyclicBarrier go( final Action target )
    throws Exception
  {
    final CyclicBarrier start = new CyclicBarrier( 2 );
    final CyclicBarrier stop = new CyclicBarrier( 2 );
    new Thread( () -> {
      try
      {
        start.await();
        target.run();
        stop.await();
      }
      catch ( Exception e )
      {
        // Ignored
      }
    } ).start();
    start.await();
    Thread.sleep( 1 );
    return stop;
  }

  @Test
  public void deleteCacheEntry()
  {
    final ChannelMetaData ch1 =
      new ChannelMetaData( 0,
                           "C1",
                           null,
                           ChannelMetaData.FilterType.NONE,
                           null,
                           ChannelMetaData.CacheType.INTERNAL,
                           false,
                           true );
    final ChannelMetaData[] channels = new ChannelMetaData[]{ ch1 };

    final ChannelAddress address1 = new ChannelAddress( ch1.getChannelId(), null );

    final TestReplicantSessionManager sm = new TestReplicantSessionManager( channels );
    assertFalse( sm.deleteCacheEntry( address1 ) );

    sm.setCacheKey( "X" );

    sm.tryGetCacheEntry( address1 );

    assertTrue( sm.deleteCacheEntry( address1 ) );
  }

  @Test
  public void ensureCdiType()
  {
    AssertUtil.assertNoFinalMethodsForCDI( ReplicantSessionManagerImpl.class );
  }

  @Test
  public void subscribe()
    throws Exception
  {
    final ChannelMetaData ch1 =
      new ChannelMetaData( 0,
                           "C1",
                           null,
                           ChannelMetaData.FilterType.NONE,
                           null,
                           ChannelMetaData.CacheType.NONE,
                           false,
                           true );
    final ChannelMetaData ch2 =
      new ChannelMetaData( 1,
                           "C2",
                           null,
                           ChannelMetaData.FilterType.DYNAMIC,
                           j -> null,
                           ChannelMetaData.CacheType.NONE,
                           false,
                           true );
    final ChannelMetaData ch3 =
      new ChannelMetaData( 2,
                           "C3",
                           null,
                           ChannelMetaData.FilterType.STATIC,
                           j -> null,
                           ChannelMetaData.CacheType.NONE,
                           false,
                           true );
    final ChannelMetaData[] channels = new ChannelMetaData[]{ ch1, ch2, ch3 };

    final ChannelAddress address1 = new ChannelAddress( ch1.getChannelId(), null );
    final ChannelAddress address2 = new ChannelAddress( ch2.getChannelId(), null );
    final ChannelAddress address3 = new ChannelAddress( ch3.getChannelId(), null );

    final TestFilter originalFilter = new TestFilter( 41 );
    final TestFilter filter = new TestFilter( 42 );

    final TestReplicantSessionManager sm = new TestReplicantSessionManager( channels );
    final ReplicantSession session = createSession( sm );
    final Session webSocketSession = session.getWebSocketSession();
    when( webSocketSession.isOpen() ).thenReturn( true );

    with( session, () -> assertNull( session.findSubscriptionEntry( address1 ) ) );

    // subscribe
    {
      EntityMessageCacheUtil.removeSessionChanges();
      with( session, () -> sm.subscribe( session, address1, false, null, EntityMessageCacheUtil.getSessionChanges() ) );

      final SubscriptionEntry entry1 = with( session, () -> session.findSubscriptionEntry( address1 ) );
      assertNotNull( entry1 );
      assertEntry( entry1, false, 0, 0, null );

      assertChannelActionCount( 1 );
      assertSessionChangesCount( 1 );
    }

    // re-subscribe- should be noop
    {
      EntityMessageCacheUtil.removeSessionChanges();
      with( session, () -> sm.subscribe( session, address1, false, null, EntityMessageCacheUtil.getSessionChanges() ) );
      assertEntry( with( session, () -> session.getSubscriptionEntry( address1 ) ), false, 0, 0, null );
      assertChannelActionCount( 0 );
      assertSessionChangesCount( 0 );
    }

    // re-subscribe explicitly - should only set explicit flag
    {
      EntityMessageCacheUtil.removeSessionChanges();
      with( session, () -> sm.subscribe( session, address1, null ) );
      assertEntry( with( session, () -> session.getSubscriptionEntry( address1 ) ), true, 0, 0, null );
      assertChannelActionCount( 0 );
      assertSessionChangesCount( 0 );
    }

    // subscribe when existing subscription present
    {
      EntityMessageCacheUtil.removeSessionChanges();
      with( session, () -> sm.subscribe( session, address2, originalFilter ) );

      assertEntry( with( session, () -> session.getSubscriptionEntry( address2 ) ), true, 0, 0, originalFilter );
      assertChannelActionCount( 1 );
      assertSessionChangesCount( 1 );

      EntityMessageCacheUtil.removeSessionChanges();

      TransactionSynchronizationRegistryUtil.lookup()
        .putResource( ServerConstants.REQUEST_ID_KEY, ValueUtil.randomInt() );

      //Should be a noop as same filter
      with( session, () -> sm.subscribe( session, address2, originalFilter ) );

      assertEntry( with( session, () -> session.getSubscriptionEntry( address2 ) ), true, 0, 0, originalFilter );
      assertChannelActionCount( 0 );
      assertSessionChangesCount( 0 );

      EntityMessageCacheUtil.removeSessionChanges();

      //Should be a filter update
      with( session, () -> sm.subscribe( session, address2, filter ) );

      assertEntry( with( session, () -> session.getSubscriptionEntry( address2 ) ), true, 0, 0, filter );
      assertChannelActionCount( 1 );
      assertSessionChangesCount( 1 );
    }

    //Subscribe and attempt to update static filter
    {
      EntityMessageCacheUtil.removeSessionChanges();
      with( session, () -> sm.subscribe( session, address3, originalFilter ) );
      assertEntry( with( session, () -> session.getSubscriptionEntry( address3 ) ), true, 0, 0, originalFilter );
      assertChannelActionCount( 1 );
      assertSessionChangesCount( 1 );

      EntityMessageCacheUtil.removeSessionChanges();

      //Should be a noop as same filter
      with( session, () -> sm.subscribe( session, address3, originalFilter ) );

      assertEntry( with( session, () -> session.getSubscriptionEntry( address3 ) ), true, 0, 0, originalFilter );
      assertChannelActionCount( 0 );
      assertSessionChangesCount( 0 );

      try
      {
        with( session, () -> sm.subscribe( session, address3, filter ) );
        fail( "Successfully updated a static filter" );
      }
      catch ( final AttemptedToUpdateStaticFilterException ignore )
      {
        //Ignore. Fine
      }
    }
  }

  @Test
  public void subscribe_withCache()
    throws Exception
  {
    final ChannelMetaData ch1 = new ChannelMetaData( 0,
                                                     "C1",
                                                     null,
                                                     ChannelMetaData.FilterType.NONE,
                                                     null,
                                                     ChannelMetaData.CacheType.INTERNAL,
                                                     false,
                                                     true );
    final ChannelMetaData[] channels = new ChannelMetaData[]{ ch1 };

    final ChannelAddress address1 = new ChannelAddress( ch1.getChannelId(), null );

    final TestReplicantSessionManager sm = new TestReplicantSessionManager( channels );

    sm.setCacheKey( "X" );

    // subscribe - matching cacheKey
    {
      final ReplicantSession session = createSession( sm );
      final Session webSocketSession = session.getWebSocketSession();
      when( webSocketSession.isOpen() ).thenReturn( true );

      with( session, () -> assertNull( session.findSubscriptionEntry( address1 ) ) );
      EntityMessageCacheUtil.removeSessionChanges();
      with( session, () -> session.setETag( address1, "X" ) );

      with( session, () -> sm.subscribe( session, address1, false, null, EntityMessageCacheUtil.getSessionChanges() ) );

      final SubscriptionEntry entry1 = with( session, () -> session.findSubscriptionEntry( address1 ) );
      assertNotNull( entry1 );
      assertEntry( entry1, false, 0, 0, null );

      verify( webSocketSession.getBasicRemote() ).sendText( "{\"type\":\"use-cache\",\"channel\":\"0\",\"etag\":\"X\"}" );

      assertChannelActionCount( 0 );
      assertSessionChangesCount( 0 );
    }

    // subscribe - cacheKey differs
    {
      final ReplicantSession session = createSession( sm );
      with( session, () -> assertNull( session.findSubscriptionEntry( address1 ) ) );
      EntityMessageCacheUtil.removeSessionChanges();
      with( session, () -> session.setETag( address1, "Y" ) );
      with( session, () -> sm.subscribe( session, address1, false, null, EntityMessageCacheUtil.getSessionChanges() ) );

      final SubscriptionEntry entry1 = with( session, () -> session.findSubscriptionEntry( address1 ) );
      assertNotNull( entry1 );
      assertEntry( entry1, false, 0, 0, null );

      verify( sm.getReplicantMessageBroker() )
        .queueChangeMessage( eq( session ), eq( true ), isNull(), isNull(), eq( "X" ), any(), any() );
    }
  }

  @Test
  public void subscribe_withSessionID()
    throws Exception
  {
    final ChannelMetaData ch1 =
      new ChannelMetaData( 0,
                           "C1",
                           null,
                           ChannelMetaData.FilterType.NONE,
                           null,
                           ChannelMetaData.CacheType.NONE,
                           false,
                           true );
    final ChannelMetaData[] channels = new ChannelMetaData[]{ ch1 };

    final ChannelAddress address1 = new ChannelAddress( ch1.getChannelId(), null );

    final TestReplicantSessionManager sm = new TestReplicantSessionManager( channels );
    final ReplicantSession session = createSession( sm );

    with( session, () -> assertNull( session.findSubscriptionEntry( address1 ) ) );

    with( session, () -> sm.subscribe( session, address1, null ) );

    final SubscriptionEntry entry1 = with( session, () -> session.findSubscriptionEntry( address1 ) );
    assertNotNull( entry1 );
    assertEntry( entry1, true, 0, 0, null );

    assertChannelActionCount( 1 );
    assertSessionChangesCount( 1 );

    final RemoteEndpoint.Basic remote = session.getWebSocketSession().getBasicRemote();
    verify( remote, never() ).sendText( anyString() );
  }

  @Test
  public void subscribe_withSessionID_andCaching()
    throws Exception
  {
    final ChannelMetaData ch1 = new ChannelMetaData( 0,
                                                     "C1",
                                                     null,
                                                     ChannelMetaData.FilterType.NONE,
                                                     null,
                                                     ChannelMetaData.CacheType.INTERNAL,
                                                     false,
                                                     true );
    final ChannelMetaData[] channels = new ChannelMetaData[]{ ch1 };

    final ChannelAddress address1 = new ChannelAddress( ch1.getChannelId(), null );

    final TestReplicantSessionManager sm = new TestReplicantSessionManager( channels );
    final ReplicantSession session = createSession( sm );

    sm.setCacheKey( "X" );

    assertNull( session.getETag( address1 ) );

    with( session, () -> assertNull( session.findSubscriptionEntry( address1 ) ) );

    with( session, () -> session.setETag( address1, "X" ) );
    with( session, () -> sm.subscribe( session, address1, null ) );

    assertEquals( session.getETag( address1 ), "X" );

    final SubscriptionEntry entry1 = with( session, () -> session.findSubscriptionEntry( address1 ) );
    assertNotNull( entry1 );
    assertEntry( entry1, true, 0, 0, null );

    assertChannelActionCount( 0 );
    assertSessionChangesCount( 0 );
    final RemoteEndpoint.Basic remote = session.getWebSocketSession().getBasicRemote();
    verify( remote ).sendText( "{\"type\":\"use-cache\",\"channel\":\"0\",\"etag\":\"X\"}" );
  }

  @Test
  public void subscribe_withSessionID_andCachingThatNoMatch()
    throws Exception
  {
    final ChannelMetaData ch1 = new ChannelMetaData( 0,
                                                     "C1",
                                                     null,
                                                     ChannelMetaData.FilterType.NONE,
                                                     null,
                                                     ChannelMetaData.CacheType.INTERNAL,
                                                     false,
                                                     true );
    final ChannelMetaData[] channels = new ChannelMetaData[]{ ch1 };

    final ChannelAddress address1 = new ChannelAddress( ch1.getChannelId(), null );

    final TestReplicantSessionManager sm = new TestReplicantSessionManager( channels );
    final ReplicantSession session = createSession( sm );

    sm.setCacheKey( "X" );

    assertNull( session.getETag( address1 ) );

    with( session, () -> assertNull( session.findSubscriptionEntry( address1 ) ) );

    with( session, () -> session.setETag( address1, "Y" ) );
    with( session, () -> sm.subscribe( session, address1, null ) );

    assertNull( session.getETag( address1 ) );

    final SubscriptionEntry entry1 = with( session, () -> session.findSubscriptionEntry( address1 ) );
    assertNotNull( entry1 );
    assertEntry( entry1, true, 0, 0, null );

    verify( sm.getReplicantMessageBroker() )
      .queueChangeMessage( eq( session ), eq( true ), isNull(), isNull(), eq( "X" ), any(), any() );
  }

  @Test
  public void performSubscribe()
    throws Exception
  {
    final ChannelMetaData ch1 =
      new ChannelMetaData( 0,
                           "C1",
                           null,
                           ChannelMetaData.FilterType.NONE,
                           null,
                           ChannelMetaData.CacheType.NONE,
                           false,
                           true );
    final ChannelMetaData ch2 =
      new ChannelMetaData( 1,
                           "C2",
                           null,
                           ChannelMetaData.FilterType.DYNAMIC,
                           j -> null,
                           ChannelMetaData.CacheType.NONE,
                           false,
                           true );
    final ChannelMetaData ch3 =
      new ChannelMetaData( 2,
                           "C3",
                           42,
                           ChannelMetaData.FilterType.NONE,
                           null,
                           ChannelMetaData.CacheType.NONE,
                           false,
                           true );
    final ChannelMetaData[] channels = new ChannelMetaData[]{ ch1, ch2, ch3 };

    final ChannelAddress address1 = new ChannelAddress( ch1.getChannelId() );
    final ChannelAddress address2 = new ChannelAddress( ch2.getChannelId() );
    final ChannelAddress address3 = new ChannelAddress( ch2.getChannelId(), ValueUtil.randomInt() );

    final TestReplicantSessionManager sm = new TestReplicantSessionManager( channels );
    final ReplicantSession session = createSession( sm );

    // Test with no filter
    {
      final SubscriptionEntry e1 = session.createSubscriptionEntry( address1 );
      //Rebind clears the state
      EntityMessageCacheUtil.removeSessionChanges();

      assertChannelActionCount( 0 );
      assertEntry( e1, false, 0, 0, null );

      with( session, () -> sm.performSubscribe( session, e1, true, null, EntityMessageCacheUtil.getSessionChanges() ) );

      assertEntry( e1, true, 0, 0, null );

      final List<ChannelAction> actions = getChannelActions();
      assertEquals( actions.size(), 1 );
      assertChannelAction( actions.get( 0 ), address1, ChannelAction.Action.ADD, null );

      // 1 Change comes from collectDataForSubscribe
      final Collection<Change> changes = getChanges();
      assertEquals( changes.size(), 1 );
      assertEquals( changes.iterator().next().getEntityMessage().getId(), 79 );

      assertEntry( e1, true, 0, 0, null );
    }

    {
      final TestFilter filter = new TestFilter( 42 );

      EntityMessageCacheUtil.removeSessionChanges();
      final SubscriptionEntry e1 = session.createSubscriptionEntry( address2 );

      assertChannelActionCount( 0 );
      assertEntry( e1, false, 0, 0, null );

      with( session,
            () -> sm.performSubscribe( session, e1, true, filter, EntityMessageCacheUtil.getSessionChanges() ) );

      assertEntry( e1, true, 0, 0, filter );

      final List<ChannelAction> actions = getChannelActions();
      assertEquals( actions.size(), 1 );
      assertChannelAction( actions.get( 0 ), address2, ChannelAction.Action.ADD, "{\"myField\":42}" );

      // 1 Change comes from collectDataForSubscribe
      final Collection<Change> changes = getChanges();
      assertEquals( changes.size(), 1 );
      assertEquals( changes.iterator().next().getEntityMessage().getId(), 79 );

      assertEntry( e1, true, 0, 0, filter );

      with( session, () -> session.deleteSubscriptionEntry( e1 ) );
    }

    // Root instance is deleted
    {
      final SubscriptionEntry e1 = session.createSubscriptionEntry( address3 );
      //Rebind clears the state
      EntityMessageCacheUtil.removeSessionChanges();

      assertChannelActionCount( 0 );
      assertEntry( e1, false, 0, 0, null );

      sm.markChannelRootAsDeleted();

      with( session, () -> sm.performSubscribe( session, e1, true, null, EntityMessageCacheUtil.getSessionChanges() ) );

      assertEntry( e1, false, 0, 0, null );

      final List<ChannelAction> actions = getChannelActions();
      assertEquals( actions.size(), 1 );
      assertChannelAction( actions.get( 0 ), address3, ChannelAction.Action.DELETE, null );

      assertEquals( getChanges().size(), 0 );
    }
  }

  @Test
  public void performSubscribe_withCaching()
    throws Exception
  {
    final ChannelMetaData ch1 = new ChannelMetaData( 0,
                                                     "C1",
                                                     null,
                                                     ChannelMetaData.FilterType.NONE,
                                                     null,
                                                     ChannelMetaData.CacheType.INTERNAL,
                                                     false,
                                                     true );
    final ChannelMetaData ch2 =
      new ChannelMetaData( 1,
                           "C2",
                           42,
                           ChannelMetaData.FilterType.NONE,
                           null,
                           ChannelMetaData.CacheType.INTERNAL,
                           false,
                           true );
    final ChannelMetaData[] channels = new ChannelMetaData[]{ ch1, ch2 };

    final ChannelAddress address1 = new ChannelAddress( ch1.getChannelId() );
    final ChannelAddress address2 = new ChannelAddress( ch2.getChannelId(), 1 );

    final TestReplicantSessionManager sm = new TestReplicantSessionManager( channels );

    sm.setCacheKey( "X" );

    //Locally cached
    {
      sm.deleteAllCacheEntries();
      final ReplicantSession session = createSession( sm );
      with( session, () -> session.setETag( address1, "X" ) );
      final SubscriptionEntry e1 = session.createSubscriptionEntry( address1 );
      //Rebind clears the state
      EntityMessageCacheUtil.removeSessionChanges();

      assertChannelActionCount( 0 );
      assertEntry( e1, false, 0, 0, null );

      with( session, () -> sm.performSubscribe( session, e1, true, null, EntityMessageCacheUtil.getSessionChanges() ) );

      assertEntry( e1, true, 0, 0, null );

      assertChannelActionCount( 0 );
      assertSessionChangesCount( 0 );

      verify( session.getWebSocketSession().getBasicRemote() ).sendText(
        "{\"type\":\"use-cache\",\"channel\":\"0\",\"etag\":\"X\"}" );
    }

    //Locally cached but an old version
    {
      sm.deleteAllCacheEntries();
      final ReplicantSession session = createSession( sm );
      with( session, () -> session.setETag( address1, "NOT" + sm._cacheKey ) );
      final SubscriptionEntry e1 = session.createSubscriptionEntry( address1 );
      //Rebind clears the state
      EntityMessageCacheUtil.removeSessionChanges();

      assertChannelActionCount( 0 );
      assertEntry( e1, false, 0, 0, null );

      with( session, () -> sm.performSubscribe( session, e1, true, null, EntityMessageCacheUtil.getSessionChanges() ) );

      assertEntry( e1, true, 0, 0, null );

      verify( sm.getReplicantMessageBroker() )
        .queueChangeMessage( eq( session ), eq( true ), isNull(), isNull(), eq( "X" ), any(), any() );
    }

    //Not cached locally
    {
      reset( sm.getReplicantMessageBroker() );
      sm.deleteAllCacheEntries();
      final ReplicantSession session = createSession( sm );
      final SubscriptionEntry e1 = session.createSubscriptionEntry( address1 );
      //Rebind clears the state
      EntityMessageCacheUtil.removeSessionChanges();

      assertChannelActionCount( 0 );
      assertEntry( e1, false, 0, 0, null );

      with( session, () -> sm.performSubscribe( session, e1, true, null, EntityMessageCacheUtil.getSessionChanges() ) );

      assertEntry( e1, true, 0, 0, null );

      verify( sm.getReplicantMessageBroker() )
        .queueChangeMessage( eq( session ), eq( true ), isNull(), isNull(), eq( "X" ), any(), any() );
    }

    //Locally cached but deleted
    {
      reset( sm.getReplicantMessageBroker() );
      sm.deleteAllCacheEntries();
      final ReplicantSession session = createSession( sm );
      with( session, () -> session.setETag( address1, "X" ) );
      final SubscriptionEntry e1 = session.createSubscriptionEntry( address2 );
      //Rebind clears the state
      EntityMessageCacheUtil.removeSessionChanges();

      assertChannelActionCount( 0 );
      assertEntry( e1, false, 0, 0, null );

      sm.markChannelRootAsDeleted();

      with( session, () -> sm.performSubscribe( session, e1, true, null, EntityMessageCacheUtil.getSessionChanges() ) );

      assertEntry( e1, false, 0, 0, null );

      assertChannelActionCount( 0 );
      assertSessionChangesCount( 0 );

      // Queue a cached response that contains a delete
      verify( sm.getReplicantMessageBroker() )
        .queueChangeMessage( eq( session ),
                             eq( true ),
                             isNull(),
                             isNull(),
                             isNull(),
                             eq( Collections.emptyList() ),
                             any() );
    }
  }

  @Test
  public void performUnsubscribe()
    throws Exception
  {
    final ChannelMetaData ch1 =
      new ChannelMetaData( 0,
                           "C1",
                           42,
                           ChannelMetaData.FilterType.NONE,
                           null,
                           ChannelMetaData.CacheType.NONE,
                           false,
                           true );
    final ChannelMetaData[] channels = new ChannelMetaData[]{ ch1 };

    final ChannelAddress address1 = new ChannelAddress( ch1.getChannelId(), ValueUtil.randomInt() );
    final ChannelAddress address2 = new ChannelAddress( ch1.getChannelId(), ValueUtil.randomInt() );
    final ChannelAddress address3 = new ChannelAddress( ch1.getChannelId(), ValueUtil.randomInt() );
    final ChannelAddress address4 = new ChannelAddress( ch1.getChannelId(), ValueUtil.randomInt() );
    final ChannelAddress address5 = new ChannelAddress( ch1.getChannelId(), ValueUtil.randomInt() );

    final TestReplicantSessionManager sm = new TestReplicantSessionManager( channels );
    final ReplicantSession session = createSession( sm );

    // Unsubscribe from channel that was explicitly subscribed
    {
      EntityMessageCacheUtil.removeSessionChanges();
      with( session, () -> sm.subscribe( session, address1, null ) );
      final SubscriptionEntry entry = with( session, () -> session.getSubscriptionEntry( address1 ) );

      //Rebind clears the state
      EntityMessageCacheUtil.removeSessionChanges();

      assertChannelActionCount( 0 );

      with( session,
            () -> sm.performUnsubscribe( session, entry, true, false, EntityMessageCacheUtil.getSessionChanges() ) );

      assertChannelActionCount( 1 );
      assertChannelAction( getChannelActions().get( 0 ), address1, ChannelAction.Action.REMOVE, null );

      with( session, () -> assertNull( session.findSubscriptionEntry( address1 ) ) );
    }

    // implicit unsubscribe from channel that was implicitly subscribed should leave explicit subscription
    {
      EntityMessageCacheUtil.removeSessionChanges();
      with( session, () -> sm.subscribe( session, address1, null ) );
      final SubscriptionEntry entry = with( session, () -> session.getSubscriptionEntry( address1 ) );

      //Rebind clears the state
      EntityMessageCacheUtil.removeSessionChanges();

      assertChannelActionCount( 0 );

      with( session,
            () -> sm.performUnsubscribe( session, entry, false, false, EntityMessageCacheUtil.getSessionChanges() ) );

      assertChannelActionCount( 0 );

      with( session, () -> assertNotNull( session.findSubscriptionEntry( address1 ) ) );

      with( session,
            () -> sm.performUnsubscribe( session, entry, true, false, EntityMessageCacheUtil.getSessionChanges() ) );

      assertChannelActionCount( 1 );

      with( session, () -> assertNull( session.findSubscriptionEntry( address1 ) ) );
    }

    // implicit unsubscribe from channel that was implicitly subscribed should delete subscription
    {
      EntityMessageCacheUtil.removeSessionChanges();
      with( session, () -> sm.subscribe( session, address1, false, null, EntityMessageCacheUtil.getSessionChanges() ) );
      final SubscriptionEntry entry = with( session, () -> session.getSubscriptionEntry( address1 ) );

      //Rebind clears the state
      EntityMessageCacheUtil.removeSessionChanges();

      assertChannelActionCount( 0 );

      with( session,
            () -> sm.performUnsubscribe( session, entry, false, false, EntityMessageCacheUtil.getSessionChanges() ) );

      assertChannelActionCount( 1 );
      assertChannelAction( getChannelActions().get( 0 ), address1, ChannelAction.Action.REMOVE, null );

      with( session, () -> assertNull( session.findSubscriptionEntry( address1 ) ) );
    }

    // implicit unsubscribe from channel that was implicitly subscribed should leave subscription that
    // implicitly linked from elsewhere
    {
      EntityMessageCacheUtil.removeSessionChanges();
      with( session, () -> sm.subscribe( session, address1, false, null, EntityMessageCacheUtil.getSessionChanges() ) );
      final SubscriptionEntry entry = with( session, () -> session.getSubscriptionEntry( address1 ) );

      with( session, () -> sm.subscribe( session, address2, false, null, EntityMessageCacheUtil.getSessionChanges() ) );
      final SubscriptionEntry entry2 = with( session, () -> session.getSubscriptionEntry( address2 ) );
      with( session, () -> sm.linkSubscriptionEntries( entry2, entry ) );

      //Rebind clears the state
      EntityMessageCacheUtil.removeSessionChanges();

      assertChannelActionCount( 0 );

      with( session,
            () -> sm.performUnsubscribe( session, entry, false, false, EntityMessageCacheUtil.getSessionChanges() ) );

      assertChannelActionCount( 0 );
      with( session, () -> assertNotNull( session.findSubscriptionEntry( address1 ) ) );

      with( session, () -> sm.delinkSubscriptionEntries( entry2, entry ) );

      with( session,
            () -> sm.performUnsubscribe( session, entry, false, false, EntityMessageCacheUtil.getSessionChanges() ) );

      assertChannelActionCount( 1 );
      assertChannelAction( getChannelActions().get( 0 ), address1, ChannelAction.Action.REMOVE, null );
      with( session, () -> assertNull( session.findSubscriptionEntry( address1 ) ) );
    }

    // Unsubscribe also results in unsubscribe for all downstream channels
    {
      EntityMessageCacheUtil.removeSessionChanges();
      with( session, () -> sm.subscribe( session, address1, null ) );
      with( session, () -> sm.subscribe( session, address2, false, null, EntityMessageCacheUtil.getSessionChanges() ) );
      with( session, () -> sm.subscribe( session, address3, false, null, EntityMessageCacheUtil.getSessionChanges() ) );
      with( session, () -> sm.subscribe( session, address4, false, null, EntityMessageCacheUtil.getSessionChanges() ) );
      with( session, () -> sm.subscribe( session, address5, false, null, EntityMessageCacheUtil.getSessionChanges() ) );

      final SubscriptionEntry entry = with( session, () -> session.getSubscriptionEntry( address1 ) );
      final SubscriptionEntry entry2 = with( session, () -> session.getSubscriptionEntry( address2 ) );
      final SubscriptionEntry entry3 = with( session, () -> session.getSubscriptionEntry( address3 ) );
      final SubscriptionEntry entry4 = with( session, () -> session.getSubscriptionEntry( address4 ) );
      final SubscriptionEntry entry5 = with( session, () -> session.getSubscriptionEntry( address5 ) );

      with( session, () -> sm.linkSubscriptionEntries( entry, entry2 ) );
      with( session, () -> sm.linkSubscriptionEntries( entry, entry3 ) );
      with( session, () -> sm.linkSubscriptionEntries( entry3, entry4 ) );
      with( session, () -> sm.linkSubscriptionEntries( entry4, entry5 ) );

      //Rebind clears the state
      EntityMessageCacheUtil.removeSessionChanges();

      assertChannelActionCount( 0 );

      with( session,
            () -> sm.performUnsubscribe( session, entry, true, false, EntityMessageCacheUtil.getSessionChanges() ) );

      assertChannelActionCount( 5 );
      for ( final ChannelAction action : getChannelActions() )
      {
        assertEquals( action.action(), ChannelAction.Action.REMOVE );
      }

      with( session, () -> assertNull( session.findSubscriptionEntry( address1 ) ) );
      with( session, () -> assertNull( session.findSubscriptionEntry( address2 ) ) );
      with( session, () -> assertNull( session.findSubscriptionEntry( address3 ) ) );
      with( session, () -> assertNull( session.findSubscriptionEntry( address4 ) ) );
      with( session, () -> assertNull( session.findSubscriptionEntry( address5 ) ) );
    }
  }

  @Test
  public void unsubscribe()
    throws Exception
  {
    final ChannelMetaData ch1 =
      new ChannelMetaData( 0,
                           "C1",
                           42,
                           ChannelMetaData.FilterType.NONE,
                           null,
                           ChannelMetaData.CacheType.NONE,
                           false,
                           true );
    final ChannelMetaData[] channels = new ChannelMetaData[]{ ch1 };

    final ChannelAddress address1 = new ChannelAddress( ch1.getChannelId(), ValueUtil.randomInt() );

    final TestReplicantSessionManager sm = new TestReplicantSessionManager( channels );
    final ReplicantSession session = createSession( sm );

    // Unsubscribe from channel that was explicitly subscribed
    {
      EntityMessageCacheUtil.removeSessionChanges();
      with( session, () -> sm.subscribe( session, address1, null ) );
      final SubscriptionEntry entry = with( session, () -> session.getSubscriptionEntry( address1 ) );

      //Rebind clears the state
      EntityMessageCacheUtil.removeSessionChanges();

      assertChannelActionCount( 0 );

      with( session, () -> sm.unsubscribe( session, entry.address(), EntityMessageCacheUtil.getSessionChanges() ) );

      assertChannelActionCount( 1 );
      assertChannelAction( getChannelActions().get( 0 ), address1, ChannelAction.Action.REMOVE, null );

      with( session, () -> assertNull( session.findSubscriptionEntry( address1 ) ) );
    }

    // unsubscribe from unsubscribed
    {
      EntityMessageCacheUtil.removeSessionChanges();

      assertChannelActionCount( 0 );

      with( session, () -> sm.unsubscribe( session, address1, EntityMessageCacheUtil.getSessionChanges() ) );

      assertChannelActionCount( 0 );
    }
  }

  @Test
  public void unsubscribe_usingSessionID()
    throws Exception
  {
    final ChannelMetaData ch1 =
      new ChannelMetaData( 0,
                           "C1",
                           42,
                           ChannelMetaData.FilterType.NONE,
                           null,
                           ChannelMetaData.CacheType.NONE,
                           false,
                           true );
    final ChannelMetaData[] channels = new ChannelMetaData[]{ ch1 };

    final ChannelAddress address1 = new ChannelAddress( ch1.getChannelId(), ValueUtil.randomInt() );

    final TestReplicantSessionManager sm = new TestReplicantSessionManager( channels );
    final ReplicantSession session = createSession( sm );

    EntityMessageCacheUtil.removeSessionChanges();
    with( session, () -> sm.subscribe( session, address1, null ) );

    EntityMessageCacheUtil.removeSessionChanges();

    assertChannelActionCount( 0 );

    with( session, () -> sm.unsubscribe( session, address1, EntityMessageCacheUtil.getSessionChanges() ) );

    assertChannelActionCount( 1 );
    assertChannelAction( getChannelActions().get( 0 ), address1, ChannelAction.Action.REMOVE, null );

    with( session, () -> assertNull( session.findSubscriptionEntry( address1 ) ) );
  }

  @Test
  public void bulkUnsubscribe()
    throws Exception
  {
    final ChannelMetaData ch1 =
      new ChannelMetaData( 0,
                           "C1",
                           42,
                           ChannelMetaData.FilterType.NONE,
                           null,
                           ChannelMetaData.CacheType.NONE,
                           false,
                           true );
    final ChannelMetaData ch2 =
      new ChannelMetaData( 1,
                           "C2",
                           42,
                           ChannelMetaData.FilterType.NONE,
                           null,
                           ChannelMetaData.CacheType.NONE,
                           false,
                           true );
    final ChannelMetaData[] channels = new ChannelMetaData[]{ ch1, ch2 };

    final ChannelAddress address1 = new ChannelAddress( ch1.getChannelId(), ValueUtil.randomInt() );
    final ChannelAddress address2 = new ChannelAddress( ch1.getChannelId(), ValueUtil.randomInt() );
    final ChannelAddress address3 = new ChannelAddress( ch1.getChannelId(), ValueUtil.randomInt() );
    final ChannelAddress address4 = new ChannelAddress( ch2.getChannelId(), ValueUtil.randomInt() );

    final TestReplicantSessionManager sm = new TestReplicantSessionManager( channels );
    final ReplicantSession session = createSession( sm );

    // Unsubscribe from channel that was explicitly subscribed
    {
      EntityMessageCacheUtil.removeSessionChanges();
      with( session, () -> sm.subscribe( session, address1, null ) );
      with( session, () -> sm.subscribe( session, address2, null ) );
      with( session, () -> sm.subscribe( session, address4, null ) );

      //Rebind clears the state
      EntityMessageCacheUtil.removeSessionChanges();

      assertChannelActionCount( 0 );

      final ArrayList<Integer> rootIds = new ArrayList<>();
      rootIds.add( address1.rootId() );
      rootIds.add( address2.rootId() );
      //This next one is not subscribed
      rootIds.add( address3.rootId() );
      // This next one is for wrong channel so should be no-op
      rootIds.add( address4.rootId() );
      sm.bulkUnsubscribe( session, ch1.getChannelId(), rootIds );

      assertChannelActionCount( 2 );
      assertChannelAction( getChannelActions().get( 0 ), address1, ChannelAction.Action.REMOVE, null );
      assertChannelAction( getChannelActions().get( 1 ), address2, ChannelAction.Action.REMOVE, null );

      with( session, () -> assertNull( session.findSubscriptionEntry( address1 ) ) );
      with( session, () -> assertNull( session.findSubscriptionEntry( address2 ) ) );
    }
  }

  @Test
  public void bulkUnsubscribe_withSessionID()
    throws Exception
  {
    final ChannelMetaData ch1 =
      new ChannelMetaData( 0,
                           "C1",
                           42,
                           ChannelMetaData.FilterType.NONE,
                           null,
                           ChannelMetaData.CacheType.NONE,
                           false,
                           true );
    final ChannelMetaData ch2 =
      new ChannelMetaData( 1,
                           "C2",
                           42,
                           ChannelMetaData.FilterType.NONE,
                           null,
                           ChannelMetaData.CacheType.NONE,
                           false,
                           true );
    final ChannelMetaData[] channels = new ChannelMetaData[]{ ch1, ch2 };

    final ChannelAddress address1 = new ChannelAddress( ch1.getChannelId(), ValueUtil.randomInt() );
    final ChannelAddress address2 = new ChannelAddress( ch1.getChannelId(), ValueUtil.randomInt() );
    final ChannelAddress address3 = new ChannelAddress( ch1.getChannelId(), ValueUtil.randomInt() );
    final ChannelAddress address4 = new ChannelAddress( ch2.getChannelId(), ValueUtil.randomInt() );

    final TestReplicantSessionManager sm = new TestReplicantSessionManager( channels );
    final ReplicantSession session = createSession( sm );

    // Unsubscribe from channel that was explicitly subscribed
    {
      EntityMessageCacheUtil.removeSessionChanges();
      with( session, () -> sm.subscribe( session, address1, null ) );
      with( session, () -> sm.subscribe( session, address2, null ) );
      with( session, () -> sm.subscribe( session, address4, null ) );

      //Rebind clears the state
      EntityMessageCacheUtil.removeSessionChanges();

      assertChannelActionCount( 0 );

      final ArrayList<Integer> rootIds = new ArrayList<>();
      rootIds.add( address1.rootId() );
      rootIds.add( address2.rootId() );
      //This next one is not subscribed
      rootIds.add( address3.rootId() );
      // This next one is for wrong channel so should be no-op
      rootIds.add( address4.rootId() );
      //sm.setupRegistryContext( sessionId );
      sm.bulkUnsubscribe( session, ch1.getChannelId(), rootIds );

      assertChannelActionCount( 2 );
      assertChannelAction( getChannelActions().get( 0 ), address1, ChannelAction.Action.REMOVE, null );
      assertChannelAction( getChannelActions().get( 1 ), address2, ChannelAction.Action.REMOVE, null );

      with( session, () -> assertNull( session.findSubscriptionEntry( address1 ) ) );
      with( session, () -> assertNull( session.findSubscriptionEntry( address2 ) ) );
    }
  }

  @Test
  public void updateSubscription()
    throws Exception
  {
    final ChannelMetaData ch =
      new ChannelMetaData( 0,
                           "C2",
                           null,
                           ChannelMetaData.FilterType.DYNAMIC,
                           j -> null,
                           ChannelMetaData.CacheType.NONE,
                           false,
                           true );
    final ChannelMetaData[] channels = new ChannelMetaData[]{ ch };

    final ChannelAddress cd = new ChannelAddress( ch.getChannelId(), null );

    final TestReplicantSessionManager sm = new TestReplicantSessionManager( channels );
    final ReplicantSession session = createSession( sm );

    final TestFilter originalFilter = new TestFilter( 41 );
    final TestFilter filter = new TestFilter( 42 );

    EntityMessageCacheUtil.removeSessionChanges();

    with( session, () -> sm.subscribe( session, cd, originalFilter ) );
    EntityMessageCacheUtil.removeSessionChanges();

    TransactionSynchronizationRegistryUtil.lookup()
      .putResource( ServerConstants.REQUEST_ID_KEY, ValueUtil.randomInt() );
    // Attempt to update to same filter - should be a noop
    with( session, () -> sm.subscribe( session, cd, originalFilter ) );

    final SubscriptionEntry e1 = with( session, () -> session.getSubscriptionEntry( cd ) );
    assertEntry( e1, true, 0, 0, originalFilter );

    assertChannelActionCount( 0 );
    assertSessionChangesCount( 0 );

    with( session, () -> sm.subscribe( session, cd, filter ) );

    assertEntry( e1, true, 0, 0, filter );

    assertChannelActionCount( 1 );
    assertSessionChangesCount( 1 );
  }

  @Test
  public void bulkSubscribe_forUpdate()
    throws Exception
  {
    final ChannelMetaData ch1 =
      new ChannelMetaData( 0,
                           "C1",
                           42,
                           ChannelMetaData.FilterType.DYNAMIC,
                           j -> null,
                           ChannelMetaData.CacheType.NONE,
                           false,
                           true );
    final ChannelMetaData[] channels = new ChannelMetaData[]{ ch1 };

    final ChannelAddress address1 = new ChannelAddress( ch1.getChannelId(), ValueUtil.randomInt() );
    final ChannelAddress address2 = new ChannelAddress( ch1.getChannelId(), ValueUtil.randomInt() );

    final TestReplicantSessionManager sm = new TestReplicantSessionManager( channels );
    final ReplicantSession session = createSession( sm );

    final TestFilter originalFilter = new TestFilter( 41 );
    final TestFilter filter = new TestFilter( 42 );

    final SubscriptionEntry e1 = session.createSubscriptionEntry( address1 );
    with( session, () -> e1.setFilter( originalFilter ) );
    final SubscriptionEntry e2 = session.createSubscriptionEntry( address2 );
    with( session, () -> e2.setFilter( originalFilter ) );

    final ArrayList<Integer> rootIds = new ArrayList<>();
    rootIds.add( address1.rootId() );
    rootIds.add( address2.rootId() );

    TransactionSynchronizationRegistryUtil.lookup()
      .putResource( ServerConstants.REQUEST_ID_KEY, ValueUtil.randomInt() );

    sm.bulkSubscribe( session, ch1.getChannelId(), rootIds, originalFilter );

    EntityMessageCacheUtil.removeSessionChanges();

    assertChannelActionCount( 0 );
    assertEntry( e1, true, 0, 0, originalFilter );
    assertEntry( e2, true, 0, 0, originalFilter );

    // Attempt to update to same filter - should be a noop
    sm.bulkSubscribe( session, ch1.getChannelId(), rootIds, originalFilter );

    assertEntry( e1, true, 0, 0, originalFilter );
    assertEntry( e2, true, 0, 0, originalFilter );

    assertChannelActionCount( 0 );
    assertSessionChangesCount( 0 );

    // Attempt to update no channels - should be noop
    sm.bulkSubscribe( session, ch1.getChannelId(), new ArrayList<>(), filter );

    assertEntry( e1, true, 0, 0, originalFilter );
    assertEntry( e2, true, 0, 0, originalFilter );

    assertChannelActionCount( 0 );
    assertSessionChangesCount( 0 );

    // Attempt to update both channels
    sm.bulkSubscribe( session, ch1.getChannelId(), rootIds, filter );

    assertEntry( e1, true, 0, 0, filter );
    assertEntry( e2, true, 0, 0, filter );

    assertChannelActionCount( 2 );
    assertSessionChangesCount( 1 );

    //Clear counts
    EntityMessageCacheUtil.removeSessionChanges();

    //Set original filter so next action updates this one again
    with( session, () -> e2.setFilter( originalFilter ) );

    // Attempt to update one channels
    sm.bulkSubscribe( session, ch1.getChannelId(), rootIds, filter );

    assertEntry( e1, true, 0, 0, filter );
    assertEntry( e2, true, 0, 0, filter );

    assertChannelActionCount( 1 );
    assertSessionChangesCount( 1 );
  }

  @Test
  public void bulkSubscribe_ForUpdate_whereBulkUpdateHookIsUsed()
    throws Exception
  {
    final ChannelMetaData ch1 =
      new ChannelMetaData( 0,
                           "C1",
                           42,
                           ChannelMetaData.FilterType.DYNAMIC,
                           j -> null,
                           ChannelMetaData.CacheType.NONE,
                           true,
                           true );
    final ChannelMetaData[] channels = new ChannelMetaData[]{ ch1 };

    final ChannelAddress address1 = new ChannelAddress( ch1.getChannelId(), ValueUtil.randomInt() );
    final ChannelAddress address2 = new ChannelAddress( ch1.getChannelId(), ValueUtil.randomInt() );

    final TestReplicantSessionManager sm = new TestReplicantSessionManager( channels );
    final ReplicantSession session = createSession( sm );

    final TestFilter originalFilter = new TestFilter( 41 );
    final TestFilter filter = new TestFilter( 42 );

    final SubscriptionEntry e1 = session.createSubscriptionEntry( address1 );
    with( session, () -> e1.setFilter( originalFilter ) );
    final SubscriptionEntry e2 = session.createSubscriptionEntry( address2 );
    with( session, () -> e2.setFilter( originalFilter ) );

    final ArrayList<Integer> rootIds = new ArrayList<>();
    rootIds.add( address1.rootId() );
    rootIds.add( address2.rootId() );

    EntityMessageCacheUtil.removeSessionChanges();

    assertChannelActionCount( 0 );
    assertEntry( e1, false, 0, 0, originalFilter );
    assertEntry( e2, false, 0, 0, originalFilter );

    sm.markAsBulkCollectDataForSubscriptionUpdate();

    assertEquals( sm.getBulkCollectDataForSubscriptionUpdateCallCount(), 0 );

    // Attempt to update both channels
    sm.bulkSubscribe( session, ch1.getChannelId(), rootIds, filter );

    // the original filter is still set as it is expected that the hook method does the magic
    assertEntry( e1, false, 0, 0, originalFilter );
    assertEntry( e2, false, 0, 0, originalFilter );

    // These are 0 as expected the hook to do magic
    assertChannelActionCount( 0 );
    assertSessionChangesCount( 0 );
  }

  @Test
  public void linkSubscriptionEntries()
    throws Exception
  {
    final ChannelMetaData ch1 =
      new ChannelMetaData( 0,
                           "Roster",
                           null,
                           ChannelMetaData.FilterType.DYNAMIC,
                           j -> null,
                           ChannelMetaData.CacheType.NONE,
                           false,
                           true );
    final ChannelMetaData ch2 =
      new ChannelMetaData( 1,
                           "Resource",
                           42,
                           ChannelMetaData.FilterType.NONE,
                           null,
                           ChannelMetaData.CacheType.NONE,
                           false,
                           true );
    final ChannelMetaData[] channels = new ChannelMetaData[]{ ch1, ch2 };

    final ChannelAddress address1 = new ChannelAddress( ch1.getChannelId(), null );
    final ChannelAddress address2a = new ChannelAddress( ch2.getChannelId(), ValueUtil.randomInt() );
    final ChannelAddress address2b = new ChannelAddress( ch2.getChannelId(), ValueUtil.randomInt() );

    final TestReplicantSessionManager sm = new TestReplicantSessionManager( channels );
    final ReplicantSession session = createSession( sm );

    final SubscriptionEntry se1 = session.createSubscriptionEntry( address1 );
    final SubscriptionEntry se2a = session.createSubscriptionEntry( address2a );
    final SubscriptionEntry se2b = session.createSubscriptionEntry( address2b );

    assertEntry( se1, false, 0, 0, null );
    assertEntry( se2a, false, 0, 0, null );
    assertEntry( se2b, false, 0, 0, null );

    with( session, () -> sm.linkSubscriptionEntries( se1, se2a ) );

    assertEntry( se1, false, 0, 1, null );
    assertEntry( se2a, false, 1, 0, null );
    assertEntry( se2b, false, 0, 0, null );
    assertTrue( se1.getOutwardSubscriptions().contains( address2a ) );
    assertTrue( se2a.getInwardSubscriptions().contains( address1 ) );

    with( session, () -> sm.linkSubscriptionEntries( se2a, se2b ) );

    assertEntry( se1, false, 0, 1, null );
    assertEntry( se2a, false, 1, 1, null );
    assertEntry( se2b, false, 1, 0, null );
    assertTrue( se2a.getOutwardSubscriptions().contains( address2b ) );
    assertTrue( se2b.getInwardSubscriptions().contains( address2a ) );

    with( session, () -> sm.delinkSubscriptionEntries( se2a, se2b ) );

    assertEntry( se1, false, 0, 1, null );
    assertEntry( se2a, false, 1, 0, null );
    assertEntry( se2b, false, 0, 0, null );
    assertFalse( se2a.getOutwardSubscriptions().contains( address2b ) );
    assertFalse( se2b.getInwardSubscriptions().contains( address2a ) );

    //Duplicate delink - noop
    with( session, () -> sm.delinkSubscriptionEntries( se2a, se2b ) );

    assertEntry( se1, false, 0, 1, null );
    assertEntry( se2a, false, 1, 0, null );
    assertEntry( se2b, false, 0, 0, null );
    assertFalse( se2a.getOutwardSubscriptions().contains( address2b ) );
    assertFalse( se2b.getInwardSubscriptions().contains( address2a ) );

    with( session, () -> sm.delinkSubscriptionEntries( se1, se2a ) );

    assertEntry( se1, false, 0, 0, null );
    assertEntry( se2a, false, 0, 0, null );
    assertEntry( se2b, false, 0, 0, null );
    assertFalse( se1.getOutwardSubscriptions().contains( address2a ) );
    assertFalse( se2a.getInwardSubscriptions().contains( address1 ) );
  }

  @Test
  public void newReplicantSession()
  {
    final TestReplicantSessionManager sm = new TestReplicantSessionManager();
    final ReplicantSession session = createSession( sm );

    assertTrue( session.getId().matches( "[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}" ) );
  }

  @Test
  public void sendPacket()
    throws Exception
  {
    final TestReplicantSessionManager sm = new TestReplicantSessionManager();
    final ReplicantSession session = createSession( sm );

    sm.getRegistry().putResource( ServerConstants.REQUEST_ID_KEY, 1 );

    final ChangeSet changeSet = new ChangeSet();
    with( session, () -> sm.queueCachedChangeSet( session, "X", changeSet ) );

    verify( sm.getReplicantMessageBroker() )
      .queueChangeMessage( eq( session ),
                           eq( true ),
                           eq( 1 ),
                           isNull(),
                           eq( "X" ),
                           eq( Collections.emptyList() ),
                           eq( changeSet ) );
  }

  @Test
  public void tryGetCacheEntry()
  {
    final ChannelMetaData ch1 = new ChannelMetaData( 0,
                                                     "C1",
                                                     null,
                                                     ChannelMetaData.FilterType.NONE,
                                                     null,
                                                     ChannelMetaData.CacheType.INTERNAL,
                                                     false,
                                                     true );
    final ChannelMetaData[] channels = new ChannelMetaData[]{ ch1 };

    final ChannelAddress address1 = new ChannelAddress( ch1.getChannelId(), null );

    final TestReplicantSessionManager sm = new TestReplicantSessionManager( channels );

    final String cacheKey = ValueUtil.randomString();
    sm.setCacheKey( cacheKey );

    assertFalse( sm.getCacheEntry( address1 ).isInitialized() );

    final ChannelCacheEntry entry = sm.tryGetCacheEntry( address1 );

    assertNotNull( entry );
    assertTrue( entry.isInitialized() );
    assertEquals( entry.getDescriptor(), address1 );
    assertEquals( entry.getCacheKey(), cacheKey );
    assertEquals( entry.getChangeSet().getChanges().size(), 1 );
  }

  @Test
  public void tryGetCacheEntry_entryNotCached()
  {
    final ChannelMetaData ch1 = new ChannelMetaData( 0,
                                                     "C1",
                                                     null,
                                                     ChannelMetaData.FilterType.NONE,
                                                     null,
                                                     ChannelMetaData.CacheType.INTERNAL,
                                                     false,
                                                     true );
    final ChannelMetaData[] channels = new ChannelMetaData[]{ ch1 };

    final ChannelAddress address1 = new ChannelAddress( ch1.getChannelId(), null );

    final TestReplicantSessionManager sm = new TestReplicantSessionManager( channels );

    final String cacheKey = ValueUtil.randomString();
    sm.setCacheKey( cacheKey );

    assertFalse( sm.getCacheEntry( address1 ).isInitialized() );

    final ChannelCacheEntry entry = sm.tryGetCacheEntry( address1 );

    assertNotNull( entry );
    assertTrue( entry.isInitialized() );
    assertEquals( entry.getDescriptor(), address1 );
    assertEquals( entry.getCacheKey(), cacheKey );
    assertEquals( entry.getChangeSet().getChanges().size(), 1 );
  }

  @Test
  public void tryGetCacheEntry_entryNotCached_instanceDeleted()
  {
    final ChannelMetaData ch1 = new ChannelMetaData( 0,
                                                     "C1",
                                                     null,
                                                     ChannelMetaData.FilterType.NONE,
                                                     null,
                                                     ChannelMetaData.CacheType.INTERNAL,
                                                     false,
                                                     true );
    final ChannelMetaData[] channels = new ChannelMetaData[]{ ch1 };

    final ChannelAddress address1 = new ChannelAddress( ch1.getChannelId(), null );

    final TestReplicantSessionManager sm = new TestReplicantSessionManager( channels );

    sm.setCacheKey( ValueUtil.randomString() );
    sm.markChannelRootAsDeleted();

    assertFalse( sm.getCacheEntry( address1 ).isInitialized() );

    final ChannelCacheEntry entry = sm.tryGetCacheEntry( address1 );

    assertNull( entry );
  }

  private void assertEntry( @Nonnull final SubscriptionEntry entry,
                            final boolean explicitlySubscribed,
                            final int inwardCount,
                            final int outwardCount,
                            @Nullable final Object filter )
  {
    assertEquals( entry.isExplicitlySubscribed(), explicitlySubscribed );
    assertEquals( entry.getInwardSubscriptions().size(), inwardCount );
    assertEquals( entry.getOutwardSubscriptions().size(), outwardCount );
    assertEquals( entry.getFilter(), filter );
  }

  private Collection<Change> getChanges()
  {
    return EntityMessageCacheUtil.getSessionChanges().getChanges();
  }

  private void assertSessionChangesCount( final int sessionChangesCount )
  {
    assertEquals( getChanges().size(), sessionChangesCount );
  }

  @Nonnull
  private List<ChannelAction> getChannelActions()
  {
    return EntityMessageCacheUtil.getSessionChanges().getChannelActions();
  }

  private void assertChannelAction( @Nonnull final ChannelAction channelAction,
                                    @Nonnull final ChannelAddress address,
                                    @Nonnull final ChannelAction.Action action,
                                    @Nullable final String filterAsString )
  {
    assertEquals( channelAction.action(), action );
    assertEquals( channelAction.address(), address );
    if ( null == filterAsString )
    {
      assertNull( channelAction.filter() );
    }
    else
    {
      assertNotNull( channelAction.filter() );
      assertEquals( channelAction.filter().toString(), filterAsString );
    }
  }

  private void assertChannelActionCount( final int channelActionCount )
  {
    assertEquals( getChannelActions().size(), channelActionCount );
  }

  static class TestReplicantSessionManager
    extends ReplicantSessionManagerImpl
  {
    private final SchemaMetaData _schemaMetaData;
    @Nonnull
    private final ReplicantMessageBroker _broker = mock( ReplicantMessageBroker.class );
    private ChannelAddress _followSource;
    private String _cacheKey;
    private int _bulkCollectDataForSubscriptionUpdateCallCount;
    private boolean _channelRootDeleted;

    private TestReplicantSessionManager()
    {
      this( new SchemaMetaData( ValueUtil.randomString() ) );
    }

    private TestReplicantSessionManager( final ChannelMetaData[] channels )
    {
      this( new SchemaMetaData( ValueUtil.randomString(), channels ) );
    }

    private TestReplicantSessionManager( final SchemaMetaData schemaMetaData )
    {
      _schemaMetaData = schemaMetaData;
    }

    int getBulkCollectDataForSubscriptionUpdateCallCount()
    {
      return _bulkCollectDataForSubscriptionUpdateCallCount;
    }

    void markAsBulkCollectDataForSubscriptionUpdate()
    {
    }

    void markChannelRootAsDeleted()
    {
      _channelRootDeleted = true;
    }

    private void setCacheKey( final String cacheKey )
    {
      _cacheKey = cacheKey;
    }

    @Nonnull
    @Override
    public SchemaMetaData getSchemaMetaData()
    {
      return _schemaMetaData;
    }

    @Nonnull
    @Override
    protected ReplicantMessageBroker getReplicantMessageBroker()
    {
      return _broker;
    }

    @Override
    protected void bulkCollectDataForSubscriptionUpdate( @Nonnull final ReplicantSession session,
                                                         @Nonnull final List<ChannelAddress> addresses,
                                                         @Nullable final Object originalFilter,
                                                         @Nullable final Object filter,
                                                         @Nonnull final ChangeSet changeSet,
                                                         final boolean isExplicitSubscribe )
    {
      _bulkCollectDataForSubscriptionUpdateCallCount += 1;
    }

    @Nonnull
    @Override
    protected SubscribeResult collectDataForSubscribe( @Nonnull final ChannelAddress descriptor,
                                                       @Nonnull final ChangeSet changeSet,
                                                       @Nullable final Object filter )
    {
      if ( !_channelRootDeleted )
      {
        final HashMap<String, Serializable> routingKeys = new HashMap<>();
        final HashMap<String, Serializable> attributes = new HashMap<>();
        attributes.put( "ID", 79 );
        final EntityMessage message = new EntityMessage( 79, 1, 0, routingKeys, attributes, null );
        changeSet.merge( new Change( message, descriptor.channelId(), descriptor.rootId() ) );
        return new SubscribeResult( false, _cacheKey );
      }
      else
      {
        return new SubscribeResult( true, null );
      }
    }

    @Override
    protected void collectDataForSubscriptionUpdate( @Nonnull final ReplicantSession session,
                                                     @Nonnull final ChannelAddress descriptor,
                                                     @Nonnull final ChangeSet changeSet,
                                                     @Nullable final Object originalFilter,
                                                     @Nullable final Object filter )
    {
      final HashMap<String, Serializable> routingKeys = new HashMap<>();
      final HashMap<String, Serializable> attributes = new HashMap<>();
      attributes.put( "ID", 78 );
      //Ugly hack to pass back filters
      attributes.put( "OriginalFilter", (Serializable) originalFilter );
      attributes.put( "Filter", (Serializable) filter );
      final EntityMessage message = new EntityMessage( 78, 1, 0, routingKeys, attributes, null );
      changeSet.merge( new Change( message, descriptor.channelId(), descriptor.rootId() ) );
    }

    private void setFollowSource( final ChannelAddress followSource )
    {
      _followSource = followSource;
    }

    @Override
    protected boolean shouldFollowLink( @Nonnull final SubscriptionEntry sourceEntry,
                                        @Nonnull final ChannelAddress target )
    {
      return null != _followSource && _followSource.equals( sourceEntry.address() );
    }

    @Nonnull
    @Override
    protected TransactionSynchronizationRegistry getRegistry()
    {
      return TransactionSynchronizationRegistryUtil.lookup();
    }
  }

  @Nonnull
  private ReplicantSession createSession( @Nonnull final TestReplicantSessionManager sm )
  {
    final Session webSocketSession = mock( Session.class );
    when( webSocketSession.getId() ).thenReturn( ValueUtil.randomString() );
    when( webSocketSession.isOpen() ).thenReturn( true );
    final RemoteEndpoint.Basic remote = mock( RemoteEndpoint.Basic.class );
    when( webSocketSession.getBasicRemote() ).thenReturn( remote );
    return sm.createSession( webSocketSession );
  }

  private <T> T with( @Nonnull final ReplicantSession session, @Nonnull final Callable<T> action )
    throws Exception
  {
    final ReentrantLock lock = session.getLock();
    lock.lock();
    try
    {
      return action.call();
    }
    finally
    {
      lock.unlock();
    }
  }

  private void with( @Nonnull final ReplicantSession session, @Nonnull final Action action )
    throws Exception
  {
    final ReentrantLock lock = session.getLock();
    lock.lock();
    try
    {
      action.run();
    }
    finally
    {
      lock.unlock();
    }
  }
}
