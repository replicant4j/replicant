package org.realityforge.replicant.server;

import javax.annotation.Nonnull;
import org.realityforge.replicant.server.transport.ReplicantSession;

public class TestSession
  extends ReplicantSession
{
  public TestSession( @Nonnull final String sessionID )
  {
    super( sessionID );
  }
}
