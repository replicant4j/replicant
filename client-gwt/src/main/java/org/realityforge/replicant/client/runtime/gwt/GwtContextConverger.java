package org.realityforge.replicant.client.runtime.gwt;

import arez.annotations.ArezComponent;
import com.google.gwt.user.client.Timer;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.inject.Singleton;
import org.realityforge.replicant.client.EntitySubscriptionManager;
import org.realityforge.replicant.client.runtime.AreaOfInterestService;
import org.realityforge.replicant.client.runtime.ContextConverger;
import org.realityforge.replicant.client.runtime.ReplicantClientSystem;

@Singleton
@ArezComponent
public abstract class GwtContextConverger
  extends ContextConverger
{
  private final EntitySubscriptionManager _subscriptionManager;
  private final AreaOfInterestService _areaOfInterestService;
  private final ReplicantClientSystem _replicantClientSystem;
  private Timer _timer;

  GwtContextConverger( @Nonnull final EntitySubscriptionManager subscriptionManager,
                       @Nonnull final AreaOfInterestService areaOfInterestService,
                       @Nonnull final ReplicantClientSystem replicantClientSystem )
  {
    _subscriptionManager = Objects.requireNonNull( subscriptionManager );
    _areaOfInterestService = Objects.requireNonNull( areaOfInterestService );
    _replicantClientSystem = Objects.requireNonNull( replicantClientSystem );
    addListeners();
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

  @Override
  @Nonnull
  protected EntitySubscriptionManager getSubscriptionManager()
  {
    return _subscriptionManager;
  }

  @Override
  @Nonnull
  protected AreaOfInterestService getAreaOfInterestService()
  {
    return _areaOfInterestService;
  }

  @Override
  @Nonnull
  protected ReplicantClientSystem getReplicantClientSystem()
  {
    return _replicantClientSystem;
  }
}
