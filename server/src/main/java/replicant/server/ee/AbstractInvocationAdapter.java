package replicant.server.ee;

import java.util.concurrent.Callable;
import javax.annotation.Nonnull;
import javax.persistence.EntityManager;
import javax.transaction.TransactionSynchronizationRegistry;
import replicant.server.EntityMessageEndpoint;
import replicant.server.ServerConstants;
import replicant.server.transport.ReplicantSession;
import replicant.server.transport.ReplicantSessionManager;

/**
 * A base class that used to wrap around invocation of an action.
 */
@SuppressWarnings( "WeakerAccess" )
public abstract class AbstractInvocationAdapter
{
  protected <T> T invokeAction( @Nonnull final String key, @Nonnull final Callable<T> action )
    throws Exception
  {
    final String sessionId = (String) ReplicantContextHolder.remove( ServerConstants.SESSION_ID_KEY );
    final Integer requestId = (Integer) ReplicantContextHolder.remove( ServerConstants.REQUEST_ID_KEY );
    final ReplicantSession session =
      null != sessionId ? getReplicantSessionManager().getSession( sessionId ) : null;
    return ReplicationRequestUtil.runRequest( getRegistry(),
                                              getEntityManager(),
                                              getEndpoint(),
                                              key,
                                              session,
                                              requestId,
                                              action );
  }

  @Nonnull
  protected abstract ReplicantSessionManager getReplicantSessionManager();

  @Nonnull
  protected abstract EntityManager getEntityManager();

  @Nonnull
  protected abstract EntityMessageEndpoint getEndpoint();

  @Nonnull
  protected abstract TransactionSynchronizationRegistry getRegistry();
}
