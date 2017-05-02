package org.realityforge.replicant.client.transport;

import javax.annotation.Nullable;

public interface RequestAction
{
  void invokeReqest( @Nullable ClientSession session, @Nullable RequestEntry request );
}
