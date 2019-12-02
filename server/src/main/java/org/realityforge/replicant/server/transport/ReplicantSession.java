package org.realityforge.replicant.server.transport;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.replicant.server.ChannelAddress;

public final class ReplicantSession
  implements Serializable
{
  private final String _userID;
  private final String _sessionID;
  private long _createdAt;
  private long _lastAccessedAt;
  private final PacketQueue _queue = new PacketQueue();
  private final HashMap<ChannelAddress, String> _eTags = new HashMap<>();
  private final Map<ChannelAddress, String> _roETags = Collections.unmodifiableMap( _eTags );
  private final HashMap<ChannelAddress, SubscriptionEntry> _subscriptions = new HashMap<>();
  private final Map<ChannelAddress, SubscriptionEntry> _roSubscriptions =
    Collections.unmodifiableMap( _subscriptions );

  public ReplicantSession( @Nullable final String userID, @Nonnull final String sessionID )
  {
    _userID = userID;
    _sessionID = sessionID;
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
    return _sessionID;
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
  public void updateAccessTime()
  {
    _lastAccessedAt = System.currentTimeMillis();
  }

  @Nonnull
  public final PacketQueue getQueue()
  {
    return _queue;
  }

  @Nonnull
  public Map<ChannelAddress, String> getETags()
  {
    return _roETags;
  }

  @Nullable
  public String getETag( @Nonnull final ChannelAddress descriptor )
  {
    return _eTags.get( descriptor );
  }

  public void setETag( @Nonnull final ChannelAddress descriptor, @Nullable final String eTag )
  {
    if ( null == eTag )
    {
      _eTags.remove( descriptor );
    }
    else
    {
      _eTags.put( descriptor, eTag );
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
  @Nonnull
  public final SubscriptionEntry getSubscriptionEntry( @Nonnull final ChannelAddress descriptor )
  {
    final SubscriptionEntry entry = findSubscriptionEntry( descriptor );
    if ( null == entry )
    {
      throw new IllegalStateException( "Unable to locate subscription entry for " + descriptor );
    }
    return entry;
  }

  /**
   * Create and return a subscription entry for specified channel.
   *
   * @throws IllegalStateException if subscription already exists.
   */
  @Nonnull
  public final SubscriptionEntry createSubscriptionEntry( @Nonnull final ChannelAddress descriptor )
  {
    if ( !_subscriptions.containsKey( descriptor ) )
    {
      final SubscriptionEntry entry = new SubscriptionEntry( descriptor );
      _subscriptions.put( descriptor, entry );
      return entry;
    }
    else
    {
      throw new IllegalStateException( "SubscriptionEntry for channel " + descriptor + " already exists" );
    }
  }

  /**
   * Return subscription entry for specified channel.
   */
  @Nullable
  public final SubscriptionEntry findSubscriptionEntry( @Nonnull final ChannelAddress descriptor )
  {
    return _subscriptions.get( descriptor );
  }

  /**
   * Return true if specified channel is present.
   */
  public final boolean isSubscriptionEntryPresent( final ChannelAddress descriptor )
  {
    return null != findSubscriptionEntry( descriptor );
  }

  /**
   * Delete specified subscription entry.
   */
  public final boolean deleteSubscriptionEntry( @Nonnull final SubscriptionEntry entry )
  {
    return null != _subscriptions.remove( entry.getDescriptor() );
  }
}
