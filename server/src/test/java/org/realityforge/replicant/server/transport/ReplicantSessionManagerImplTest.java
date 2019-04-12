package org.realityforge.replicant.server.transport;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.locks.ReadWriteLock;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.transaction.TransactionSynchronizationRegistry;
import javax.websocket.RemoteEndpoint;
import javax.websocket.Session;
import org.realityforge.guiceyloops.server.AssertUtil;
import org.realityforge.guiceyloops.server.TestInitialContextFactory;
import org.realityforge.guiceyloops.shared.ValueUtil;
import org.realityforge.replicant.server.Change;
import org.realityforge.replicant.server.ChangeAccumulator;
import org.realityforge.replicant.server.ChangeSet;
import org.realityforge.replicant.server.ChannelAction;
import org.realityforge.replicant.server.ChannelAddress;
import org.realityforge.replicant.server.ChannelLink;
import org.realityforge.replicant.server.EntityMessage;
import org.realityforge.replicant.server.ServerConstants;
import org.realityforge.replicant.server.ee.EntityMessageCacheUtil;
import org.realityforge.replicant.server.ee.RegistryUtil;
import org.realityforge.replicant.server.ee.TransactionSynchronizationRegistryUtil;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

/**
 * TODO: Add tests for saveEntityMessages
 */
