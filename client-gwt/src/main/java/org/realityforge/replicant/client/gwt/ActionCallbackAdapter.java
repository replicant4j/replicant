package org.realityforge.replicant.client.gwt;

import com.google.gwt.http.client.Response;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.replicant.client.AbstractRequestAdapter;
import org.realityforge.replicant.client.transport.ClientSession;
import org.realityforge.replicant.client.transport.RequestEntry;

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
    super(errorCallback, request, session );
    _callback = callback;
  }

  void onSuccess( @Nonnull final Response result )
  {
    final Runnable action = () ->
    {
      if ( null != _callback )
      {
        _callback.accept( result );
      }
    };
    completeNormalRequest( action );
  }
}
