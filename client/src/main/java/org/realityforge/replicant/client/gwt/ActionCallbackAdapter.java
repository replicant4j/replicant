package org.realityforge.replicant.client.gwt;

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.replicant.client.AbstractRequestAdapter;
import org.realityforge.replicant.client.transport.ClientSession;
import org.realityforge.replicant.client.transport.InvalidHttpResponseException;
import org.realityforge.replicant.client.transport.RequestEntry;
import org.realityforge.replicant.shared.SharedConstants;

final class ActionCallbackAdapter
  extends AbstractRequestAdapter
  implements RequestCallback
{
  ActionCallbackAdapter( @Nonnull final Runnable onSuccess,
                         @Nullable final Runnable onCacheValid,
                         @Nonnull final Consumer<Throwable> onError,
                         @Nullable final RequestEntry request,
                         @Nullable final ClientSession session )
  {
    super( onSuccess, onCacheValid, onError, request, session );
  }

  @Override
  public void onResponseReceived( final Request request, final Response response )
  {
    final int statusCode = response.getStatusCode();
    if ( Response.SC_OK == statusCode )
    {
      final Runnable onSuccess = getOnSuccess();
      calculateExpectingResults( response );
      completeNormalRequest( onSuccess );
    }
    else if ( Response.SC_NO_CONTENT == statusCode )
    {
      final Runnable onCacheValid = getOnCacheValid();
      calculateExpectingResults( response );
      completeNormalRequest( null != onCacheValid ? onCacheValid : NOOP );
    }
    else
    {
      onFailure( new InvalidHttpResponseException( statusCode, response.getStatusText() ) );
    }
  }

  @Override
  public void onError( final Request request, final Throwable exception )
  {
    onFailure( exception );
  }

  private void calculateExpectingResults( @Nonnull final Response response )
  {
    if ( null != getRequest() )
    {
      final boolean messageComplete = "1".equals( response.getHeader( SharedConstants.REQUEST_COMPLETE_HEADER ) );
      getRequest().setExpectingResults( !messageComplete );
    }
  }
}
