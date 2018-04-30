package org.realityforge.replicant.client.aoi;

import javax.annotation.Nonnull;
import org.realityforge.replicant.client.Channel;

public interface AreaOfInterestListener
{
  void areaOfInterestCreated( @Nonnull AreaOfInterest areaOfInterest );

  void areaOfInterestUpdated( @Nonnull AreaOfInterest areaOfInterest );

  void areaOfInterestDeleted( @Nonnull AreaOfInterest areaOfInterest );

  class Adapter
    implements AreaOfInterestListener
  {
    @Override
    public void areaOfInterestCreated( @Nonnull final AreaOfInterest areaOfInterest )
    {
    }

    @Override
    public void areaOfInterestUpdated( @Nonnull final AreaOfInterest areaOfInterest )
    {
    }

    @Override
    public void areaOfInterestDeleted( @Nonnull final AreaOfInterest areaOfInterest )
    {
    }
  }
}
