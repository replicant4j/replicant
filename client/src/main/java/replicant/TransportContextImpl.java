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

  @Override
  public int getSchemaId()
  {
    return _connector.getSchema().getId();
  }

  @Nullable
  @Override
  public String getConnectionId()
  {
    return isDisposed() ? null : _connector.ensureConnection().getConnectionId();
  }

  @Override
  public int getLastTxRequestId()
  {
    return isDisposed() ? -1 : _connector.ensureConnection().getLastTxRequestId();
  }

  @Override
  public void recordLastSyncRxRequestId( final int requestId )
  {
    if ( isNotDisposed() )
    {
      _connector.recordLastSyncRxRequestId( requestId );
    }
  }

  @Override
  public void recordLastSyncTxRequestId( final int requestId )
  {
    if ( isNotDisposed() )
    {
      _connector.recordLastSyncTxRequestId( requestId );
    }
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

  @Override
  public void disconnect()
  {
    if ( isNotDisposed() )
    {
      _connector.transportDisconnect();
    }
  }
}
