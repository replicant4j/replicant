package org.realityforge.replicant.server.ee;

import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.persistence.EntityManager;
import javax.transaction.TransactionSynchronizationRegistry;
import org.realityforge.replicant.server.EntityMessageEndpoint;
import org.realityforge.replicant.shared.transport.ReplicantContext;

/**
 * A base class that used to wrap around invocation of an action.
 */
public abstract class AbstractInvocationAdapter
{
  private static final Logger LOG = Logger.getLogger( AbstractInvocationAdapter.class.getName() );

  protected <T> T invokeAction( @Nonnull final String key, @Nonnull final Callable<T> action )
    throws Exception
  {
    final String sessionID = (String) ReplicantContextHolder.remove( ReplicantContext.SESSION_ID_KEY );
    final String requestID = (String) ReplicantContextHolder.remove( ReplicantContext.REQUEST_ID_KEY );

    // Clear the context completely, in case the caller is not a GwtRpcServlet or does not reset the state.
    ReplicantContextHolder.clean();

    ReplicationRequestUtil.startReplication( getRegistry(), key, sessionID, requestID );

    if ( LOG.isLoggable( Level.FINE ) )
    {
      LOG.fine( "Starting invocation of " + key +
                " ReplicantContext: " + ReplicantContextHolder.getContext() +
                " Thread: " + Thread.currentThread().getId() );
    }

    try
    {
      return action.call();
    }
    finally
    {
      final boolean requestComplete =
        ReplicationRequestUtil.completeReplication( getRegistry(), getEntityManager(), getEndpoint() );

      // Clear the context completely to ensure it contains only the completion key.
      ReplicantContextHolder.clean();

      ReplicantContextHolder.put( ReplicantContext.REQUEST_COMPLETE_KEY, requestComplete ? "1" : "0" );

      if ( LOG.isLoggable( Level.FINE ) )
      {
        LOG.fine( "Completed invocation of " + key +
                  " ReplicantContext: " + ReplicantContextHolder.getContext() +
                  " Thread: " + Thread.currentThread().getId() );
      }
    }
  }

  @Nonnull
  protected abstract EntityManager getEntityManager();

  @Nonnull
  protected abstract EntityMessageEndpoint getEndpoint();

  @Nonnull
  protected abstract TransactionSynchronizationRegistry getRegistry();
}
