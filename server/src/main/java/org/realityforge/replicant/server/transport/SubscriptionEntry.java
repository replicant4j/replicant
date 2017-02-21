package org.realityforge.replicant.server.transport;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.replicant.server.ChannelDescriptor;

/**
 * An object defining the state of the subscription to a particular channel and
 * all the dependency relationships to other graphs.
 */
public final class SubscriptionEntry
  implements Comparable<SubscriptionEntry>
{
  private final ChannelDescriptor _descriptor;
  /**
   * This is a list of channels that this auto-subscribed to.
   */
  private final HashSet<ChannelDescriptor> _outwardSubscriptions = new HashSet<>();
  private final Set<ChannelDescriptor> _roOutwardSubscriptions = Collections.unmodifiableSet( _outwardSubscriptions );

  /**
   * This is a list of channels that auto-subscribed to this channel.
   */
  private final HashSet<ChannelDescriptor> _inwardSubscriptions = new HashSet<>();
  private final Set<ChannelDescriptor> _roInwardSubscriptions = Collections.unmodifiableSet( _inwardSubscriptions );

  private boolean _explicitlySubscribed;
  @Nullable
  private Object _filter;

  public SubscriptionEntry( @Nonnull final ChannelDescriptor descriptor )
  {
    _descriptor = descriptor;
  }

  public ChannelDescriptor getDescriptor()
  {
    return _descriptor;
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
    _explicitlySubscribed = explicitlySubscribed;
  }

  /**
   * Return the filter that was applied to this subscription. A particular channel
   * may or may not have a
   */
  @Nullable
  public Object getFilter()
  {
    return _filter;
  }

  public void setFilter( @Nullable final Object filter )
  {
    _filter = filter;
  }

  /**
   * Return the channels that were subscribed as a result of subscribing to this channel.
   */
  public Set<ChannelDescriptor> getOutwardSubscriptions()
  {
    return _roOutwardSubscriptions;
  }

  /**
   * Register the specified channel as outward links. Returns the set of links that were actually added.
   */
  @Nonnull
  public ChannelDescriptor[] registerOutwardSubscriptions( @Nonnull final ChannelDescriptor... channels )
  {
    final ArrayList<ChannelDescriptor> results = new ArrayList<>( channels.length );
    for ( final ChannelDescriptor channel : channels )
    {
      if ( !_outwardSubscriptions.contains( channel ) )
      {
        _outwardSubscriptions.add( channel );
        results.add( channel );
      }
    }
    return results.toArray( new ChannelDescriptor[ results.size() ] );
  }

  /**
   * Deregister the specified channels as outward links. Returns the set of links that were actually deregistered.
   */
  @Nonnull
  public ChannelDescriptor[] deregisterOutwardSubscriptions( final ChannelDescriptor... channels )
  {
    final ArrayList<ChannelDescriptor> results = new ArrayList<>( channels.length );
    for ( final ChannelDescriptor channel : channels )
    {
      if ( _outwardSubscriptions.contains( channel ) )
      {
        _outwardSubscriptions.remove( channel );
        results.add( channel );
      }
    }
    return results.toArray( new ChannelDescriptor[ results.size() ] );
  }

  /**
   * Return the channels that were auto-subscribed to the current channel.
   */
  public Set<ChannelDescriptor> getInwardSubscriptions()
  {
    return _roInwardSubscriptions;
  }

  /**
   * Register the specified channel as inward links. Returns the set of links that were actually added.
   */
  @Nonnull
  public ChannelDescriptor[] registerInwardSubscriptions( @Nonnull final ChannelDescriptor... channels )
  {
    final ArrayList<ChannelDescriptor> results = new ArrayList<>( channels.length );
    for ( final ChannelDescriptor channel : channels )
    {
      if ( !_inwardSubscriptions.contains( channel ) )
      {
        _inwardSubscriptions.add( channel );
        results.add( channel );
      }
    }
    return results.toArray( new ChannelDescriptor[ results.size() ] );
  }

  /**
   * Deregister the specified channels as outward links. Returns the set of links that were actually deregistered.
   */
  @Nonnull
  public ChannelDescriptor[] deregisterInwardSubscriptions( @Nonnull final ChannelDescriptor... channels )
  {
    final ArrayList<ChannelDescriptor> results = new ArrayList<>( channels.length );
    for ( final ChannelDescriptor channel : channels )
    {
      if ( _inwardSubscriptions.contains( channel ) )
      {
        _inwardSubscriptions.remove( channel );
        results.add( channel );
      }
    }
    return results.toArray( new ChannelDescriptor[ results.size() ] );
  }

  @Override
  public int compareTo( final SubscriptionEntry o )
  {
    return getDescriptor().compareTo( o.getDescriptor() );
  }
}
