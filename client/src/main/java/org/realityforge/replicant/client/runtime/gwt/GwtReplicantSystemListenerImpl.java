package org.realityforge.replicant.client.runtime.gwt;

import com.google.web.bindery.event.shared.EventBus;
import javax.annotation.Nonnull;
import org.realityforge.replicant.client.runtime.ReplicantClientSystem;
import org.realityforge.replicant.client.runtime.ReplicantSystemListener;

public class GwtReplicantSystemListenerImpl
  implements ReplicantSystemListener
{
  @Nonnull
  private final EventBus _eventBus;

  public GwtReplicantSystemListenerImpl( @Nonnull final EventBus eventBus )
  {
    _eventBus = eventBus;
  }

  @Override
  public void stateChanged( @Nonnull final ReplicantClientSystem system,
                            @Nonnull final ReplicantClientSystem.State newState,
                            @Nonnull final ReplicantClientSystem.State oldState )
  {
    _eventBus.fireEvent( new StateChangedEvent( system, newState, oldState ) );
  }
}
