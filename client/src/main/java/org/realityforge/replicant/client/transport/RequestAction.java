package org.realityforge.replicant.client.transport;

import javax.annotation.Nullable;
import replicant.RequestEntry;

public interface RequestAction
{
  void invokeRequest( @Nullable Connection connection, @Nullable RequestEntry request );
}
