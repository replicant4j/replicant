package org.realityforge.replicant.client.runtime;

import java.util.ArrayList;
import javax.annotation.Nonnull;

public final class AreaOfInterestListenerSupport
  implements AreaOfInterestListener
{
  private final ArrayList<AreaOfInterestListener> _listeners = new ArrayList<>();

  public boolean addListener( @Nonnull final AreaOfInterestListener listener )
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

  public boolean removeListener( @Nonnull final AreaOfInterestListener listener )
  {
    return _listeners.remove( listener );
  }

  @Override
  public void scopeCreated( @Nonnull final Scope scope )
  {
    _listeners.forEach( l -> l.scopeCreated( scope ) );
  }

  @Override
  public void scopeDeleted( @Nonnull final Scope scope )
  {
    _listeners.forEach( l -> l.scopeDeleted( scope ) );
  }

  @Override
  public void subscriptionCreated( @Nonnull final Subscription subscription )
  {
    _listeners.forEach( l -> l.subscriptionCreated( subscription ) );
  }

  @Override
  public void subscriptionUpdated( @Nonnull final Subscription subscription )
  {
    _listeners.forEach( l -> l.subscriptionUpdated( subscription ) );
  }

  @Override
  public void subscriptionDeleted( @Nonnull final Subscription subscription )
  {
    _listeners.forEach( l -> l.subscriptionDeleted( subscription ) );
  }
}
