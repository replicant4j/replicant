package org.realityforge.replicant.client.gwt;

import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;
import java.util.Objects;
import java.util.function.Supplier;
import javax.annotation.Nonnull;

final class SessionLockednewRequestCallback
  implements RequestCallback
{
  @Nonnull
  private final String _expectedConnectionId;
  @Nonnull
  private final RequestCallback _target;
  @Nonnull
  private final Supplier<String> _connectionIdSupplier;

  SessionLockednewRequestCallback( @Nonnull final String expectedConnectionId,
                                   @Nonnull final RequestCallback target,
                                   @Nonnull final Supplier<String> connectionIdSupplier )
  {
    _expectedConnectionId = Objects.requireNonNull( expectedConnectionId );
    _target = Objects.requireNonNull( target );
    _connectionIdSupplier = Objects.requireNonNull( connectionIdSupplier );
  }

  @Override
  public void onResponseReceived( final Request request, final Response response )
  {
    if ( shouldCallTarget() )
    {
      _target.onResponseReceived( request, response );
    }
  }

  @Override
  public void onError( final Request request, final Throwable exception )
  {
    if ( shouldCallTarget() )
    {
      _target.onError( request, exception );
    }
  }

  private boolean shouldCallTarget()
  {
    return Objects.equals( _expectedConnectionId, _connectionIdSupplier.get() );
  }
}
