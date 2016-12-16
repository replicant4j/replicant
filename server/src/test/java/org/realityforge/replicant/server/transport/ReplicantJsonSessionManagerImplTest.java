package org.realityforge.replicant.server.transport;

import java.util.Collection;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.transaction.TransactionSynchronizationRegistry;
import org.realityforge.replicant.server.ChangeAccumulator;
import org.realityforge.replicant.server.ChangeSet;
import org.realityforge.replicant.server.ChannelDescriptor;
import org.realityforge.replicant.server.EntityMessage;
import org.realityforge.replicant.server.TestSession;
import org.testng.annotations.Test;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class ReplicantJsonSessionManagerImplTest
{
  @Test
  public void poll()
    throws Exception
  {
    final TestReplicantSessionManagerImpl sm = new TestReplicantSessionManagerImpl();
    final TestSession session = sm.createSession();
    session.getQueue().addPacket( null, null, new ChangeSet() );

    assertEquals( sm.pollJsonData( session, 0 ), "{\"last_id\":1,\"request_id\":null,\"etag\":null}" );
    assertEquals( sm.pollJsonData( session, 1 ), null );
  }

  static class TestReplicantSessionManagerImpl
    extends ReplicantJsonSessionManagerImpl<TestSession>
  {
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
                                          @Nonnull final Collection<TestSession> sessions,
                                          @Nonnull final ChangeAccumulator accumulator )
    {
    }

    @Override
    protected void processDeleteMessages( @Nonnull final EntityMessage message,
                                          @Nonnull final Collection<TestSession> sessions )
    {
    }

    @Override
    protected void collectDataForSubscribe( @Nonnull final TestSession session,
                                            @Nonnull final ChannelDescriptor descriptor,
                                            @Nonnull final ChangeSet changeSet,
                                            @Nullable final Object filter )
    {
    }

    @Override
    protected void collectDataForSubscriptionUpdate( @Nonnull final TestSession session,
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
      return mock( TransactionSynchronizationRegistry.class );
    }

    @Nonnull
    @Override
    protected TestSession newSessionInfo()
    {
      return new TestSession( UUID.randomUUID().toString() );
    }
  }
}
