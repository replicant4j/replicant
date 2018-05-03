package org.realityforge.replicant.client.converger;

import javax.annotation.Nonnull;
import org.realityforge.replicant.client.runtime.ReplicantClientSystem;

final class TestContextConverger
  extends ContextConverger
{
  TestContextConverger( @Nonnull final ReplicantClientSystem replicantClientSystem )
  {
    super( replicantClientSystem );
  }

  @Override
  public void activate()
  {
  }

  @Override
  public void deactivate()
  {
  }

  @Override
  public boolean isActive()
  {
    return true;
  }
}
