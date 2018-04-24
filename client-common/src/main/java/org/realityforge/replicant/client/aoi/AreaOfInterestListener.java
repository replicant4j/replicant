package org.realityforge.replicant.client.aoi;

import javax.annotation.Nonnull;
import org.realityforge.replicant.client.Channel;

public interface AreaOfInterestListener
{
  void channelCreated( @Nonnull Channel channel );

  void channelUpdated( @Nonnull Channel channel );

  void channelDeleted( @Nonnull Channel channel );

  class Adapter
    implements AreaOfInterestListener
  {
    @Override
    public void channelCreated( @Nonnull final Channel channel )
    {
    }

    @Override
    public void channelUpdated( @Nonnull final Channel channel )
    {
    }

    @Override
    public void channelDeleted( @Nonnull final Channel channel )
    {
    }
  }
}
