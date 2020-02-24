package org.realityforge.replicant.server.transport;

import java.io.Closeable;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
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
  private final Session _webSocketSession;
  @Nonnull
  private final Map<ChannelAddress, String> _eTags = new HashMap<>();
  @Nonnull
  private final Map<ChannelAddress, SubscriptionEntry> _subscriptions = new HashMap<>();
  @Nullable
  private String _authToken;

  public ReplicantSession( @Nonnull final Session webSocketSession )
  {
    _webSocketSession = Objects.requireNonNull( webSocketSession );
  }

  public void close( @Nonnull final CloseReason closeReason )
  {
    if ( _webSocketSession.isOpen() )
    {
      try
      {
        _webSocketSession.close( closeReason );
      }
      catch ( final IOException ignored )
      {
        // Assume it is already closing
      }
    }
  }

  @Override
  public void close()
  {
    if ( _webSocketSession.isOpen() )
    {
      try
      {
        _webSocketSession.close();
      }
      catch ( final IOException ignored )
      {
        // Assume it is already closing
      }
    }
  }

  /**
   * Send a ping at the network level to ensure the connection is kept alive.
   *
   * This is required to keep connection alove when passing through some load balancers
   * that proxy non-ssl websockets and close the socket after an idle period.
   */
  public void pingTransport()
  {
    if ( _webSocketSession.isOpen() )
    {
      try
      {
        _webSocketSession.getBasicRemote().sendPing( null );
      }
      catch ( final IOException ignored )
      {
        // All scenarios we can envision imply the session is shutting down, and thus can be ignored
      }
    }
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
    WebSocketUtil.sendText( getWebSocketSession(), JsonEncoder.encodeChangeSet( requestId, etag, changeSet ) );
  }

  /**
   * Send a packet to the client if the changeSet is not empty or it is marked as required.
   *
   * @param requestId the request id that caused these changes if this session requested the changes.
   * @param changeSet the changeSet to create packet from.
   * @return true if the packet was sent, false if it was ignorable.
   */
  public boolean maybeSendPacket( @Nullable final Integer requestId, @Nonnull final ChangeSet changeSet )
  {
    if ( changeSet.isRequired() || !changeSet.getChannelActions().isEmpty() || !changeSet.getChanges().isEmpty() )
    {
      sendPacket( requestId, null, changeSet );
      return true;
    }
    else
    {
      return false;
    }
  }

  @SuppressWarnings( "WeakerAccess" )
  @Nonnull
  public Map<ChannelAddress, String> getETags()
  {
    return Collections.unmodifiableMap( _eTags );
  }

  @SuppressWarnings( "WeakerAccess" )
  @Nullable
  public String getETag( @Nonnull final ChannelAddress address )
  {
    return _eTags.get( address );
  }

  @SuppressWarnings( "WeakerAccess" )
  public void setETag( @Nonnull final ChannelAddress address, @Nullable final String eTag )
  {
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
    return Collections.unmodifiableMap( _subscriptions );
  }

  /**
   * Return subscription entry for specified channel.
   */
  @SuppressWarnings( "WeakerAccess" )
  @Nonnull
  public SubscriptionEntry getSubscriptionEntry( @Nonnull final ChannelAddress address )
  {
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
      final SubscriptionEntry entry = new SubscriptionEntry( address );
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
    return _subscriptions.get( address );
  }

  /**
   * Return true if specified channel is present.
   */
  @SuppressWarnings( "WeakerAccess" )
  public boolean isSubscriptionEntryPresent( @Nonnull final ChannelAddress address )
  {
    return null != findSubscriptionEntry( address );
  }

  /**
   * Delete specified subscription entry.
   */
  boolean deleteSubscriptionEntry( @Nonnull final SubscriptionEntry entry )
  {
    return null != _subscriptions.remove( entry.getAddress() );
  }
}
