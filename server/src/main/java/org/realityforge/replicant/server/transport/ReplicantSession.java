package org.realityforge.replicant.server.transport;

import java.io.Closeable;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.websocket.CloseReason;
import javax.websocket.Session;
import org.realityforge.replicant.server.ChangeSet;
import org.realityforge.replicant.server.ChannelAddress;
import org.realityforge.replicant.server.json.JsonEncoder;

public final class ReplicantSession
  implements Serializable, Closeable
{
  @Nonnull
  private static final Logger LOG = Logger.getLogger( ReplicantSession.class.getName() );
  @Nonnull
  private final Session _webSocketSession;
  @Nonnull
  private final Map<ChannelAddress, String> _eTags = new HashMap<>();
  @Nonnull
  private final Map<ChannelAddress, SubscriptionEntry> _subscriptions = new HashMap<>();
  @Nonnull
  private final BlockingQueue<Packet> _pendingSubscriptionPackets = new LinkedBlockingQueue<>();
  @Nonnull
  private final BlockingQueue<Packet> _pendingPackets = new LinkedBlockingQueue<>();
  @Nonnull
  private final ReentrantLock _lock = new ReentrantLock( true );
  @Nullable
  private String _authToken;
  @Nullable
  private Object _userObject;

  public ReplicantSession( @Nonnull final Session webSocketSession )
  {
    _webSocketSession = Objects.requireNonNull( webSocketSession );
  }

  @Nullable
  public Object getUserObject()
  {
    return _userObject;
  }

  public void setUserObject( @Nullable final Object userObject )
  {
    _userObject = userObject;
  }

  public void closeDueToInterrupt()
  {
    close( new CloseReason( CloseReason.CloseCodes.UNEXPECTED_CONDITION, "Action interrupted" ) );
  }

  public void close( @Nonnull final CloseReason closeReason )
  {
    if ( isOpen() )
    {
      LOG.log( Level.FINE, () -> "Closing websocket for replicant session " + getId() + " with " + closeReason );
      try
      {
        _webSocketSession.close( closeReason );
      }
      catch ( final IOException ioe )
      {
        LOG.log( Level.FINE, () -> "Websocket close for replicant session " + getId() + " generated error " + ioe );
      }
    }
    else
    {
      LOG.log( Level.FINE,
               () -> "Websocket close requested for replicant session " + getId() + " with " + closeReason +
                     " but the websocket is already closed" );
    }
  }

  @Override
  public void close()
  {
    if ( isOpen() )
    {
      LOG.log( Level.FINE, () -> "Closing websocket for replicant session " + getId() );
      try
      {
        _webSocketSession.close();
      }
      catch ( final IOException ioe )
      {
        LOG.log( Level.FINE, () -> "Websocket close for replicant session " + getId() + " generated error " + ioe );
      }
    }
    else
    {
      LOG.log( Level.FINE,
               () -> "Websocket close requested for replicant session " + getId() +
                     " but the websocket is already closed" );
    }
  }

  /**
   * Send a ping at the network level to ensure the connection is kept alive.
   *
   * <p>This is required to keep connection alive when passing through some load balancers
   * that proxy non-ssl websockets and close the socket after an idle period.</p>
   */
  public void pingTransport()
  {
    if ( isOpen() )
    {
      LOG.log( Level.FINE, () -> "Pinging websocket for replicant session " + getId() );
      try
      {
        _webSocketSession.getBasicRemote().sendPing( null );
      }
      catch ( final IOException ioe )
      {
        // All scenarios we can envision imply the session is shutting down, and thus can be ignored
        LOG.log( Level.FINER, () -> "Websocket ping for replicant session " + getId() + " generated error " + ioe );
      }
    }
    else
    {
      LOG.log( Level.FINE,
               () -> "Websocket ping requested for replicant session " + getId() +
                     " but the websocket is already closed" );
    }
  }

  public boolean isOpen()
  {
    return _webSocketSession.isOpen();
  }

  @Nonnull
  public Session getWebSocketSession()
  {
    return _webSocketSession;
  }

  public void setAuthToken( @Nullable final String authToken )
  {
    _authToken = authToken;
  }

  /**
   * @return a token used for authentication, if any.
   */
  @SuppressWarnings( "unused" )
  @Nullable
  public String getAuthToken()
  {
    return _authToken;
  }

  /**
   * @return an opaque ID representing session.
   */
  @Nonnull
  public String getId()
  {
    return getWebSocketSession().getId();
  }

  @Nonnull
  public ReentrantLock getLock()
  {
    return _lock;
  }

  void queuePacket( @Nonnull final Packet packet )
  {
    if ( packet.altersExplicitSubscriptions() )
    {
      _pendingSubscriptionPackets.add( packet );
    }
    else
    {
      _pendingPackets.add( packet );
    }
  }

  @Nullable
  Packet popPendingPacket()
  {
    /*
     * We prioritize subscription packets ahead of other packets.
     * As the subscription data on the session object has already been
     * updated, we need to tell the client that these subscription changes
     * have occurred before we try and route other messages to the client.
     *
     * Only after the client has been updated with all subscription changing
     * packets do we send other packets.
     */
    final Packet packet = _pendingSubscriptionPackets.poll();
    return null == packet ? _pendingPackets.poll() : packet;
  }

  /**
   * Send a packet to the client.
   *
   * @param requestId the request id that caused these changes if this session requested the changes.
   * @param etag      the opaque identifier identifying the version. May be null if packet is not cache-able
   * @param changeSet the changeSet to create packet from.
   */
  public void sendPacket( @Nullable final Integer requestId,
                          @Nullable final String etag,
                          @Nonnull final ChangeSet changeSet )
  {
    ensureLockedByCurrentThread();
    final String message = JsonEncoder.encodeChangeSet( requestId, etag, changeSet );
    LOG.log( Level.FINE, () -> "Sending text message for replicant session " + getId() + " with payload " + message );
    if ( !WebSocketUtil.sendText( getWebSocketSession(), message ) )
    {
      LOG.log( Level.FINE,
               () -> "Failed to send text message for replicant session " + getId() + " with payload " + message );
    }
  }

  void ensureLockedByCurrentThread()
  {
    if ( !_lock.isHeldByCurrentThread() )
    {
      throw new IllegalStateException( "Expected session to be locked by the current thread" );
    }
  }

  @Nullable
  String getETag( @Nonnull final ChannelAddress address )
  {
    return _eTags.get( address );
  }

  public void setETags( @Nonnull final Map<ChannelAddress, String> etags )
  {
    ensureLockedByCurrentThread();
    _eTags.clear();
    for ( final Map.Entry<ChannelAddress, String> etag : etags.entrySet() )
    {
      setETag( etag.getKey(), etag.getValue() );
    }
  }

  void setETag( @Nonnull final ChannelAddress address, @Nullable final String eTag )
  {
    ensureLockedByCurrentThread();
    if ( null == eTag )
    {
      _eTags.remove( address );
    }
    else
    {
      _eTags.put( address, eTag );
    }
  }

  @Nonnull
  public Map<ChannelAddress, SubscriptionEntry> getSubscriptions()
  {
    ensureLockedByCurrentThread();
    return Collections.unmodifiableMap( _subscriptions );
  }

  /**
   * Return subscription entry for specified channel.
   */
  @SuppressWarnings( "WeakerAccess" )
  @Nonnull
  public SubscriptionEntry getSubscriptionEntry( @Nonnull final ChannelAddress address )
  {
    ensureLockedByCurrentThread();
    final SubscriptionEntry entry = findSubscriptionEntry( address );
    if ( null == entry )
    {
      throw new IllegalStateException( "Unable to locate subscription entry for " + address );
    }
    return entry;
  }

  /**
   * Create and return a subscription entry for specified channel.
   *
   * @throws IllegalStateException if subscription already exists.
   */
  @Nonnull
  public SubscriptionEntry createSubscriptionEntry( @Nonnull final ChannelAddress address )
  {
    if ( !_subscriptions.containsKey( address ) )
    {
      LOG.log( Level.FINE,
               () -> "Creating subscription entry for replicant session " + getId() + " on address " + address );
      final SubscriptionEntry entry = new SubscriptionEntry( this, address );
      _subscriptions.put( address, entry );
      return entry;
    }
    else
    {
      throw new IllegalStateException( "SubscriptionEntry for channel " + address + " already exists" );
    }
  }

  /**
   * Return subscription entry for specified channel.
   */
  @Nullable
  public SubscriptionEntry findSubscriptionEntry( @Nonnull final ChannelAddress address )
  {
    ensureLockedByCurrentThread();
    return _subscriptions.get( address );
  }

  /**
   * Return true if specified channel is present.
   */
  public boolean isSubscriptionEntryPresent( @Nonnull final ChannelAddress address )
  {
    ensureLockedByCurrentThread();
    return null != findSubscriptionEntry( address );
  }

  /**
   * Delete specified subscription entry.
   */
  boolean deleteSubscriptionEntry( @Nonnull final SubscriptionEntry entry )
  {
    ensureLockedByCurrentThread();
    final ChannelAddress address = entry.getAddress();
    final boolean removed = null != _subscriptions.remove( address );
    if ( removed )
    {
      LOG.log( Level.FINE,
               () -> "Removed subscription entry for replicant session " + getId() + " on address " + address );
    }
    else
    {
      LOG.log( Level.FINE,
               () -> "Attempted to remove subscription entry for replicant session " + getId() + " on address " +
                     address + " but no such subscription existed" );
    }
    return removed;
  }
}
