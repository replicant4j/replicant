package org.realityforge.replicant.client.runtime.gwt;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsonUtils;
import com.google.gwt.user.client.Timer;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.realityforge.replicant.client.EntitySubscriptionManager;
import org.realityforge.replicant.client.runtime.AreaOfInterestService;
import org.realityforge.replicant.client.runtime.ContextConvergerImpl;
import org.realityforge.replicant.client.runtime.ReplicantClientSystem;

@Singleton
public class GwtContextConvergerImpl
  extends ContextConvergerImpl
{
  private final EntitySubscriptionManager _subscriptionManager;
  private final AreaOfInterestService _areaOfInterestService;
  private final ReplicantClientSystem _replicantClientSystem;
  private Timer _timer;

  @Inject
  public GwtContextConvergerImpl( @Nonnull final EntitySubscriptionManager subscriptionManager,
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

  @Override
  @Nullable
  protected String filterToString( @Nullable final Object filter )
  {
    return JsonUtils.stringify( (JavaScriptObject) filter );
  }
}
