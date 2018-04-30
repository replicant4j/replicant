package org.realityforge.replicant.client.aoi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;

final class AreaOfInterestListenerSupport
  implements AreaOfInterestListener
{
  private final ArrayList<AreaOfInterestListener> _listeners = new ArrayList<>();
  private final List<AreaOfInterestListener> _roListeners = Collections.unmodifiableList( _listeners );

  synchronized boolean addListener( @Nonnull final AreaOfInterestListener listener )
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

  synchronized boolean removeListener( @Nonnull final AreaOfInterestListener listener )
  {
    return _listeners.remove( listener );
  }

  @Nonnull
  List<AreaOfInterestListener> getListeners()
  {
    return _roListeners;
  }

  @Override
  public void areaOfInterestCreated( @Nonnull final AreaOfInterest areaOfInterest )
  {
    cloneListeners().forEach( l -> l.areaOfInterestCreated( areaOfInterest ) );
  }

  @Override
  public void areaOfInterestUpdated( @Nonnull final AreaOfInterest areaOfInterest )
  {
    cloneListeners().forEach( l -> l.areaOfInterestUpdated( areaOfInterest ) );
  }

  @Override
  public void areaOfInterestDeleted( @Nonnull final AreaOfInterest areaOfInterest )
  {
    cloneListeners().forEach( l -> l.areaOfInterestDeleted( areaOfInterest ) );
  }

  /**
   * Return a copy of the listeners.
   * This avoids concurrent operation exceptions.
   */
  @Nonnull
  private synchronized ArrayList<AreaOfInterestListener> cloneListeners()
  {
    final ArrayList<AreaOfInterestListener> listeners = new ArrayList<>( _listeners.size() );
    listeners.addAll( _listeners );
    return listeners;
  }
}
