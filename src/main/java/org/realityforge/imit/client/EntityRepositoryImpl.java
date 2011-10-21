package org.realityforge.imit.client;

import java.util.ArrayList;
import java.util.HashMap;
import javax.annotation.Nonnull;

public class EntityRepositoryImpl
  implements EntityRepository
{
  private HashMap<Class,HashMap> _dataStore = new HashMap<Class, HashMap>(  );

  public <T> void registerEntity( final Class<T> type, final Object id, final T entity )
  {
    final HashMap<Object, T> objectMap = getObjectMap( type );
    if( objectMap.containsKey( id ) )
    {
      throw new IllegalStateException( "Attempting to register duplicate " + describeEntity( type, id ) );
    }
    objectMap.put( id, entity );
    final Class<? super T> superclass = type.getSuperclass();
    if ( Object.class != superclass )
    {
      registerEntity( superclass, id, entity );
    }
  }

  @Nonnull
  public <T> T deregisterEntity( final Class<T> type, final Object id )
  {
    final HashMap<Object, T> objectMap = getObjectMap( type );
    final T existing = objectMap.remove( id );
    if( null == existing )
    {
      throw new IllegalStateException( "Attempting to de-register non existent " + describeEntity( type, id ) );
    }
    final Class<? super T> superclass = type.getSuperclass();
    if ( Object.class != superclass )
    {
      deregisterEntity( superclass, id );
    }
    return existing;
  }

  @Nonnull
  public <T> T getByID( final Class<T> type, final Object id )
  {
    final T entity = findByID( type, id );
    if( null == entity )
    {
      throw new IllegalStateException( "Unable to locate " + describeEntity( type, id ) );
    }
    return entity;
  }

  @Override
  public <T> T findByID( final Class<T> type, final Object id )
  {
    return getObjectMap( type ).get( id );
  }

  @Override
  @Nonnull
  public <T> ArrayList<T> findAll( final Class<T> type )
  {
    final HashMap<Object, T> map = getObjectMap( type );
    final ArrayList<T> results = new ArrayList<T>( map.size() );
    results.addAll( map.values() );
    return results;
  }

  @SuppressWarnings( { "unchecked" } )
  private <T> HashMap<Object, T> getObjectMap( final Class<T> type )
  {
    HashMap<Object, T> objectMap = (HashMap<Object, T>) _dataStore.get( type );
    if( null == objectMap )
    {
      objectMap = new HashMap<Object, T>();
      _dataStore.put( type, objectMap );
    }
    return objectMap;
  }

  private <T> String describeEntity( final Class<T> type, final Object id )
  {
    return "entity with type '" + type.getName() + "' and id = '" + id + "'";
  }
}
