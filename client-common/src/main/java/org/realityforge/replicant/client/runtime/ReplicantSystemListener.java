package org.realityforge.replicant.client.runtime;

import javax.annotation.Nonnull;

public interface ReplicantSystemListener
{
  void stateChanged( @Nonnull ReplicantClientSystem system,
                     @Nonnull ReplicantClientSystem.State newState,
                     @Nonnull ReplicantClientSystem.State oldState );
}
