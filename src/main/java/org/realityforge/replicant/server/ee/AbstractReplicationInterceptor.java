package org.realityforge.replicant.server.ee;

import java.util.Collection;
import java.util.Collections;
import javax.annotation.Resource;
import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;
import javax.persistence.EntityManager;
import javax.transaction.TransactionSynchronizationRegistry;
import org.realityforge.replicant.server.EntityMessage;
import org.realityforge.replicant.server.EntityMessageEndpoint;
import org.realityforge.replicant.server.EntityMessageSet;
import org.realityforge.replicant.shared.transport.ReplicantContext;

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
  public Object businessIntercept( final InvocationContext context )
    throws Exception
  {
    final Integer depth = (Integer) _registry.getResource( REPLICATION_TX_DEPTH );
    if ( null == depth )
    {
      final String sessionID = (String) ReplicantContextHolder.get( ReplicantContext.SESSION_ID_KEY );
      if ( null != sessionID )
      {
        _registry.putResource( ReplicantContext.SESSION_ID_KEY, sessionID );
        ReplicantContextHolder.remove( ReplicantContext.SESSION_ID_KEY );
      }
      final String requestID = (String) ReplicantContextHolder.get( ReplicantContext.REQUEST_ID_KEY );
      if ( null != requestID )
      {
        _registry.putResource( ReplicantContext.REQUEST_ID_KEY, requestID );
        ReplicantContextHolder.remove( ReplicantContext.REQUEST_ID_KEY );
      }
    }
    final String sessionID = (String) _registry.getResource( ReplicantContext.SESSION_ID_KEY );
    final String requestID = (String) _registry.getResource( ReplicantContext.REQUEST_ID_KEY );
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
        boolean requestComplete = true;
        if( getEntityManager().isOpen() )
        {
          getEntityManager().flush();
          final EntityMessageSet messageSet = EntityMessageCacheUtil.removeEntityMessageSet( _registry );
          final EntityMessageSet sessionMessageSet = EntityMessageCacheUtil.removeSessionEntityMessageSet( _registry );
          if ( ( null != messageSet || null != sessionMessageSet ) && !_registry.getRollbackOnly() )
          {
            final Collection<EntityMessage> messages =
              null == messageSet ? Collections.<EntityMessage>emptySet() : messageSet.getEntityMessages();
            final Collection<EntityMessage> sessionMessages =
              null == sessionMessageSet ? Collections.<EntityMessage>emptySet() : sessionMessageSet.getEntityMessages();
            if( sessionMessages.size() > 0 || messages.size() > 0 )
            {
              requestComplete = !getEndpoint().saveEntityMessages( sessionID, requestID, messages, sessionMessages );
            }
          }
        }
        ReplicantContextHolder.put( ReplicantContext.REQUEST_COMPLETE_KEY, requestComplete ? "1" : "0" );
      }
    }
  }

  protected abstract EntityManager getEntityManager();

  protected abstract EntityMessageEndpoint getEndpoint();
}

