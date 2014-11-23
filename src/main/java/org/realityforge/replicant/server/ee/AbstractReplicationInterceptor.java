package org.realityforge.replicant.server.ee;

import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;
import org.realityforge.replicant.shared.transport.ReplicantContext;

/**
 * A base class for an interceptor that should be applied to all services that need to send out EntityChange messages
 * on completion.
 */
public abstract class AbstractReplicationInterceptor
{
  @AroundInvoke
  public Object businessIntercept( final InvocationContext context )
    throws Exception
  {
    final int depth = getRequestManager().getReplicationCallDepth();
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

      getRequestManager().startReplication( sessionID, requestID );
    }
    getRequestManager().setReplicationCallDepth( depth + 1 );
    try
    {
      return context.proceed();
    }
    finally
    {
      getRequestManager().setReplicationCallDepth( depth );
      if ( 0 == depth )
      {
        final boolean requestComplete = getRequestManager().completeReplication();
        if ( !ReplicantContextHolder.contains( ReplicantContext.REQUEST_COMPLETE_KEY ) )
        {
          ReplicantContextHolder.put( ReplicantContext.REQUEST_COMPLETE_KEY, requestComplete ? "1" : "0" );
        }
      }
    }
  }

  protected abstract ReplicationRequestManager getRequestManager();
}
