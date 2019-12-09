package org.realityforge.replicant.client;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A class that records the subscriptions to graphs and entities.
 */
public class EntitySubscriptionManagerImpl
  implements EntitySubscriptionManager
{
  //Graph => InstanceID
  private final Map<Enum<?>, Map<Integer, ChannelSubscriptionEntry>> _instanceSubscriptions = new HashMap<>();
  //Graph => Type
  private final Map<Enum<?>, ChannelSubscriptionEntry> _typeSubscriptions = new HashMap<>();
  // Entity map: Type => ID
  private final Map<Class<?>, Map<Object, EntitySubscriptionEntry>> _entityMapping = new HashMap<>();

  @Nonnull
  @Override
  public Set<Enum<?>> getTypeSubscriptions()
  {
    return Collections.unmodifiableSet( _typeSubscriptions.keySet() );
  }

  @Nonnull
  @Override
  public Set<Enum<?>> getInstanceSubscriptionKeys()
  {
    return Collections.unmodifiableSet( _instanceSubscriptions.keySet() );
  }

  @Nonnull
  @Override
  public Set<Integer> getInstanceSubscriptions( @Nonnull final Enum<?> graph )
  {
    final Map<Integer, ChannelSubscriptionEntry> map = _instanceSubscriptions.get( graph );
    if ( null == map )
    {
      return Collections.emptySet();
    }
    else
    {
      return Collections.unmodifiableSet( map.keySet() );
    }
  }

  @Override
  @Nonnull
  public final ChannelSubscriptionEntry recordSubscription( @Nonnull final ChannelAddress graph,
                                                            @Nullable final Object filter,
                                                            final boolean explicitSubscription )
    throws IllegalStateException
  {
    final ChannelSubscriptionEntry existing = findSubscription( graph );
    if ( null == existing )
    {
      final ChannelSubscriptionEntry entry = new ChannelSubscriptionEntry( graph, filter, explicitSubscription );
      final Integer id = graph.getId();
      if ( null == id )
      {
        _typeSubscriptions.put( graph.getGraph(), entry );
      }
      else
      {
        _instanceSubscriptions.computeIfAbsent( graph.getGraph(), k -> new HashMap<>() ).put( id, entry );
      }
      return entry;
    }
    else
    {
      throw new IllegalStateException( "Graph already subscribed: " + graph );
    }
  }

  @Override
  @Nonnull
  public ChannelSubscriptionEntry updateSubscription( @Nonnull final ChannelAddress graph,
                                                      @Nullable final Object filter )
    throws IllegalStateException
  {
    final ChannelSubscriptionEntry subscription = getSubscription( graph );
    subscription.setFilter( filter );
    return subscription;
  }

  @Override
  @Nullable
  public final ChannelSubscriptionEntry findSubscription( @Nonnull final ChannelAddress graph )
  {
    final Integer id = graph.getId();
    if ( null == id )
    {
      return _typeSubscriptions.get( graph.getGraph() );
    }
    else
    {
      final Map<Integer, ChannelSubscriptionEntry> instanceMap = _instanceSubscriptions.get( graph.getGraph() );
      return null == instanceMap ? null : instanceMap.get( id );
    }
  }

  @Nonnull
  @Override
  public ChannelSubscriptionEntry getSubscription( @Nonnull final ChannelAddress graph )
    throws IllegalArgumentException
  {
    final ChannelSubscriptionEntry subscription = findSubscription( graph );
    if ( null == subscription )
    {
      throw new IllegalStateException( "Graph not subscribed: " + graph );
    }
    return subscription;
  }

  @Nonnull
  @Override
  public final ChannelSubscriptionEntry removeSubscription( @Nonnull final ChannelAddress graph )
    throws IllegalStateException
  {
    final Integer id = graph.getId();
    if ( null == id )
    {
      final ChannelSubscriptionEntry entry = _typeSubscriptions.remove( graph.getGraph() );
      if ( null == entry )
      {
        throw new IllegalStateException( "Graph not subscribed: " + graph );
      }
      return entry;
    }
    else
    {
      final Map<Integer, ChannelSubscriptionEntry> instanceMap = _instanceSubscriptions.get( graph.getGraph() );
      if ( null == instanceMap )
      {
        throw new IllegalStateException( "Graph not subscribed: " + graph );
      }
      final ChannelSubscriptionEntry entry = instanceMap.remove( id );
      if ( null == entry )
      {
        throw new IllegalStateException( "Graph not subscribed: " + graph );
      }
      return entry;
    }
  }

  @Nonnull
  @Override
  public EntitySubscriptionEntry getSubscription( @Nonnull final Class<?> type, @Nonnull final Object id )
  {
    final EntitySubscriptionEntry entityEntry = findSubscription( type, id );
    if ( null == entityEntry )
    {
      throw new IllegalStateException( "Entity not subscribed: " + type.getSimpleName() + "/" + id );
    }
    return entityEntry;
  }

  @Nullable
  @Override
  public EntitySubscriptionEntry findSubscription( @Nonnull final Class<?> type, @Nonnull final Object id )
  {
    return getEntityTypeMap( type ).get( id );
  }

  @Override
  public void updateEntity( @Nonnull final Class<?> type,
                            @Nonnull final Object id,
                            @Nonnull final ChannelAddress[] graphs )
  {
    final EntitySubscriptionEntry entityEntry = getEntitySubscriptions( type, id );
    for ( final ChannelAddress graph : graphs )
    {
      final ChannelSubscriptionEntry entry =
        entityEntry.getRwGraphSubscriptions().computeIfAbsent( graph, this::getSubscription );
      Map<Object, EntitySubscriptionEntry> typeMap = entry.getEntities().get( type );
      if ( null == typeMap )
      {
        typeMap = new HashMap<>();
        entry.getRwEntities().put( type, typeMap );
      }
      if ( !typeMap.containsKey( id ) )
      {
        typeMap.put( id, entityEntry );
      }
    }
  }

  @Nonnull
  @Override
  public EntitySubscriptionEntry removeEntityFromGraph( @Nonnull final Class<?> type,
                                                        @Nonnull final Object id,
                                                        @Nonnull final ChannelAddress graph )
    throws IllegalStateException
  {
    final EntitySubscriptionEntry entry = getEntitySubscriptions( type, id );
    final Map<ChannelAddress, ChannelSubscriptionEntry> subscriptions = entry.getRwGraphSubscriptions();
    final ChannelSubscriptionEntry graphEntry = subscriptions.remove( graph );
    if ( null == graphEntry )
    {
      final String message = "Unable to locate graph " + graph + " for entity " + type.getSimpleName() + "/" + id;
      throw new IllegalStateException( message );
    }
    removeEntityFromGraph( type, id, graphEntry );
    return entry;
  }

  @Override
  public void removeEntity( @Nonnull final Class<?> type, @Nonnull final Object id )
  {
    final Map<Object, EntitySubscriptionEntry> typeMap = _entityMapping.get( type );
    if ( null == typeMap )
    {
      return;
    }
    final EntitySubscriptionEntry entityEntry = typeMap.remove( id );
    if ( null == entityEntry )
    {
      return;
    }
    for ( final ChannelSubscriptionEntry entry : entityEntry.getRwGraphSubscriptions().values() )
    {
      removeEntityFromGraph( type, id, entry );
    }
  }

  private void removeEntityFromGraph( final Class<?> type, final Object id, final ChannelSubscriptionEntry entry )
  {
    final Map<Class<?>, Map<Object, EntitySubscriptionEntry>> map = entry.getRwEntities();
    final Map<Object, EntitySubscriptionEntry> typeMap = map.get( type );
    final EntitySubscriptionEntry removed = null != typeMap ? typeMap.remove( id ) : null;
    if ( null == removed )
    {
      final String message =
        "Unable to remove entity " + type.getSimpleName() + "/" + id + " from " + entry.getDescriptor();
      throw new IllegalStateException( message );
    }
    if ( typeMap.isEmpty() )
    {
      map.remove( type );
    }
  }

  private EntitySubscriptionEntry getEntitySubscriptions( final Class<?> type, final Object id )
  {
    return getEntityTypeMap( type ).computeIfAbsent( id, k -> new EntitySubscriptionEntry( type, id ) );
  }

  @Nonnull
  private Map<Object, EntitySubscriptionEntry> getEntityTypeMap( @Nonnull final Class<?> type )
  {
    return _entityMapping.computeIfAbsent( type, k -> new HashMap<>() );
  }
}
