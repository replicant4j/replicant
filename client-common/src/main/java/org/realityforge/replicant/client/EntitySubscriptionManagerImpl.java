package org.realityforge.replicant.client;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A class that records the subscriptions to entity graphs.
 */
public class EntitySubscriptionManagerImpl
  implements EntitySubscriptionManager
{
  //Graph => InstanceID
  private final HashMap<Enum, Map<Object, ChannelSubscriptionEntry>> _instanceSubscriptions = new HashMap<>();

  //Graph => Type
  private final HashMap<Enum, ChannelSubscriptionEntry> _typeSubscriptions = new HashMap<>();

  // Entity map: Type => ID
  private final HashMap<Class<?>, Map<Object, EntitySubscriptionEntry>> _entityMapping = new HashMap<>();

  @Nonnull
  @Override
  public Set<Enum> getTypeSubscriptions()
  {
    return Collections.unmodifiableSet( _typeSubscriptions.keySet() );
  }

  @Nonnull
  @Override
  public Set<Enum> getInstanceSubscriptionKeys()
  {
    return Collections.unmodifiableSet( _instanceSubscriptions.keySet() );
  }

  @Nonnull
  @Override
  public Set<Object> getInstanceSubscriptions( @Nonnull final Enum graph )
  {
    final Map<Object, ChannelSubscriptionEntry> map = _instanceSubscriptions.get( graph );
    if ( null == map )
    {
      return Collections.emptySet();
    }
    else
    {
      return Collections.unmodifiableSet( map.keySet() );
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @Nonnull
  public final ChannelSubscriptionEntry subscribe( @Nonnull final Enum graph,
                                                   @Nullable final Object filter )
    throws IllegalStateException
  {
    ChannelSubscriptionEntry typeMap = findSubscription( graph );
    if ( null == typeMap )
    {
      final ChannelSubscriptionEntry entry =
        new ChannelSubscriptionEntry( new ChannelDescriptor( graph, null ), filter );
      _typeSubscriptions.put( graph, entry );
      return entry;
    }
    else
    {
      throw new IllegalStateException( "Graph already subscribed: " + graph );
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @Nonnull
  public final ChannelSubscriptionEntry subscribe( @Nonnull final Enum graph,
                                                   @Nonnull final Object id,
                                                   @Nullable final Object filter )
  {
    final Map<Object, ChannelSubscriptionEntry> instanceMap =
      _instanceSubscriptions.computeIfAbsent( graph, k -> new HashMap<>() );
    if ( !instanceMap.containsKey( id ) )
    {
      final ChannelSubscriptionEntry entry = new ChannelSubscriptionEntry( new ChannelDescriptor( graph, id ), filter );
      instanceMap.put( id, entry );
      return entry;
    }
    else
    {
      throw new IllegalStateException( "Graph already subscribed: " + graph + ":" + id );
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @Nonnull
  public ChannelSubscriptionEntry updateSubscription( @Nonnull final Enum graph, @Nullable final Object filter )
    throws IllegalStateException
  {
    final ChannelSubscriptionEntry subscription = getSubscription( graph );
    subscription.setFilter( filter );
    return subscription;
  }

  @Nonnull
  @Override
  public ChannelSubscriptionEntry updateSubscription( @Nonnull final Enum graph,
                                                      @Nonnull final Object id,
                                                      @Nullable final Object filter )
  {
    final ChannelSubscriptionEntry subscription = getSubscription( graph, id );
    subscription.setFilter( filter );
    return subscription;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @Nullable
  public final ChannelSubscriptionEntry findSubscription( @Nonnull final Enum graph )
  {
    return _typeSubscriptions.get( graph );
  }

  /**
   * {@inheritDoc}
   */
  @Override
  @Nullable
  public final ChannelSubscriptionEntry findSubscription( @Nonnull final Enum graph, @Nonnull final Object id )
  {
    Map<Object, ChannelSubscriptionEntry> instanceMap = _instanceSubscriptions.get( graph );
    if ( null == instanceMap )
    {
      return null;
    }
    else
    {
      return instanceMap.get( id );
    }
  }

  /**
   * {@inheritDoc}
   */
  @Nonnull
  @Override
  public ChannelSubscriptionEntry getSubscription( @Nonnull final Enum graph )
    throws IllegalArgumentException
  {
    final ChannelSubscriptionEntry subscription = findSubscription( graph );
    if ( null == subscription )
    {
      throw new IllegalStateException( "Graph not subscribed: " + graph );
    }
    return subscription;
  }

  /**
   * {@inheritDoc}
   */
  @Nonnull
  @Override
  public ChannelSubscriptionEntry getSubscription( @Nonnull final Enum graph, @Nonnull final Object id )
    throws IllegalArgumentException
  {
    final ChannelSubscriptionEntry subscription = findSubscription( graph, id );
    if ( null == subscription )
    {
      throw new IllegalStateException( "Graph not subscribed: " + graph + "/" + id );
    }
    return subscription;
  }

  /**
   * {@inheritDoc}
   */
  @Nonnull
  @Override
  public final ChannelSubscriptionEntry unsubscribe( @Nonnull final Enum graph )
    throws IllegalStateException
  {
    final ChannelSubscriptionEntry entry = _typeSubscriptions.remove( graph );
    if ( null == entry )
    {
      throw new IllegalStateException( "Graph not subscribed: " + graph );
    }
    return entry;
  }

  /**
   * {@inheritDoc}
   */
  @Nonnull
  @Override
  public final ChannelSubscriptionEntry unsubscribe( @Nonnull final Enum graph, @Nonnull final Object id )
    throws IllegalStateException
  {
    final Map<Object, ChannelSubscriptionEntry> instanceMap = _instanceSubscriptions.get( graph );
    if ( null == instanceMap )
    {
      throw new IllegalStateException( "Graph not subscribed: " + graph + "/" + id );
    }
    final ChannelSubscriptionEntry entry = instanceMap.remove( id );
    if ( null == entry )
    {
      throw new IllegalStateException( "Graph not subscribed: " + graph + "/" + id );
    }
    return entry;
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
                            @Nonnull final ChannelDescriptor[] graphs )
  {
    final EntitySubscriptionEntry entityEntry = getEntitySubscriptions( type, id );
    for ( final ChannelDescriptor graph : graphs )
    {
      ChannelSubscriptionEntry entry = entityEntry.getRwGraphSubscriptions().get( graph );
      if ( null == entry )
      {
        final Enum g = graph.getGraph();
        entry = null == graph.getID() ? getSubscription( g ) : getSubscription( g, graph.getID() );
        entityEntry.getRwGraphSubscriptions().put( graph, entry );
      }
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
                                                        @Nonnull final ChannelDescriptor graph )
    throws IllegalStateException
  {
    final EntitySubscriptionEntry entry = getEntitySubscriptions( type, id );
    final Map<ChannelDescriptor, ChannelSubscriptionEntry> subscriptions = entry.getRwGraphSubscriptions();
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
