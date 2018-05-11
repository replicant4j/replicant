package org.realityforge.replicant.client.transport;

import javax.annotation.Nullable;

public interface RequestAction
{
  void invokeRequest( @Nullable ClientSession session, @Nullable RequestEntry request );
}
