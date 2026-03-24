package replicant.server.transport;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import replicant.server.ChannelAddress;

/**
 * An object defining the state of the subscription to a particular channel and
 * all the dependency relationships to other graphs.
 */
final class SubscriptionEntry
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
  @Nonnull
  private final Map<LinkOwner, Set<ChannelAddress>> _ownedOutwardSubscriptions = new HashMap<>();
  @Nonnull
  private final Map<ChannelAddress, Integer> _outwardSubscriptionReferenceCounts = new HashMap<>();
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

  SubscriptionEntry( @Nonnull final ReplicantSession session, @Nonnull final ChannelAddress address )
  {
    _session = Objects.requireNonNull( session );
    _address = Objects.requireNonNull( address );
  }

  @Nonnull
  ChannelAddress address()
  {
    return _address;
  }

  /**
   * Return true if this channel can be automatically un-subscribed. This means it has not
   * been explicitly subscribed and has no incoming subscriptions.
   */
  boolean canUnsubscribe()
  {
    return !isExplicitlySubscribed() && _inwardSubscriptions.isEmpty();
  }

  /**
   * Return true if this channel has been explicitly subscribed to from the client,
   * false the subscription occurred due to a graph link.
   */
  boolean isExplicitlySubscribed()
  {
    return _explicitlySubscribed;
  }

  void setExplicitlySubscribed( final boolean explicitlySubscribed )
  {
    _session.ensureLockedByCurrentThread();
    _explicitlySubscribed = explicitlySubscribed;
  }

  /**
   * Return the filter that was applied to this subscription. A particular channel
   * may or may not have a filter.
   */
  @Nullable
  Object getFilter()
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
  void setFilter( @Nullable final Object filter )
  {
    _session.ensureLockedByCurrentThread();
    _filter = filter;
  }

  /**
   * Return the channels that were subscribed as a result of subscribing to this channel.
   */
  @Nonnull
  Set<ChannelAddress> getOutwardSubscriptions()
  {
    return _roOutwardSubscriptions;
  }

  @Nonnull
  Set<ChannelAddress> getOwnedOutwardSubscriptions( @Nonnull final LinkOwner owner )
  {
    assert null != owner;
    _session.ensureLockedByCurrentThread();
    final var channels = _ownedOutwardSubscriptions.get( owner );
    return null == channels ? Collections.emptySet() : Set.copyOf( channels );
  }

  /**
   * Register the specified channel as outward links. Returns the set of links that were actually added.
   */
  @Nonnull
  ChannelAddress[] registerOutwardSubscriptions( @Nonnull final LinkOwner owner,
                                                 @Nonnull final ChannelAddress... channels )
  {
    assert null != owner;
    _session.ensureLockedByCurrentThread();
    final var results = new ArrayList<ChannelAddress>( channels.length );
    final var owned = _ownedOutwardSubscriptions.computeIfAbsent( owner, k -> new HashSet<>() );
    for ( final var channel : channels )
    {
      if ( owned.add( channel ) )
      {
        final int referenceCount = _outwardSubscriptionReferenceCounts.merge( channel, 1, Integer::sum );
        if ( 1 == referenceCount )
        {
          _outwardSubscriptions.add( channel );
          results.add( channel );
        }
      }
    }
    return results.toArray( new ChannelAddress[ 0 ] );
  }

  /**
   * Deregister the specified channels as outward links. Returns the set of links that were actually deregistered.
   */
  @Nonnull
  ChannelAddress[] deregisterOutwardSubscriptions( @Nonnull final LinkOwner owner,
                                                   @Nonnull final ChannelAddress... channels )
  {
    assert null != owner;
    _session.ensureLockedByCurrentThread();
    final var owned = _ownedOutwardSubscriptions.get( owner );
    if ( null == owned )
    {
      return new ChannelAddress[ 0 ];
    }
    else
    {
      final var results = new ArrayList<ChannelAddress>( channels.length );
      for ( final var channel : channels )
      {
        if ( owned.remove( channel ) )
        {
          final var existing = _outwardSubscriptionReferenceCounts.get( channel );
          assert null != existing;
          assert existing > 0;
          if ( 1 == existing )
          {
            _outwardSubscriptionReferenceCounts.remove( channel );
            _outwardSubscriptions.remove( channel );
            results.add( channel );
          }
          else
          {
            _outwardSubscriptionReferenceCounts.put( channel, existing - 1 );
          }
        }
      }
      if ( owned.isEmpty() )
      {
        _ownedOutwardSubscriptions.remove( owner );
      }
      return results.toArray( new ChannelAddress[ 0 ] );
    }
  }

  /**
   * Deregister the specified channels from all graph-link owners. Returns the set of links that were actually deregistered.
   */
  @Nonnull
  ChannelAddress[] deregisterAllOutwardSubscriptions( @Nonnull final ChannelAddress... channels )
  {
    _session.ensureLockedByCurrentThread();
    final var results = new ArrayList<ChannelAddress>( channels.length );
    for ( final var channel : channels )
    {
      if ( _outwardSubscriptions.remove( channel ) )
      {
        _outwardSubscriptionReferenceCounts.remove( channel );
        _ownedOutwardSubscriptions.entrySet().removeIf( e -> {
          e.getValue().remove( channel );
          return e.getValue().isEmpty();
        } );
        results.add( channel );
      }
    }
    return results.toArray( new ChannelAddress[ 0 ] );
  }

  /**
   * Return the channels that were auto-subscribed to the current channel.
   */
  @Nonnull
  Set<ChannelAddress> getInwardSubscriptions()
  {
    return _roInwardSubscriptions;
  }

  /**
   * Register the specified channel as inward links. Returns the set of links that were actually added.
   */
  @Nonnull
  ChannelAddress[] registerInwardSubscriptions( @Nonnull final ChannelAddress... channels )
  {
    _session.ensureLockedByCurrentThread();
    final var results = new ArrayList<ChannelAddress>( channels.length );
    for ( final var channel : channels )
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
    final var results = new ArrayList<ChannelAddress>( channels.length );
    for ( final var channel : channels )
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
