package org.realityforge.replicant.server.transport;

import java.util.Collection;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.transaction.TransactionSynchronizationRegistry;
import org.realityforge.guiceyloops.server.AssertUtil;
import org.realityforge.guiceyloops.server.TestTransactionSynchronizationRegistry;
import org.realityforge.guiceyloops.shared.ValueUtil;
import org.realityforge.replicant.server.ChangeAccumulator;
import org.realityforge.replicant.server.ChangeSet;
import org.realityforge.replicant.server.ChannelDescriptor;
import org.realityforge.replicant.server.EntityMessage;
import org.realityforge.replicant.shared.transport.ReplicantContext;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class ReplicantSessionManagerImplTest
{
  @Test
  public void ensureCdiType()
  {
    AssertUtil.assertNoFinalMethodsForCDI( ReplicantSessionManagerImpl.class );
    AssertUtil.assertNoFinalMethodsForCDI( ReplicantJsonSessionManagerImpl.class );
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
