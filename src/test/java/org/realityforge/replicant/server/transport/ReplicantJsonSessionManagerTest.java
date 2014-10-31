package org.realityforge.replicant.server.transport;

import java.util.Collection;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.replicant.server.ChangeSet;
import org.realityforge.replicant.server.EntityMessage;
import org.realityforge.replicant.server.TestSession;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class ReplicantJsonSessionManagerTest
{
  @Test
  public void poll()
    throws Exception
  {
    final TestReplicantSessionManager sm = new TestReplicantSessionManager();
    final TestSession session = sm.createSession();
    session.getQueue().addPacket( null, null, new ChangeSet() );

    assertEquals( sm.pollJsonData( session, 0 ), "{\"last_id\":1,\"request_id\":null,\"etag\":null}" );
    assertEquals( sm.pollJsonData( session, 1 ), null );
  }

  static class TestReplicantSessionManager
    extends ReplicantJsonSessionManager<TestSession>
  {
    @Override
    public boolean saveEntityMessages( @Nullable final String sessionID,
                                       @Nullable final String requestID,
                                       @Nonnull final Collection<EntityMessage> messages,
                                       @Nullable final ChangeSet changeSet )
    {
      return false;
    }

    @Nonnull
    @Override
    protected TestSession newSessionInfo()
    {
      return new TestSession( UUID.randomUUID().toString() );
    }
  }
}
