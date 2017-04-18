package org.realityforge.replicant.client.runtime.ee;

import javax.enterprise.inject.Typed;
import javax.inject.Singleton;
import javax.transaction.Transactional;
import org.realityforge.replicant.client.EntitySystem;
import org.realityforge.replicant.client.EntitySystemImpl;

@Singleton
@Transactional( Transactional.TxType.NOT_SUPPORTED )
@Typed( EntitySystem.class )
public class EeEntitySystemImpl
  extends EntitySystemImpl
{
}
