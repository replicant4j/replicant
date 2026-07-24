package replicant.server.ee;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import javax.interceptor.InvocationContext;

public class TestInvocationContext
  implements InvocationContext
{
  public static final Object RESULT = new Object();
  @Nullable
  private Runnable _runnable;
  private boolean _invoked;

  public void setRunnable( final Runnable runnable )
  {
    _runnable = runnable;
  }

  @Override
  public @Nullable Object getTarget()
  {
    return null;
  }

  @Override
  public @Nullable Method getMethod()
  {
    return null;
  }

  @Override
  public @Nullable Constructor<?> getConstructor()
  {
    return null;
  }

  @Override
  public Object[] getParameters()
  {
    return new Object[ 0 ];
  }

  @Override
  public void setParameters( final Object[] parameters )
  {
  }

  @Nullable
  @Override
  public Map<String, Object> getContextData()
  {
    return null;
  }

  @Override
  public Object proceed()
    throws Exception
  {
    _invoked = true;
    if ( null != _runnable )
    {
      _runnable.run();
    }
    return RESULT;
  }

  public boolean isInvoked()
  {
    return _invoked;
  }

  @Override
  public @Nullable Object getTimer()
  {
    return null;
  }
}
