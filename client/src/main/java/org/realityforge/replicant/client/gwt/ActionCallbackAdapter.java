package org.realityforge.replicant.client.gwt;

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.replicant.client.AbstractRequestAdapter;
import org.realityforge.replicant.client.transport.InvalidHttpResponseException;
import org.realityforge.replicant.shared.SharedConstants;
import replicant.Connection;
import replicant.RequestEntry;
import replicant.SafeProcedure;

final class ActionCallbackAdapter
  extends AbstractRequestAdapter
  implements RequestCallback
{
  ActionCallbackAdapter( @Nonnull final SafeProcedure onSuccess,
                         @Nullable final SafeProcedure onCacheValid,
                         @Nonnull final Consumer<Throwable> onError,
                         @Nullable final RequestEntry request,
                         @Nullable final Connection connection )
  {
    super( onSuccess, onCacheValid, onError, request, connection );
  }

  @Override
  public void onResponseReceived( final Request request, final Response response )
  {
    final int statusCode = response.getStatusCode();
    if ( Response.SC_OK == statusCode )
    {
      setNormalCompletion( response );
      completeRequest( getOnSuccess() );
    }
    else if ( Response.SC_NO_CONTENT == statusCode )
    {
      setNormalCompletion( response );
      completeRequest( getOnCacheValid() );
    }
    else
    {
      onFailure( new InvalidHttpResponseException( statusCode, response.getStatusText() ) );
    }
  }

  private void setNormalCompletion( @Nonnull final Response response )
  {
    final RequestEntry request = getRequest();
    if ( null != request )
    {
      request.setNormalCompletion( true );
      final boolean messageComplete = "1".equals( response.getHeader( SharedConstants.REQUEST_COMPLETE_HEADER ) );
      request.setExpectingResults( !messageComplete );
    }
  }

  @Override
  public void onError( final Request request, final Throwable exception )
  {
    onFailure( exception );
  }
}
