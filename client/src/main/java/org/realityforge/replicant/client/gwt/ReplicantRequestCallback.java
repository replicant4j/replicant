package org.realityforge.replicant.client.gwt;

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import java.util.Objects;
import javax.annotation.Nonnull;
import org.realityforge.replicant.shared.SharedConstants;

final class ReplicantRequestCallback
  implements RequestCallback
{
  @Nonnull
  private final replicant.Request _replicantRequest;
  @Nonnull
  private final RequestCallback _callback;

  ReplicantRequestCallback( @Nonnull final replicant.Request replicantRequest, @Nonnull final RequestCallback callback )
  {
    _replicantRequest = Objects.requireNonNull( replicantRequest );
    _callback = Objects.requireNonNull( callback );
  }

  @Override
  public void onResponseReceived( @Nonnull final Request request, @Nonnull final Response response )
  {
    final int statusCode = response.getStatusCode();
    if ( Response.SC_OK == statusCode )
    {
      final boolean messageComplete = "1".equals( response.getHeader( SharedConstants.REQUEST_COMPLETE_HEADER ) );
      _replicantRequest.onSuccess( messageComplete, () -> _callback.onResponseReceived( request, response ) );
    }
    else
    {
      onError( request, new InvalidHttpResponseException( statusCode, response.getStatusText() ) );
    }
  }

  @Override
  public void onError( @Nonnull final Request request, @Nonnull final Throwable exception )
  {
    _replicantRequest.onFailure( () -> _callback.onError( request, exception ) );
  }
}
