package org.realityforge.replicant.client.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;

public final class ReplicantSystemListenerSupport
  implements ReplicantSystemListener
{
  private final ArrayList<ReplicantSystemListener> _listeners = new ArrayList<>();
  private final List<ReplicantSystemListener> _roListeners = Collections.unmodifiableList( _listeners );

  public boolean addListener( @Nonnull final ReplicantSystemListener listener )
  {
    Objects.requireNonNull( listener );
    if ( !_listeners.contains( listener ) )
    {
      _listeners.add( listener );
      return true;
    }
    else
    {
      return false;
    }
  }

  public boolean removeListener( @Nonnull final ReplicantSystemListener listener )
  {
    Objects.requireNonNull( listener );
    return _listeners.remove( listener );
  }

  public List<ReplicantSystemListener> getListeners()
  {
    return _roListeners;
  }

  @Override
  public void stateChanged( @Nonnull final ReplicantClientSystem system,
                            @Nonnull final ReplicantClientSystem.State newState,
                            @Nonnull final ReplicantClientSystem.State oldState )
  {
    _listeners.forEach( l -> l.stateChanged( system, newState, oldState ) );
  }
}
