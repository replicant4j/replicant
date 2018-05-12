package org.realityforge.replicant.client.transport;

import javax.annotation.Nullable;
import replicant.RequestEntry;

public interface RequestAction
{
  void invokeRequest( @Nullable ClientSession session, @Nullable RequestEntry request );
}
