package org.realityforge.replicant.client.runtime;

import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface ContextConverger
{
  /**
   * Set action that is run prior to converging.
   * This is typically used to ensure the subscriptions are uptodate prior to attempting to convergeStep.
   */
  void setPreConvergeAction( @Nullable Runnable preConvergeAction );

  void activate();

  void deactivate();

  boolean isActive();

  void pause();

  void resume();

  boolean isPaused();

  /**
   * Pause the converger for the duration of the action.
   */
  default void pauseAndRun( @Nonnull final Runnable action )
  {
    pause();
    try
    {
      Objects.requireNonNull( action ).run();
    }
    finally
    {
      resume();
    }
  }
}
