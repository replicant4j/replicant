package org.realityforge.replicant.client.gwt;

import arez.annotations.ArezComponent;
import com.google.gwt.user.client.Timer;
import javax.annotation.Nonnull;
import javax.inject.Singleton;
import org.realityforge.replicant.client.runtime.DataLoaderEntry;
import org.realityforge.replicant.client.runtime.ReplicantClientSystem;

@Singleton
@ArezComponent
public abstract class GwtReplicantClientSystem
  extends ReplicantClientSystem
{
  private final Timer _timer;

  @Nonnull
  public static GwtReplicantClientSystem create( @Nonnull final DataLoaderEntry[] dataLoaders )
  {
    return new Arez_GwtReplicantClientSystem( dataLoaders );
  }

  GwtReplicantClientSystem( @Nonnull final DataLoaderEntry[] dataLoaders )
  {
    super( dataLoaders );
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
