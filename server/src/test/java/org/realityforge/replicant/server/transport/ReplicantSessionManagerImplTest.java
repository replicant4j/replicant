package org.realityforge.replicant.server.transport;

import java.util.Collection;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.transaction.TransactionSynchronizationRegistry;
import org.realityforge.guiceyloops.server.AssertUtil;
import org.realityforge.replicant.server.ChangeAccumulator;
import org.realityforge.replicant.server.ChangeSet;
import org.realityforge.replicant.server.ChannelDescriptor;
import org.realityforge.replicant.server.EntityMessage;
import org.realityforge.replicant.server.ee.TestTransactionSynchronizationRegistry;
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
    extends ReplicantSessionManagerImpl<ReplicantSession>
  {
    private final TestTransactionSynchronizationRegistry _registry = new TestTransactionSynchronizationRegistry();

    @Nonnull
    @Override
    protected ChannelMetaData[] getChannelMetaData()
    {
      return new ChannelMetaData[ 0 ];
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

    @Nonnull
    @Override
    protected ReplicantSession newSessionInfo()
    {
      return new ReplicantSession( UUID.randomUUID().toString() );
    }
  }
}
