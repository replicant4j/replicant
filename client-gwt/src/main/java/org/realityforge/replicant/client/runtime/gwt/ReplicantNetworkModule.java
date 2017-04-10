package org.realityforge.replicant.client.runtime.gwt;

import com.google.gwt.inject.client.AbstractGinModule;
import org.realityforge.replicant.client.runtime.AreaOfInterestService;
import org.realityforge.replicant.client.runtime.AreaOfInterestServiceImpl;

public class ReplicantNetworkModule
  extends AbstractGinModule
{
  @Override
  protected void configure()
  {
    bind( AreaOfInterestService.class ).to( AreaOfInterestServiceImpl.class ).asEagerSingleton();
  }
}
