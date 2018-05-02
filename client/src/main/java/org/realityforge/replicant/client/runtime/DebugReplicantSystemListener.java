package org.realityforge.replicant.client.runtime;

import java.util.logging.Logger;
import javax.annotation.Nonnull;

public class DebugReplicantSystemListener
  implements ReplicantSystemListener
{
  private static final Logger LOG = Logger.getLogger( DebugReplicantSystemListener.class.getName() );

  /**
   * {@inheritDoc}
   */
  @Override
  public void stateChanged( @Nonnull final ReplicantClientSystem system,
                            @Nonnull final ReplicantClientSystem.State newState,
                            @Nonnull final ReplicantClientSystem.State oldState )
  {
    LOG.warning( "ReplicantSystem.stateChanged: NewState: " + newState + " OldState: " + oldState );
  }
}
