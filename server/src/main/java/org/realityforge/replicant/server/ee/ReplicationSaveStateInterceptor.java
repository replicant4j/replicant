package org.realityforge.replicant.server.ee;

import java.io.Serializable;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Priority;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;

/**
 * This interceptor clears the replication thread-local and then restores it after a call.
 * This is typically used on CDI event receiving methods or services.
 */
@Interceptor
@Priority( Interceptor.Priority.LIBRARY_BEFORE + 100 )
@PushReplicationState
public class ReplicationSaveStateInterceptor
{
  @AroundInvoke
  public Object businessIntercept( @Nonnull final InvocationContext context )
    throws Exception
  {
    final Map<String, Serializable> rc = ReplicantContextHolder.getContext();
    ReplicantContextHolder.clean();
    try
    {
      return context.proceed();
    }
    finally
    {
      ReplicantContextHolder.clean();
      ReplicantContextHolder.putAll( rc );
    }
  }
}
