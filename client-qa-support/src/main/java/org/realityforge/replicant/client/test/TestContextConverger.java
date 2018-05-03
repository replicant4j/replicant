package org.realityforge.replicant.client.test;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import org.realityforge.replicant.client.converger.ContextConverger;
import org.realityforge.replicant.client.runtime.ReplicantClientSystem;

public class TestContextConverger
  extends ContextConverger
{
  private boolean _active;

  @Inject
  public TestContextConverger( @Nonnull final ReplicantClientSystem replicantClientSystem )
  {
    super( replicantClientSystem );
  }

  @Override
  public boolean isActive()
  {
    return _active;
  }

  @Override
  public void activate()
  {
    deactivate();
    _active = true;
  }

  @Override
  public void deactivate()
  {
    unpause();
    _active = false;
  }
}
