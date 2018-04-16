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
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.TransactionSynchronizationRegistry;
import org.realityforge.guiceyloops.server.AssertUtil;
import org.realityforge.guiceyloops.server.TestInitialContextFactory;
import org.realityforge.guiceyloops.shared.ValueUtil;
import org.realityforge.replicant.server.Change;
import org.realityforge.replicant.server.ChangeAccumulator;
import org.realityforge.replicant.server.ChangeSet;
import org.realityforge.replicant.server.ChannelAction;
import org.realityforge.replicant.server.ChannelDescriptor;
import org.realityforge.replicant.server.ChannelLink;
import org.realityforge.replicant.server.EntityMessage;
import org.realityforge.replicant.server.ee.RegistryUtil;
import org.realityforge.replicant.shared.transport.ReplicantContext;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

/**
 * TODO: Add tests for saveEntityMessages
 */
public class ReplicantSessionManagerImplTest
{
  private ChangeSet _changeSet;

  private void resetChangeSet()
  {
    _changeSet = new ChangeSet();
  }

  @BeforeMethod
  public void setupTransactionContext()
  {
    resetChangeSet();
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
    final ReplicantSessionManagerImpl sm = new TestReplicantSessionManager();
    assertEquals( sm.getSessionKey(), "sid" );
    assertEquals( sm.getSessionIDs().size(), 0 );
    assertEquals( sm.getSession( "MySessionID" ), null );
    final ReplicantSession sessionInfo = sm.createSession();
    assertNotNull( sessionInfo );
    assertNull( sessionInfo.getUserID() );
    assertNotNull( sessionInfo.getSessionID() );
    assertEquals( sm.getSessionIDs().size(), 1 );
    assertEquals( sessionInfo.getCreatedAt(), sessionInfo.getLastAccessedAt() );
    assertTrue( System.currentTimeMillis() - sessionInfo.getCreatedAt() < 100L );
    assertTrue( System.currentTimeMillis() - sessionInfo.getLastAccessedAt() < 100L );
    Thread.sleep( 1 );

    // Make sure we can also get it thorugh the map interface
    assertEquals( sm.getSessions().get( sessionInfo.getSessionID() ), sessionInfo );

    // The next line should update the last accessed time too!
    assertEquals( sm.getSession( sessionInfo.getSessionID() ), sessionInfo );
    assertNotEquals( sessionInfo.getCreatedAt(), sessionInfo.getLastAccessedAt() );

    assertTrue( sm.invalidateSession( sessionInfo.getSessionID() ) );
    assertEquals( sm.getSessionIDs().size(), 0 );
    assertFalse( sm.invalidateSession( sessionInfo.getSessionID() ) );
    assertNull( sm.getSession( sessionInfo.getSessionID() ) );
  }

  @Test
  public void removeIdleSessions()
    throws Exception
  {
    final ReplicantSessionManagerImpl sm = new TestReplicantSessionManager();
    final ReplicantSession session = sm.createSession();
    final long accessedAt = session.getLastAccessedAt();
    while ( System.currentTimeMillis() == accessedAt )
    {
      Thread.yield();
    }
    final int removeCount = sm.removeIdleSessions( 10000 );
    assertEquals( removeCount, 0 );
    assertEquals( sm.getSessions().get( session.getSessionID() ), session );

    final int removeCount2 = sm.removeIdleSessions( 0 );
    assertEquals( removeCount2, 1 );
    assertNull( sm.getSessions().get( session.getSessionID() ) );
  }

  @Test
  public void locking()
    throws Exception
  {
    final ReplicantSessionManagerImpl sm = new TestReplicantSessionManager();
    final ReadWriteLock lock = sm.getLock();

    // Variable used to pass data back from threads
    final ReplicantSession[] sessions = new ReplicantSession[ 2 ];

    lock.readLock().lock();

    // Make sure createSession can not complete if something has a read lock
    final CyclicBarrier end = go( () -> sessions[ 0 ] = sm.createSession() );

    assertNull( sessions[ 0 ] );
    lock.readLock().unlock();
    end.await();
    assertNotNull( sessions[ 0 ] );

    lock.writeLock().lock();

    // Make sure getSession can acquire a read lock
    final CyclicBarrier end2 = go( () -> sessions[ 1 ] = sm.getSession( sessions[ 0 ].getSessionID() ) );

    assertNull( sessions[ 1 ] );
    lock.writeLock().unlock();
    end2.await();
    assertNotNull( sessions[ 1 ] );

    lock.readLock().lock();

    final Boolean[] invalidated = new Boolean[ 1 ];
    // Make sure createSession can not complete if something has a read lock
    final CyclicBarrier end3 = go( () -> invalidated[ 0 ] = sm.invalidateSession( sessions[ 0 ].getSessionID() ) );

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
    final ChannelMetaData ch1 = new ChannelMetaData( 0, "C1", null, ChannelMetaData.FilterType.NONE, null, true );
    final ChannelMetaData[] channels = new ChannelMetaData[]{ ch1 };

    final ChannelDescriptor cd1 = new ChannelDescriptor( ch1.getChannelID(), null );

    final TestReplicantSessionManager sm = new TestReplicantSessionManager( channels );
    assertFalse( sm.deleteCacheEntry( cd1 ) );

    sm.setCacheKey( "X" );

    sm.ensureCacheEntry( cd1 );

    assertTrue( sm.deleteCacheEntry( cd1 ) );
  }

