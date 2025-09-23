package replicant.server.ee;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.concurrent.Callable;
import javax.annotation.Nonnull;
import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;
import replicant.server.ServerConstants;
import replicant.server.transport.ReplicantSession;
import replicant.server.transport.ReplicantSessionManager;

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
    return invokeAction( getInvocationKey( context ), context::proceed );
  }

  @Nonnull
  private String getInvocationKey( @Nonnull final InvocationContext context )
  {
    final Method method = context.getMethod();
    if ( null != method )
    {
      return method.getDeclaringClass().getName() + "." + method.getName();
    }
    final Constructor<?> constructor = context.getConstructor();
    if ( null != constructor )
    {
      return constructor.getDeclaringClass().getName() + "." + constructor.getName();
    }
    return "Unknown";
  }

  private <T> T invokeAction( @Nonnull final String key, @Nonnull final Callable<T> action )
    throws Exception
  {
    final String sessionId = (String) ReplicantContextHolder.remove( ServerConstants.SESSION_ID_KEY );
    final Integer requestId = (Integer) ReplicantContextHolder.remove( ServerConstants.REQUEST_ID_KEY );
    final ReplicantSession session = null != sessionId ? getReplicantSessionManager().getSession( sessionId ) : null;
    ReplicantContextHolder.clean();
    try
    {
      final Callable<T> actionWrapper = () -> {
        final T result = action.call();
        ReplicantContextHolder.clean();
        return result;
      };
      return getReplicantSessionManager().runRequest( key, session, requestId, actionWrapper );
    }
    finally
    {
      ReplicantContextHolder.clean();
    }
  }

  @Nonnull
  protected abstract ReplicantSessionManager getReplicantSessionManager();
}
