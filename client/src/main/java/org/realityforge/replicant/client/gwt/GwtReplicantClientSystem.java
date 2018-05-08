package org.realityforge.replicant.client.gwt;

import arez.annotations.ArezComponent;
import arez.annotations.PreDispose;
import com.google.gwt.user.client.Timer;
import javax.annotation.Nonnull;
import javax.inject.Singleton;
import org.realityforge.replicant.client.runtime.ReplicantClientSystem;

@Singleton
@ArezComponent
public abstract class GwtReplicantClientSystem
  extends ReplicantClientSystem
{
  private final Timer _timer;

  @Nonnull
  public static GwtReplicantClientSystem create()
  {
    return new Arez_GwtReplicantClientSystem();
  }

  GwtReplicantClientSystem()
  {
    _timer = new Timer()
    {
      @Override
      public void run()
      {
        converge();
      }
    };
    _timer.scheduleRepeating( CONVERGE_DELAY_IN_MS );
  }

  @PreDispose
  final void preDispose()
  {
    cancelTimer();
  }

  private void cancelTimer()
  {
    _timer.cancel();
  }
}
