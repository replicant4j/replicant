package org.realityforge.replicant.client.gwt;

import com.google.gwt.http.client.Response;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.replicant.client.transport.ClientSession;
import org.realityforge.replicant.client.transport.RequestEntry;

final class ActionCallbackAdapter
{
  @Nullable
  private final Consumer<Response> _callback;
  @Nullable
  private final Consumer<Throwable> _errorCallback;
  @Nullable
  private final RequestEntry _request;
  @Nullable
  private final ClientSession _session;

  ActionCallbackAdapter( @Nullable final Consumer<Response> callback,
                         @Nullable final Consumer<Throwable> errorCallback,
                         @Nullable final RequestEntry request,
                         @Nullable final ClientSession session )
  {
    _callback = callback;
    _errorCallback = errorCallback;
    _request = request;
    _session = session;
  }

  void onFailure( @Nonnull final Throwable caught )
  {
    final Runnable action = () ->
    {
      if ( null != _errorCallback )
      {
        _errorCallback.accept( caught );
      }
    };
    if ( null != _request && null != _session )
    {
      _session.completeNonNormalRequest( _request, action );
    }
    else
    {
      action.run();
    }
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
    if ( null != _request && null != _session )
    {
      _session.completeNormalRequest( _request, action );
    }
    else
    {
      action.run();
    }
  }
}
