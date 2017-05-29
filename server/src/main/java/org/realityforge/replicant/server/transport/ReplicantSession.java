package org.realityforge.replicant.server.transport;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.replicant.server.ChannelDescriptor;

/**
 * Base class for sessions within replicant.
 */
public final class ReplicantSession
  extends SimpleSessionInfo
{
  private final PacketQueue _queue = new PacketQueue();
  private final HashMap<ChannelDescriptor, String> _cacheKeys = new HashMap<>();
  private final Map<ChannelDescriptor, String> _roCacheKeys = Collections.unmodifiableMap( _cacheKeys );
  private final HashMap<ChannelDescriptor, SubscriptionEntry> _subscriptions = new HashMap<>();
  private final Map<ChannelDescriptor, SubscriptionEntry> _roSubscriptions =
    Collections.unmodifiableMap( _subscriptions );

  public ReplicantSession( @Nullable final String userID, @Nonnull final String sessionID )
  {
    super( userID, sessionID );
  }

  @Nonnull
  public final PacketQueue getQueue()
  {
    return _queue;
  }

  @Nonnull
  public Map<ChannelDescriptor, String> getCacheKeys()
  {
    return _roCacheKeys;
  }

  @Nullable
  public String getCacheKey( @Nonnull final ChannelDescriptor descriptor )
  {
    return _cacheKeys.get( descriptor );
  }

  public void setCacheKey( @Nonnull final ChannelDescriptor descriptor, @Nullable final String key )
  {
    if ( null == key )
    {
      _cacheKeys.remove( descriptor );
    }
    else
    {
      _cacheKeys.put( descriptor, key );
    }
  }

  @Nonnull
  public final Map<ChannelDescriptor, SubscriptionEntry> getSubscriptions()
  {
    return _roSubscriptions;
  }

  /**
   * Return subscription entry for specified channel.
   */
  @Nonnull
  public final SubscriptionEntry getSubscriptionEntry( @Nonnull final ChannelDescriptor descriptor )
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
  public final SubscriptionEntry createSubscriptionEntry( @Nonnull final ChannelDescriptor descriptor )
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
  public final SubscriptionEntry findSubscriptionEntry( @Nonnull final ChannelDescriptor descriptor )
  {
    return _subscriptions.get( descriptor );
  }

  /**
   * Return true if specified channel is present.
   */
  public final boolean isSubscriptionEntryPresent( final ChannelDescriptor descriptor )
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
