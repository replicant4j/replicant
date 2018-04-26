package org.realityforge.replicant.client.runtime.ee;

import javax.annotation.Nullable;
import org.realityforge.replicant.client.converger.ContextConverger;
import org.realityforge.replicant.shared.ee.JsonUtil;

public abstract class AbstractEeContextConverger
  extends ContextConverger
{
  private boolean _active;

  protected void postConstruct()
  {
    addListeners();
  }

  protected void preDestroy()
  {
    deactivate();
    release();
  }

  @Override
  public void converge()
  {
    if ( isActive() )
    {
      super.converge();
    }
  }

  @Override
  public void activate()
  {
    _active = true;
  }

  @Override
  public void deactivate()
  {
    _active = false;
  }

  @Override
  public boolean isActive()
  {
    return _active;
  }

  @Override
  @Nullable
  protected String filterToString( @Nullable final Object filter )
  {
    return JsonUtil.toJsonString( filter );
  }
}
