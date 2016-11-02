package org.realityforge.replicant.client.gwt;

import com.google.gwt.user.client.rpc.InvocationException;
import javax.annotation.Nonnull;
import org.realityforge.gwt.webpoller.client.WebPoller;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import static org.mockito.Mockito.*;

public class GwtWebPollerDataLoaderServiceTest
{
  @BeforeMethod
  public void preTest()
    throws Exception
  {
    WebPoller.register( new WebPoller.Factory()
    {
      @Nonnull
      @Override
      public WebPoller newWebPoller()
      {
        return new TestWebPoller();
      }
    } );
  }

  @Test
  public void unwrapExceptionOnInvocationException()
    throws Exception
  {
    final TestGwtWebPollerDataLoaderService service = new TestGwtWebPollerDataLoaderService();
    final SystemErrorEvent.Handler systemHandler = mock( SystemErrorEvent.Handler.class );
    service.lookupEventBus().addHandler( SystemErrorEvent.TYPE, systemHandler );
    final Exception cause = new Exception();
    service.handleSystemFailure( new InvocationException( "Nasty Exception", cause ), "Failed to poll" );
    verify( systemHandler, times( 1 ) ).onSystemError( refEq( new SystemErrorEvent( "Failed to poll", cause ) ) );
  }

  @Test
  public void firesOnlySystemErrorEventOnNonInvocationException()
    throws Exception
  {
    final TestGwtWebPollerDataLoaderService service = new TestGwtWebPollerDataLoaderService();
    final SystemErrorEvent.Handler systemHandler = mock( SystemErrorEvent.Handler.class );
    service.lookupEventBus().addHandler( SystemErrorEvent.TYPE, systemHandler );
    final Exception cause = new Exception( "Exception" );
    service.handleSystemFailure( cause, "Failed to poll" );
    verify( systemHandler, times( 1 ) ).onSystemError( refEq( new SystemErrorEvent( "Failed to poll", cause ) ) );
  }
}
