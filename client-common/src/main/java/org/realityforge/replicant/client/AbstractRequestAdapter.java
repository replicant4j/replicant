package org.realityforge.replicant.client;

import java.util.function.Consumer;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.replicant.client.transport.ClientSession;
import org.realityforge.replicant.client.transport.RequestEntry;

public abstract class AbstractRequestAdapter
{
  @Nullable
  private final Consumer<Throwable> _errorCallback;
  @Nullable
  private final RequestEntry _request;
  @Nullable
  private final ClientSession _session;

  protected AbstractRequestAdapter( @Nullable final Consumer<Throwable> errorCallback,
                                    @Nullable final RequestEntry request,
                                    @Nullable final ClientSession session )
  {
    _errorCallback = errorCallback;
    _request = request;
    _session = session;
  }

  public void onFailure( @Nonnull final Throwable caught )
  {
    final Runnable action = () ->
    {
      if ( null != _errorCallback )
      {
        _errorCallback.accept( caught );
      }
    };
    completeNonNormalRequest( action );
  }

  protected final void completeNonNormalRequest( @Nonnull final Runnable action )
  {
    if ( null != _request && null != _session )
    {
      _session.completeNonNormalRequest( _request, action );
    }
    else
    {
      action.run();
    }
  }

  protected void completeNormalRequest( @Nonnull final Runnable action )
  {
    if ( null != _request && null != _session )
    {
      _session.completeNormalRequest( _request, action );
    }
    else
    {
      action.run();
    }
  }

  @Nullable
  protected final Consumer<Throwable> getErrorCallback()
  {
    return _errorCallback;
  }

  @Nullable
  protected final RequestEntry getRequest()
  {
    return _request;
  }

  @Nullable
  protected final ClientSession getSession()
  {
    return _session;
  }
}
