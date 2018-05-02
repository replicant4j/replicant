package org.realityforge.replicant.client.gwt;

import arez.annotations.ArezComponent;
import com.google.gwt.user.client.Timer;
import javax.annotation.Nonnull;
import javax.inject.Singleton;
import org.realityforge.replicant.client.aoi.AreaOfInterestService;
import org.realityforge.replicant.client.converger.ContextConverger;
import org.realityforge.replicant.client.runtime.ReplicantClientSystem;
import org.realityforge.replicant.client.subscription.SubscriptionService;

@Singleton
@ArezComponent
public abstract class GwtContextConverger
  extends ContextConverger
{
  private Timer _timer;

  GwtContextConverger( @Nonnull final SubscriptionService subscriptionService,
                       @Nonnull final AreaOfInterestService areaOfInterestService,
                       @Nonnull final ReplicantClientSystem replicantClientSystem )
  {
    super( subscriptionService, areaOfInterestService, replicantClientSystem );
  }

  @Override
  public boolean isActive()
  {
    return null != _timer;
  }

  @Override
  public void activate()
  {
    deactivate();
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

  @Override
  public void deactivate()
  {
    cancelTimer();
    unpause();
  }

  @Override
  protected void release()
  {
    cancelTimer();
    super.release();
  }

  private void cancelTimer()
  {
    if ( null != _timer )
    {
      _timer.cancel();
      _timer = null;
    }
  }
}
