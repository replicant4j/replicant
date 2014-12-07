package org.realityforge.replicant.server.ee;

import javax.annotation.Resource;
import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;
import javax.persistence.EntityManager;
import javax.transaction.TransactionSynchronizationRegistry;
import org.realityforge.replicant.server.EntityMessageEndpoint;
import org.realityforge.replicant.shared.transport.ReplicantContext;

/**
 * A base class for an interceptor that should be applied to all services that need to send out EntityChange messages
 * on completion.
 */
public abstract class AbstractReplicationInterceptor
{
  @Resource
  private TransactionSynchronizationRegistry _registry;

  @AroundInvoke
  public Object businessIntercept( final InvocationContext context )
    throws Exception
  {
    final int depth = ReplicationRequestUtil.getReplicationCallDepth();
    if ( 0 == depth )
    {
      final String sessionID = (String) ReplicantContextHolder.get( ReplicantContext.SESSION_ID_KEY );
      if ( null != sessionID )
      {
        ReplicantContextHolder.remove( ReplicantContext.SESSION_ID_KEY );
      }
      final String requestID = (String) ReplicantContextHolder.get( ReplicantContext.REQUEST_ID_KEY );
      if ( null != requestID )
      {
        ReplicantContextHolder.remove( ReplicantContext.REQUEST_ID_KEY );
      }

      ReplicationRequestUtil.startReplication( _registry, sessionID, requestID );
    }
    ReplicationRequestUtil.setReplicationCallDepth( depth + 1 );
    try
    {
      return context.proceed();
    }
    finally
    {
      ReplicationRequestUtil.setReplicationCallDepth( depth );
      if ( 0 == depth )
      {
        final boolean requestComplete =
          ReplicationRequestUtil.completeReplication( _registry, getEntityManager(), getEndpoint() );
        if ( !ReplicantContextHolder.contains( ReplicantContext.REQUEST_COMPLETE_KEY ) )
        {
          ReplicantContextHolder.put( ReplicantContext.REQUEST_COMPLETE_KEY, requestComplete ? "1" : "0" );
        }
      }
    }
  }

  protected abstract EntityManager getEntityManager();

  protected abstract EntityMessageEndpoint getEndpoint();
}
