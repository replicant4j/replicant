package org.realityforge.replicant.server.transport;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.transaction.TransactionSynchronizationRegistry;
import org.realityforge.guiceyloops.server.AssertUtil;
import org.realityforge.guiceyloops.server.TestInitialContextFactory;
import org.realityforge.guiceyloops.server.TestTransactionSynchronizationRegistry;
import org.realityforge.guiceyloops.shared.ValueUtil;
import org.realityforge.replicant.server.Change;
import org.realityforge.replicant.server.ChangeAccumulator;
import org.realityforge.replicant.server.ChangeSet;
import org.realityforge.replicant.server.ChannelAction;
import org.realityforge.replicant.server.ChannelDescriptor;
import org.realityforge.replicant.server.EntityMessage;
import org.realityforge.replicant.server.ee.EntityMessageCacheUtil;
import org.realityforge.replicant.server.ee.RegistryUtil;
import org.realityforge.replicant.shared.transport.ReplicantContext;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class ReplicantSessionManagerImplTest
{
  @AfterMethod
  public void clearContext()
  {
    TestInitialContextFactory.reset();
  }

  @Test
  public void ensureCdiType()
  {
    AssertUtil.assertNoFinalMethodsForCDI( ReplicantSessionManagerImpl.class );
    AssertUtil.assertNoFinalMethodsForCDI( ReplicantJsonSessionManagerImpl.class );
  }

  @Test
  public void performSubscribe()
    throws Exception
  {
    final ChannelMetaData ch1 = new ChannelMetaData( 0, "C1", true, ChannelMetaData.FilterType.NONE );
    final ChannelMetaData ch2 = new ChannelMetaData( 1, "C2", true, ChannelMetaData.FilterType.DYNAMIC );
    final ChannelMetaData[] channels = new ChannelMetaData[]{ ch1, ch2 };

    final ChannelDescriptor cd1 = new ChannelDescriptor( ch1.getChannelID(), null );
    final ChannelDescriptor cd2 = new ChannelDescriptor( ch2.getChannelID(), null );

    final TestReplicantSessionManager sm = new TestReplicantSessionManager( channels );
    final ReplicantSession session = sm.createSession();

    // Test with no filter
    {
      final SubscriptionEntry e1 = session.createSubscriptionEntry( cd1 );
      //Rebind clears the state
      RegistryUtil.bind();
      final ChangeSet changeSet = EntityMessageCacheUtil.getSessionChanges();

      assertEquals( changeSet.getChannelActions().size(), 0 );
      assertEntry( e1, false, 0, 0, null );

      sm.performSubscribe( session, e1, true, null );

      assertEntry( e1, true, 0, 0, null );

      final LinkedList<ChannelAction> actions = changeSet.getChannelActions();
      assertEquals( actions.size(), 1 );
      assertChannelAction( actions.get( 0 ), cd1, ChannelAction.Action.ADD, null );

      // 1 Change comes from collectDataForSubscribe
      final Collection<Change> changes = changeSet.getChanges();
      assertEquals( changes.size(), 1 );
      assertEquals( changes.iterator().next().getEntityMessage().getID(), 79 );

      assertEntry( e1, true, 0, 0, null );
    }

    {
      final TestFilter filter = new TestFilter( 42 );

      RegistryUtil.bind();
      final SubscriptionEntry e1 = session.createSubscriptionEntry( cd2 );
      final ChangeSet changeSet = EntityMessageCacheUtil.getSessionChanges();

      assertEquals( changeSet.getChannelActions().size(), 0 );
      assertEntry( e1, false, 0, 0, null );

      sm.performSubscribe( session, e1, true, filter );

      assertEntry( e1, true, 0, 0, filter );

      final LinkedList<ChannelAction> actions = changeSet.getChannelActions();
      assertEquals( actions.size(), 1 );
      assertChannelAction( actions.get( 0 ), cd2, ChannelAction.Action.ADD, "{\"myField\":42}" );

      // 1 Change comes from collectDataForSubscribe
      final Collection<Change> changes = changeSet.getChanges();
      assertEquals( changes.size(), 1 );
      assertEquals( changes.iterator().next().getEntityMessage().getID(), 79 );

      assertEntry( e1, true, 0, 0, filter );

      session.deleteSubscriptionEntry( e1 );
    }
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

  @Test
  public void linkSubscriptionEntries()
    throws Exception
  {
    final ChannelMetaData ch1 = new ChannelMetaData( 0, "Roster", ChannelMetaData.FilterType.DYNAMIC );
    final ChannelMetaData ch2 = new ChannelMetaData( 1, "Resource", ChannelMetaData.FilterType.NONE );
    final ChannelMetaData ch3 = new ChannelMetaData( 2, "Shift", ChannelMetaData.FilterType.DYNAMIC );
    final ChannelMetaData ch4 = new ChannelMetaData( 3, "Plans", ChannelMetaData.FilterType.STATIC );
    final ChannelMetaData[] channels = new ChannelMetaData[]{ ch1, ch2, ch3, ch4 };

    final ChannelDescriptor cd1 = new ChannelDescriptor( ch1.getChannelID(), null );
    final ChannelDescriptor cd2a = new ChannelDescriptor( ch2.getChannelID(), ValueUtil.randomString() );
    final ChannelDescriptor cd2b = new ChannelDescriptor( ch2.getChannelID(), ValueUtil.randomString() );
    final ChannelDescriptor cd3a = new ChannelDescriptor( ch3.getChannelID(), ValueUtil.randomString() );
    final ChannelDescriptor cd3b = new ChannelDescriptor( ch3.getChannelID(), ValueUtil.randomString() );
    final ChannelDescriptor cd4a = new ChannelDescriptor( ch4.getChannelID(), ValueUtil.randomString() );

    final TestReplicantSessionManager sm = new TestReplicantSessionManager( channels );
    final ReplicantSession session = sm.createSession();

    final SubscriptionEntry se1 = session.createSubscriptionEntry( cd1 );
    final SubscriptionEntry se2a = session.createSubscriptionEntry( cd2a );
    final SubscriptionEntry se2b = session.createSubscriptionEntry( cd2b );
    final SubscriptionEntry se3a = session.createSubscriptionEntry( cd3a );
    final SubscriptionEntry se3b = session.createSubscriptionEntry( cd3b );
    final SubscriptionEntry se4a = session.createSubscriptionEntry( cd4a );

    assertEntry( se1, false, 0, 0, null );
    assertEntry( se2a, false, 0, 0, null );
    assertEntry( se2b, false, 0, 0, null );
    assertEntry( se3a, false, 0, 0, null );
    assertEntry( se3b, false, 0, 0, null );
    assertEntry( se4a, false, 0, 0, null );

    // Link channels where target is unfiltered instance channel
    sm.linkSubscriptionEntries( session, cd1, cd2a );

    assertEntry( se1, false, 0, 1, null );
    assertEntry( se2a, false, 1, 0, null );
    assertEntry( se2b, false, 0, 0, null );
    assertEntry( se3a, false, 0, 0, null );
    assertEntry( se3b, false, 0, 0, null );
    assertEntry( se4a, false, 0, 0, null );
    assertTrue( se1.getOutwardSubscriptions().contains( cd2a ) );
    assertTrue( se2a.getInwardSubscriptions().contains( cd1 ) );

    // Link channels where target has DYNAMIC filter
    sm.linkSubscriptionEntries( session, cd1, cd3a );

    assertEntry( se1, false, 0, 2, null );
    assertEntry( se2a, false, 1, 0, null );
    assertEntry( se2b, false, 0, 0, null );
    assertEntry( se3a, false, 1, 0, null );
    assertEntry( se3b, false, 0, 0, null );
    assertEntry( se4a, false, 0, 0, null );
    assertTrue( se1.getOutwardSubscriptions().contains( cd3a ) );
    assertTrue( se3a.getInwardSubscriptions().contains( cd1 ) );

    //Duplicate link - no change
    sm.linkSubscriptionEntries( session, cd1, cd3a );

    assertEntry( se1, false, 0, 2, null );
    assertEntry( se2a, false, 1, 0, null );
    assertEntry( se2b, false, 0, 0, null );
    assertEntry( se3a, false, 1, 0, null );
    assertEntry( se3b, false, 0, 0, null );
    assertEntry( se4a, false, 0, 0, null );
    assertTrue( se1.getOutwardSubscriptions().contains( cd3a ) );
    assertTrue( se3a.getInwardSubscriptions().contains( cd1 ) );

    // Link channels where target has STATIC filter
    sm.linkSubscriptionEntries( session, cd1, cd4a );

    assertEntry( se1, false, 0, 3, null );
    assertEntry( se2a, false, 1, 0, null );
    assertEntry( se2b, false, 0, 0, null );
    assertEntry( se3a, false, 1, 0, null );
    assertEntry( se3b, false, 0, 0, null );
    assertEntry( se4a, false, 1, 0, null );
    assertTrue( se1.getOutwardSubscriptions().contains( cd4a ) );
    assertTrue( se4a.getInwardSubscriptions().contains( cd1 ) );

    // More links to ensure both in and out links align
    sm.linkSubscriptionEntries( session, cd3a, cd4a );

    assertEntry( se1, false, 0, 3, null );
    assertEntry( se2a, false, 1, 0, null );
    assertEntry( se2b, false, 0, 0, null );
    assertEntry( se3a, false, 1, 1, null );
    assertEntry( se3b, false, 0, 0, null );
    assertEntry( se4a, false, 2, 0, null );
    assertTrue( se3a.getOutwardSubscriptions().contains( cd4a ) );
    assertTrue( se4a.getInwardSubscriptions().contains( cd3a ) );
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

    assertFalse( ensureFailed, "Ensure expected to fail with non-existent session" );

    final ReplicantSession session = sm.createSession();
    assertEquals( sm.ensureSession( session.getSessionID() ), session );
  }

  @Test
  public void newSessionInfo()
    throws Exception
  {
    final TestReplicantSessionManager sm = new TestReplicantSessionManager();
    final ReplicantSession session = sm.newSessionInfo();

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

  static class TestReplicantSessionManager
    extends ReplicantSessionManagerImpl
  {
    private final TestTransactionSynchronizationRegistry _registry = new TestTransactionSynchronizationRegistry();
    private final ChannelMetaData[] _channelMetaDatas;

    private TestReplicantSessionManager()
    {
      this( new ChannelMetaData[ 0 ] );
    }

    private TestReplicantSessionManager( final ChannelMetaData[] channelMetaDatas )
    {
      _channelMetaDatas = channelMetaDatas;
    }

    @Nonnull
    @Override
    protected ChannelMetaData[] getChannelMetaData()
    {
      return _channelMetaDatas;
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
                                          @Nonnull final Collection<ReplicantSession> sessions )
    {
    }

    @Override
    protected void collectDataForSubscribe( @Nonnull final ReplicantSession session,
                                            @Nonnull final ChannelDescriptor descriptor,
                                            @Nonnull final ChangeSet changeSet,
                                            @Nullable final Object filter )
    {
      final HashMap<String, Serializable> routingKeys = new HashMap<>();
      final HashMap<String, Serializable> attributes = new HashMap<>();
      attributes.put( "ID", 79 );
      final EntityMessage message = new EntityMessage( 79, 1, 0, routingKeys, attributes, null );
      changeSet.merge( new Change( message, descriptor.getChannelID(), descriptor.getSubChannelID() ) );
    }

    @Override
    protected void collectDataForSubscriptionUpdate( @Nonnull final ReplicantSession session,
                                                     @Nonnull final ChannelDescriptor descriptor,
                                                     @Nonnull final ChangeSet changeSet,
                                                     @Nullable final Object originalFilter,
                                                     @Nullable final Object filter )
    {
    }

    @Override
    protected boolean shouldFollowLink( @Nonnull final ChannelDescriptor source,
                                        @Nonnull final ChannelDescriptor target )
    {
      return false;
    }

    @Nonnull
    @Override
    protected TransactionSynchronizationRegistry getRegistry()
    {
      return _registry;
    }
  }
}
