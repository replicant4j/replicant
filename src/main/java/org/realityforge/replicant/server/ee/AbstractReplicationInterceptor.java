package org.realityforge.replicant.server.ee;

import java.util.Collection;
import javax.annotation.Nonnull;
import javax.annotation.Resource;
import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;
import javax.transaction.TransactionSynchronizationRegistry;
import org.realityforge.replicant.server.EntityMessage;
import org.realityforge.replicant.server.EntityMessageSet;

/**
 * A base class for an interceptor that should be applied to all services that need to send out EntityChange messages
 * on completion.
 */
public abstract class AbstractReplicationInterceptor
{
  /**
   * The key used to access the registry to get the current call depth.
   */
  private static final String REPLICATION_TX_DEPTH = "ReplicationTxDepth";

  @Resource
  private TransactionSynchronizationRegistry _registry;

  @AroundInvoke
  public final Object businessIntercept( final InvocationContext context )
      throws Exception
  {
    final Integer depth = (Integer) _registry.getResource( REPLICATION_TX_DEPTH );
    _registry.putResource( REPLICATION_TX_DEPTH, ( null == depth ? 1 : depth + 1 ) );
    try
    {
      return context.proceed();
    }
    finally
    {
      _registry.putResource( REPLICATION_TX_DEPTH, depth );
      if( null == depth )
      {
        final EntityMessageSet messageSet = EntityMessageCacheUtil.removeEntityMessageSet( _registry );
        if( null != messageSet && !_registry.getRollbackOnly() )
        {
          final Collection<EntityMessage> messages = messageSet.getEntityMessages();
          if( messages.size() > 0 )
          {
            saveEntityMessages( messages );
          }
        }
      }
    }
  }

  protected abstract void saveEntityMessages( @Nonnull Collection<EntityMessage> messages );
}

