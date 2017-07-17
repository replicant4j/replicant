package org.realityforge.replicant.client;

import java.util.function.Consumer;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.replicant.client.transport.ClientSession;
import org.realityforge.replicant.client.transport.RequestEntry;

public abstract class AbstractRequestAdapter
{
  protected static final Runnable NOOP = () -> {};
  @Nonnull
  private final Runnable _onSuccess;
  @Nullable
  private final Runnable _onCacheValid;
  @Nonnull
  private final Consumer<Throwable> _onError;
  @Nullable
  private final RequestEntry _request;
  @Nullable
  private final ClientSession _session;

  protected AbstractRequestAdapter( @Nonnull final Runnable onSuccess,
                                    @Nullable final Runnable onCacheValid,
                                    @Nonnull final Consumer<Throwable> onError,
                                    @Nullable final RequestEntry request,
                                    @Nullable final ClientSession session )
  {
    _onSuccess = onSuccess;
    _onCacheValid = onCacheValid;
    _onError = onError;
    _request = request;
    _session = session;
  }

  public void onFailure( @Nonnull final Throwable caught )
  {
    final RequestEntry request = getRequest();
    if ( null != request )
    {
      request.setExpectingResults( false );
    }
    completeNonNormalRequest( () -> _onError.accept( caught ) );
  }

  private void completeNonNormalRequest( @Nonnull final Runnable action )
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

  @Nonnull
  protected final Runnable getOnSuccess()
  {
    return _onSuccess;
  }

  @Nullable
  protected final Runnable getOnCacheValid()
  {
    return _onCacheValid;
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
