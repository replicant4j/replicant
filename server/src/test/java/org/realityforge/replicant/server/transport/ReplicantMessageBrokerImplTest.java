package org.realityforge.replicant.server.transport;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.websocket.Session;
import org.realityforge.guiceyloops.shared.ValueUtil;
import org.realityforge.replicant.server.ChangeSet;
import org.realityforge.replicant.server.EntityMessage;
import org.testng.annotations.Test;
import static org.mockito.Mockito.*;

public class ReplicantMessageBrokerImplTest
{
  @Test
  public void basicOperation()
  {
    final ReplicantSession session = newSession();

    final ReplicantMessageBroker broker = new TestReplicantMessageBrokerImpl();

    // No actions
    broker.processPendingSessions();

    verifyNoSend( broker );

    final int requestId = ValueUtil.randomInt();
    final String etag = ValueUtil.randomString();
    final List<EntityMessage> messages = Collections.emptyList();
    final ChangeSet changeSet = new ChangeSet();

    broker.queueChangeMessage( session, false, requestId, etag, messages, changeSet );

    verifyNoSend( broker );

    broker.processPendingSessions();

    verifySendOnce( broker, session, requestId, etag, messages, changeSet );
  }

  @Test
  public void multipleSendsToSameSession()
  {
    final ReplicantSession session = newSession();

    final ReplicantMessageBroker broker = new TestReplicantMessageBrokerImpl();

    // No actions
    broker.processPendingSessions();

    verifyNoSend( broker );

    final int requestId1 = ValueUtil.randomInt();
    final String etag1 = ValueUtil.randomString();
    final List<EntityMessage> messages1 = Collections.emptyList();
    final ChangeSet changeSet1 = new ChangeSet();

    broker.queueChangeMessage( session, false, requestId1, etag1, messages1, changeSet1 );

    final int requestId2 = ValueUtil.randomInt();
    final String etag2 = ValueUtil.randomString();
    final List<EntityMessage> messages2 = Collections.emptyList();
    final ChangeSet changeSet2 = new ChangeSet();

    broker.queueChangeMessage( session, false, requestId2, etag2, messages2, changeSet2 );

    verifyNoSend( broker );

    broker.processPendingSessions();

    verifySendOnce( broker, session, requestId1, etag1, messages1, changeSet1 );
    verifySendOnce( broker, session, requestId2, etag2, messages2, changeSet2 );
  }

  @Test
  public void multipleSendsToDifferentSession()
  {
    final ReplicantSession session1 = newSession();
    final ReplicantSession session2 = newSession();

    final ReplicantMessageBroker broker = new TestReplicantMessageBrokerImpl();

    // No actions
    broker.processPendingSessions();

    verifyNoSend( broker );

    final Integer requestId1 = null;
    final String etag1 = null;
    final List<EntityMessage> messages1 = Collections.emptyList();
    final ChangeSet changeSet1 = new ChangeSet();

    broker.queueChangeMessage( session1, false, requestId1, etag1, messages1, changeSet1 );

    final Integer requestId2 = null;
    final String etag2 = null;
    final List<EntityMessage> messages2 = Collections.emptyList();
    final ChangeSet changeSet2 = new ChangeSet();

    broker.queueChangeMessage( session2, false, requestId2, etag2, messages2, changeSet2 );

    verifyNoSend( broker );

    broker.processPendingSessions();

    verifySendOnce( broker, session1, requestId1, etag1, messages1, changeSet1 );
    verifySendOnce( broker, session2, requestId2, etag2, messages2, changeSet2 );
  }

  private void verifySendOnce( @Nonnull final ReplicantMessageBroker broker,
                               @Nonnull final ReplicantSession session,
                               @Nullable final Integer requestId,
                               @Nullable final String etag,
                               @Nonnull final Collection<EntityMessage> messages,
                               @Nonnull final ChangeSet changeSet )
  {
    verify( ( (TestReplicantMessageBrokerImpl) broker ).getReplicantSessionManager(), times( 1 ) )
      .sendChangeMessage( eq( session ), eq( requestId ), eq( etag ), eq( messages ), eq( changeSet ) );
  }

  private void verifyNoSend( @Nonnull final ReplicantMessageBroker broker )
  {
    verify( ( (TestReplicantMessageBrokerImpl) broker ).getReplicantSessionManager(), never() )
      .sendChangeMessage( any(), any(), any(), any(), any() );
  }

  @Nonnull
  private ReplicantSession newSession()
  {
    final Session session = mock( Session.class );
    when( session.isOpen() ).thenReturn( Boolean.TRUE );
    return new ReplicantSession( session );
  }

  private static class TestReplicantMessageBrokerImpl
    extends ReplicantMessageBrokerImpl
  {
    @Nonnull
    private final ReplicantSessionManager _sessionManager = mock( ReplicantSessionManager.class );

    @Nonnull
    @Override
    protected ReplicantSessionManager getReplicantSessionManager()
    {
      return _sessionManager;
    }
  }
}
