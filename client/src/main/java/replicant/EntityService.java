package replicant;

import arez.Disposable;
import arez.ObservableValue;
import arez.annotations.ArezComponent;
import arez.annotations.Feature;
import arez.annotations.Observable;
import arez.annotations.ObservableValueRef;
import arez.component.CollectionsUtil;
import arez.component.ComponentObservable;
import arez.component.DisposeNotifier;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import static org.realityforge.braincheck.Guards.*;

/**
 * The container of Entity instances within replicant system.
 */
@ArezComponent( disposeNotifier = Feature.DISABLE )
abstract class EntityService
  extends ReplicantService
{
  // Entity map: Type => ID
  private final Map<Class<?>, Map<Integer, Entity>> _entities = new HashMap<>();

  @Nonnull
  static EntityService create( @Nullable final ReplicantContext context )
  {
    return new Arez_EntityService( context );
  }

  EntityService( @Nullable final ReplicantContext context )
  {
    super( context );
  }

  @ObservableValueRef
  protected abstract ObservableValue<?> getEntitiesObservableValue();

  @Observable( expectSetter = false )
  Map<Class<?>, Map<Integer, Entity>> getEntities()
  {
    return _entities;
  }

  /**
   * Return the collection of entity types that exist in the system.
   * Only entity types that have at least one instance will be returned from this method unless
   * an Entity has been disposed and the scheduler is yet to invoke code to remove type from set.
   * This is a unlikely to be exposed to normal user code.
   *
   * @return the collection of entity types.
   */
  @Nonnull
  Collection<Class<?>> findAllEntityTypes()
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
  Entity findEntityByTypeAndId( @Nonnull final Class<?> type, final int id )
  {
    final Map<Integer, Entity> typeMap = _entities.get( type );
    if ( null == typeMap )
    {
      getEntitiesObservableValue().reportObserved();
      return null;
    }
    else
    {
      final Entity entity = typeMap.get( id );
      if ( null == entity )
      {
        getEntitiesObservableValue().reportObserved();
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
  List<Entity> findAllEntitiesByType( @Nonnull final Class<?> type )
  {
    final Map<Integer, Entity> typeMap = getEntities().get( type );
    return null == typeMap ? Collections.emptyList() : CollectionsUtil.asList( typeMap.values().stream() );
  }

  /**
   * Remove the entity and unlink from associated subscriptions.
   *
   * @param entity the entity.
   */
  void unlinkEntity( @Nonnull final Entity entity )
  {
    getEntitiesObservableValue().preReportChanged();

    final Class<?> entityType = entity.getType();
    final int id = entity.getId();
    final Map<Integer, Entity> typeMap = _entities.get( entityType );
    if ( Replicant.shouldCheckInvariants() )
    {
      invariant( () -> null != typeMap,
                 () -> "Entity type " + entityType.getSimpleName() + " not present in EntityService" );
    }
    assert null != typeMap;
    final Entity removed = typeMap.remove( id );
    if ( Replicant.shouldCheckInvariants() )
    {
      invariant( () -> null != removed, () -> "Entity instance " + entity + " not present in EntityService" );
    }
    detachEntity( entity );
    Disposable.dispose( removed );
    if ( typeMap.isEmpty() )
    {
      _entities.remove( entityType );
    }
    getEntitiesObservableValue().reportChanged();
  }

  /**
   * Return the entity specified by type and id, creating an Entity if one does not already exist.
   *
   * @param name the name of the entity if any. Must be null unless {@link Replicant#areNamesEnabled()} returns true.
   * @param type the type of the entity.
   * @param id   the id of the entity.
   * @return the existing Entity if it exists, otherwise the newly created entity.
   */
  @Nonnull
  Entity findOrCreateEntity( @Nullable final String name, @Nonnull final Class<?> type, final int id )
  {
    final Map<Integer, Entity> typeMap = _entities.get( type );
    if ( null == typeMap )
    {
      final HashMap<Integer, Entity> newTypeMap = new HashMap<>();
      _entities.put( type, newTypeMap );
      return createEntity( newTypeMap, name, type, id );
    }
    else
    {
      final Entity entity = typeMap.get( id );
      if ( null == entity )
      {
        return createEntity( typeMap, name, type, id );
      }
      else
      {
        ComponentObservable.observe( entity );
        return entity;
      }
    }
  }

  @Nonnull
  private Entity createEntity( @Nonnull final Map<Integer, Entity> typeMap,
                               @Nullable final String name,
                               @Nonnull final Class<?> type,
                               final int id )
  {
    getEntitiesObservableValue().preReportChanged();
    final Entity entity = Entity.create( Replicant.areZonesEnabled() ? getReplicantContext() : null, name, type, id );
    DisposeNotifier
      .asDisposeNotifier( entity )
      .addOnDisposeListener( this, () -> destroy( entity ) );
    typeMap.put( id, entity );
    getEntitiesObservableValue().reportChanged();
    ComponentObservable.observe( entity );
    return entity;
  }

  private void destroy( @Nonnull final Entity entity )
  {
    unlinkEntity( entity );
  }

  private void detachEntity( @Nonnull final Entity entity )
  {
    DisposeNotifier.asDisposeNotifier( entity ).removeOnDisposeListener( this );
  }
}
