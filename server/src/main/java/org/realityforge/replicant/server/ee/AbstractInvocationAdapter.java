package org.realityforge.replicant.server.ee;

import java.util.concurrent.Callable;
import javax.annotation.Nonnull;
import javax.persistence.EntityManager;
import javax.transaction.TransactionSynchronizationRegistry;
import org.realityforge.replicant.server.EntityMessageEndpoint;
import org.realityforge.replicant.shared.transport.SharedConstants;

/**
 * A base class that used to wrap around invocation of an action.
 */
@SuppressWarnings( "WeakerAccess" )
public abstract class AbstractInvocationAdapter
{
  protected <T> T invokeAction( @Nonnull final String key, @Nonnull final Callable<T> action )
    throws Exception
  {
    final String sessionID = (String) ReplicantContextHolder.remove( SharedConstants.SESSION_ID_KEY );
    final String requestID = (String) ReplicantContextHolder.remove( SharedConstants.REQUEST_ID_KEY );

    return ReplicationRequestUtil.runRequest( getRegistry(),
                                              getEntityManager(),
                                              getEndpoint(),
                                              key,
                                              sessionID,
                                              requestID,
                                              action );
  }

  @Nonnull
  protected abstract EntityManager getEntityManager();

  @Nonnull
  protected abstract EntityMessageEndpoint getEndpoint();

  @Nonnull
  protected abstract TransactionSynchronizationRegistry getRegistry();
}
