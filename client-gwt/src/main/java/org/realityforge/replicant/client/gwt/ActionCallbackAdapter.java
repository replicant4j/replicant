package org.realityforge.replicant.client.gwt;

import com.google.gwt.http.client.Response;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.replicant.client.AbstractRequestAdapter;
import org.realityforge.replicant.client.transport.ClientSession;
import org.realityforge.replicant.client.transport.InvalidHttpResponseException;
import org.realityforge.replicant.client.transport.RequestEntry;
import org.realityforge.replicant.shared.transport.ReplicantContext;

final class ActionCallbackAdapter
  extends AbstractRequestAdapter
{
  @Nullable
  private final Consumer<Response> _callback;

  ActionCallbackAdapter( @Nullable final Consumer<Response> callback,
                         @Nullable final Consumer<Throwable> errorCallback,
                         @Nullable final RequestEntry request,
                         @Nullable final ClientSession session )
  {
    super( errorCallback, request, session );
    _callback = callback;
  }

  @Override
  public void onFailure( @Nonnull final Throwable caught )
  {
    if ( null != getRequest() )
    {
      getRequest().setExpectingResults( false );
    }
    super.onFailure( caught );
  }

  void onSuccess( @Nonnull final Response response )
  {
    final int statusCode = response.getStatusCode();
    if ( Response.SC_OK == statusCode )
    {
      final Runnable action = () ->
      {
        if ( null != _callback )
        {
          _callback.accept( response );
        }
      };
      calculateExpectingResults( response );
      completeNormalRequest( action );
    }
    else
    {
      onFailure( new InvalidHttpResponseException( statusCode, response.getStatusText() ) );
    }
  }

  private void calculateExpectingResults( @Nonnull final Response response )
  {
    if ( null != getRequest() )
    {
      final boolean messageComplete = "1".equals( response.getHeader( ReplicantContext.REQUEST_COMPLETE_HEADER ) );
      getRequest().setExpectingResults( !messageComplete );
    }
  }
}
