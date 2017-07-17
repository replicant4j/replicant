package org.realityforge.replicant.client.ee;

import java.util.function.Consumer;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.ws.rs.client.InvocationCallback;
import javax.ws.rs.core.Response;
import org.realityforge.replicant.client.AbstractRequestAdapter;
import org.realityforge.replicant.client.transport.ClientSession;
import org.realityforge.replicant.client.transport.InvalidHttpResponseException;
import org.realityforge.replicant.client.transport.RequestEntry;
import org.realityforge.replicant.shared.transport.ReplicantContext;

final class ActionCallbackAdapter
  extends AbstractRequestAdapter
  implements InvocationCallback<Response>
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
  public void completed( final Response response )
  {
    final int statusCode = response.getStatus();
    if ( Response.Status.OK.getStatusCode() == statusCode )
    {
      final Runnable onSuccess = getOnSuccess();
      calculateExpectingResults( response );
      completeNormalRequest( onSuccess );
    }
    else if ( Response.Status.NO_CONTENT.getStatusCode() == statusCode )
    {
      final Runnable onCacheValid = getOnCacheValid();
      calculateExpectingResults( response );
      completeNormalRequest( null == onCacheValid ? NOOP : onCacheValid );
    }
    else
    {
      onFailure( new InvalidHttpResponseException( statusCode, response.getStatusInfo().getReasonPhrase() ) );
    }
  }

  @Override
  public void failed( final Throwable throwable )
  {
    onFailure( throwable );
  }

  private void calculateExpectingResults( @Nonnull final Response response )
  {
    final RequestEntry request = getRequest();
    if ( null != request )
    {
      final boolean messageComplete =
        "1".equals( response.getHeaderString( ReplicantContext.REQUEST_COMPLETE_HEADER ) );
      request.setExpectingResults( !messageComplete );
    }
  }
}
