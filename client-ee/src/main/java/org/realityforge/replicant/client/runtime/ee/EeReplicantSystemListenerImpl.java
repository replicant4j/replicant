package org.realityforge.replicant.client.runtime.ee;

import java.util.Objects;
import javax.annotation.Nonnull;
import javax.enterprise.inject.spi.BeanManager;
import org.realityforge.replicant.client.runtime.ReplicantClientSystem;
import org.realityforge.replicant.client.runtime.ReplicantSystemListener;

public class EeReplicantSystemListenerImpl
  implements ReplicantSystemListener
{
  private final BeanManager _beanManager;

  public EeReplicantSystemListenerImpl( @Nonnull final BeanManager beanManager )
  {
    _beanManager = Objects.requireNonNull( beanManager );
  }

  @Override
  public void stateChanged( @Nonnull final ReplicantClientSystem system,
                            @Nonnull final ReplicantClientSystem.State newState,
                            @Nonnull final ReplicantClientSystem.State oldState )
  {
    fireEvent( new StateChangedEvent( system, newState, oldState ) );
  }

  protected void fireEvent( @Nonnull final Object event )
  {
    _beanManager.fireEvent( event );
  }
}
