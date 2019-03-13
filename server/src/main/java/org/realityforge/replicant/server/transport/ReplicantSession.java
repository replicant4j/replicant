package org.realityforge.replicant.server.transport;

import java.io.Closeable;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.websocket.Session;
import org.realityforge.replicant.server.ChangeSet;
import org.realityforge.replicant.server.ChannelAddress;
import org.realityforge.replicant.server.json.JsonEncoder;

public final class ReplicantSession
  implements Serializable, Closeable
{
  @Nullable
  private final String _userID;
  @Nullable
  private final Session _webSocketSession;
  @Nonnull
  private final String _sessionId;
  @Nonnull
  private final PacketQueue _queue = new PacketQueue();
  @Nonnull
  private final HashMap<ChannelAddress, String> _eTags = new HashMap<>();
  @Nonnull
  private final Map<ChannelAddress, String> _roETags = Collections.unmodifiableMap( _eTags );
  @Nonnull
  private final HashMap<ChannelAddress, SubscriptionEntry> _subscriptions = new HashMap<>();
  @Nonnull
  private final Map<ChannelAddress, SubscriptionEntry> _roSubscriptions = Collections.unmodifiableMap( _subscriptions );

  public ReplicantSession( @Nullable final String userID, @Nullable Session webSocketSession )
  {
    _userID = userID;
    _webSocketSession = webSocketSession;
    _sessionId =
      null != webSocketSession ? webSocketSession.getId() : Objects.requireNonNull( UUID.randomUUID().toString() );
  }


  @Override
  public void close()
    throws IOException
  {
    if ( null != _webSocketSession && _webSocketSession.isOpen() )
    {
      _webSocketSession.close();
    }
  }

  public boolean isWebSocketSession()
  {
    return null != _webSocketSession;
  }

  @Nonnull
  public Session getWebSocketSession()
  {
    assert null != _webSocketSession;
    return _webSocketSession;
  }

  /**
   * @return an opaque ID representing user that created session.
   */
  @Nullable
  public String getUserID()
  {
    return _userID;
  }

  /**
   * @return an opaque ID representing session.
   */
  @Nonnull
  public String getSessionID()
  {
    return _sessionId;
  }

  @Nonnull
  public final PacketQueue getQueue()
  {
    return _queue;
  }

  /**
   * Add packet to queue and potentially send packet to client if client has acked last message.
   *
   * @param requestId the opaque identifier indicating the request that caused the changes if the owning session initiated the changes.
   * @param etag      the opaque identifier identifying the version. May be null if packet is not cache-able
   * @param changeSet the changeSet to create packet from.
   */
  public void sendPacket( @Nullable final Integer requestId,
                          @Nullable final String etag,
                          @Nonnull final ChangeSet changeSet )
  {
    _queue.addPacket( requestId, etag, changeSet );
    if ( null != _webSocketSession && _webSocketSession.isOpen() && 1 == _queue.size() )
    {
      final Packet packet = _queue.nextPacketToProcess();
      if ( null != packet )
      {
        final String message = JsonEncoder.
          encodeChangeSet( packet.getSequence(), packet.getRequestId(), packet.getETag(), packet.getChangeSet() );
        try
        {
          _webSocketSession.getBasicRemote().sendText( message );
        }
        catch ( final IOException ignored )
        {
          // This typically means that either the buffer is full or the websocket is in a bad state
          // either way we can ignore it and wait till it recovers or the connection is reaped.
        }
      }
    }
  }

  @SuppressWarnings( "WeakerAccess" )
  @Nonnull
  public Map<ChannelAddress, String> getETags()
  {
    return _roETags;
  }

  @SuppressWarnings( "WeakerAccess" )
  @Nullable
  public String getETag( @Nonnull final ChannelAddress address )
  {
    return _eTags.get( address );
  }

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
  public final Map<ChannelAddress, SubscriptionEntry> getSubscriptions()
  {
    return _roSubscriptions;
  }

  /**
   * Return subscription entry for specified channel.
   */
  @SuppressWarnings( "WeakerAccess" )
  @Nonnull
  public final SubscriptionEntry getSubscriptionEntry( @Nonnull final ChannelAddress address )
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
  final SubscriptionEntry createSubscriptionEntry( @Nonnull final ChannelAddress address )
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
  public final SubscriptionEntry findSubscriptionEntry( @Nonnull final ChannelAddress address )
  {
    return _subscriptions.get( address );
  }

  /**
   * Return true if specified channel is present.
   */
  @SuppressWarnings( "WeakerAccess" )
  public final boolean isSubscriptionEntryPresent( @Nonnull final ChannelAddress address )
  {
    return null != findSubscriptionEntry( address );
  }

  /**
   * Delete specified subscription entry.
   */
  final boolean deleteSubscriptionEntry( @Nonnull final SubscriptionEntry entry )
  {
    return null != _subscriptions.remove( entry.getDescriptor() );
  }
}
