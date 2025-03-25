package replicant;

import akasha.VisibilityState;
import akasha.WindowGlobal;
import arez.Disposable;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import replicant.messages.ServerToClientMessage;
import replicant.messages.SessionCreatedMessage;

final class TransportContextImpl
  implements TransportContext, Disposable
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
  public int newRequestId( @Nullable final String name,
                           final boolean syncRequest,
                           @Nullable final ResponseHandler responseHandler )
  {
    assert isNotDisposed();
    return _connector.ensureConnection().newRequest( name, syncRequest, responseHandler ).getRequestId();
  }

  @Override
  public void onMessageReceived( @Nonnull final ServerToClientMessage message )
  {
    if ( isNotDisposed() )
    {
      if ( SessionCreatedMessage.TYPE.equals( message.getType() ) )
      {
        _connector.onConnection( ( (SessionCreatedMessage) message ).getSessionId() );
      }
      else
      {
        final boolean active = _connector.isSchedulerActive();
        final boolean paused = _connector.isSchedulerPaused();
        _connector.onMessageReceived( message );
        /*
         * If the browser page is not visible then do all processing within the message handler callback
         * to avoid suffering under the vagaries of the background timer throttling.
         */
        if ( !active && !paused && !VisibilityState.visible.equals( WindowGlobal.document().visibilityState() ) )
        {
          //noinspection StatementWithEmptyBody
          while ( _connector.progressMessages() )
          {
            //keep processing messages until done
          }
        }
      }
    }
  }

  @Override
  public void onError()
  {
    if ( isNotDisposed() )
    {
      final ConnectorState state = _connector.getState();
      if ( ConnectorState.CONNECTING == state )
      {
        _connector.onConnectFailure();
      }
      else if ( ConnectorState.CONNECTED == state )
      {
        _connector.onMessageReadFailure();
      }
      else if ( ConnectorState.DISCONNECTING == state )
      {
        _connector.onDisconnectFailure();
      }
    }
  }

  @Override
  public void onDisconnect()
  {
    if ( isNotDisposed() )
    {
      _connector.onDisconnection();
    }
  }
}
