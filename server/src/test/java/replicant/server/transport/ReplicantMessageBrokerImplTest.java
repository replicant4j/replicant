package replicant.server.transport;

import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import javax.json.Json;
import javax.json.JsonObject;
import javax.websocket.Session;
import org.realityforge.guiceyloops.shared.ValueUtil;
import org.testng.annotations.Test;
import replicant.server.ChangeSet;
import replicant.server.EntityMessage;
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

    final var packet = broker.queueChangeMessage( session, false,
                                                  ValueUtil.randomInt(),
                                                  Json.createObjectBuilder()
                                                    .add( ValueUtil.randomString(), ValueUtil.randomString() )
                                                    .build(),
                                                  ValueUtil.randomString(),
                                                  Collections.emptyList(),
                                                  new ChangeSet() );

    verifyNoSend( broker );

    broker.processPendingSessions();

    verifySendOnce( broker, session, packet );
  }

  @Test
  public void multipleSendsToSameSession()
  {
    final ReplicantSession session = newSession();

    final ReplicantMessageBroker broker = new TestReplicantMessageBrokerImpl();

    // No actions
    broker.processPendingSessions();

    verifyNoSend( broker );

    final var packet1 = broker.queueChangeMessage( session,
                                                   false,
                                                   ValueUtil.randomInt(),
                                                   Json.createObjectBuilder()
                                                     .add( ValueUtil.randomString(), ValueUtil.randomString() )
                                                     .build(),
                                                   ValueUtil.randomString(),
                                                   Collections.emptyList(),
                                                   new ChangeSet() );

    final var packet2 = broker.queueChangeMessage( session,
                                                   false,
                                                   ValueUtil.randomInt(),
                                                   Json.createObjectBuilder()
                                                     .add( ValueUtil.randomString(), ValueUtil.randomString() )
                                                     .build(),
                                                   ValueUtil.randomString(),
                                                   Collections.emptyList(),
                                                   new ChangeSet() );

    verifyNoSend( broker );

    broker.processPendingSessions();

    verifySendOnce( broker, session, packet1 );
    verifySendOnce( broker, session, packet2 );
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

    final var packet1 = broker.queueChangeMessage( session1,
                                                   false,
                                                   null,
                                                   null,
                                                   null,
                                                   Collections.emptyList(),
                                                   new ChangeSet() );

    final var packet2 = broker.queueChangeMessage( session2,
                                                   false,
                                                   null,
                                                   null,
                                                   null,
                                                   Collections.emptyList(),
                                                   new ChangeSet() );

    verifyNoSend( broker );

    broker.processPendingSessions();

    verifySendOnce( broker, session1, packet1 );
    verifySendOnce( broker, session2, packet2 );
  }

  private void verifySendOnce( @Nonnull final ReplicantMessageBroker broker,
                               @Nonnull final ReplicantSession session,
                               @Nonnull final Packet packet )
  {
    verify( ( (TestReplicantMessageBrokerImpl) broker )._sessionManager, times( 1 ) )
      .sendChangeMessage( eq( session ), eq( packet ) );
  }

  private void verifyNoSend( @Nonnull final ReplicantMessageBroker broker )
  {
    verify( ( (TestReplicantMessageBrokerImpl) broker )._sessionManager, never() ).sendChangeMessage( any(), any() );
  }

  @Nonnull
  private ReplicantSession newSession()
  {
    final Session session = mock( Session.class );
    when( session.isOpen() ).thenReturn( Boolean.TRUE );
    when( session.getId() ).thenReturn( ValueUtil.randomString() );
    return new ReplicantSession( session );
  }

  private static class TestReplicantMessageBrokerImpl
    extends ReplicantMessageBrokerImpl
  {
    public TestReplicantMessageBrokerImpl()
    {
      _sessionManager = mock( ReplicantSessionManager.class );
    }
  }
}
