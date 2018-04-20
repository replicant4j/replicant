package org.realityforge.replicant.client.runtime;

import javax.annotation.Nonnull;
import org.realityforge.replicant.client.Channel;

public interface AreaOfInterestListener
{
  void channelCreated( @Nonnull Channel channel );

  void channelUpdated( @Nonnull Channel channel );

  void channelDeleted( @Nonnull Channel channel );
}
