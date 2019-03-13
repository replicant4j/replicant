package org.realityforge.replicant.server.transport;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.replicant.server.ChannelAddress;

public final class ReplicantSession
  implements Serializable
{
  @Nullable
  private final String _userID;
  @Nonnull
  private final String _sessionId;
  private long _createdAt;
  private long _lastAccessedAt;
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

  public ReplicantSession( @Nullable final String userID, @Nonnull final String sessionId )
  {
    _userID = userID;
    _sessionId = Objects.requireNonNull( sessionId );
    _createdAt = _lastAccessedAt = System.currentTimeMillis();
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

  /**
   * @return the time at which session was created.
   */
  public long getCreatedAt()
  {
    return _createdAt;
  }

  /**
   * @return the time at which session was last accessed.
   */
  public long getLastAccessedAt()
  {
    return _lastAccessedAt;
  }

  /**
   * Update the access time to now.
   */
  void updateAccessTime()
  {
    _lastAccessedAt = System.currentTimeMillis();
  }

  @Nonnull
  public final PacketQueue getQueue()
  {
    return _queue;
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
