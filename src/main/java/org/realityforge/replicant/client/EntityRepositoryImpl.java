package org.realityforge.replicant.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class EntityRepositoryImpl
    implements EntityRepository
{
  private HashMap<Class, HashMap<Object, ?>> _dataStore = new HashMap<Class, HashMap<Object, ?>>();

  public <T> void registerEntity( final Class<T> type, final Object id, final T entity )
  {
    final HashMap<Object, T> objectMap = getObjectMap( type );
    if( objectMap.containsKey( id ) )
    {
      throw new IllegalStateException( "Attempting to register duplicate " + describeEntity( type, id ) );
    }
    objectMap.put( id, entity );
    final Class<? super T> superclass = type.getSuperclass();
    if( Object.class != superclass )
    {
      registerEntity( superclass, id, entity );
    }
  }

  @Nonnull
  public <T> T deregisterEntity( final Class<T> type, final Object id )
  {
    final T existing = doDeregisterEntity( type, id );
    if( existing instanceof Linkable )
    {
      final Linkable linkable = (Linkable) existing;
      linkable.delink();
      linkable.invalidate();
    }
    return existing;
  }

  @Nonnull
  private <T> T doDeregisterEntity( final Class<T> type, final Object id )
  {
    final HashMap<Object, T> objectMap = getObjectMap( type );
    final T existing = objectMap.remove( id );
    if( null == existing )
    {
      throw new IllegalStateException( "Attempting to de-register non existent " + describeEntity( type, id ) );
    }
    final Class<? super T> superclass = type.getSuperclass();
    if( Object.class != superclass )
    {
      doDeregisterEntity( superclass, id );
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
  @Nullable
  public <T> T findByID( final Class<T> type, final Object id )
  {
    return findByID( type, id, true );
  }

  @Override
  public <T> T findByID( final Class<T> type, final Object id, final boolean forceLink )
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
  public <T> ArrayList<T> findAll( final Class<T> type )
  {
    final HashMap<Object, T> map = getObjectMap( type );
    final ArrayList<T> results = new ArrayList<T>( map.size() );
    results.addAll( map.values() );
    for( final T result : results )
    {
      linkEntity( result );
    }
    return results;
  }

  @Override
  public void validate()
    throws Exception
  {
    for ( final Entry<Class, HashMap<Object, ?>> entry : _dataStore.entrySet() )
    {
      for ( final Entry<Object, ?> entityEntry : entry.getValue().entrySet() )
      {
        final Object entity = entityEntry.getValue();
        if( entity instanceof Linkable )
        {
          if ( !( (Linkable) entity ).isValid() )
          {
            final String message =
              "Invalid " + describeEntity( entry.getKey(), entityEntry.getKey() ) + " found. Entity = " + entity;
            throw new Exception( message );
          }
        }
      }
    }
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

  private void linkEntity( final Object t )
  {
    if( t instanceof Linkable )
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
