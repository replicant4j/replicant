package org.realityforge.replicant.client;

import java.util.ArrayList;
import java.util.HashMap;
import javax.annotation.Nonnull;

public class EntityRepositoryImpl
  implements EntityRepository
{
  private final HashMap<Class, HashMap<Object, ?>> _dataStore = new HashMap<>();

  @Override
  public <T> void registerEntity( @Nonnull final Class<T> type, @Nonnull final Object id, @Nonnull final T entity )
  {
    final HashMap<Object, T> objectMap = getObjectMap( type );
    if ( objectMap.containsKey( id ) )
    {
      throw new IllegalStateException( "Attempting to register duplicate " + describeEntity( type, id ) );
    }
    objectMap.put( id, entity );
    final Class<? super T> superclass = type.getSuperclass();
    if ( null != superclass && Object.class != superclass )
    {
      registerEntity( superclass, id, entity );
    }
  }

  @Override
  @Nonnull
  public <T> T deregisterEntity( @Nonnull final Class<T> type, @Nonnull final Object id )
  {
    final T existing = doDeregisterEntity( type, id );
    if ( existing instanceof Linkable )
    {
      final Linkable linkable = (Linkable) existing;
      linkable.invalidate();
    }
    return existing;
  }

  @Nonnull
  private <T> T doDeregisterEntity( final Class<T> type, final Object id )
  {
    final HashMap<Object, T> objectMap = getObjectMap( type );
    final T existing = objectMap.remove( id );
    if ( null == existing )
    {
      throw new IllegalStateException( "Attempting to de-register non existent " + describeEntity( type, id ) );
    }
    final Class<? super T> superclass = type.getSuperclass();
    if ( null != superclass && Object.class != superclass )
    {
      doDeregisterEntity( superclass, id );
    }
    return existing;
  }

  @Override
  public <T> T findByID( @Nonnull final Class<T> type, @Nonnull final Object id, final boolean forceLink )
  {
    final T t = getObjectMap( type ).get( id );
    if ( forceLink )
    {
      linkEntity( t );
    }
    return t;
  }

  @Override
  @Nonnull
  public <T> ArrayList<T> findAll( @Nonnull final Class<T> type )
  {
    final HashMap<Object, T> map = getObjectMap( type );
    final ArrayList<T> results = new ArrayList<>( map.size() );
    results.addAll( map.values() );
    for ( final T result : results )
    {
      linkEntity( result );
    }
    return results;
  }

  @Nonnull
  @Override
  public <T> ArrayList<Object> findAllIDs( @Nonnull final Class<T> type )
  {
    final HashMap<Object, T> map = getObjectMap( type );
    final ArrayList<Object> results = new ArrayList<>( map.size() );
    results.addAll( map.keySet() );
    return results;
  }

  @Nonnull
  @Override
  public ArrayList<Class> getTypes()
  {
    return new ArrayList<>( _dataStore.keySet() );
  }

  @SuppressWarnings( { "unchecked" } )
  private <T> HashMap<Object, T> getObjectMap( final Class<T> type )
  {
    HashMap<Object, T> objectMap = (HashMap<Object, T>) _dataStore.get( type );
    if ( null == objectMap )
    {
      objectMap = new HashMap<>();
      _dataStore.put( type, objectMap );
    }
    return objectMap;
  }

  private void linkEntity( final Object t )
  {
    if ( t instanceof Linkable )
    {
      final Linkable linkable = (Linkable) t;
      assert linkable.isValid();
      linkable.link();
    }
  }

  private String describeEntity( final Class<?> type, final Object id )
  {
    return "entity with type '" + type.getName() + "' and id = '" + id + "'";
  }
}
