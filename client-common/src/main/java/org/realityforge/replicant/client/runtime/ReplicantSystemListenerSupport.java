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

  public synchronized boolean addListener( @Nonnull final ReplicantSystemListener listener )
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

  public synchronized boolean removeListener( @Nonnull final ReplicantSystemListener listener )
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
    cloneListeners().forEach( l -> l.stateChanged( system, newState, oldState ) );
  }

  /**
   * Return a copy of the listeners.
   * This avoids concurrent operation exceptions.
   */
  @Nonnull
  protected synchronized ArrayList<ReplicantSystemListener> cloneListeners()
  {
    final ArrayList<ReplicantSystemListener> listeners = new ArrayList<>( _listeners.size() );
    listeners.addAll( _listeners );
    return listeners;
  }
}
