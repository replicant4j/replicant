package org.realityforge.replicant.client.runtime.ee;

import javax.enterprise.inject.Typed;
import javax.inject.Singleton;
import javax.transaction.Transactional;
import org.realityforge.replicant.client.runtime.AreaOfInterestService;
import org.realityforge.replicant.client.runtime.AreaOfInterestServiceImpl;

@Singleton
@Transactional( Transactional.TxType.NOT_SUPPORTED )
@Typed( AreaOfInterestService.class )
public class EeAreaOfInterestServiceImpl
  extends AreaOfInterestServiceImpl
{
}
