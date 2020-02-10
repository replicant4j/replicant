package org.realityforge.replicant.server.ee;

import javax.annotation.Nonnull;
import javax.annotation.Resource;
import javax.inject.Inject;
import javax.transaction.TransactionSynchronizationRegistry;
import org.realityforge.replicant.server.EntityMessageEndpoint;
import org.realityforge.replicant.server.transport.ReplicantSessionManager;

public abstract class AbstractEeReplicantEndpoint
  extends AbstractReplicantEndpoint
{
  @Inject
  private ReplicantSessionManager _sessionManager;
  @Resource
  private TransactionSynchronizationRegistry _registry;
  @Inject
  private EntityMessageEndpoint _endpoint;

  @Nonnull
  @Override
  protected ReplicantSessionManager getSessionManager()
  {
    return _sessionManager;
  }

  @Nonnull
  @Override
  protected TransactionSynchronizationRegistry getRegistry()
  {
    return _registry;
  }

  @Nonnull
  @Override
  protected EntityMessageEndpoint getEndpoint()
  {
    return _endpoint;
  }
}