  @Test
  public void pollJsonData()
    throws Exception
  {
    final TestReplicantSessionManager sm = new TestReplicantSessionManager();
    final ReplicantSession session = sm.createSession();
    session.getQueue().addPacket( null, null, new ChangeSet() );

    assertEquals( sm.pollJsonData( session, 0 ), "{\"last_id\":1,\"request_id\":null,\"etag\":null}" );
    assertEquals( sm.pollJsonData( session, 1 ), null );
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
    final ChannelMetaData ch1 = new ChannelMetaData( 0, "C1", null, ChannelMetaData.FilterType.NONE, null, false );
    final ChannelMetaData ch2 =
      new ChannelMetaData( 1, "C2", null, ChannelMetaData.FilterType.DYNAMIC, String.class, false );
    final ChannelMetaData ch3 =
      new ChannelMetaData( 2, "C3", null, ChannelMetaData.FilterType.STATIC, String.class, false );
    final ChannelMetaData[] channels = new ChannelMetaData[]{ ch1, ch2, ch3 };

    final ChannelDescriptor cd1 = new ChannelDescriptor( ch1.getChannelID(), null );
    final ChannelDescriptor cd2 = new ChannelDescriptor( ch2.getChannelID(), null );
    final ChannelDescriptor cd3 = new ChannelDescriptor( ch3.getChannelID(), null );

    final TestFilter originalFilter = new TestFilter( 41 );
    final TestFilter filter = new TestFilter( 42 );

    final TestReplicantSessionManager sm = new TestReplicantSessionManager( channels );
    final ReplicantSession session = sm.createSession();

    assertNull( session.findSubscriptionEntry( cd1 ) );

    // subscribe
    {
      resetChangeSet();
      final ReplicantSessionManagerImpl.CacheStatus status =
        sm.subscribe( session, cd1, false, null, getChangeSet() );
      assertEquals( status, ReplicantSessionManager.CacheStatus.REFRESH );

      final SubscriptionEntry entry1 = session.findSubscriptionEntry( cd1 );
      assertNotNull( entry1 );
      assertEntry( entry1, false, 0, 0, null );

      assertChannelActionCount( 1 );
      assertSessionChangesCount( 1 );
    }

    // re-subscribe- should be noop
    {
      resetChangeSet();
      final ReplicantSessionManagerImpl.CacheStatus status =
        sm.subscribe( session, cd1, false, null, getChangeSet() );
      assertEquals( status, ReplicantSessionManager.CacheStatus.IGNORE );
      assertEntry( session.getSubscriptionEntry( cd1 ), false, 0, 0, null );
      assertChannelActionCount( 0 );
      assertSessionChangesCount( 0 );
    }

    // re-subscribe explicitly - should only set explicit flag
    {
      resetChangeSet();
      final ReplicantSessionManagerImpl.CacheStatus status = sm.subscribe( session, cd1, true, null, getChangeSet() );
      assertEquals( status, ReplicantSessionManager.CacheStatus.IGNORE );
      assertEntry( session.getSubscriptionEntry( cd1 ), true, 0, 0, null );
      assertChannelActionCount( 0 );
      assertSessionChangesCount( 0 );
    }

    // subscribe when existing subscription present
    {
      resetChangeSet();
      final ReplicantSessionManagerImpl.CacheStatus status =
        sm.subscribe( session, cd2, true, originalFilter, getChangeSet() );
      assertEquals( status, ReplicantSessionManager.CacheStatus.REFRESH );
      assertEntry( session.getSubscriptionEntry( cd2 ), true, 0, 0, originalFilter );
      assertChannelActionCount( 1 );
      assertSessionChangesCount( 1 );

      resetChangeSet();

      //Should be a noop as same filter
      final ReplicantSessionManagerImpl.CacheStatus status1 =
        sm.subscribe( session, cd2, true, originalFilter, getChangeSet() );
      assertEquals( status1, ReplicantSessionManager.CacheStatus.IGNORE );

      assertEntry( session.getSubscriptionEntry( cd2 ), true, 0, 0, originalFilter );
      assertChannelActionCount( 0 );
      assertSessionChangesCount( 0 );

      //Should be a filter update
      final ReplicantSessionManagerImpl.CacheStatus status2 =
        sm.subscribe( session, cd2, true, filter, getChangeSet() );
      assertEquals( status2, ReplicantSessionManager.CacheStatus.IGNORE );

      assertEntry( session.getSubscriptionEntry( cd2 ), true, 0, 0, filter );
      assertChannelActionCount( 1 );
      assertSessionChangesCount( 1 );
    }

    //Subscribe and attempt to update static filter
    {
      resetChangeSet();
      sm.subscribe( session, cd3, true, originalFilter, getChangeSet() );
      assertEntry( session.getSubscriptionEntry( cd3 ), true, 0, 0, originalFilter );
      assertChannelActionCount( 1 );
      assertSessionChangesCount( 1 );

      resetChangeSet();

      //Should be a noop as same filter
      sm.subscribe( session, cd3, true, originalFilter, getChangeSet() );

      assertEntry( session.getSubscriptionEntry( cd3 ), true, 0, 0, originalFilter );
      assertChannelActionCount( 0 );
      assertSessionChangesCount( 0 );

      try
      {
        sm.subscribe( session, cd3, true, filter, getChangeSet() );
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
    final ChannelMetaData ch1 = new ChannelMetaData( 0, "C1", null, ChannelMetaData.FilterType.NONE, null, true );
    final ChannelMetaData[] channels = new ChannelMetaData[]{ ch1 };

    final ChannelDescriptor cd1 = new ChannelDescriptor( ch1.getChannelID(), null );

    final TestReplicantSessionManager sm = new TestReplicantSessionManager( channels );

    sm.setCacheKey( "X" );

    // subscribe - matching cacheKey
    {
      final ReplicantSession session = sm.createSession();
      assertNull( session.findSubscriptionEntry( cd1 ) );
      resetChangeSet();
      session.setETag( cd1, "X" );
      final ReplicantSessionManagerImpl.CacheStatus status =
        sm.subscribe( session, cd1, false, null, getChangeSet() );
      assertEquals( status, ReplicantSessionManager.CacheStatus.USE );

      final SubscriptionEntry entry1 = session.findSubscriptionEntry( cd1 );
      assertNotNull( entry1 );
      assertEntry( entry1, false, 0, 0, null );

      assertChannelActionCount( 0 );
      assertSessionChangesCount( 0 );
    }

    // subscribe - cacheKey differs
    {
      final ReplicantSession session = sm.createSession();
      assertNull( session.findSubscriptionEntry( cd1 ) );
      resetChangeSet();
      session.setETag( cd1, "Y" );
      final ReplicantSessionManagerImpl.CacheStatus status =
        sm.subscribe( session, cd1, false, null, getChangeSet() );
      assertEquals( status, ReplicantSessionManager.CacheStatus.REFRESH );

      final SubscriptionEntry entry1 = session.findSubscriptionEntry( cd1 );
      assertNotNull( entry1 );
      assertEntry( entry1, false, 0, 0, null );

      final PacketQueue queue = session.getQueue();
      assertEquals( queue.size(), 1 );
      final Packet packet = queue.getPacket( 1 );
      assertNotNull( packet );
      assertEquals( packet.getETag(), "X" );
      assertEquals( packet.getChangeSet().getChanges().size(), 1 );
      assertEquals( packet.getChangeSet().getChannelActions().size(), 1 );
    }
  }

  @Test
  public void subscribe_withSessionID()
    throws Exception
  {
    final ChannelMetaData ch1 = new ChannelMetaData( 0, "C1", null, ChannelMetaData.FilterType.NONE, null, false );
    final ChannelMetaData[] channels = new ChannelMetaData[]{ ch1 };

    final ChannelDescriptor cd1 = new ChannelDescriptor( ch1.getChannelID(), null );

    final TestReplicantSessionManager sm = new TestReplicantSessionManager( channels );
    final ReplicantSession session = sm.createSession();

    assertNull( session.findSubscriptionEntry( cd1 ) );

    final ReplicantSessionManagerImpl.CacheStatus status =
      sm.subscribe( session.getSessionID(), cd1, null, null, getChangeSet() );
    assertEquals( status, ReplicantSessionManager.CacheStatus.REFRESH );

    final SubscriptionEntry entry1 = session.findSubscriptionEntry( cd1 );
    assertNotNull( entry1 );
    assertEntry( entry1, true, 0, 0, null );

    assertChannelActionCount( 1 );
    assertSessionChangesCount( 1 );
  }

  @Test
  public void subscribe_withSessionID_andCaching()
    throws Exception
  {
    final ChannelMetaData ch1 = new ChannelMetaData( 0, "C1", null, ChannelMetaData.FilterType.NONE, null, true );
    final ChannelMetaData[] channels = new ChannelMetaData[]{ ch1 };

    final ChannelDescriptor cd1 = new ChannelDescriptor( ch1.getChannelID(), null );

    final TestReplicantSessionManager sm = new TestReplicantSessionManager( channels );
    final ReplicantSession session = sm.createSession();

    sm.setCacheKey( "X" );

    assertNull( session.getETag( cd1 ) );

    assertNull( session.findSubscriptionEntry( cd1 ) );

    final ReplicantSessionManagerImpl.CacheStatus status =
      sm.subscribe( session.getSessionID(), cd1, null, "X", getChangeSet() );
    assertEquals( status, ReplicantSessionManager.CacheStatus.USE );

    assertEquals( session.getETag( cd1 ), "X" );

    final SubscriptionEntry entry1 = session.findSubscriptionEntry( cd1 );
    assertNotNull( entry1 );
    assertEntry( entry1, true, 0, 0, null );

    assertChannelActionCount( 0 );
    assertSessionChangesCount( 0 );
  }

  @Test
  public void subscribe_withSessionID_andCachingThatNoMatch()
    throws Exception
  {
    final ChannelMetaData ch1 = new ChannelMetaData( 0, "C1", null, ChannelMetaData.FilterType.NONE, null, true );
    final ChannelMetaData[] channels = new ChannelMetaData[]{ ch1 };

    final ChannelDescriptor cd1 = new ChannelDescriptor( ch1.getChannelID(), null );

    final TestReplicantSessionManager sm = new TestReplicantSessionManager( channels );
    final ReplicantSession session = sm.createSession();

    sm.setCacheKey( "X" );

    assertNull( session.getETag( cd1 ) );

    assertNull( session.findSubscriptionEntry( cd1 ) );

    final ReplicantSessionManagerImpl.CacheStatus status =
      sm.subscribe( session.getSessionID(), cd1, null, "Y", getChangeSet() );
    assertEquals( status, ReplicantSessionManager.CacheStatus.REFRESH );

    assertNull( session.getETag( cd1 ) );

    final SubscriptionEntry entry1 = session.findSubscriptionEntry( cd1 );
    assertNotNull( entry1 );
    assertEntry( entry1, true, 0, 0, null );

    final PacketQueue queue = session.getQueue();
    assertEquals( queue.size(), 1 );
    final Packet packet = queue.getPacket( 1 );
    assertNotNull( packet );
    assertEquals( packet.getETag(), "X" );
    assertEquals( packet.getChangeSet().getChanges().size(), 1 );
    assertEquals( packet.getChangeSet().getChannelActions().size(), 1 );
  }

  @Test
  public void performSubscribe()
    throws Exception
  {
    final ChannelMetaData ch1 = new ChannelMetaData( 0, "C1", null, ChannelMetaData.FilterType.NONE, null, false );
    final ChannelMetaData ch2 =
      new ChannelMetaData( 1, "C2", null, ChannelMetaData.FilterType.DYNAMIC, String.class, false );
    final ChannelMetaData[] channels = new ChannelMetaData[]{ ch1, ch2 };

    final ChannelDescriptor cd1 = new ChannelDescriptor( ch1.getChannelID(), null );
    final ChannelDescriptor cd2 = new ChannelDescriptor( ch2.getChannelID(), null );

    final TestReplicantSessionManager sm = new TestReplicantSessionManager( channels );
    final ReplicantSession session = sm.createSession();

    // Test with no filter
    {
      final SubscriptionEntry e1 = session.createSubscriptionEntry( cd1 );
      //Rebind clears the state
      resetChangeSet();

      assertChannelActionCount( 0 );
      assertEntry( e1, false, 0, 0, null );

      final ReplicantSessionManagerImpl.CacheStatus status =
        sm.performSubscribe( session, e1, true, null, getChangeSet() );
      assertEquals( status, ReplicantSessionManager.CacheStatus.REFRESH );

      assertEntry( e1, true, 0, 0, null );

      final LinkedList<ChannelAction> actions = getChannelActions();
      assertEquals( actions.size(), 1 );
      assertChannelAction( actions.get( 0 ), cd1, ChannelAction.Action.ADD, null );

      // 1 Change comes from collectDataForSubscribe
      final Collection<Change> changes = getChanges();
      assertEquals( changes.size(), 1 );
      assertEquals( changes.iterator().next().getEntityMessage().getID(), 79 );

      assertEntry( e1, true, 0, 0, null );
    }

    {
      final TestFilter filter = new TestFilter( 42 );

      resetChangeSet();
      final SubscriptionEntry e1 = session.createSubscriptionEntry( cd2 );

      assertChannelActionCount( 0 );
      assertEntry( e1, false, 0, 0, null );

      final ReplicantSessionManagerImpl.CacheStatus status =
        sm.performSubscribe( session, e1, true, filter, getChangeSet() );
      assertEquals( status, ReplicantSessionManager.CacheStatus.REFRESH );

      assertEntry( e1, true, 0, 0, filter );

      final LinkedList<ChannelAction> actions = getChannelActions();
      assertEquals( actions.size(), 1 );
      assertChannelAction( actions.get( 0 ), cd2, ChannelAction.Action.ADD, "{\"myField\":42}" );

      // 1 Change comes from collectDataForSubscribe
      final Collection<Change> changes = getChanges();
      assertEquals( changes.size(), 1 );
      assertEquals( changes.iterator().next().getEntityMessage().getID(), 79 );

      assertEntry( e1, true, 0, 0, filter );

      session.deleteSubscriptionEntry( e1 );
    }
  }

  @Test
  public void performSubscribe_withCaching()
    throws Exception
  {
    final ChannelMetaData ch1 = new ChannelMetaData( 0, "C1", null, ChannelMetaData.FilterType.NONE, null, true );
    final ChannelMetaData[] channels = new ChannelMetaData[]{ ch1 };

    final ChannelDescriptor cd1 = new ChannelDescriptor( ch1.getChannelID(), null );

    final TestReplicantSessionManager sm = new TestReplicantSessionManager( channels );

    sm.setCacheKey( "X" );

    //Locally cached
    {
      final ReplicantSession session = sm.createSession();
      session.setETag( cd1, "X" );
      final SubscriptionEntry e1 = session.createSubscriptionEntry( cd1 );
      //Rebind clears the state
      resetChangeSet();

      assertChannelActionCount( 0 );
      assertEntry( e1, false, 0, 0, null );

      final ReplicantSessionManagerImpl.CacheStatus status =
        sm.performSubscribe( session, e1, true, null, getChangeSet() );
      assertEquals( status, ReplicantSessionManager.CacheStatus.USE );

      assertEntry( e1, true, 0, 0, null );

      assertChannelActionCount( 0 );
      assertSessionChangesCount( 0 );
    }

    //Locally cached but an old version
    {
      final ReplicantSession session = sm.createSession();
      session.setETag( cd1, "NOT" + sm._cacheKey );
      final SubscriptionEntry e1 = session.createSubscriptionEntry( cd1 );
      //Rebind clears the state
      resetChangeSet();

      assertChannelActionCount( 0 );
      assertEntry( e1, false, 0, 0, null );

      final ReplicantSessionManagerImpl.CacheStatus status =
        sm.performSubscribe( session, e1, true, null, getChangeSet() );
      assertEquals( status, ReplicantSessionManager.CacheStatus.REFRESH );

      assertEntry( e1, true, 0, 0, null );

      final PacketQueue queue = session.getQueue();
      assertEquals( queue.size(), 1 );
      final Packet packet = queue.getPacket( 1 );
      assertNotNull( packet );
      assertEquals( packet.getETag(), "X" );
      assertEquals( packet.getChangeSet().getChanges().size(), 1 );
      assertEquals( packet.getChangeSet().getChannelActions().size(), 1 );
    }

    //Not cached locally
    {
      final ReplicantSession session = sm.createSession();
      final SubscriptionEntry e1 = session.createSubscriptionEntry( cd1 );
      //Rebind clears the state
      resetChangeSet();

      assertChannelActionCount( 0 );
      assertEntry( e1, false, 0, 0, null );

      final ReplicantSessionManagerImpl.CacheStatus status =
        sm.performSubscribe( session, e1, true, null, getChangeSet() );
      assertEquals( status, ReplicantSessionManager.CacheStatus.REFRESH );

      assertEntry( e1, true, 0, 0, null );

      final PacketQueue queue = session.getQueue();
      assertEquals( queue.size(), 1 );
      final Packet packet = queue.getPacket( 1 );
      assertNotNull( packet );
      assertEquals( packet.getETag(), "X" );
      assertEquals( packet.getChangeSet().getChanges().size(), 1 );
      assertEquals( packet.getChangeSet().getChannelActions().size(), 1 );
    }
  }

  @Test
  public void performUnsubscribe()
    throws Exception
  {
    final ChannelMetaData ch1 =
      new ChannelMetaData( 0, "C1", Integer.class, ChannelMetaData.FilterType.NONE, null, false );
    final ChannelMetaData[] channels = new ChannelMetaData[]{ ch1 };

    final ChannelDescriptor cd1 = new ChannelDescriptor( ch1.getChannelID(), ValueUtil.randomString() );
    final ChannelDescriptor cd2 = new ChannelDescriptor( ch1.getChannelID(), ValueUtil.randomString() );
    final ChannelDescriptor cd3 = new ChannelDescriptor( ch1.getChannelID(), ValueUtil.randomString() );
    final ChannelDescriptor cd4 = new ChannelDescriptor( ch1.getChannelID(), ValueUtil.randomString() );
    final ChannelDescriptor cd5 = new ChannelDescriptor( ch1.getChannelID(), ValueUtil.randomString() );

    final TestReplicantSessionManager sm = new TestReplicantSessionManager( channels );
    final ReplicantSession session = sm.createSession();

    // Unsubscribe from channel that was explicitly subscribed
    {
      resetChangeSet();
      sm.subscribe( session, cd1, true, null, getChangeSet() );
      final SubscriptionEntry entry = session.getSubscriptionEntry( cd1 );

      //Rebind clears the state
      resetChangeSet();

      assertChannelActionCount( 0 );

      sm.performUnsubscribe( session, entry, true, getChangeSet() );

      assertChannelActionCount( 1 );
      assertChannelAction( getChannelActions().get( 0 ), cd1, ChannelAction.Action.REMOVE, null );

      assertNull( session.findSubscriptionEntry( cd1 ) );
    }

    // implicit unsubscribe from channel that was implicitly subscribed should leave explicit subscription
    {
      resetChangeSet();
      sm.subscribe( session, cd1, true, null, getChangeSet() );
      final SubscriptionEntry entry = session.getSubscriptionEntry( cd1 );

      //Rebind clears the state
      resetChangeSet();

      assertChannelActionCount( 0 );

      sm.performUnsubscribe( session, entry, false, getChangeSet() );

      assertChannelActionCount( 0 );

      assertNotNull( session.findSubscriptionEntry( cd1 ) );

      sm.performUnsubscribe( session, entry, true, getChangeSet() );

      assertChannelActionCount( 1 );

      assertNull( session.findSubscriptionEntry( cd1 ) );
    }

    // implicit unsubscribe from channel that was implicitly subscribed should delete subscription
    {
      resetChangeSet();
      sm.subscribe( session, cd1, false, null, getChangeSet() );
      final SubscriptionEntry entry = session.getSubscriptionEntry( cd1 );

      //Rebind clears the state
      resetChangeSet();

      assertChannelActionCount( 0 );

      sm.performUnsubscribe( session, entry, false, getChangeSet() );

      assertChannelActionCount( 1 );
      assertChannelAction( getChannelActions().get( 0 ), cd1, ChannelAction.Action.REMOVE, null );

      assertNull( session.findSubscriptionEntry( cd1 ) );
    }

    // implicit unsubscribe from channel that was implicitly subscribed should leave subscription that
    // implicitly linked from elsewhere
    {
      resetChangeSet();
      sm.subscribe( session, cd1, false, null, getChangeSet() );
      final SubscriptionEntry entry = session.getSubscriptionEntry( cd1 );

      sm.subscribe( session, cd2, false, null, getChangeSet() );
      final SubscriptionEntry entry2 = session.getSubscriptionEntry( cd2 );
      sm.linkSubscriptionEntries( entry2, entry );

      //Rebind clears the state
      resetChangeSet();

      assertChannelActionCount( 0 );

      sm.performUnsubscribe( session, entry, false, getChangeSet() );

      assertChannelActionCount( 0 );
      assertNotNull( session.findSubscriptionEntry( cd1 ) );

      sm.delinkSubscriptionEntries( entry2, entry );

      sm.performUnsubscribe( session, entry, false, getChangeSet() );

      assertChannelActionCount( 1 );
      assertChannelAction( getChannelActions().get( 0 ), cd1, ChannelAction.Action.REMOVE, null );
      assertNull( session.findSubscriptionEntry( cd1 ) );
    }

    // Unsubscribe als results in unsubscribe for all downstream channels
    {
      resetChangeSet();
      sm.subscribe( session, cd1, true, null, getChangeSet() );
      sm.subscribe( session, cd2, false, null, getChangeSet() );
      sm.subscribe( session, cd3, false, null, getChangeSet() );
      sm.subscribe( session, cd4, false, null, getChangeSet() );
      sm.subscribe( session, cd5, false, null, getChangeSet() );

      final SubscriptionEntry entry = session.getSubscriptionEntry( cd1 );
      final SubscriptionEntry entry2 = session.getSubscriptionEntry( cd2 );
      final SubscriptionEntry entry3 = session.getSubscriptionEntry( cd3 );
      final SubscriptionEntry entry4 = session.getSubscriptionEntry( cd4 );
      final SubscriptionEntry entry5 = session.getSubscriptionEntry( cd5 );

      sm.linkSubscriptionEntries( entry, entry2 );
      sm.linkSubscriptionEntries( entry, entry3 );
      sm.linkSubscriptionEntries( entry3, entry4 );
      sm.linkSubscriptionEntries( entry4, entry5 );

      //Rebind clears the state
      resetChangeSet();

      assertChannelActionCount( 0 );

      sm.performUnsubscribe( session, entry, true, getChangeSet() );

      assertChannelActionCount( 5 );
      for ( final ChannelAction action : getChannelActions() )
      {
        assertEquals( action.getAction(), ChannelAction.Action.REMOVE );
      }

      assertNull( session.findSubscriptionEntry( cd1 ) );
      assertNull( session.findSubscriptionEntry( cd2 ) );
      assertNull( session.findSubscriptionEntry( cd3 ) );
      assertNull( session.findSubscriptionEntry( cd4 ) );
      assertNull( session.findSubscriptionEntry( cd5 ) );
    }
  }

  @Test
  public void unsubscribe()
    throws Exception
  {
    final ChannelMetaData ch1 =
      new ChannelMetaData( 0, "C1", Integer.class, ChannelMetaData.FilterType.NONE, null, false );
    final ChannelMetaData[] channels = new ChannelMetaData[]{ ch1 };

    final ChannelDescriptor cd1 = new ChannelDescriptor( ch1.getChannelID(), ValueUtil.randomString() );

    final TestReplicantSessionManager sm = new TestReplicantSessionManager( channels );
    final ReplicantSession session = sm.createSession();

    // Unsubscribe from channel that was explicitly subscribed
    {
      resetChangeSet();
      sm.subscribe( session, cd1, true, null, getChangeSet() );
      final SubscriptionEntry entry = session.getSubscriptionEntry( cd1 );

      //Rebind clears the state
      resetChangeSet();

      assertChannelActionCount( 0 );

      sm.unsubscribe( session, entry.getDescriptor(), true, getChangeSet() );

      assertChannelActionCount( 1 );
      assertChannelAction( getChannelActions().get( 0 ), cd1, ChannelAction.Action.REMOVE, null );

      assertNull( session.findSubscriptionEntry( cd1 ) );
    }

    // unsubscribe from unsubscribed
    {
      resetChangeSet();

      assertChannelActionCount( 0 );

      sm.unsubscribe( session, cd1, true, getChangeSet() );

      assertChannelActionCount( 0 );
    }
  }

  @Test
  public void unsubscribe_usingSessionID()
    throws Exception
  {
    final ChannelMetaData ch1 =
      new ChannelMetaData( 0, "C1", Integer.class, ChannelMetaData.FilterType.NONE, null, false );
    final ChannelMetaData[] channels = new ChannelMetaData[]{ ch1 };

    final ChannelDescriptor cd1 = new ChannelDescriptor( ch1.getChannelID(), ValueUtil.randomString() );

    final TestReplicantSessionManager sm = new TestReplicantSessionManager( channels );
    final ReplicantSession session = sm.createSession();

    resetChangeSet();
    sm.subscribe( session, cd1, true, null, getChangeSet() );

    resetChangeSet();
    final TransactionSynchronizationRegistry registry = RegistryUtil.bind();

    assertChannelActionCount( 0 );

    sm.unsubscribe( session.getSessionID(), cd1, getChangeSet() );
    assertEquals( registry.getResource( ReplicantContext.SESSION_ID_KEY ), session.getSessionID() );

    assertChannelActionCount( 1 );
    assertChannelAction( getChannelActions().get( 0 ), cd1, ChannelAction.Action.REMOVE, null );

    assertNull( session.findSubscriptionEntry( cd1 ) );
  }

  @Test
  public void bulkUnsubscribe()
  {
    final ChannelMetaData ch1 =
      new ChannelMetaData( 0, "C1", Integer.class, ChannelMetaData.FilterType.NONE, null, false );
    final ChannelMetaData ch2 =
      new ChannelMetaData( 1, "C2", Integer.class, ChannelMetaData.FilterType.NONE, null, false );
    final ChannelMetaData[] channels = new ChannelMetaData[]{ ch1, ch2 };

    final ChannelDescriptor cd1 = new ChannelDescriptor( ch1.getChannelID(), ValueUtil.randomString() );
    final ChannelDescriptor cd2 = new ChannelDescriptor( ch1.getChannelID(), ValueUtil.randomString() );
    final ChannelDescriptor cd3 = new ChannelDescriptor( ch1.getChannelID(), ValueUtil.randomString() );
    final ChannelDescriptor cd4 = new ChannelDescriptor( ch2.getChannelID(), ValueUtil.randomString() );

    final TestReplicantSessionManager sm = new TestReplicantSessionManager( channels );
    final ReplicantSession session = sm.createSession();

    // Unsubscribe from channel that was explicitly subscribed
    {
      resetChangeSet();
      sm.subscribe( session, cd1, true, null, getChangeSet() );
      sm.subscribe( session, cd2, true, null, getChangeSet() );
      sm.subscribe( session, cd4, true, null, getChangeSet() );

      //Rebind clears the state
      resetChangeSet();

      assertChannelActionCount( 0 );

      final ArrayList<Serializable> subChannelIDs = new ArrayList<>();
      subChannelIDs.add( cd1.getSubChannelID() );
      subChannelIDs.add( cd2.getSubChannelID() );
      //This next one is not subscribed
      subChannelIDs.add( cd3.getSubChannelID() );
      // This next one is for wrong channel so should be no-op
      subChannelIDs.add( cd4.getSubChannelID() );
      sm.bulkUnsubscribe( session, ch1.getChannelID(), subChannelIDs, true, getChangeSet() );

      assertChannelActionCount( 2 );
      assertChannelAction( getChannelActions().get( 0 ), cd1, ChannelAction.Action.REMOVE, null );
      assertChannelAction( getChannelActions().get( 1 ), cd2, ChannelAction.Action.REMOVE, null );

      assertNull( session.findSubscriptionEntry( cd1 ) );
      assertNull( session.findSubscriptionEntry( cd2 ) );
    }
  }

  @Test
  public void bulkUnsubscribe_withSessionID()
  {
    final ChannelMetaData ch1 =
      new ChannelMetaData( 0, "C1", Integer.class, ChannelMetaData.FilterType.NONE, null, false );
    final ChannelMetaData ch2 =
      new ChannelMetaData( 1, "C2", Integer.class, ChannelMetaData.FilterType.NONE, null, false );
    final ChannelMetaData[] channels = new ChannelMetaData[]{ ch1, ch2 };

    final ChannelDescriptor cd1 = new ChannelDescriptor( ch1.getChannelID(), ValueUtil.randomString() );
    final ChannelDescriptor cd2 = new ChannelDescriptor( ch1.getChannelID(), ValueUtil.randomString() );
    final ChannelDescriptor cd3 = new ChannelDescriptor( ch1.getChannelID(), ValueUtil.randomString() );
    final ChannelDescriptor cd4 = new ChannelDescriptor( ch2.getChannelID(), ValueUtil.randomString() );

    final TestReplicantSessionManager sm = new TestReplicantSessionManager( channels );
    final ReplicantSession session = sm.createSession();

    // Unsubscribe from channel that was explicitly subscribed
    {
      resetChangeSet();
      sm.subscribe( session, cd1, true, null, getChangeSet() );
      sm.subscribe( session, cd2, true, null, getChangeSet() );
      sm.subscribe( session, cd4, true, null, getChangeSet() );

      //Rebind clears the state
      resetChangeSet();

      assertChannelActionCount( 0 );

      final ArrayList<Serializable> subChannelIDs = new ArrayList<>();
      subChannelIDs.add( cd1.getSubChannelID() );
      subChannelIDs.add( cd2.getSubChannelID() );
      //This next one is not subscribed
      subChannelIDs.add( cd3.getSubChannelID() );
      // This next one is for wrong channel so should be no-op
      subChannelIDs.add( cd4.getSubChannelID() );
      sm.bulkUnsubscribe( session.getSessionID(), ch1.getChannelID(), subChannelIDs, true, getChangeSet() );

      assertChannelActionCount( 2 );
      assertChannelAction( getChannelActions().get( 0 ), cd1, ChannelAction.Action.REMOVE, null );
      assertChannelAction( getChannelActions().get( 1 ), cd2, ChannelAction.Action.REMOVE, null );

      assertNull( session.findSubscriptionEntry( cd1 ) );
      assertNull( session.findSubscriptionEntry( cd2 ) );
    }
  }

  @Test
  public void performUpdateSubscription()
    throws Exception
  {
    final ChannelMetaData ch =
      new ChannelMetaData( 0, "C2", null, ChannelMetaData.FilterType.DYNAMIC, String.class, false );
    final ChannelMetaData[] channels = new ChannelMetaData[]{ ch };

    final ChannelDescriptor cd = new ChannelDescriptor( ch.getChannelID(), null );

    final TestReplicantSessionManager sm = new TestReplicantSessionManager( channels );
    final ReplicantSession session = sm.createSession();

    final TestFilter originalFilter = new TestFilter( 41 );
    final TestFilter filter = new TestFilter( 42 );

    resetChangeSet();

    final SubscriptionEntry e1 = session.createSubscriptionEntry( cd );

    assertChannelActionCount( 0 );
    assertEntry( e1, false, 0, 0, null );

    sm.performUpdateSubscription( session, e1, originalFilter, filter, getChangeSet() );

    assertEntry( e1, false, 0, 0, filter );

    final LinkedList<ChannelAction> actions = getChannelActions();
    assertEquals( actions.size(), 1 );
    assertChannelAction( actions.get( 0 ), cd, ChannelAction.Action.UPDATE, "{\"myField\":42}" );

    // 1 Change comes from collectDataForUpdate
    final Collection<Change> changes = getChanges();
    assertEquals( changes.size(), 1 );
    final Change change = changes.iterator().next();
    final EntityMessage entityMessage = change.getEntityMessage();
    assertEquals( entityMessage.getID(), 78 );
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
      new ChannelMetaData( 0, "C2", null, ChannelMetaData.FilterType.DYNAMIC, String.class, false );
    final ChannelMetaData[] channels = new ChannelMetaData[]{ ch };

    final ChannelDescriptor cd = new ChannelDescriptor( ch.getChannelID(), null );

    final TestReplicantSessionManager sm = new TestReplicantSessionManager( channels );
    final ReplicantSession session = sm.createSession();

    final TestFilter originalFilter = new TestFilter( 41 );
    final TestFilter filter = new TestFilter( 42 );

    resetChangeSet();

    final SubscriptionEntry e1 = session.createSubscriptionEntry( cd );
    e1.setFilter( originalFilter );

    assertChannelActionCount( 0 );
    assertEntry( e1, false, 0, 0, originalFilter );

    // Attempt to update to same filter - should be a noop
    sm.updateSubscription( session, cd, originalFilter, getChangeSet() );

    assertEntry( e1, false, 0, 0, originalFilter );

    assertChannelActionCount( 0 );
    assertSessionChangesCount( 0 );

    sm.updateSubscription( session, cd, filter, getChangeSet() );

    assertEntry( e1, false, 0, 0, filter );

    assertChannelActionCount( 1 );
    assertSessionChangesCount( 1 );
  }

  @Test
  public void updateSubscription_usingSessionID()
    throws Exception
  {
    final ChannelMetaData ch =
      new ChannelMetaData( 0, "C2", null, ChannelMetaData.FilterType.DYNAMIC, String.class, false );
    final ChannelMetaData[] channels = new ChannelMetaData[]{ ch };

    final ChannelDescriptor cd = new ChannelDescriptor( ch.getChannelID(), null );

    final TestReplicantSessionManager sm = new TestReplicantSessionManager( channels );
    final ReplicantSession session = sm.createSession();

    final TestFilter originalFilter = new TestFilter( 41 );
    final TestFilter filter = new TestFilter( 42 );

    final TransactionSynchronizationRegistry registry = RegistryUtil.bind();

    final SubscriptionEntry e1 = session.createSubscriptionEntry( cd );
    e1.setFilter( originalFilter );

    assertChannelActionCount( 0 );

    sm.updateSubscription( session.getSessionID(), cd, filter, getChangeSet() );

    assertEquals( registry.getResource( ReplicantContext.SESSION_ID_KEY ), session.getSessionID() );

    assertEntry( e1, false, 0, 0, filter );

    assertChannelActionCount( 1 );
    assertSessionChangesCount( 1 );
  }

  @Test
  public void bulkUpdateSubscription()
    throws Exception
  {
    final ChannelMetaData ch1 =
      new ChannelMetaData( 0, "C1", Integer.class, ChannelMetaData.FilterType.DYNAMIC, String.class, false );
    final ChannelMetaData[] channels = new ChannelMetaData[]{ ch1 };

    final ChannelDescriptor cd1 = new ChannelDescriptor( ch1.getChannelID(), ValueUtil.randomString() );
    final ChannelDescriptor cd2 = new ChannelDescriptor( ch1.getChannelID(), ValueUtil.randomString() );

    final TestReplicantSessionManager sm = new TestReplicantSessionManager( channels );
    final ReplicantSession session = sm.createSession();

    final TestFilter originalFilter = new TestFilter( 41 );
    final TestFilter filter = new TestFilter( 42 );

    final SubscriptionEntry e1 = session.createSubscriptionEntry( cd1 );
    e1.setFilter( originalFilter );
    final SubscriptionEntry e2 = session.createSubscriptionEntry( cd2 );
    e2.setFilter( originalFilter );

    final ArrayList<Serializable> subChannelIDs = new ArrayList<>();
    subChannelIDs.add( cd1.getSubChannelID() );
    subChannelIDs.add( cd2.getSubChannelID() );

    resetChangeSet();

    assertChannelActionCount( 0 );
    assertEntry( e1, false, 0, 0, originalFilter );
    assertEntry( e2, false, 0, 0, originalFilter );

    // Attempt to update to same filter - should be a noop
    sm.bulkUpdateSubscription( session, ch1.getChannelID(), subChannelIDs, originalFilter, getChangeSet() );

    assertEntry( e1, false, 0, 0, originalFilter );
    assertEntry( e2, false, 0, 0, originalFilter );

    assertChannelActionCount( 0 );
    assertSessionChangesCount( 0 );

    // Attempt to update no channels - should be noop
    sm.bulkUpdateSubscription( session, ch1.getChannelID(), new ArrayList<>(), filter, getChangeSet() );

    assertEntry( e1, false, 0, 0, originalFilter );
    assertEntry( e2, false, 0, 0, originalFilter );

    assertChannelActionCount( 0 );
    assertSessionChangesCount( 0 );

    // Attempt to update both channels
    sm.bulkUpdateSubscription( session.getSessionID(), ch1.getChannelID(), subChannelIDs, filter, getChangeSet() );

    assertEntry( e1, false, 0, 0, filter );
    assertEntry( e2, false, 0, 0, filter );

    assertChannelActionCount( 2 );
    assertSessionChangesCount( 1 );

    //Clear counts
    resetChangeSet();

    //Set original filter so next action updates this one again
    e2.setFilter( originalFilter );

    // Attempt to update one channels
    sm.bulkUpdateSubscription( session, ch1.getChannelID(), subChannelIDs, filter, getChangeSet() );

    assertEntry( e1, false, 0, 0, filter );
    assertEntry( e2, false, 0, 0, filter );

    assertChannelActionCount( 1 );
    assertSessionChangesCount( 1 );
  }

  @Test
  public void bulkUpdateSubscription_wherebulkUpdateHookIsUsed()
    throws Exception
  {
    final ChannelMetaData ch1 =
      new ChannelMetaData( 0, "C1", Integer.class, ChannelMetaData.FilterType.DYNAMIC, String.class, false );
    final ChannelMetaData[] channels = new ChannelMetaData[]{ ch1 };

    final ChannelDescriptor cd1 = new ChannelDescriptor( ch1.getChannelID(), ValueUtil.randomString() );
    final ChannelDescriptor cd2 = new ChannelDescriptor( ch1.getChannelID(), ValueUtil.randomString() );

    final TestReplicantSessionManager sm = new TestReplicantSessionManager( channels );
    final ReplicantSession session = sm.createSession();

    final TestFilter originalFilter = new TestFilter( 41 );
    final TestFilter filter = new TestFilter( 42 );

    final SubscriptionEntry e1 = session.createSubscriptionEntry( cd1 );
    e1.setFilter( originalFilter );
    final SubscriptionEntry e2 = session.createSubscriptionEntry( cd2 );
    e2.setFilter( originalFilter );

    final ArrayList<Serializable> subChannelIDs = new ArrayList<>();
    subChannelIDs.add( cd1.getSubChannelID() );
    subChannelIDs.add( cd2.getSubChannelID() );

    resetChangeSet();

    assertChannelActionCount( 0 );
    assertEntry( e1, false, 0, 0, originalFilter );
    assertEntry( e2, false, 0, 0, originalFilter );

    sm.setBulkCollectDataForSubscriptionUpdate( true );

    assertEquals( sm.getBulkCollectDataForSubscriptionUpdateCallCount(), 0 );

    // Attempt to update both channels
    sm.bulkUpdateSubscription( session, ch1.getChannelID(), subChannelIDs, filter, getChangeSet() );

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
      new ChannelMetaData( 0, "Roster", null, ChannelMetaData.FilterType.DYNAMIC, String.class, false );
    final ChannelMetaData ch2 =
      new ChannelMetaData( 1, "Resource", Integer.class, ChannelMetaData.FilterType.NONE, null, false );
    final ChannelMetaData[] channels = new ChannelMetaData[]{ ch1, ch2 };

    final ChannelDescriptor cd1 = new ChannelDescriptor( ch1.getChannelID(), null );
    final ChannelDescriptor cd2a = new ChannelDescriptor( ch2.getChannelID(), ValueUtil.randomString() );
    final ChannelDescriptor cd2b = new ChannelDescriptor( ch2.getChannelID(), ValueUtil.randomString() );

    final TestReplicantSessionManager sm = new TestReplicantSessionManager( channels );
    final ReplicantSession session = sm.createSession();

    final SubscriptionEntry se1 = session.createSubscriptionEntry( cd1 );
    final SubscriptionEntry se2a = session.createSubscriptionEntry( cd2a );
    final SubscriptionEntry se2b = session.createSubscriptionEntry( cd2b );

    assertEntry( se1, false, 0, 0, null );
    assertEntry( se2a, false, 0, 0, null );
    assertEntry( se2b, false, 0, 0, null );

    sm.linkSubscriptionEntries( se1, se2a );

    assertEntry( se1, false, 0, 1, null );
    assertEntry( se2a, false, 1, 0, null );
    assertEntry( se2b, false, 0, 0, null );
    assertTrue( se1.getOutwardSubscriptions().contains( cd2a ) );
    assertTrue( se2a.getInwardSubscriptions().contains( cd1 ) );

    sm.linkSubscriptionEntries( se2a, se2b );

    assertEntry( se1, false, 0, 1, null );
    assertEntry( se2a, false, 1, 1, null );
    assertEntry( se2b, false, 1, 0, null );
    assertTrue( se2a.getOutwardSubscriptions().contains( cd2b ) );
    assertTrue( se2b.getInwardSubscriptions().contains( cd2a ) );

    sm.delinkSubscriptionEntries( se2a, se2b );

    assertEntry( se1, false, 0, 1, null );
    assertEntry( se2a, false, 1, 0, null );
    assertEntry( se2b, false, 0, 0, null );
    assertFalse( se2a.getOutwardSubscriptions().contains( cd2b ) );
    assertFalse( se2b.getInwardSubscriptions().contains( cd2a ) );

    //Duplicate delink - noop
    sm.delinkSubscriptionEntries( se2a, se2b );

    assertEntry( se1, false, 0, 1, null );
    assertEntry( se2a, false, 1, 0, null );
    assertEntry( se2b, false, 0, 0, null );
    assertFalse( se2a.getOutwardSubscriptions().contains( cd2b ) );
    assertFalse( se2b.getInwardSubscriptions().contains( cd2a ) );

    sm.delinkSubscriptionEntries( se1, se2a );

    assertEntry( se1, false, 0, 0, null );
    assertEntry( se2a, false, 0, 0, null );
    assertEntry( se2b, false, 0, 0, null );
    assertFalse( se1.getOutwardSubscriptions().contains( cd2a ) );
    assertFalse( se2a.getInwardSubscriptions().contains( cd1 ) );
  }

  @Test
  public void delinkSubscription()
    throws Exception
  {
    final ChannelMetaData ch1 =
      new ChannelMetaData( 0, "Roster", null, ChannelMetaData.FilterType.DYNAMIC, String.class, false );
    final ChannelMetaData ch2 =
      new ChannelMetaData( 1, "Resource", Integer.class, ChannelMetaData.FilterType.NONE, null, false );
    final ChannelMetaData[] channels = new ChannelMetaData[]{ ch1, ch2 };

    final ChannelDescriptor cd1 = new ChannelDescriptor( ch1.getChannelID(), null );
    final ChannelDescriptor cd2 = new ChannelDescriptor( ch2.getChannelID(), ValueUtil.randomString() );

    final TestReplicantSessionManager sm = new TestReplicantSessionManager( channels );

    {
      final ReplicantSession session = sm.createSession();
      final SubscriptionEntry se1 = session.createSubscriptionEntry( cd1 );
      se1.setExplicitlySubscribed( true );
      final SubscriptionEntry se2 = session.createSubscriptionEntry( cd2 );
      se2.setExplicitlySubscribed( true );

      sm.linkSubscriptionEntries( se1, se2 );

      assertEntry( se1, true, 0, 1, null );
      assertEntry( se2, true, 1, 0, null );

      final ChangeSet changeSet = new ChangeSet();
      sm.delinkSubscription( session, cd1, cd2, changeSet );

      //No action as se2 explicitly subscribed
      assertEquals( changeSet.getChannelActions().size(), 0 );
    }

    {
      final ReplicantSession session = sm.createSession();
      final SubscriptionEntry se1 = session.createSubscriptionEntry( cd1 );
      se1.setExplicitlySubscribed( true );
      final SubscriptionEntry se2 = session.createSubscriptionEntry( cd2 );

      sm.linkSubscriptionEntries( se1, se2 );

      assertEntry( se1, true, 0, 1, null );
      assertEntry( se2, false, 1, 0, null );

      final ChangeSet changeSet = new ChangeSet();
      sm.delinkSubscription( session, cd1, cd2, changeSet );

      //Action as se2 not explicitly subscribed and now is delinked
      final LinkedList<ChannelAction> channelActions = changeSet.getChannelActions();
      assertEquals( channelActions.size(), 1 );
      final ChannelAction action = channelActions.get( 0 );
      assertEquals( action.getAction(), ChannelAction.Action.REMOVE );
      assertEquals( action.getChannelDescriptor(), cd2 );
      assertEquals( action.getFilter(), null );
    }
  }

  @Test
  public void ensureSession()
    throws Exception
  {
    final TestReplicantSessionManager sm = new TestReplicantSessionManager();

    boolean ensureFailed = false;
    try
    {
      sm.ensureSession( "NotExist" );
    }
    catch ( final RuntimeException re )
    {
      ensureFailed = true;
    }

    assertTrue( ensureFailed, "Ensure expected to fail with non-existent session" );

    final ReplicantSession session = sm.createSession();
    assertEquals( sm.ensureSession( session.getSessionID() ), session );
  }

  @Test
  public void newReplicantSession()
    throws Exception
  {
    final TestReplicantSessionManager sm = new TestReplicantSessionManager();
    final ReplicantSession session = sm.newReplicantSession();

    assertEquals( session.getSessionID().matches( "[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}" ),
                  true );
  }

  @Test
  public void sendPacket()
    throws Exception
  {
    final TestReplicantSessionManager sm = new TestReplicantSessionManager();
    final ReplicantSession session = sm.createSession();

    sm.getRegistry().putResource( ReplicantContext.REQUEST_ID_KEY, "r1" );

    final Packet packet = sm.sendPacket( session, "X", new ChangeSet() );
    assertEquals( packet.getETag(), "X" );
    assertEquals( packet.getRequestID(), "r1" );
    assertEquals( packet.getChangeSet().getChanges().size(), 0 );
    assertEquals( sm.getRegistry().getResource( ReplicantContext.REQUEST_COMPLETE_KEY ), Boolean.FALSE );
  }

  @Test
  public void poll()
    throws Exception
  {
    final TestReplicantSessionManager sm = new TestReplicantSessionManager();
    final ReplicantSession session = sm.createSession();
    final Packet p1 = session.getQueue().addPacket( null, null, new ChangeSet() );
    final Packet p2 = session.getQueue().addPacket( null, null, new ChangeSet() );
    final Packet p3 = session.getQueue().addPacket( null, null, new ChangeSet() );

    assertEquals( sm.pollPacket( session, 0 ), p1 );
    assertEquals( sm.pollPacket( session, 0 ), p1 );

    assertEquals( sm.pollPacket( session, p1.getSequence() ), p2 );
    assertEquals( sm.pollPacket( session, p2.getSequence() ), p3 );
    assertEquals( sm.pollPacket( session, p3.getSequence() ), null );
  }

  @Test
  public void expandLinkIfRequired()
  {
    resetChangeSet();

    final ChannelMetaData ch1 =
      new ChannelMetaData( 0, "C1", null, ChannelMetaData.FilterType.DYNAMIC, String.class, false );
    final ChannelMetaData ch2 =
      new ChannelMetaData( 1, "C2", Integer.class, ChannelMetaData.FilterType.NONE, null, false );
    final ChannelMetaData ch3 =
      new ChannelMetaData( 2, "C3", Integer.class, ChannelMetaData.FilterType.DYNAMIC, String.class, false );
    final ChannelMetaData[] channels = new ChannelMetaData[]{ ch1, ch2, ch3 };

    final ChannelDescriptor cd1 = new ChannelDescriptor( ch1.getChannelID(), null );
    final ChannelDescriptor cd2a = new ChannelDescriptor( ch2.getChannelID(), ValueUtil.randomString() );
    final ChannelDescriptor cd2b = new ChannelDescriptor( ch2.getChannelID(), ValueUtil.randomString() );
    final ChannelDescriptor cd3a = new ChannelDescriptor( ch3.getChannelID(), ValueUtil.randomString() );
    final ChannelDescriptor cd3b = new ChannelDescriptor( ch3.getChannelID(), ValueUtil.randomString() );

    final ChannelLink link1 = new ChannelLink( cd1, cd2a );
    final ChannelLink link2 = new ChannelLink( cd1, cd2b );
    final ChannelLink link3 = new ChannelLink( cd1, cd3a );
    final ChannelLink link4 = new ChannelLink( cd1, cd3b );

    final TestReplicantSessionManager sm = new TestReplicantSessionManager( channels );
    final ReplicantSession session = sm.createSession();

    //No expand as cd1 is not subscribed
    assertFalse( sm.expandLinkIfRequired( session, link1, getChangeSet() ) );

    sm.subscribe( session, cd1, true, new TestFilter( 33 ), getChangeSet() );

    final SubscriptionEntry entry1 = session.getSubscriptionEntry( cd1 );

    assertEquals( entry1.getInwardSubscriptions().size(), 0 );
    assertEquals( entry1.getOutwardSubscriptions().size(), 0 );

    //Expand as cd1 is subscribed
    assertTrue( sm.expandLinkIfRequired( session, link1, getChangeSet() ) );

    assertEquals( entry1.getInwardSubscriptions().size(), 0 );
    assertEquals( entry1.getOutwardSubscriptions().size(), 1 );

    final SubscriptionEntry entry2a = session.getSubscriptionEntry( cd2a );

    assertEquals( entry2a.isExplicitlySubscribed(), false );
    assertEquals( entry2a.getInwardSubscriptions().size(), 1 );
    assertEquals( entry2a.getOutwardSubscriptions().size(), 0 );

    //No expand as cd2a is already subscribed
    assertFalse( sm.expandLinkIfRequired( session, link1, getChangeSet() ) );

    // Subscribe to 2 explicitly
    sm.subscribe( session, cd2b, true, null, getChangeSet() );

    final SubscriptionEntry entry2b = session.getSubscriptionEntry( cd2b );

    assertEquals( entry1.getInwardSubscriptions().size(), 0 );
    assertEquals( entry1.getOutwardSubscriptions().size(), 1 );
    assertEquals( entry2b.getInwardSubscriptions().size(), 0 );
    assertEquals( entry2b.getOutwardSubscriptions().size(), 0 );

    assertFalse( sm.expandLinkIfRequired( session, link2, getChangeSet() ) );

    //expandLinkIfRequired should still "Link" them even if no subscribe occurs

    assertEquals( entry1.getInwardSubscriptions().size(), 0 );
    assertEquals( entry1.getOutwardSubscriptions().size(), 2 );
    assertEquals( entry2b.getInwardSubscriptions().size(), 1 );
    assertEquals( entry2b.getOutwardSubscriptions().size(), 0 );

    //Fails the filter
    assertFalse( sm.expandLinkIfRequired( session, link3, getChangeSet() ) );

    sm.setFollowSource( link3.getSourceChannel() );

    //We create a new subscription and copy the filter across
    assertTrue( sm.expandLinkIfRequired( session, link3, getChangeSet() ) );

    final SubscriptionEntry entry3a = session.getSubscriptionEntry( cd3a );

    assertEquals( entry1.getInwardSubscriptions().size(), 0 );
    assertEquals( entry1.getOutwardSubscriptions().size(), 3 );
    assertEquals( entry3a.getInwardSubscriptions().size(), 1 );
    assertEquals( entry3a.getOutwardSubscriptions().size(), 0 );
    assertEquals( entry3a.getFilter(), entry1.getFilter() );

    sm.subscribe( session, cd3b, true, new TestFilter( ValueUtil.randomInt() ), getChangeSet() );

    final SubscriptionEntry entry3b = session.getSubscriptionEntry( cd3b );

    //Do not update the filter... does this crazyness actually occur?
    assertFalse( sm.expandLinkIfRequired( session, link4, getChangeSet() ) );

    assertEquals( entry1.getInwardSubscriptions().size(), 0 );
    assertEquals( entry1.getOutwardSubscriptions().size(), 4 );
    assertEquals( entry3b.getInwardSubscriptions().size(), 1 );
    assertEquals( entry3b.getOutwardSubscriptions().size(), 0 );
    assertNotEquals( entry3b.getFilter(), entry1.getFilter() );
  }

  @Test
  public void expandLink()
  {
    resetChangeSet();

    final ChannelMetaData ch1 =
      new ChannelMetaData( 0, "C1", null, ChannelMetaData.FilterType.DYNAMIC, String.class, false );
    final ChannelMetaData ch2 =
      new ChannelMetaData( 1, "C2", Integer.class, ChannelMetaData.FilterType.NONE, null, false );
    final ChannelMetaData[] channels = new ChannelMetaData[]{ ch1, ch2 };

    final ChannelDescriptor cd1 = new ChannelDescriptor( ch1.getChannelID(), null );
    final ChannelDescriptor cd2a = new ChannelDescriptor( ch2.getChannelID(), ValueUtil.randomString() );
    final ChannelDescriptor cd2b = new ChannelDescriptor( ch2.getChannelID(), ValueUtil.randomString() );

    final ChannelLink link1 = new ChannelLink( cd1, cd2a );
    final ChannelLink link2 = new ChannelLink( cd1, cd2b );

    final TestReplicantSessionManager sm = new TestReplicantSessionManager( channels );
    final ReplicantSession session = sm.createSession();

    final ChangeSet changeSet = getChangeSet();

    //No expand as cd1 is not subscribed
    assertFalse( sm.expandLink( session, changeSet ) );

    assertChannelActionCount( 0 );

    sm.subscribe( session, cd1, true, null, getChangeSet() );

    assertChannelActionCount( 1 );

    //No expand as no data to expand
    assertFalse( sm.expandLink( session, changeSet ) );

    assertChannelActionCount( 1 );

    final HashMap<String, Serializable> routes = new HashMap<>();
    final HashMap<String, Serializable> attributes = new HashMap<>();
    final HashSet<ChannelLink> links = new LinkedHashSet<>();
    links.add( link1 );
    links.add( link2 );
    final EntityMessage message =
      new EntityMessage( ValueUtil.randomInt(), ValueUtil.randomInt(), 0, routes, attributes, links );
    changeSet.merge( new Change( message ) );

    assertTrue( sm.expandLink( session, changeSet ) );

    assertChannelActionCount( 2 );

    final SubscriptionEntry entry1 = session.getSubscriptionEntry( cd1 );
    final SubscriptionEntry entry2a = session.getSubscriptionEntry( cd2a );

    // Should not subscribe this until second wave
    assertNull( session.findSubscriptionEntry( cd2b ) );

    assertEquals( entry1.getInwardSubscriptions().size(), 0 );
    assertEquals( entry1.getOutwardSubscriptions().size(), 1 );
    assertEquals( entry2a.getInwardSubscriptions().size(), 1 );
    assertEquals( entry2a.getOutwardSubscriptions().size(), 0 );

    // Second wave starts now
    assertTrue( sm.expandLink( session, changeSet ) );

    assertChannelActionCount( 3 );

    final SubscriptionEntry entry2b = session.getSubscriptionEntry( cd2b );

    assertEquals( entry1.getInwardSubscriptions().size(), 0 );
    assertEquals( entry1.getOutwardSubscriptions().size(), 2 );
    assertEquals( entry2a.getInwardSubscriptions().size(), 1 );
    assertEquals( entry2a.getOutwardSubscriptions().size(), 0 );
    assertEquals( entry2b.getInwardSubscriptions().size(), 1 );
    assertEquals( entry2b.getOutwardSubscriptions().size(), 0 );

    // No more data to process
    assertFalse( sm.expandLink( session, changeSet ) );

    assertChannelActionCount( 3 );
  }

  @Test
  public void expandLinks()
  {
    resetChangeSet();

    final ChannelMetaData ch1 =
      new ChannelMetaData( 0, "C1", null, ChannelMetaData.FilterType.DYNAMIC, String.class, false );
    final ChannelMetaData ch2 =
      new ChannelMetaData( 1, "C2", Integer.class, ChannelMetaData.FilterType.NONE, null, false );
    final ChannelMetaData[] channels = new ChannelMetaData[]{ ch1, ch2 };

    final ChannelDescriptor cd1 = new ChannelDescriptor( ch1.getChannelID(), null );
    final ChannelDescriptor cd2a = new ChannelDescriptor( ch2.getChannelID(), ValueUtil.randomString() );
    final ChannelDescriptor cd2b = new ChannelDescriptor( ch2.getChannelID(), ValueUtil.randomString() );

    final ChannelLink link1 = new ChannelLink( cd1, cd2a );
    final ChannelLink link2 = new ChannelLink( cd1, cd2b );

    final TestReplicantSessionManager sm = new TestReplicantSessionManager( channels );
    final ReplicantSession session = sm.createSession();

    final ChangeSet changeSet = getChangeSet();

    assertChannelActionCount( 0 );

    sm.subscribe( session, cd1, true, null, getChangeSet() );

    assertChannelActionCount( 1 );

    final HashMap<String, Serializable> routes = new HashMap<>();
    final HashMap<String, Serializable> attributes = new HashMap<>();
    final HashSet<ChannelLink> links = new HashSet<>();
    links.add( link1 );
    links.add( link2 );
    final EntityMessage message =
      new EntityMessage( ValueUtil.randomInt(), ValueUtil.randomInt(), 0, routes, attributes, links );
    changeSet.merge( new Change( message ) );

    sm.expandLinks( session, changeSet );

    final SubscriptionEntry entry1 = session.getSubscriptionEntry( cd1 );
    final SubscriptionEntry entry2a = session.getSubscriptionEntry( cd2a );
    final SubscriptionEntry entry2b = session.getSubscriptionEntry( cd2b );

    assertEquals( entry1.getInwardSubscriptions().size(), 0 );
    assertEquals( entry1.getOutwardSubscriptions().size(), 2 );
    assertEquals( entry2a.getInwardSubscriptions().size(), 1 );
    assertEquals( entry2a.getOutwardSubscriptions().size(), 0 );
    assertEquals( entry2b.getInwardSubscriptions().size(), 1 );
    assertEquals( entry2b.getOutwardSubscriptions().size(), 0 );

    assertChannelActionCount( 3 );
  }

  @Test
  public void ensureCacheEntry()
  {
    final ChannelMetaData ch1 = new ChannelMetaData( 0, "C1", null, ChannelMetaData.FilterType.NONE, null, true );
    final ChannelMetaData[] channels = new ChannelMetaData[]{ ch1 };

    final ChannelDescriptor cd1 = new ChannelDescriptor( ch1.getChannelID(), null );

    final TestReplicantSessionManager sm = new TestReplicantSessionManager( channels );

    sm.setCacheKey( "MyCache" );

    assertEquals( sm.getCacheEntry( cd1 ).isInitialized(), false );

    final ChannelCacheEntry entry = sm.ensureCacheEntry( cd1 );

    assertEquals( entry.isInitialized(), true );
    assertEquals( entry.getDescriptor(), cd1 );
    assertEquals( entry.getCacheKey(), "MyCache" );
    assertEquals( entry.getChangeSet().getChanges().size(), 1 );
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
    return getChangeSet().getChanges();
  }

  private ChangeSet getChangeSet()
  {
    return _changeSet;
  }

  private void assertSessionChangesCount( final int sessionChangesCount )
  {
    assertEquals( getChanges().size(), sessionChangesCount );
  }

  private LinkedList<ChannelAction> getChannelActions()
  {
    return getChangeSet().getChannelActions();
  }

  private void assertChannelAction( @Nonnull final ChannelAction channelAction,
                                    @Nonnull final ChannelDescriptor channelDescriptor,
                                    @Nonnull final ChannelAction.Action action,
                                    @Nullable final String filterAsString )
  {
    assertEquals( channelAction.getAction(), action );
    assertEquals( channelAction.getChannelDescriptor(), channelDescriptor );
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
    private ChannelDescriptor _followSource;
    private String _cacheKey;
    private boolean _bulkCollectDataForSubscriptionUpdate;
    private int _bulkCollectDataForSubscriptionUpdateCallCount;

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

    void setBulkCollectDataForSubscriptionUpdate( final boolean bulkCollectDataForSubscriptionUpdate )
    {
      _bulkCollectDataForSubscriptionUpdate = bulkCollectDataForSubscriptionUpdate;
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

    @Nonnull
    @Override
    protected RuntimeException newBadSessionException( @Nonnull final String sessionID )
    {
      return new IllegalStateException();
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
                                                   @Nonnull final ArrayList<ChannelDescriptor> descriptors,
                                                   @Nonnull final ChangeSet changeSet,
                                                   @Nullable final Object filter,
                                                   final boolean explicitSubscribe )
    {
      return false;
    }

    @Override
    protected boolean bulkCollectDataForSubscriptionUpdate( @Nonnull final ReplicantSession session,
                                                            @Nonnull final ArrayList<ChannelDescriptor> descriptors,
                                                            @Nonnull final ChangeSet changeSet,
                                                            @Nullable final Object originalFilter,
                                                            @Nullable final Object filter )
    {
      _bulkCollectDataForSubscriptionUpdateCallCount += 1;
      return _bulkCollectDataForSubscriptionUpdate;
    }

    @Override
    protected String collectDataForSubscribe( @Nullable final ReplicantSession session,
                                              @Nonnull final ChannelDescriptor descriptor,
                                              @Nonnull final ChangeSet changeSet,
                                              @Nullable final Object filter )
    {
      final HashMap<String, Serializable> routingKeys = new HashMap<>();
      final HashMap<String, Serializable> attributes = new HashMap<>();
      attributes.put( "ID", 79 );
      final EntityMessage message = new EntityMessage( 79, 1, 0, routingKeys, attributes, null );
      changeSet.merge( new Change( message, descriptor.getChannelID(), descriptor.getSubChannelID() ) );
      return _cacheKey;
    }

    @Override
    protected void collectDataForSubscriptionUpdate( @Nonnull final ReplicantSession session,
                                                     @Nonnull final ChannelDescriptor descriptor,
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
      changeSet.merge( new Change( message, descriptor.getChannelID(), descriptor.getSubChannelID() ) );
    }

    private void setFollowSource( final ChannelDescriptor followSource )
    {
      _followSource = followSource;
    }

    @Override
    protected boolean shouldFollowLink( @Nonnull final SubscriptionEntry sourceEntry,
                                        @Nonnull final ChannelDescriptor target )
    {
      return null != _followSource && _followSource.equals( sourceEntry.getDescriptor() );
    }

    @Nonnull
    @Override
    protected TransactionSynchronizationRegistry getRegistry()
    {
      final String key = "java:comp/TransactionSynchronizationRegistry";
      try
      {
        return (TransactionSynchronizationRegistry) new InitialContext().lookup( key );
      }
      catch ( final NamingException ne )
      {
        final String message =
          "Unable to locate TransactionSynchronizationRegistry at " + key + " due to " + ne;
        throw new IllegalStateException( message, ne );
      }
    }
  }
}