@SuppressWarnings( "Duplicates" )
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
    throws Exception
  {
    final TestReplicantSessionManager sm = new TestReplicantSessionManager();
    assertEquals( sm.getSessionIDs().size(), 0 );
    assertNull( sm.getSession( "MySessionID" ) );
    final ReplicantSession session = createSession( sm );
    assertNotNull( session );
    assertNull( session.getUserId() );
    assertNotNull( session.getId() );
    assertEquals( sm.getSessionIDs().size(), 1 );

    // Make sure we can also get it thorugh the map interface
    assertEquals( sm.getSessions().get( session.getId() ), session );

    // The next line should update the last accessed time too!
    assertEquals( sm.getSession( session.getId() ), session );

    assertTrue( sm.invalidateSession( session ) );
    assertEquals( sm.getSessionIDs().size(), 0 );
    assertFalse( sm.invalidateSession( session ) );
    assertNull( sm.getSession( session.getId() ) );
  }

  @Test
  public void removeClosedSessions_openWebSocketSession()
    throws Exception
  {
    final Session webSocketSession = mock( Session.class );
    final String id = ValueUtil.randomString();
    when( webSocketSession.getId() ).thenReturn( id );
    when( webSocketSession.isOpen() ).thenReturn( true );

    final ReplicantSessionManagerImpl sm = new TestReplicantSessionManager();
    final ReplicantSession session = sm.createSession( webSocketSession );

    assertEquals( sm.getSessions().get( session.getId() ), session );
    sm.removeClosedSessions();
    assertEquals( sm.getSessions().get( session.getId() ), session );
  }

  @Test
  public void removeClosedSessions_closedWebSocketSession()
    throws Exception
  {
    final Session webSocketSession = mock( Session.class );
    final String id = ValueUtil.randomString();
    when( webSocketSession.getId() ).thenReturn( id );
    when( webSocketSession.isOpen() ).thenReturn( false );

    final ReplicantSessionManagerImpl sm = new TestReplicantSessionManager();
    final ReplicantSession session = sm.createSession( webSocketSession );

    assertEquals( sm.getSessions().get( session.getId() ), session );
    sm.removeClosedSessions();
    assertNull( sm.getSessions().get( session.getId() ) );
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

  private CyclicBarrier go( final Runnable target )
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
    final ChannelMetaData ch1 = new ChannelMetaData( 0, "C1", true, ChannelMetaData.FilterType.NONE, null, true, true );
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
      new ChannelMetaData( 0, "C1", true, ChannelMetaData.FilterType.NONE, null, false, true );
    final ChannelMetaData ch2 =
      new ChannelMetaData( 1, "C2", true, ChannelMetaData.FilterType.DYNAMIC, String.class, false, true );
    final ChannelMetaData ch3 =
      new ChannelMetaData( 2, "C3", true, ChannelMetaData.FilterType.STATIC, String.class, false, true );
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

    assertNull( session.findSubscriptionEntry( address1 ) );

    // subscribe
    {
      EntityMessageCacheUtil.removeSessionChanges();
      sm.subscribe( session, address1, false, null, EntityMessageCacheUtil.getSessionChanges() );

      final SubscriptionEntry entry1 = session.findSubscriptionEntry( address1 );
      assertNotNull( entry1 );
      assertEntry( entry1, false, 0, 0, null );

      assertChannelActionCount( 1 );
      assertSessionChangesCount( 1 );
    }

    // re-subscribe- should be noop
    {
      EntityMessageCacheUtil.removeSessionChanges();
      sm.subscribe( session, address1, false, null, EntityMessageCacheUtil.getSessionChanges() );
      assertEntry( session.getSubscriptionEntry( address1 ), false, 0, 0, null );
      assertChannelActionCount( 0 );
      assertSessionChangesCount( 0 );
    }

    // re-subscribe explicitly - should only set explicit flag
    {
      EntityMessageCacheUtil.removeSessionChanges();
      sm.subscribe( session, address1, null );
      assertEntry( session.getSubscriptionEntry( address1 ), true, 0, 0, null );
      assertChannelActionCount( 0 );
      assertSessionChangesCount( 0 );
    }

    // subscribe when existing subscription present
    {
      EntityMessageCacheUtil.removeSessionChanges();
      sm.subscribe( session, address2, originalFilter );

      assertEntry( session.getSubscriptionEntry( address2 ), true, 0, 0, originalFilter );
      assertChannelActionCount( 1 );
      assertSessionChangesCount( 1 );

      EntityMessageCacheUtil.removeSessionChanges();

    TransactionSynchronizationRegistryUtil.lookup()
      .putResource( ServerConstants.REQUEST_ID_KEY, ValueUtil.randomInt() );

      //Should be a noop as same filter
      sm.subscribe( session, address2, originalFilter );

      assertEntry( session.getSubscriptionEntry( address2 ), true, 0, 0, originalFilter );
      assertChannelActionCount( 0 );
      assertSessionChangesCount( 0 );

      EntityMessageCacheUtil.removeSessionChanges();

      //Should be a filter update
      sm.subscribe( session, address2, filter );

      assertEntry( session.getSubscriptionEntry( address2 ), true, 0, 0, filter );
      assertChannelActionCount( 1 );
      assertSessionChangesCount( 1 );
    }

    //Subscribe and attempt to update static filter
    {
      EntityMessageCacheUtil.removeSessionChanges();
      sm.subscribe( session, address3, originalFilter );
      assertEntry( session.getSubscriptionEntry( address3 ), true, 0, 0, originalFilter );
      assertChannelActionCount( 1 );
      assertSessionChangesCount( 1 );

      EntityMessageCacheUtil.removeSessionChanges();

      //Should be a noop as same filter
      sm.subscribe( session, address3, originalFilter );

      assertEntry( session.getSubscriptionEntry( address3 ), true, 0, 0, originalFilter );
      assertChannelActionCount( 0 );
      assertSessionChangesCount( 0 );

      try
      {
        sm.subscribe( session, address3, filter );
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
    final ChannelMetaData ch1 = new ChannelMetaData( 0, "C1", true, ChannelMetaData.FilterType.NONE, null, true, true );
    final ChannelMetaData[] channels = new ChannelMetaData[]{ ch1 };

    final ChannelAddress address1 = new ChannelAddress( ch1.getChannelId(), null );

    final TestReplicantSessionManager sm = new TestReplicantSessionManager( channels );

    sm.setCacheKey( "X" );

    // subscribe - matching cacheKey
    {
      final ReplicantSession session = createSession( sm );
      final Session webSocketSession = session.getWebSocketSession();
      when( webSocketSession.isOpen() ).thenReturn( true );

      assertNull( session.findSubscriptionEntry( address1 ) );
      EntityMessageCacheUtil.removeSessionChanges();
      session.setETag( address1, "X" );

      sm.subscribe( session, address1, false, null, EntityMessageCacheUtil.getSessionChanges() );

      final SubscriptionEntry entry1 = session.findSubscriptionEntry( address1 );
      assertNotNull( entry1 );
      assertEntry( entry1, false, 0, 0, null );

      verify( webSocketSession.getBasicRemote() ).sendText( "{\"type\":\"use-cache\",\"channel\":\"0\",\"etag\":\"X\"}" );

      assertChannelActionCount( 0 );
      assertSessionChangesCount( 0 );
    }

    // subscribe - cacheKey differs
    {
      final ReplicantSession session = createSession( sm );
      assertNull( session.findSubscriptionEntry( address1 ) );
      EntityMessageCacheUtil.removeSessionChanges();
      session.setETag( address1, "Y" );
      sm.subscribe( session, address1, false, null, EntityMessageCacheUtil.getSessionChanges() );

      final SubscriptionEntry entry1 = session.findSubscriptionEntry( address1 );
      assertNotNull( entry1 );
      assertEntry( entry1, false, 0, 0, null );

      final RemoteEndpoint.Basic remote = session.getWebSocketSession().getBasicRemote();
      verify( remote ).sendText(
        "{\"type\":\"update\",\"etag\":\"X\",\"channels\":[\"+0\"],\"changes\":[{\"id\":\"1.79\",\"channels\":[\"0\"],\"data\":{\"ID\":79}}]}" );
    }
  }

  @Test
  public void subscribe_withSessionID()
    throws Exception
  {
    final ChannelMetaData ch1 =
      new ChannelMetaData( 0, "C1", true, ChannelMetaData.FilterType.NONE, null, false, true );
    final ChannelMetaData[] channels = new ChannelMetaData[]{ ch1 };

    final ChannelAddress address1 = new ChannelAddress( ch1.getChannelId(), null );

    final TestReplicantSessionManager sm = new TestReplicantSessionManager( channels );
    final ReplicantSession session = createSession( sm );

    assertNull( session.findSubscriptionEntry( address1 ) );

    sm.subscribe( session, address1, null );

    final SubscriptionEntry entry1 = session.findSubscriptionEntry( address1 );
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
    final ChannelMetaData ch1 = new ChannelMetaData( 0, "C1", true, ChannelMetaData.FilterType.NONE, null, true, true );
    final ChannelMetaData[] channels = new ChannelMetaData[]{ ch1 };

    final ChannelAddress address1 = new ChannelAddress( ch1.getChannelId(), null );

    final TestReplicantSessionManager sm = new TestReplicantSessionManager( channels );
    final ReplicantSession session = createSession( sm );

    sm.setCacheKey( "X" );

    assertNull( session.getETag( address1 ) );

    assertNull( session.findSubscriptionEntry( address1 ) );

    session.setETag( address1, "X" );
    sm.subscribe( session, address1, null );

    assertEquals( session.getETag( address1 ), "X" );

    final SubscriptionEntry entry1 = session.findSubscriptionEntry( address1 );
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
    final ChannelMetaData ch1 = new ChannelMetaData( 0, "C1", true, ChannelMetaData.FilterType.NONE, null, true, true );
    final ChannelMetaData[] channels = new ChannelMetaData[]{ ch1 };

    final ChannelAddress address1 = new ChannelAddress( ch1.getChannelId(), null );

    final TestReplicantSessionManager sm = new TestReplicantSessionManager( channels );
    final ReplicantSession session = createSession( sm );

    sm.setCacheKey( "X" );

    assertNull( session.getETag( address1 ) );

    assertNull( session.findSubscriptionEntry( address1 ) );

    session.setETag( address1, "Y" );
    sm.subscribe( session, address1, null );

    assertNull( session.getETag( address1 ) );

    final SubscriptionEntry entry1 = session.findSubscriptionEntry( address1 );
    assertNotNull( entry1 );
    assertEntry( entry1, true, 0, 0, null );

    final RemoteEndpoint.Basic remote = session.getWebSocketSession().getBasicRemote();
    verify( remote ).sendText(
      "{\"type\":\"update\",\"etag\":\"X\",\"channels\":[\"+0\"],\"changes\":[{\"id\":\"1.79\",\"channels\":[\"0\"],\"data\":{\"ID\":79}}]}" );
  }

  @Test
  public void performSubscribe()
    throws Exception
  {
    final ChannelMetaData ch1 =
      new ChannelMetaData( 0, "C1", true, ChannelMetaData.FilterType.NONE, null, false, true );
    final ChannelMetaData ch2 =
      new ChannelMetaData( 1, "C2", true, ChannelMetaData.FilterType.DYNAMIC, String.class, false, true );
    final ChannelMetaData ch3 =
      new ChannelMetaData( 2, "C3", false, ChannelMetaData.FilterType.NONE, null, false, true );
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

      sm.performSubscribe( session, e1, true, null, EntityMessageCacheUtil.getSessionChanges() );

      assertEntry( e1, true, 0, 0, null );

      final LinkedList<ChannelAction> actions = getChannelActions();
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

      sm.performSubscribe( session, e1, true, filter, EntityMessageCacheUtil.getSessionChanges() );

      assertEntry( e1, true, 0, 0, filter );

      final LinkedList<ChannelAction> actions = getChannelActions();
      assertEquals( actions.size(), 1 );
      assertChannelAction( actions.get( 0 ), address2, ChannelAction.Action.ADD, "{\"myField\":42}" );

      // 1 Change comes from collectDataForSubscribe
      final Collection<Change> changes = getChanges();
      assertEquals( changes.size(), 1 );
      assertEquals( changes.iterator().next().getEntityMessage().getId(), 79 );

      assertEntry( e1, true, 0, 0, filter );

      session.deleteSubscriptionEntry( e1 );
    }

    // Root instance is deleted
    {
      final SubscriptionEntry e1 = session.createSubscriptionEntry( address3 );
      //Rebind clears the state
      EntityMessageCacheUtil.removeSessionChanges();

      assertChannelActionCount( 0 );
      assertEntry( e1, false, 0, 0, null );

      sm.markChannelRootAsDeleted();

      sm.performSubscribe( session, e1, true, null, EntityMessageCacheUtil.getSessionChanges() );

      assertEntry( e1, false, 0, 0, null );

      final LinkedList<ChannelAction> actions = getChannelActions();
      assertEquals( actions.size(), 1 );
      assertChannelAction( actions.get( 0 ), address3, ChannelAction.Action.DELETE, null );

      assertEquals( getChanges().size(), 0 );
    }
  }

  @Test
  public void performSubscribe_withCaching()
    throws Exception
  {
    final ChannelMetaData ch1 = new ChannelMetaData( 0, "C1", true, ChannelMetaData.FilterType.NONE, null, true, true );
    final ChannelMetaData ch2 =
      new ChannelMetaData( 1, "C2", false, ChannelMetaData.FilterType.NONE, null, true, true );
    final ChannelMetaData[] channels = new ChannelMetaData[]{ ch1, ch2 };

    final ChannelAddress address1 = new ChannelAddress( ch1.getChannelId() );
    final ChannelAddress address2 = new ChannelAddress( ch2.getChannelId(), 1 );

    final TestReplicantSessionManager sm = new TestReplicantSessionManager( channels );

    sm.setCacheKey( "X" );

    //Locally cached
    {
      sm.deleteAllCacheEntries();
      final ReplicantSession session = createSession( sm );
      session.setETag( address1, "X" );
      final SubscriptionEntry e1 = session.createSubscriptionEntry( address1 );
      //Rebind clears the state
      EntityMessageCacheUtil.removeSessionChanges();

      assertChannelActionCount( 0 );
      assertEntry( e1, false, 0, 0, null );

      sm.performSubscribe( session, e1, true, null, EntityMessageCacheUtil.getSessionChanges() );

      assertEntry( e1, true, 0, 0, null );

      assertChannelActionCount( 0 );
      assertSessionChangesCount( 0 );

      verify( session.getWebSocketSession().getBasicRemote() ).sendText( "{\"type\":\"use-cache\",\"channel\":\"0\",\"etag\":\"X\"}" );
    }

    //Locally cached but an old version
    {
      sm.deleteAllCacheEntries();
      final ReplicantSession session = createSession( sm );
      session.setETag( address1, "NOT" + sm._cacheKey );
      final SubscriptionEntry e1 = session.createSubscriptionEntry( address1 );
      //Rebind clears the state
      EntityMessageCacheUtil.removeSessionChanges();

      assertChannelActionCount( 0 );
      assertEntry( e1, false, 0, 0, null );

      sm.performSubscribe( session, e1, true, null, EntityMessageCacheUtil.getSessionChanges() );

      assertEntry( e1, true, 0, 0, null );

      final RemoteEndpoint.Basic remote = session.getWebSocketSession().getBasicRemote();
      verify( remote ).sendText(
        "{\"type\":\"update\",\"etag\":\"X\",\"channels\":[\"+0\"],\"changes\":[{\"id\":\"1.79\",\"channels\":[\"0\"],\"data\":{\"ID\":79}}]}" );
    }

    //Not cached locally
    {
      sm.deleteAllCacheEntries();
      final ReplicantSession session = createSession( sm );
      final SubscriptionEntry e1 = session.createSubscriptionEntry( address1 );
      //Rebind clears the state
      EntityMessageCacheUtil.removeSessionChanges();

      assertChannelActionCount( 0 );
      assertEntry( e1, false, 0, 0, null );

      sm.performSubscribe( session, e1, true, null, EntityMessageCacheUtil.getSessionChanges() );

      assertEntry( e1, true, 0, 0, null );

      final RemoteEndpoint.Basic remote = session.getWebSocketSession().getBasicRemote();
      verify( remote ).sendText(
        "{\"type\":\"update\",\"etag\":\"X\",\"channels\":[\"+0\"],\"changes\":[{\"id\":\"1.79\",\"channels\":[\"0\"],\"data\":{\"ID\":79}}]}" );
    }

    //Locally cached but deleted
    {
      sm.deleteAllCacheEntries();
      final ReplicantSession session = createSession( sm );
      session.setETag( address1, "X" );
      final SubscriptionEntry e1 = session.createSubscriptionEntry( address2 );
      //Rebind clears the state
      EntityMessageCacheUtil.removeSessionChanges();

      assertChannelActionCount( 0 );
      assertEntry( e1, false, 0, 0, null );

      sm.markChannelRootAsDeleted();

      sm.performSubscribe( session, e1, true, null, EntityMessageCacheUtil.getSessionChanges() );

      assertEntry( e1, false, 0, 0, null );

      assertChannelActionCount( 0 );
      assertSessionChangesCount( 0 );
    }
  }

  @Test
  public void performUnsubscribe()
    throws Exception
  {
    final ChannelMetaData ch1 =
      new ChannelMetaData( 0, "C1", false, ChannelMetaData.FilterType.NONE, null, false, true );
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
      sm.subscribe( session, address1, null );
      final SubscriptionEntry entry = session.getSubscriptionEntry( address1 );

      //Rebind clears the state
      EntityMessageCacheUtil.removeSessionChanges();

      assertChannelActionCount( 0 );

      sm.performUnsubscribe( session, entry, true, false, EntityMessageCacheUtil.getSessionChanges() );

      assertChannelActionCount( 1 );
      assertChannelAction( getChannelActions().get( 0 ), address1, ChannelAction.Action.REMOVE, null );

      assertNull( session.findSubscriptionEntry( address1 ) );
    }

    // implicit unsubscribe from channel that was implicitly subscribed should leave explicit subscription
    {
      EntityMessageCacheUtil.removeSessionChanges();
      sm.subscribe( session, address1, null );
      final SubscriptionEntry entry = session.getSubscriptionEntry( address1 );

      //Rebind clears the state
      EntityMessageCacheUtil.removeSessionChanges();

      assertChannelActionCount( 0 );

      sm.performUnsubscribe( session, entry, false, false, EntityMessageCacheUtil.getSessionChanges() );

      assertChannelActionCount( 0 );

      assertNotNull( session.findSubscriptionEntry( address1 ) );

      sm.performUnsubscribe( session, entry, true, false, EntityMessageCacheUtil.getSessionChanges() );

      assertChannelActionCount( 1 );

      assertNull( session.findSubscriptionEntry( address1 ) );
    }

    // implicit unsubscribe from channel that was implicitly subscribed should delete subscription
    {
      EntityMessageCacheUtil.removeSessionChanges();
      sm.subscribe( session, address1, false, null, EntityMessageCacheUtil.getSessionChanges() );
      final SubscriptionEntry entry = session.getSubscriptionEntry( address1 );

      //Rebind clears the state
      EntityMessageCacheUtil.removeSessionChanges();

      assertChannelActionCount( 0 );

      sm.performUnsubscribe( session, entry, false, false, EntityMessageCacheUtil.getSessionChanges() );

      assertChannelActionCount( 1 );
      assertChannelAction( getChannelActions().get( 0 ), address1, ChannelAction.Action.REMOVE, null );

      assertNull( session.findSubscriptionEntry( address1 ) );
    }

    // implicit unsubscribe from channel that was implicitly subscribed should leave subscription that
    // implicitly linked from elsewhere
    {
      EntityMessageCacheUtil.removeSessionChanges();
      sm.subscribe( session, address1, false, null, EntityMessageCacheUtil.getSessionChanges() );
      final SubscriptionEntry entry = session.getSubscriptionEntry( address1 );

      sm.subscribe( session, address2, false, null, EntityMessageCacheUtil.getSessionChanges() );
      final SubscriptionEntry entry2 = session.getSubscriptionEntry( address2 );
      sm.linkSubscriptionEntries( entry2, entry );

      //Rebind clears the state
      EntityMessageCacheUtil.removeSessionChanges();

      assertChannelActionCount( 0 );

      sm.performUnsubscribe( session, entry, false, false, EntityMessageCacheUtil.getSessionChanges() );

      assertChannelActionCount( 0 );
      assertNotNull( session.findSubscriptionEntry( address1 ) );

      sm.delinkSubscriptionEntries( entry2, entry );

      sm.performUnsubscribe( session, entry, false, false, EntityMessageCacheUtil.getSessionChanges() );

      assertChannelActionCount( 1 );
      assertChannelAction( getChannelActions().get( 0 ), address1, ChannelAction.Action.REMOVE, null );
      assertNull( session.findSubscriptionEntry( address1 ) );
    }

    // Unsubscribe also results in unsubscribe for all downstream channels
    {
      EntityMessageCacheUtil.removeSessionChanges();
      sm.subscribe( session, address1, null );
      sm.subscribe( session, address2, false, null, EntityMessageCacheUtil.getSessionChanges() );
      sm.subscribe( session, address3, false, null, EntityMessageCacheUtil.getSessionChanges() );
      sm.subscribe( session, address4, false, null, EntityMessageCacheUtil.getSessionChanges() );
      sm.subscribe( session, address5, false, null, EntityMessageCacheUtil.getSessionChanges() );

      final SubscriptionEntry entry = session.getSubscriptionEntry( address1 );
      final SubscriptionEntry entry2 = session.getSubscriptionEntry( address2 );
      final SubscriptionEntry entry3 = session.getSubscriptionEntry( address3 );
      final SubscriptionEntry entry4 = session.getSubscriptionEntry( address4 );
      final SubscriptionEntry entry5 = session.getSubscriptionEntry( address5 );

      sm.linkSubscriptionEntries( entry, entry2 );
      sm.linkSubscriptionEntries( entry, entry3 );
      sm.linkSubscriptionEntries( entry3, entry4 );
      sm.linkSubscriptionEntries( entry4, entry5 );

      //Rebind clears the state
      EntityMessageCacheUtil.removeSessionChanges();

      assertChannelActionCount( 0 );

      sm.performUnsubscribe( session, entry, true, false, EntityMessageCacheUtil.getSessionChanges() );

      assertChannelActionCount( 5 );
      for ( final ChannelAction action : getChannelActions() )
      {
        assertEquals( action.getAction(), ChannelAction.Action.REMOVE );
      }

      assertNull( session.findSubscriptionEntry( address1 ) );
      assertNull( session.findSubscriptionEntry( address2 ) );
      assertNull( session.findSubscriptionEntry( address3 ) );
      assertNull( session.findSubscriptionEntry( address4 ) );
      assertNull( session.findSubscriptionEntry( address5 ) );
    }
  }

  @Test
  public void unsubscribe()
    throws Exception
  {
    final ChannelMetaData ch1 =
      new ChannelMetaData( 0, "C1", false, ChannelMetaData.FilterType.NONE, null, false, true );
    final ChannelMetaData[] channels = new ChannelMetaData[]{ ch1 };

    final ChannelAddress address1 = new ChannelAddress( ch1.getChannelId(), ValueUtil.randomInt() );

    final TestReplicantSessionManager sm = new TestReplicantSessionManager( channels );
    final ReplicantSession session = createSession( sm );

    // Unsubscribe from channel that was explicitly subscribed
    {
      EntityMessageCacheUtil.removeSessionChanges();
      sm.subscribe( session, address1, null );
      final SubscriptionEntry entry = session.getSubscriptionEntry( address1 );

      //Rebind clears the state
      EntityMessageCacheUtil.removeSessionChanges();

      assertChannelActionCount( 0 );

      sm.unsubscribe( session, entry.getDescriptor(), EntityMessageCacheUtil.getSessionChanges() );

      assertChannelActionCount( 1 );
      assertChannelAction( getChannelActions().get( 0 ), address1, ChannelAction.Action.REMOVE, null );

      assertNull( session.findSubscriptionEntry( address1 ) );
    }

    // unsubscribe from unsubscribed
    {
      EntityMessageCacheUtil.removeSessionChanges();

      assertChannelActionCount( 0 );

      sm.unsubscribe( session, address1, EntityMessageCacheUtil.getSessionChanges() );

      assertChannelActionCount( 0 );
    }
  }

  @Test
  public void unsubscribe_usingSessionID()
    throws Exception
  {
    final ChannelMetaData ch1 =
      new ChannelMetaData( 0, "C1", false, ChannelMetaData.FilterType.NONE, null, false, true );
    final ChannelMetaData[] channels = new ChannelMetaData[]{ ch1 };

    final ChannelAddress address1 = new ChannelAddress( ch1.getChannelId(), ValueUtil.randomInt() );

    final TestReplicantSessionManager sm = new TestReplicantSessionManager( channels );
    final ReplicantSession session = createSession( sm );

    EntityMessageCacheUtil.removeSessionChanges();
    sm.subscribe( session, address1, null );

    EntityMessageCacheUtil.removeSessionChanges();

    assertChannelActionCount( 0 );

    sm.unsubscribe( session, address1, EntityMessageCacheUtil.getSessionChanges() );

    assertChannelActionCount( 1 );
    assertChannelAction( getChannelActions().get( 0 ), address1, ChannelAction.Action.REMOVE, null );

    assertNull( session.findSubscriptionEntry( address1 ) );
  }

  @Test
  public void bulkUnsubscribe()
  {
    final ChannelMetaData ch1 =
      new ChannelMetaData( 0, "C1", false, ChannelMetaData.FilterType.NONE, null, false, true );
    final ChannelMetaData ch2 =
      new ChannelMetaData( 1, "C2", false, ChannelMetaData.FilterType.NONE, null, false, true );
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
      sm.subscribe( session, address1, null );
      sm.subscribe( session, address2, null );
      sm.subscribe( session, address4, null );

      //Rebind clears the state
      EntityMessageCacheUtil.removeSessionChanges();

      assertChannelActionCount( 0 );

      final ArrayList<Integer> subChannelIds = new ArrayList<>();
      subChannelIds.add( address1.getSubChannelId() );
      subChannelIds.add( address2.getSubChannelId() );
      //This next one is not subscribed
      subChannelIds.add( address3.getSubChannelId() );
      // This next one is for wrong channel so should be no-op
      subChannelIds.add( address4.getSubChannelId() );
      sm.bulkUnsubscribe( session, ch1.getChannelId(), subChannelIds );

      assertChannelActionCount( 2 );
      assertChannelAction( getChannelActions().get( 0 ), address1, ChannelAction.Action.REMOVE, null );
      assertChannelAction( getChannelActions().get( 1 ), address2, ChannelAction.Action.REMOVE, null );

      assertNull( session.findSubscriptionEntry( address1 ) );
      assertNull( session.findSubscriptionEntry( address2 ) );
    }
  }

  @Test
  public void bulkUnsubscribe_withSessionID()
  {
    final ChannelMetaData ch1 =
      new ChannelMetaData( 0, "C1", false, ChannelMetaData.FilterType.NONE, null, false, true );
    final ChannelMetaData ch2 =
      new ChannelMetaData( 1, "C2", false, ChannelMetaData.FilterType.NONE, null, false, true );
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
      sm.subscribe( session, address1, null );
      sm.subscribe( session, address2, null );
      sm.subscribe( session, address4, null );

      //Rebind clears the state
      EntityMessageCacheUtil.removeSessionChanges();

      assertChannelActionCount( 0 );

      final ArrayList<Integer> subChannelIds = new ArrayList<>();
      subChannelIds.add( address1.getSubChannelId() );
      subChannelIds.add( address2.getSubChannelId() );
      //This next one is not subscribed
      subChannelIds.add( address3.getSubChannelId() );
      // This next one is for wrong channel so should be no-op
      subChannelIds.add( address4.getSubChannelId() );
      //sm.setupRegistryContext( sessionId );
      sm.bulkUnsubscribe( session, ch1.getChannelId(), subChannelIds );

      assertChannelActionCount( 2 );
      assertChannelAction( getChannelActions().get( 0 ), address1, ChannelAction.Action.REMOVE, null );
      assertChannelAction( getChannelActions().get( 1 ), address2, ChannelAction.Action.REMOVE, null );

      assertNull( session.findSubscriptionEntry( address1 ) );
      assertNull( session.findSubscriptionEntry( address2 ) );
    }
  }

  @Test
  public void performUpdateSubscription()
    throws Exception
  {
    final ChannelMetaData ch =
      new ChannelMetaData( 0, "C2", true, ChannelMetaData.FilterType.DYNAMIC, String.class, false, true );
    final ChannelMetaData[] channels = new ChannelMetaData[]{ ch };

    final ChannelAddress cd = new ChannelAddress( ch.getChannelId(), null );

    final TestReplicantSessionManager sm = new TestReplicantSessionManager( channels );
    final ReplicantSession session = createSession( sm );

    final TestFilter originalFilter = new TestFilter( 41 );
    final TestFilter filter = new TestFilter( 42 );

    EntityMessageCacheUtil.removeSessionChanges();

    final SubscriptionEntry e1 = session.createSubscriptionEntry( cd );

    assertChannelActionCount( 0 );
    assertEntry( e1, false, 0, 0, null );

    sm.performUpdateSubscription( e1, originalFilter, filter, EntityMessageCacheUtil.getSessionChanges() );

    assertEntry( e1, false, 0, 0, filter );

    final LinkedList<ChannelAction> actions = getChannelActions();
    assertEquals( actions.size(), 1 );
    assertChannelAction( actions.get( 0 ), cd, ChannelAction.Action.UPDATE, "{\"myField\":42}" );

    // 1 Change comes from collectDataForUpdate
    final Collection<Change> changes = getChanges();
    assertEquals( changes.size(), 1 );
    final Change change = changes.iterator().next();
    final EntityMessage entityMessage = change.getEntityMessage();
    assertEquals( entityMessage.getId(), 78 );
    //Ugly hack to check the filters correctly passed through
    assertNotNull( entityMessage.getAttributeValues() );
    assertEquals( entityMessage.getAttributeValues().get( "OriginalFilter" ), originalFilter );
    assertEquals( entityMessage.getAttributeValues().get( "Filter" ), filter );
  }

  @Test
  public void updateSubscription()
    throws Exception
  {
    final ChannelMetaData ch =
      new ChannelMetaData( 0, "C2", true, ChannelMetaData.FilterType.DYNAMIC, String.class, false, true );
    final ChannelMetaData[] channels = new ChannelMetaData[]{ ch };

    final ChannelAddress cd = new ChannelAddress( ch.getChannelId(), null );

    final TestReplicantSessionManager sm = new TestReplicantSessionManager( channels );
    final ReplicantSession session = createSession( sm );

    final TestFilter originalFilter = new TestFilter( 41 );
    final TestFilter filter = new TestFilter( 42 );

    EntityMessageCacheUtil.removeSessionChanges();

    sm.subscribe( session, cd, originalFilter );
    EntityMessageCacheUtil.removeSessionChanges();

    TransactionSynchronizationRegistryUtil.lookup()
      .putResource( ServerConstants.REQUEST_ID_KEY, ValueUtil.randomInt() );
    // Attempt to update to same filter - should be a noop
    sm.subscribe( session, cd, originalFilter );

    final SubscriptionEntry e1 = session.getSubscriptionEntry( cd );
    assertEntry( e1, true, 0, 0, originalFilter );

    assertChannelActionCount( 0 );
    assertSessionChangesCount( 0 );

    sm.subscribe( session, cd, filter );

    assertEntry( e1, true, 0, 0, filter );

    assertChannelActionCount( 1 );
    assertSessionChangesCount( 1 );
  }

  @Test
  public void bulkSubscribe_forUpdate()
    throws Exception
  {
    final ChannelMetaData ch1 =
      new ChannelMetaData( 0, "C1", false, ChannelMetaData.FilterType.DYNAMIC, String.class, false, true );
    final ChannelMetaData[] channels = new ChannelMetaData[]{ ch1 };

    final ChannelAddress address1 = new ChannelAddress( ch1.getChannelId(), ValueUtil.randomInt() );
    final ChannelAddress address2 = new ChannelAddress( ch1.getChannelId(), ValueUtil.randomInt() );

    final TestReplicantSessionManager sm = new TestReplicantSessionManager( channels );
    final ReplicantSession session = createSession( sm );

    final TestFilter originalFilter = new TestFilter( 41 );
    final TestFilter filter = new TestFilter( 42 );

    final SubscriptionEntry e1 = session.createSubscriptionEntry( address1 );
    e1.setFilter( originalFilter );
    final SubscriptionEntry e2 = session.createSubscriptionEntry( address2 );
    e2.setFilter( originalFilter );

    final ArrayList<Integer> subChannelIds = new ArrayList<>();
    subChannelIds.add( address1.getSubChannelId() );
    subChannelIds.add( address2.getSubChannelId() );

    TransactionSynchronizationRegistryUtil.lookup()
      .putResource( ServerConstants.REQUEST_ID_KEY, ValueUtil.randomInt() );

    sm.bulkSubscribe( session, ch1.getChannelId(), subChannelIds, originalFilter );

    EntityMessageCacheUtil.removeSessionChanges();

    assertChannelActionCount( 0 );
    assertEntry( e1, true, 0, 0, originalFilter );
    assertEntry( e2, true, 0, 0, originalFilter );

    // Attempt to update to same filter - should be a noop
    sm.bulkSubscribe( session, ch1.getChannelId(), subChannelIds, originalFilter );

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
    sm.bulkSubscribe( session, ch1.getChannelId(), subChannelIds, filter );

    assertEntry( e1, true, 0, 0, filter );
    assertEntry( e2, true, 0, 0, filter );

    assertChannelActionCount( 2 );
    assertSessionChangesCount( 1 );

    //Clear counts
    EntityMessageCacheUtil.removeSessionChanges();

    //Set original filter so next action updates this one again
    e2.setFilter( originalFilter );

    // Attempt to update one channels
    sm.bulkSubscribe( session, ch1.getChannelId(), subChannelIds, filter );

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
      new ChannelMetaData( 0, "C1", false, ChannelMetaData.FilterType.DYNAMIC, String.class, false, true );
    final ChannelMetaData[] channels = new ChannelMetaData[]{ ch1 };

    final ChannelAddress address1 = new ChannelAddress( ch1.getChannelId(), ValueUtil.randomInt() );
    final ChannelAddress address2 = new ChannelAddress( ch1.getChannelId(), ValueUtil.randomInt() );

    final TestReplicantSessionManager sm = new TestReplicantSessionManager( channels );
    final ReplicantSession session = createSession( sm );

    final TestFilter originalFilter = new TestFilter( 41 );
    final TestFilter filter = new TestFilter( 42 );

    final SubscriptionEntry e1 = session.createSubscriptionEntry( address1 );
    e1.setFilter( originalFilter );
    final SubscriptionEntry e2 = session.createSubscriptionEntry( address2 );
    e2.setFilter( originalFilter );

    final ArrayList<Integer> subChannelIds = new ArrayList<>();
    subChannelIds.add( address1.getSubChannelId() );
    subChannelIds.add( address2.getSubChannelId() );

    EntityMessageCacheUtil.removeSessionChanges();

    assertChannelActionCount( 0 );
    assertEntry( e1, false, 0, 0, originalFilter );
    assertEntry( e2, false, 0, 0, originalFilter );

    sm.markAsBulkCollectDataForSubscriptionUpdate();

    assertEquals( sm.getBulkCollectDataForSubscriptionUpdateCallCount(), 0 );

    // Attempt to update both channels
    sm.bulkSubscribe( session, ch1.getChannelId(), subChannelIds, filter );

    assertEquals( sm.getBulkCollectDataForSubscriptionUpdateCallCount(), 1 );

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
      new ChannelMetaData( 0, "Roster", true, ChannelMetaData.FilterType.DYNAMIC, String.class, false, true );
    final ChannelMetaData ch2 =
      new ChannelMetaData( 1, "Resource", false, ChannelMetaData.FilterType.NONE, null, false, true );
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

    sm.linkSubscriptionEntries( se1, se2a );

    assertEntry( se1, false, 0, 1, null );
    assertEntry( se2a, false, 1, 0, null );
    assertEntry( se2b, false, 0, 0, null );
    assertTrue( se1.getOutwardSubscriptions().contains( address2a ) );
    assertTrue( se2a.getInwardSubscriptions().contains( address1 ) );

    sm.linkSubscriptionEntries( se2a, se2b );

    assertEntry( se1, false, 0, 1, null );
    assertEntry( se2a, false, 1, 1, null );
    assertEntry( se2b, false, 1, 0, null );
    assertTrue( se2a.getOutwardSubscriptions().contains( address2b ) );
    assertTrue( se2b.getInwardSubscriptions().contains( address2a ) );

    sm.delinkSubscriptionEntries( se2a, se2b );

    assertEntry( se1, false, 0, 1, null );
    assertEntry( se2a, false, 1, 0, null );
    assertEntry( se2b, false, 0, 0, null );
    assertFalse( se2a.getOutwardSubscriptions().contains( address2b ) );
    assertFalse( se2b.getInwardSubscriptions().contains( address2a ) );

    //Duplicate delink - noop
    sm.delinkSubscriptionEntries( se2a, se2b );

    assertEntry( se1, false, 0, 1, null );
    assertEntry( se2a, false, 1, 0, null );
    assertEntry( se2b, false, 0, 0, null );
    assertFalse( se2a.getOutwardSubscriptions().contains( address2b ) );
    assertFalse( se2b.getInwardSubscriptions().contains( address2a ) );

    sm.delinkSubscriptionEntries( se1, se2a );

    assertEntry( se1, false, 0, 0, null );
    assertEntry( se2a, false, 0, 0, null );
    assertEntry( se2b, false, 0, 0, null );
    assertFalse( se1.getOutwardSubscriptions().contains( address2a ) );
    assertFalse( se2a.getInwardSubscriptions().contains( address1 ) );
  }

  @Test
  public void newReplicantSession()
    throws Exception
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

    sm.sendPacket( session, "X", new ChangeSet() );

    final RemoteEndpoint.Basic remote = session.getWebSocketSession().getBasicRemote();
    verify( remote ).sendText( "{\"type\":\"update\",\"requestId\":1,\"etag\":\"X\"}" );
    assertEquals( sm.getRegistry().getResource( ServerConstants.REQUEST_COMPLETE_KEY ), Boolean.FALSE );
  }

  @Test
  public void expandLinkIfRequired()
  {
    EntityMessageCacheUtil.removeSessionChanges();

    final ChannelMetaData ch1 =
      new ChannelMetaData( 0, "C1", true, ChannelMetaData.FilterType.DYNAMIC, String.class, false, true );
    final ChannelMetaData ch2 =
      new ChannelMetaData( 1, "C2", false, ChannelMetaData.FilterType.NONE, null, false, true );
    final ChannelMetaData ch3 =
      new ChannelMetaData( 2, "C3", false, ChannelMetaData.FilterType.DYNAMIC, String.class, false, true );
    final ChannelMetaData[] channels = new ChannelMetaData[]{ ch1, ch2, ch3 };

    final ChannelAddress address1 = new ChannelAddress( ch1.getChannelId(), null );
    final ChannelAddress address2a = new ChannelAddress( ch2.getChannelId(), ValueUtil.randomInt() );
    final ChannelAddress address2b = new ChannelAddress( ch2.getChannelId(), ValueUtil.randomInt() );
    final ChannelAddress address3a = new ChannelAddress( ch3.getChannelId(), ValueUtil.randomInt() );
    final ChannelAddress address3b = new ChannelAddress( ch3.getChannelId(), ValueUtil.randomInt() );

    final ChannelLink link1 = new ChannelLink( address1, address2a );
    final ChannelLink link2 = new ChannelLink( address1, address2b );
    final ChannelLink link3 = new ChannelLink( address1, address3a );
    final ChannelLink link4 = new ChannelLink( address1, address3b );

    final TestReplicantSessionManager sm = new TestReplicantSessionManager( channels );
    final ReplicantSession session = createSession( sm );

    //No expand as address1 is not subscribed
    assertFalse( sm.expandLinkIfRequired( session, link1, EntityMessageCacheUtil.getSessionChanges() ) );

    sm.subscribe( session, address1, new TestFilter( 33 ) );

    final SubscriptionEntry entry1 = session.getSubscriptionEntry( address1 );

    assertEquals( entry1.getInwardSubscriptions().size(), 0 );
    assertEquals( entry1.getOutwardSubscriptions().size(), 0 );

    //Expand as address1 is subscribed
    assertTrue( sm.expandLinkIfRequired( session, link1, EntityMessageCacheUtil.getSessionChanges() ) );

    assertEquals( entry1.getInwardSubscriptions().size(), 0 );
    assertEquals( entry1.getOutwardSubscriptions().size(), 1 );

    final SubscriptionEntry entry2a = session.getSubscriptionEntry( address2a );

    assertFalse( entry2a.isExplicitlySubscribed() );
    assertEquals( entry2a.getInwardSubscriptions().size(), 1 );
    assertEquals( entry2a.getOutwardSubscriptions().size(), 0 );

    //No expand as address2a is already subscribed
    assertFalse( sm.expandLinkIfRequired( session, link1, EntityMessageCacheUtil.getSessionChanges() ) );

    // Subscribe to 2 explicitly
    sm.subscribe( session, address2b, null );

    final SubscriptionEntry entry2b = session.getSubscriptionEntry( address2b );

    assertEquals( entry1.getInwardSubscriptions().size(), 0 );
    assertEquals( entry1.getOutwardSubscriptions().size(), 1 );
    assertEquals( entry2b.getInwardSubscriptions().size(), 0 );
    assertEquals( entry2b.getOutwardSubscriptions().size(), 0 );

    assertFalse( sm.expandLinkIfRequired( session, link2, EntityMessageCacheUtil.getSessionChanges() ) );

    //expandLinkIfRequired should still "Link" them even if no subscribe occurs

    assertEquals( entry1.getInwardSubscriptions().size(), 0 );
    assertEquals( entry1.getOutwardSubscriptions().size(), 2 );
    assertEquals( entry2b.getInwardSubscriptions().size(), 1 );
    assertEquals( entry2b.getOutwardSubscriptions().size(), 0 );

    //Fails the filter
    assertFalse( sm.expandLinkIfRequired( session, link3, EntityMessageCacheUtil.getSessionChanges() ) );

    sm.setFollowSource( link3.getSourceChannel() );

    //We create a new subscription and copy the filter across
    assertTrue( sm.expandLinkIfRequired( session, link3, EntityMessageCacheUtil.getSessionChanges() ) );

    final SubscriptionEntry entry3a = session.getSubscriptionEntry( address3a );

    assertEquals( entry1.getInwardSubscriptions().size(), 0 );
    assertEquals( entry1.getOutwardSubscriptions().size(), 3 );
    assertEquals( entry3a.getInwardSubscriptions().size(), 1 );
    assertEquals( entry3a.getOutwardSubscriptions().size(), 0 );
    assertEquals( entry3a.getFilter(), entry1.getFilter() );

    sm.subscribe( session, address3b, new TestFilter( ValueUtil.randomInt() ) );

    final SubscriptionEntry entry3b = session.getSubscriptionEntry( address3b );

    //Do not update the filter... does this crazyness actually occur?
    assertFalse( sm.expandLinkIfRequired( session, link4, EntityMessageCacheUtil.getSessionChanges() ) );

    assertEquals( entry1.getInwardSubscriptions().size(), 0 );
    assertEquals( entry1.getOutwardSubscriptions().size(), 4 );
    assertEquals( entry3b.getInwardSubscriptions().size(), 1 );
    assertEquals( entry3b.getOutwardSubscriptions().size(), 0 );
    assertNotEquals( entry3b.getFilter(), entry1.getFilter() );
  }

  @Test
  public void expandLink()
  {
    EntityMessageCacheUtil.removeSessionChanges();

    final ChannelMetaData ch1 =
      new ChannelMetaData( 0, "C1", true, ChannelMetaData.FilterType.DYNAMIC, String.class, false, true );
    final ChannelMetaData ch2 =
      new ChannelMetaData( 1, "C2", false, ChannelMetaData.FilterType.NONE, null, false, true );
    final ChannelMetaData[] channels = new ChannelMetaData[]{ ch1, ch2 };

    final ChannelAddress address1 = new ChannelAddress( ch1.getChannelId(), null );
    final ChannelAddress address2a = new ChannelAddress( ch2.getChannelId(), ValueUtil.randomInt() );
    final ChannelAddress address2b = new ChannelAddress( ch2.getChannelId(), ValueUtil.randomInt() );

    final ChannelLink link1 = new ChannelLink( address1, address2a );
    final ChannelLink link2 = new ChannelLink( address1, address2b );

    final TestReplicantSessionManager sm = new TestReplicantSessionManager( channels );
    final ReplicantSession session = createSession( sm );

    //No expand as address1 is not subscribed
    assertFalse( sm.expandLink( session, EntityMessageCacheUtil.getSessionChanges() ) );

    assertChannelActionCount( 0 );

    sm.subscribe( session, address1, null );

    assertChannelActionCount( 1 );

    //No expand as no data to expand
    assertFalse( sm.expandLink( session, EntityMessageCacheUtil.getSessionChanges() ) );

    assertChannelActionCount( 1 );

    final HashMap<String, Serializable> routes = new HashMap<>();
    final HashMap<String, Serializable> attributes = new HashMap<>();
    final HashSet<ChannelLink> links = new LinkedHashSet<>();
    links.add( link1 );
    links.add( link2 );
    final EntityMessage message =
      new EntityMessage( ValueUtil.randomInt(), ValueUtil.randomInt(), 0, routes, attributes, links );
    EntityMessageCacheUtil.getSessionChanges().merge( new Change( message ) );

    assertTrue( sm.expandLink( session, EntityMessageCacheUtil.getSessionChanges() ) );

    assertChannelActionCount( 2 );

    final SubscriptionEntry entry1 = session.getSubscriptionEntry( address1 );
    final SubscriptionEntry entry2a = session.getSubscriptionEntry( address2a );

    // Should not subscribe this until second wave
    assertNull( session.findSubscriptionEntry( address2b ) );

    assertEquals( entry1.getInwardSubscriptions().size(), 0 );
    assertEquals( entry1.getOutwardSubscriptions().size(), 1 );
    assertEquals( entry2a.getInwardSubscriptions().size(), 1 );
    assertEquals( entry2a.getOutwardSubscriptions().size(), 0 );

    // Second wave starts now
    assertTrue( sm.expandLink( session, EntityMessageCacheUtil.getSessionChanges() ) );

    assertChannelActionCount( 3 );

    final SubscriptionEntry entry2b = session.getSubscriptionEntry( address2b );

    assertEquals( entry1.getInwardSubscriptions().size(), 0 );
    assertEquals( entry1.getOutwardSubscriptions().size(), 2 );
    assertEquals( entry2a.getInwardSubscriptions().size(), 1 );
    assertEquals( entry2a.getOutwardSubscriptions().size(), 0 );
    assertEquals( entry2b.getInwardSubscriptions().size(), 1 );
    assertEquals( entry2b.getOutwardSubscriptions().size(), 0 );

    // No more data to process
    assertFalse( sm.expandLink( session, EntityMessageCacheUtil.getSessionChanges() ) );

    assertChannelActionCount( 3 );
  }

  @Test
  public void expandLinks()
  {
    EntityMessageCacheUtil.removeSessionChanges();

    final ChannelMetaData ch1 =
      new ChannelMetaData( 0, "C1", true, ChannelMetaData.FilterType.DYNAMIC, String.class, false, true );
    final ChannelMetaData ch2 =
      new ChannelMetaData( 1, "C2", false, ChannelMetaData.FilterType.NONE, null, false, true );
    final ChannelMetaData[] channels = new ChannelMetaData[]{ ch1, ch2 };

    final ChannelAddress address1 = new ChannelAddress( ch1.getChannelId(), null );
    final ChannelAddress address2a = new ChannelAddress( ch2.getChannelId(), ValueUtil.randomInt() );
    final ChannelAddress address2b = new ChannelAddress( ch2.getChannelId(), ValueUtil.randomInt() );

    final ChannelLink link1 = new ChannelLink( address1, address2a );
    final ChannelLink link2 = new ChannelLink( address1, address2b );

    final TestReplicantSessionManager sm = new TestReplicantSessionManager( channels );
    final ReplicantSession session = createSession( sm );

    assertChannelActionCount( 0 );

    sm.subscribe( session, address1, null );

    assertChannelActionCount( 1 );

    final HashMap<String, Serializable> routes = new HashMap<>();
    final HashMap<String, Serializable> attributes = new HashMap<>();
    final HashSet<ChannelLink> links = new HashSet<>();
    links.add( link1 );
    links.add( link2 );
    final EntityMessage message =
      new EntityMessage( ValueUtil.randomInt(), ValueUtil.randomInt(), 0, routes, attributes, links );
    EntityMessageCacheUtil.getSessionChanges().merge( new Change( message ) );

    sm.expandLinks( session, EntityMessageCacheUtil.getSessionChanges() );

    final SubscriptionEntry entry1 = session.getSubscriptionEntry( address1 );
    final SubscriptionEntry entry2a = session.getSubscriptionEntry( address2a );
    final SubscriptionEntry entry2b = session.getSubscriptionEntry( address2b );

    assertEquals( entry1.getInwardSubscriptions().size(), 0 );
    assertEquals( entry1.getOutwardSubscriptions().size(), 2 );
    assertEquals( entry2a.getInwardSubscriptions().size(), 1 );
    assertEquals( entry2a.getOutwardSubscriptions().size(), 0 );
    assertEquals( entry2b.getInwardSubscriptions().size(), 1 );
    assertEquals( entry2b.getOutwardSubscriptions().size(), 0 );

    assertChannelActionCount( 3 );
  }

  @Test
  public void tryGetCacheEntry()
  {
    final ChannelMetaData ch1 = new ChannelMetaData( 0, "C1", true, ChannelMetaData.FilterType.NONE, null, true, true );
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
    final ChannelMetaData ch1 = new ChannelMetaData( 0, "C1", true, ChannelMetaData.FilterType.NONE, null, true, true );
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
    final ChannelMetaData ch1 = new ChannelMetaData( 0, "C1", true, ChannelMetaData.FilterType.NONE, null, true, true );
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

  private LinkedList<ChannelAction> getChannelActions()
  {
    return EntityMessageCacheUtil.getSessionChanges().getChannelActions();
  }

  private void assertChannelAction( @Nonnull final ChannelAction channelAction,
                                    @Nonnull final ChannelAddress address,
                                    @Nonnull final ChannelAction.Action action,
                                    @Nullable final String filterAsString )
  {
    assertEquals( channelAction.getAction(), action );
    assertEquals( channelAction.getAddress(), address );
    if ( null == filterAsString )
    {
      assertNull( channelAction.getFilter() );
    }
    else
    {
      assertNotNull( channelAction.getFilter() );
      assertEquals( channelAction.getFilter().toString(), filterAsString );
    }
  }

  private void assertChannelActionCount( final int channelActionCount )
  {
    assertEquals( getChannelActions().size(), channelActionCount );
  }

  static class TestReplicantSessionManager
    extends ReplicantSessionManagerImpl
  {
    private final SystemMetaData _systemMetaData;
    private ChannelAddress _followSource;
    private String _cacheKey;
    private boolean _bulkCollectDataForSubscriptionUpdate;
    private int _bulkCollectDataForSubscriptionUpdateCallCount;
    private boolean _channelRootDeleted;

    private TestReplicantSessionManager()
    {
      this( new SystemMetaData( ValueUtil.randomString() ) );
    }

    private TestReplicantSessionManager( final ChannelMetaData[] channels )
    {
      this( new SystemMetaData( ValueUtil.randomString(), channels ) );
    }

    private TestReplicantSessionManager( final SystemMetaData systemMetaData )
    {
      _systemMetaData = systemMetaData;
    }

    int getBulkCollectDataForSubscriptionUpdateCallCount()
    {
      return _bulkCollectDataForSubscriptionUpdateCallCount;
    }

    void markAsBulkCollectDataForSubscriptionUpdate()
    {
      _bulkCollectDataForSubscriptionUpdate = true;
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
    public SystemMetaData getSystemMetaData()
    {
      return _systemMetaData;
    }

    @Override
    protected void processUpdateMessages( @Nonnull final EntityMessage message,
                                          @Nonnull final Collection<ReplicantSession> sessions,
                                          @Nonnull final ChangeAccumulator accumulator )
    {
    }

    @Override
    protected void processDeleteMessages( @Nonnull final EntityMessage message,
                                          @Nonnull final Collection<ReplicantSession> sessions,
                                          @Nonnull final ChangeAccumulator accumulator )
    {
    }

    @Override
    protected boolean bulkCollectDataForSubscribe( @Nonnull final ReplicantSession session,
                                                   @Nonnull final ArrayList<ChannelAddress> descriptors,
                                                   @Nullable final Object filter )
    {
      return false;
    }

    @Override
    protected boolean bulkCollectDataForSubscriptionUpdate( @Nonnull final ReplicantSession session,
                                                            @Nonnull final ArrayList<ChannelAddress> descriptors,
                                                            @Nullable final Object originalFilter,
                                                            @Nullable final Object filter )
    {
      _bulkCollectDataForSubscriptionUpdateCallCount += 1;
      return _bulkCollectDataForSubscriptionUpdate;
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
        changeSet.merge( new Change( message, descriptor.getChannelId(), descriptor.getSubChannelId() ) );
        return new SubscribeResult( false, _cacheKey );
      }
      else
      {
        return new SubscribeResult( true, null );
      }
    }

    @Override
    protected void collectDataForSubscriptionUpdate( @Nonnull final ChannelAddress descriptor,
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
      changeSet.merge( new Change( message, descriptor.getChannelId(), descriptor.getSubChannelId() ) );
    }

    private void setFollowSource( final ChannelAddress followSource )
    {
      _followSource = followSource;
    }

    @Override
    protected boolean shouldFollowLink( @Nonnull final SubscriptionEntry sourceEntry,
                                        @Nonnull final ChannelAddress target )
    {
      return null != _followSource && _followSource.equals( sourceEntry.getDescriptor() );
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
}
