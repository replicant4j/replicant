package org.realityforge.replicant.server.ee;

import java.lang.reflect.Method;
import java.util.Map;
import javax.interceptor.InvocationContext;

public class TestInvocationContext
    implements InvocationContext
{
  public static final Object RESULT = new Object();
  private boolean _invoked;

  public Object getTarget()
  {
    return null;
  }

  public Method getMethod()
  {
    return null;
  }

  public Object[] getParameters()
  {
    return new Object[ 0 ];
  }

  public void setParameters( final Object[] parameters )
  {

  }

  public Map<String, Object> getContextData()
  {
    return null;
  }

  public Object proceed()
      throws Exception
  {
    _invoked = true;
    return RESULT;
  }

  public boolean isInvoked()
  {
    return _invoked;
  }

  public Object getTimer()
  {
    return null;
  }
}
