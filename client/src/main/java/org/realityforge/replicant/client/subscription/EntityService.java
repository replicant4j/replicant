package org.realityforge.replicant.client.subscription;

import arez.Disposable;
import arez.annotations.ArezComponent;
import arez.annotations.Observable;
import arez.annotations.ObservableRef;
import arez.component.ComponentObservable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Singleton;
import org.realityforge.braincheck.BrainCheckConfig;
import static org.realityforge.braincheck.Guards.*;

/**
 * The container of Entity instances within replicant system.
 */
@Singleton
@ArezComponent
public abstract class EntityService
{
  // Entity map: Type => ID
  private final Map<Class<?>, Map<Object, Entity>> _entities = new HashMap<>();

  @Nonnull
  public static EntityService create()
  {
    return new Arez_EntityService();
  }

  EntityService()
  {
  }

  @ObservableRef
  protected abstract arez.Observable getEntitiesObservable();

  @Observable( expectSetter = false )
  Map<Class<?>, Map<Object, Entity>> getEntities()
  {
    return _entities;
  }

  /**
   * Return the collection of entity types that exist in the system.
   * Only entity types that have at least one instance will be returned from this method.
   *
   * @return the collection of entity types.
   */
  @Nonnull
  public Collection<Class<?>> findAllEntityTypes()
  {
    return getEntities().keySet();
  }

  /**
   * Find the Entity by type and id.
   *
   * @param type the type of the entity.
   * @param id   the id of the entity.
   * @return the Entity if it exists, null otherwise.
   */
  @Nullable
  public Entity findEntityByTypeAndId( @Nonnull final Class<?> type, @Nonnull final Object id )
  {
    final Map<Object, Entity> typeMap = _entities.get( type );
    if ( null == typeMap )
    {
      getEntitiesObservable().reportObserved();
      return null;
    }
    else
    {
      final Entity entity = typeMap.get( id );
      if ( null == entity )
      {
        getEntitiesObservable().reportObserved();
        return null;
      }
      else
      {
        ComponentObservable.observe( entity );
        return entity;
      }
    }
  }

  @Nonnull
  public List<Entity> findAllEntitiesByType( @Nonnull final Class<?> type )
  {
    final Map<Object, Entity> typeMap = getEntities().get( type );
    return null == typeMap ? Collections.emptyList() : new ArrayList<>( typeMap.values() );
  }

  /**
   * Remove the entity and unlink from associated subscriptions.
   *
   * @param entity the entity.
   */
  void unlinkEntity( @Nonnull final Entity entity )
  {
    getEntitiesObservable().preReportChanged();

    final Class<?> entityType = entity.getType();
    final Object id = entity.getId();
    final Map<Object, Entity> typeMap = _entities.get( entityType );
    if ( BrainCheckConfig.checkInvariants() )
    {
      invariant( () -> null != typeMap,
                 () -> "Entity type " + entityType.getSimpleName() + " not present in EntityService" );
    }
    assert null != typeMap;
    final Entity removed = typeMap.remove( id );
    if ( BrainCheckConfig.checkInvariants() )
    {
      invariant( () -> null != removed,
                 () -> "Entity instance " + entityType.getSimpleName() + "/" + id + " not present in EntityService" );
    }
    Disposable.dispose( removed );
    if ( typeMap.isEmpty() )
    {
      _entities.remove( entityType );
    }
    getEntitiesObservable().reportChanged();
  }

  /**
   * Return the entity specified by type and id, creating an Entity if one does not already exist.
   *
   * @param type the type of the entity.
   * @param id   the id of the entity.
   * @return the existing Entity if it exists, otherwise the newly created entity.
   */
  @Nonnull
  Entity findOrCreateEntity( @Nonnull final Class<?> type, @Nonnull final Object id )
  {
    final Map<Object, Entity> typeMap = _entities.get( type );
    if ( null == typeMap )
    {
      final HashMap<Object, Entity> newTypeMap = new HashMap<>();
      _entities.put( type, newTypeMap );
      return createEntity( newTypeMap, type, id );
    }
    else
    {
      final Entity entity = typeMap.get( id );
      if ( null == entity )
      {
        return createEntity( typeMap, type, id );
      }
      else
      {
        ComponentObservable.observe( entity );
        return entity;
      }
    }
  }

  @Nonnull
  private Entity createEntity( @Nonnull final Map<Object, Entity> typeMap,
                               @Nonnull final Class<?> type,
                               @Nonnull final Object id )
  {
    getEntitiesObservable().preReportChanged();
    final Entity entity = Entity.create( this, type, id );
    typeMap.put( id, entity );
    getEntitiesObservable().reportChanged();
    return entity;
  }
}
