package org.realityforge.replicant.client.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;

public final class AreaOfInterestListenerSupport
  implements AreaOfInterestListener
{
  private final ArrayList<AreaOfInterestListener> _listeners = new ArrayList<>();
  private final List<AreaOfInterestListener> _roListeners = Collections.unmodifiableList( _listeners );

  public synchronized boolean addListener( @Nonnull final AreaOfInterestListener listener )
  {
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

  public synchronized boolean removeListener( @Nonnull final AreaOfInterestListener listener )
  {
    return _listeners.remove( listener );
  }

  @Nonnull
  public List<AreaOfInterestListener> getListeners()
  {
    return _roListeners;
  }

  @Override
  public void subscriptionCreated( @Nonnull final Subscription subscription )
  {
    cloneListeners().forEach( l -> l.subscriptionCreated( subscription ) );
  }

  @Override
  public void subscriptionUpdated( @Nonnull final Subscription subscription )
  {
    cloneListeners().forEach( l -> l.subscriptionUpdated( subscription ) );
  }

  @Override
  public void subscriptionDeleted( @Nonnull final Subscription subscription )
  {
    cloneListeners().forEach( l -> l.subscriptionDeleted( subscription ) );
  }

  /**
   * Return a copy of the listeners.
   * This avoids concurrent operation exceptions.
   */
  @Nonnull
  protected synchronized ArrayList<AreaOfInterestListener> cloneListeners()
  {
    final ArrayList<AreaOfInterestListener> listeners = new ArrayList<>( _listeners.size() );
    listeners.addAll( _listeners );
    return listeners;
  }
}
