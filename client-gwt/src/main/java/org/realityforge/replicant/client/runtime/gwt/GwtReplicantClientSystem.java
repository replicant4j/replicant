package org.realityforge.replicant.client.runtime.gwt;

import com.google.gwt.user.client.Timer;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.realityforge.replicant.client.runtime.DataLoaderEntry;
import org.realityforge.replicant.client.runtime.ReplicantClientSystem;

@Singleton
public final class GwtReplicantClientSystem
  extends ReplicantClientSystem
{
  private final Timer _timer;

  @SuppressWarnings( "CdiInjectionPointsInspection" )
  @Inject
  public GwtReplicantClientSystem( @Nonnull final DataLoaderEntry[] dataLoaders )
  {
    setDataLoaders( dataLoaders );
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
  protected void release()
  {
    cancelTimer();
    super.release();
  }

  private void cancelTimer()
  {
    _timer.cancel();
  }
}
