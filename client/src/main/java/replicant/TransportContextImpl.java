package replicant;

import arez.Disposable;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

final class TransportContextImpl
  implements Transport.Context, Disposable
{
  private final Connector _connector;
  private boolean _disposed;

  TransportContextImpl( @Nonnull final Connector connector )
  {
    _connector = Objects.requireNonNull( connector );
  }

  @Override
  public void dispose()
  {
    _disposed = true;
  }

  @Override
  public boolean isDisposed()
  {
    return _disposed;
  }

  @Nonnull
  @Override
  public Request newRequest( @Nullable final String name )
  {
    assert isNotDisposed();
    return _connector.ensureConnection().newRequest( name );
  }

  @Override
  public void onMessageReceived( @Nonnull final String rawJsonData )
  {
    if ( isNotDisposed() )
    {
      _connector.onMessageReceived( rawJsonData );
    }
  }

  @Override
  public void onError( @Nonnull final Throwable error )
  {
    if ( isNotDisposed() )
    {
      final ConnectorState state = _connector.getState();
      if ( ConnectorState.CONNECTING == state )
      {
        _connector.onConnectFailure( error );
      }
      else if ( ConnectorState.CONNECTED == state )
      {
        _connector.onMessageReadFailure( error );
      }
      else if ( ConnectorState.DISCONNECTING == state )
      {
        _connector.onDisconnectFailure( error );
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onDisconnect()
  {
    if ( isNotDisposed() )
    {
      _connector.onDisconnection();
    }
  }
}
