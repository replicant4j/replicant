package org.realityforge.replicant.server.ee;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
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
  private static final Logger LOG = Logger.getLogger( AbstractReplicationInterceptor.class.getName() );

  @Resource
  private TransactionSynchronizationRegistry _registry;

  @AroundInvoke
  public Object businessIntercept( final InvocationContext context )
    throws Exception
  {
    final String invocationKey = getInvocationKey( context );
    final String sessionID = (String) ReplicantContextHolder.remove( ReplicantContext.SESSION_ID_KEY );
    final String requestID = (String) ReplicantContextHolder.remove( ReplicantContext.REQUEST_ID_KEY );

    // Clear the context completely, in case the caller is not a GwtRpcServlet or does not reset the state.
    ReplicantContextHolder.clean();

    ReplicationRequestUtil.startReplication( _registry, invocationKey, sessionID, requestID );

    if ( LOG.isLoggable( Level.FINE ) )
    {
      LOG.fine( "Starting invocation of " + invocationKey +
                " ContextData: " + context.getContextData() +
                " ReplicantContext: " + ReplicantContextHolder.getContext() +
                " Thread: " + Thread.currentThread().getId() );
    }

    try
    {
      return context.proceed();
    }
    finally
    {
      final boolean requestComplete =
        ReplicationRequestUtil.completeReplication( _registry, getEntityManager(), getEndpoint() );

      // Clear the context completely to ensure it contains only the completion key.
      ReplicantContextHolder.clean();

      ReplicantContextHolder.put( ReplicantContext.REQUEST_COMPLETE_KEY, requestComplete ? "1" : "0" );

      if ( LOG.isLoggable( Level.FINE ) )
      {
        LOG.fine( "Completed invocation of " + invocationKey +
                  " ContextData: " + context.getContextData() +
                  " ReplicantContext: " + ReplicantContextHolder.getContext() +
                  " Thread: " + Thread.currentThread().getId() );
      }
    }
  }

  @Nonnull
  protected abstract EntityManager getEntityManager();

  @Nonnull
  protected abstract EntityMessageEndpoint getEndpoint();

  @SuppressWarnings( "EjbProhibitedPackageUsageInspection" )
  private String getInvocationKey( @Nonnull final InvocationContext context )
  {
    final Method method = context.getMethod();
    if ( null != method )
    {
      return method.getDeclaringClass().getName() + "." + method.getName();
    }
    final Constructor constructor = context.getConstructor();
    if ( null != constructor )
    {
      return constructor.getDeclaringClass().getName() + "." + constructor.getName();
    }
    return "Unknown";
  }
}
