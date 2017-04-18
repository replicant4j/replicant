package org.realityforge.replicant.client.runtime.ee;

import javax.annotation.Nonnull;
import org.realityforge.replicant.client.runtime.ReplicantClientSystem;

public class StateChangedEvent
  extends AbstractReplicantClientSystemEvent
{
  @Nonnull
  private final ReplicantClientSystem.State _newState;
  @Nonnull
  private final ReplicantClientSystem.State _oldState;

  public StateChangedEvent( @Nonnull final ReplicantClientSystem replicantClientSystem,
                            @Nonnull final ReplicantClientSystem.State newState,
                            @Nonnull final ReplicantClientSystem.State oldState )
  {
    super( replicantClientSystem );
    _newState = newState;
    _oldState = oldState;
  }

  @Nonnull
  public ReplicantClientSystem.State getNewState()
  {
    return _newState;
  }

  @Nonnull
  public ReplicantClientSystem.State getOldState()
  {
    return _oldState;
  }

  public String toString()
  {
    return "StateChanged[NewState=" + getNewState() + ",OldState=" + getOldState() + "]";
  }
}
