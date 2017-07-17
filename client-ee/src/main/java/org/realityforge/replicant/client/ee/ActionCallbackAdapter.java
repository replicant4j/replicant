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
  @Nullable
  private final Runnable _callback;

  ActionCallbackAdapter( @Nullable final Runnable callback,
                         @Nullable final Consumer<Throwable> errorCallback,
                         @Nullable final RequestEntry request,
                         @Nullable final ClientSession session )
  {
    super( errorCallback, request, session );
    _callback = callback;
  }

  @Override
  public void completed( final Response response )
  {
    final int statusCode = response.getStatus();
    if ( Response.Status.OK.getStatusCode() == statusCode )
    {
      final Runnable action = () ->
      {
        if ( null != _callback )
        {
          _callback.run();
        }
      };
      calculateExpectingResults( response );
      completeNormalRequest( action );
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

  @Override
  public void onFailure( @Nonnull final Throwable caught )
  {
    if ( null != getRequest() )
    {
      getRequest().setExpectingResults( false );
    }
    super.onFailure( caught );
  }

  private void calculateExpectingResults( @Nonnull final Response response )
  {
    if ( null != getRequest() )
    {
      final boolean messageComplete =
        "1".equals( response.getHeaderString( ReplicantContext.REQUEST_COMPLETE_HEADER ) );
      getRequest().setExpectingResults( !messageComplete );
    }
  }
}
