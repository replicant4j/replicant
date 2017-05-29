package org.realityforge.replicant.server.ee;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import javax.annotation.Nonnull;
import javax.interceptor.AroundInvoke;
import javax.interceptor.InvocationContext;

/**
 * A base class for an interceptor that should be applied to all services that need to send out EntityChange messages
 * on completion.
 */
public abstract class AbstractReplicationInterceptor
  extends AbstractInvocationAdapter
{
  @AroundInvoke
  public Object businessIntercept( final InvocationContext context )
    throws Exception
  {
    return invokeAction( getInvocationKey( context ), context::proceed );
  }

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
