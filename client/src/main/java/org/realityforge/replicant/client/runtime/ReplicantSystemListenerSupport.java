package org.realityforge.replicant.client.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;

final class ReplicantSystemListenerSupport
  implements ReplicantSystemListener
{
  private final ArrayList<ReplicantSystemListener> _listeners = new ArrayList<>();
  private final List<ReplicantSystemListener> _roListeners = Collections.unmodifiableList( _listeners );

  synchronized void addListener( @Nonnull final ReplicantSystemListener listener )
  {
    Objects.requireNonNull( listener );
    if ( !_listeners.contains( listener ) )
    {
      _listeners.add( listener );
    }
  }

  synchronized void removeListener( @Nonnull final ReplicantSystemListener listener )
  {
    _listeners.remove( Objects.requireNonNull( listener ) );
  }

  List<ReplicantSystemListener> getListeners()
  {
    return _roListeners;
  }

  @Override
  public void stateChanged( @Nonnull final ReplicantClientSystem system,
                            @Nonnull final ReplicantClientSystem.State newState,
                            @Nonnull final ReplicantClientSystem.State oldState )
  {
    cloneListeners().forEach( l -> l.stateChanged( system, newState, oldState ) );
  }

  /**
   * Return a copy of the listeners.
   * This avoids concurrent operation exceptions.
   */
  @Nonnull
  private synchronized ArrayList<ReplicantSystemListener> cloneListeners()
  {
    final ArrayList<ReplicantSystemListener> listeners = new ArrayList<>( _listeners.size() );
    listeners.addAll( _listeners );
    return listeners;
  }
}
