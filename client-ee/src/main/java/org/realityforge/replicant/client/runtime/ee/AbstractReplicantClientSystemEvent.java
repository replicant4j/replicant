package org.realityforge.replicant.client.runtime.ee;

import java.util.Objects;
import javax.annotation.Nonnull;
import org.realityforge.replicant.client.runtime.ReplicantClientSystem;

public abstract class AbstractReplicantClientSystemEvent
{
  private final ReplicantClientSystem _replicantClientSystem;

  public AbstractReplicantClientSystemEvent( @Nonnull final ReplicantClientSystem replicantClientSystem )
  {
    _replicantClientSystem = Objects.requireNonNull( replicantClientSystem );
  }

  @Nonnull
  public ReplicantClientSystem getReplicantClientSystem()
  {
    return _replicantClientSystem;
  }
}
