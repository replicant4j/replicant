package org.realityforge.replicant.client;

import java.util.Objects;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.replicant.client.transport.ClientSession;
import replicant.RequestEntry;
import replicant.SafeProcedure;

public abstract class AbstractRequestAdapter
{
  protected static final SafeProcedure NOOP = () -> {
  };
  @Nonnull
  private final SafeProcedure _onSuccess;
  @Nullable
  private final SafeProcedure _onCacheValid;
  @Nonnull
  private final Consumer<Throwable> _onError;
  @Nullable
  private final RequestEntry _request;
  @Nullable
  private final ClientSession _session;

  protected AbstractRequestAdapter( @Nonnull final SafeProcedure onSuccess,
                                    @Nullable final SafeProcedure onCacheValid,
                                    @Nonnull final Consumer<Throwable> onError,
                                    @Nullable final RequestEntry request,
                                    @Nullable final ClientSession session )
  {
    _onSuccess = Objects.requireNonNull( onSuccess );
    _onCacheValid = onCacheValid;
    _onError = Objects.requireNonNull( onError );
    _request = request;
    _session = session;
  }

  protected void onFailure( @Nonnull final Throwable caught )
  {
    final RequestEntry request = getRequest();
    if ( null != request )
    {
      request.setExpectingResults( false );
    }
    completeNonNormalRequest( () -> _onError.accept( caught ) );
  }

  private void completeNonNormalRequest( @Nonnull final SafeProcedure action )
  {
    if ( null != _request && null != _session )
    {
      _session.completeNonNormalRequest( _request, action );
    }
    else
    {
      action.call();
    }
  }

  protected void completeNormalRequest( @Nonnull final SafeProcedure completionAction )
  {
    if ( null != _request && null != _session )
    {
      _session.completeNormalRequest( _request, completionAction );
    }
    else
    {
      completionAction.call();
    }
  }

  @Nonnull
  protected final SafeProcedure getOnSuccess()
  {
    return _onSuccess;
  }

  @Nullable
  protected final SafeProcedure getOnCacheValid()
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
