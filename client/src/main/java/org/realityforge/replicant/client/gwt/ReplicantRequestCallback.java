package org.realityforge.replicant.client.gwt;

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import java.util.Objects;
import javax.annotation.Nonnull;
import org.realityforge.replicant.client.transport.InvalidHttpResponseException;
import org.realityforge.replicant.shared.SharedConstants;

final class ReplicantRequestCallback
  implements RequestCallback
{
  @Nonnull
  private final replicant.Request _r;
  @Nonnull
  private final RequestCallback _callback;

  ReplicantRequestCallback( @Nonnull final replicant.Request r, @Nonnull final RequestCallback callback )
  {
    _r = Objects.requireNonNull( r );
    _callback = Objects.requireNonNull( callback );
  }

  @Override
  public void onResponseReceived( final Request request, final Response response )
  {
    final int statusCode = response.getStatusCode();
    if ( Response.SC_OK == statusCode )
    {
      final boolean messageComplete = "1".equals( response.getHeader( SharedConstants.REQUEST_COMPLETE_HEADER ) );
      _r.onSuccess( messageComplete, () -> _callback.onResponseReceived( request, response ) );
    }
    else
    {
      final InvalidHttpResponseException error =
        new InvalidHttpResponseException( statusCode, response.getStatusText() );
      _r.onFailure( () -> _callback.onError( request, error ) );
    }
  }

  @Override
  public void onError( final Request request, final Throwable exception )
  {
    _r.onFailure( () -> _callback.onError( request, exception ) );
  }
}
