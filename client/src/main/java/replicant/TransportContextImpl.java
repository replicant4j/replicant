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
  public int getLastRxSequence()
  {
    return isDisposed() ? -1 : _connector.ensureConnection().getLastRxSequence();
  }

  @Override
  public int getLastRxRequestId()
  {
    return isDisposed() ? -1 : _connector.ensureConnection().getLastRxRequestId();
  }

  @Override
  public int getLastTxRequestId()
  {
    return isDisposed() ? -1 : _connector.ensureConnection().getLastTxRequestId();
  }

  @Override
  public void recordLastSyncRxRequestId( final int requestId )
  {
    if ( !isDisposed() )
    {
      _connector.recordLastSyncRxRequestId( requestId );
    }
  }

  @Override
  public void recordLastSyncTxRequestId( final int requestId )
  {
    if ( !isDisposed() )
    {
      _connector.recordLastSyncTxRequestId( requestId );
    }
  }

  @Override
  public void onMessageReceived( @Nonnull final String rawJsonData )
  {
    if ( !isDisposed() )
    {
      _connector.onMessageReceived( rawJsonData );
    }
  }

  @Override
  public void onMessageReadFailure( @Nonnull final Throwable error )
  {
    if ( !isDisposed() )
    {
      _connector.onMessageReadFailure( error );
    }
  }

  @Override
  public void disconnect()
  {
    if ( !isDisposed() )
    {
      _connector.transportDisconnect();
    }
  }
}
