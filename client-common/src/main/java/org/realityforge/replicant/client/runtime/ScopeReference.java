package org.realityforge.replicant.client.runtime;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class ScopeReference
{
  @Nullable
  private Scope _scope;

  public ScopeReference( @Nonnull final Scope scope )
  {
    _scope = scope;
  }

  @Nonnull
  public Scope getScope()
  {
    if ( null == _scope )
    {
      throw new ReferenceReleasedException();
    }
    return _scope;
  }

  public boolean isActive()
  {
    return null != _scope;
  }

  public boolean hasBeenReleased()
  {
    return !isActive();
  }

  public void release()
  {
    if ( null != _scope )
    {
      _scope.release( this );
      _scope = null;
    }
  }
}
