package org.realityforge.replicant.server.transport;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.replicant.server.ChannelAddress;

/**
 * An object defining the state of the subscription to a particular channel and
 * all the dependency relationships to other graphs.
 */
public final class SubscriptionEntry
  implements Comparable<SubscriptionEntry>
{
  @Nonnull
  private final ReplicantSession _session;
  @Nonnull
  private final ChannelAddress _address;
  /**
   * This is a list of channels that this auto-subscribed to.
   */
  @Nonnull
  private final Set<ChannelAddress> _outwardSubscriptions = new HashSet<>();
  @Nonnull
  private final Set<ChannelAddress> _roOutwardSubscriptions = Collections.unmodifiableSet( _outwardSubscriptions );
  /**
   * This is a list of channels that auto-subscribed to this channel.
   */
  @Nonnull
  private final Set<ChannelAddress> _inwardSubscriptions = new HashSet<>();
  @Nonnull
  private final Set<ChannelAddress> _roInwardSubscriptions = Collections.unmodifiableSet( _inwardSubscriptions );
  private boolean _explicitlySubscribed;
  @Nullable
  private Object _filter;

  public SubscriptionEntry( @Nonnull final ReplicantSession session, @Nonnull final ChannelAddress address )
  {
    _session = Objects.requireNonNull( session );
    _address = Objects.requireNonNull( address );
  }

  @Nonnull
  public ChannelAddress address()
  {
    return _address;
  }

  /**
   * Return true if this channel can be automatically un-subscribed. This means it has not
   * been explicitly subscribed and has no incoming subscriptions.
   */
  public boolean canUnsubscribe()
  {
    return !isExplicitlySubscribed() && _inwardSubscriptions.isEmpty();
  }

  /**
   * Return true if this channel has been explicitly subscribed to from the client,
   * false the subscription occurred due to a graph link.
   */
  public boolean isExplicitlySubscribed()
  {
    return _explicitlySubscribed;
  }

  public void setExplicitlySubscribed( final boolean explicitlySubscribed )
  {
    _session.ensureLockedByCurrentThread();
    _explicitlySubscribed = explicitlySubscribed;
  }

  /**
   * Return the filter that was applied to this subscription. A particular channel
   * may or may not have a filter.
   */
  @Nullable
  public Object getFilter()
  {
    return _filter;
  }

  /**
   * Set the filter.
   * User code should not invoke this unless they are implementing bulk loading and are propagating
   * filters between multiple graphs loaded in a single sweep.
   *
   * @param filter the filter.
   */
  public void setFilter( @Nullable final Object filter )
  {
    _session.ensureLockedByCurrentThread();
    _filter = filter;
  }

  /**
   * Return the channels that were subscribed as a result of subscribing to this channel.
   */
  @Nonnull
  public Set<ChannelAddress> getOutwardSubscriptions()
  {
    return _roOutwardSubscriptions;
  }

  /**
   * Register the specified channel as outward links. Returns the set of links that were actually added.
   */
  @Nonnull
  public ChannelAddress[] registerOutwardSubscriptions( @Nonnull final ChannelAddress... channels )
  {
    _session.ensureLockedByCurrentThread();
    final List<ChannelAddress> results = new ArrayList<>( channels.length );
    for ( final ChannelAddress channel : channels )
    {
      if ( !_outwardSubscriptions.contains( channel ) )
      {
        _outwardSubscriptions.add( channel );
        results.add( channel );
      }
    }
    return results.toArray( new ChannelAddress[ 0 ] );
  }

  /**
   * Deregister the specified channels as outward links. Returns the set of links that were actually deregistered.
   */
  @Nonnull
  ChannelAddress[] deregisterOutwardSubscriptions( @Nonnull final ChannelAddress... channels )
  {
    _session.ensureLockedByCurrentThread();
    final List<ChannelAddress> results = new ArrayList<>( channels.length );
    for ( final ChannelAddress channel : channels )
    {
      if ( _outwardSubscriptions.contains( channel ) )
      {
        _outwardSubscriptions.remove( channel );
        results.add( channel );
      }
    }
    return results.toArray( new ChannelAddress[ 0 ] );
  }

  /**
   * Return the channels that were auto-subscribed to the current channel.
   */
  @Nonnull
  public Set<ChannelAddress> getInwardSubscriptions()
  {
    return _roInwardSubscriptions;
  }

  /**
   * Register the specified channel as inward links. Returns the set of links that were actually added.
   */
  @Nonnull
  public ChannelAddress[] registerInwardSubscriptions( @Nonnull final ChannelAddress... channels )
  {
    _session.ensureLockedByCurrentThread();
    final List<ChannelAddress> results = new ArrayList<>( channels.length );
    for ( final ChannelAddress channel : channels )
    {
      if ( !_inwardSubscriptions.contains( channel ) )
      {
        _inwardSubscriptions.add( channel );
        results.add( channel );
      }
    }
    return results.toArray( new ChannelAddress[ 0 ] );
  }

  /**
   * Deregister the specified channels as outward links. Returns the set of links that were actually deregistered.
   */
  @Nonnull
  ChannelAddress[] deregisterInwardSubscriptions( @Nonnull final ChannelAddress... channels )
  {
    _session.ensureLockedByCurrentThread();
    final List<ChannelAddress> results = new ArrayList<>( channels.length );
    for ( final ChannelAddress channel : channels )
    {
      if ( _inwardSubscriptions.contains( channel ) )
      {
        _inwardSubscriptions.remove( channel );
        results.add( channel );
      }
    }
    return results.toArray( new ChannelAddress[ 0 ] );
  }

  @Override
  public int compareTo( @Nonnull final SubscriptionEntry o )
  {
    return address().compareTo( o.address() );
  }
}
