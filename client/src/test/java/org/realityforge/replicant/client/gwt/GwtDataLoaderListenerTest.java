package org.realityforge.replicant.client.gwt;

import com.google.gwt.event.shared.SimpleEventBus;
import com.google.gwt.user.client.rpc.InvocationException;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class GwtDataLoaderListenerTest
{
  @Test
  public void unwrapExceptionOnInvocationException()
    throws Exception
  {
    final GwtDataLoaderListener service = new GwtDataLoaderListener( new SimpleEventBus() );
    final Exception cause = new Exception();
    final Throwable throwable = service.toCause( new InvocationException( "Nasty Exception", cause ) );
    assertEquals( throwable, cause );
  }

  @Test
  public void firesOnlySystemErrorEventOnNonInvocationException()
    throws Exception
  {
    final GwtDataLoaderListener service = new GwtDataLoaderListener( new SimpleEventBus() );
    final Exception cause = new Exception();
    final Throwable throwable = service.toCause( cause );
    assertEquals( throwable, cause );
  }
}
