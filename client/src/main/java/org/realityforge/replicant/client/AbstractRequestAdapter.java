package org.realityforge.replicant.client;

import java.util.Objects;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import replicant.Connection;
import replicant.RequestEntry;
import replicant.SafeProcedure;

public abstract class AbstractRequestAdapter
{
  private static final SafeProcedure NOOP = () -> {
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
  private final Connection _connection;

  protected AbstractRequestAdapter( @Nonnull final SafeProcedure onSuccess,
                                    @Nullable final SafeProcedure onCacheValid,
                                    @Nonnull final Consumer<Throwable> onError,
                                    @Nullable final RequestEntry request,
                                    @Nullable final Connection connection )
  {
    _onSuccess = Objects.requireNonNull( onSuccess );
    _onCacheValid = onCacheValid;
    _onError = Objects.requireNonNull( onError );
    _request = request;
    _connection = connection;
  }

  protected void onFailure( @Nonnull final Throwable caught )
  {
    final RequestEntry request = getRequest();
    if ( null != request )
    {
      request.setExpectingResults( false );
      request.setNormalCompletion( false );
    }
    completeRequest( () -> _onError.accept( caught ) );
  }

  protected void completeRequest( @Nonnull final SafeProcedure completionAction )
  {
    if ( null != _request && null != _connection )
    {
      _connection.completeRequest( _request, completionAction );
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

  @Nonnull
  protected final SafeProcedure getOnCacheValid()
  {
    return null == _onCacheValid ? NOOP : _onCacheValid;
  }

  @Nullable
  protected final RequestEntry getRequest()
  {
    return _request;
  }

  @Nullable
  protected final Connection getConnection()
  {
    return _connection;
  }
}
