package replicant.server.ee;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.annotation.Priority;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import org.jetbrains.annotations.VisibleForTesting;
import replicant.server.ServerConstants;
import replicant.server.transport.ReplicantSession;
import replicant.server.transport.ReplicantSessionManager;

/**
 * A base class for an interceptor that should be applied to all services that need to send out EntityChange messages
 * on completion.
 */
@Interceptor
@Priority( Interceptor.Priority.LIBRARY_BEFORE + 100 )
@Replicate
public class ReplicationInterceptor
{
  @VisibleForTesting
  @Inject
  ReplicantSessionManager _sessionManager;

  @AroundInvoke
  public Object businessIntercept( final InvocationContext context )
    throws Exception
  {
    final String sessionId = (String) ReplicantContextHolder.remove( ServerConstants.SESSION_ID_KEY );
    final Integer requestId = (Integer) ReplicantContextHolder.remove( ServerConstants.REQUEST_ID_KEY );
    final ReplicantSession session = null != sessionId ? _sessionManager.getSession( sessionId ) : null;
    ReplicantContextHolder.clean();
    try
    {
      return _sessionManager.runRequest( getInvocationKey( context ), session, requestId, () -> {
        final Object result = context.proceed();
        ReplicantContextHolder.clean();
        return result;
      } );
    }
    finally
    {
      ReplicantContextHolder.clean();
    }
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
}
