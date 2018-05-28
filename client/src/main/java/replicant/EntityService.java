package replicant;

import arez.Arez;
import arez.ArezContext;
import arez.Component;
import arez.Disposable;
import arez.Observer;
import arez.annotations.ArezComponent;
import arez.annotations.ComponentNameRef;
import arez.annotations.ComponentRef;
import arez.annotations.ContextRef;
import arez.annotations.Observable;
import arez.annotations.ObservableRef;
import arez.component.CollectionsUtil;
import arez.component.ComponentObservable;
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
@ArezComponent
abstract class EntityService
  extends ReplicantService
{
  // Entity map: Type => ID
  private final Map<Class<?>, Map<Integer, EntityEntry>> _entities = new HashMap<>();

  @Nonnull
  static EntityService create( @Nullable final ReplicantContext context )
  {
    return new Arez_EntityService( context );
  }

  EntityService( @Nullable final ReplicantContext context )
  {
    super( context );
  }

  @ObservableRef
  protected abstract arez.Observable getEntitiesObservable();

  @Observable( expectSetter = false )
  Map<Class<?>, Map<Integer, EntityEntry>> getEntities()
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
    final Map<Integer, EntityEntry> typeMap = _entities.get( type );
    if ( null == typeMap )
    {
      getEntitiesObservable().reportObserved();
      return null;
    }
    else
    {
      final EntityEntry entry = typeMap.get( id );
      if ( null == entry || Disposable.isDisposed( entry ) || Disposable.isDisposed( entry.getEntity() ) )
      {
        getEntitiesObservable().reportObserved();
        return null;
      }
      else
      {
        final Entity entity = entry.getEntity();
        ComponentObservable.observe( entity );
        return entity;
      }
    }
  }

  @Nonnull
  List<Entity> findAllEntitiesByType( @Nonnull final Class<?> type )
  {
    final Map<Integer, EntityEntry> typeMap = getEntities().get( type );
    return null == typeMap ?
           Collections.emptyList() :
           CollectionsUtil.asList( typeMap
                                     .values()
                                     .stream()
                                     .filter( Disposable::isNotDisposed )
                                     .map( EntityEntry::getEntity )
                                     .filter( Disposable::isNotDisposed ) );
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
    final int id = entity.getId();
    final Map<Integer, EntityEntry> typeMap = _entities.get( entityType );
    if ( Replicant.shouldCheckInvariants() )
    {
      invariant( () -> null != typeMap,
                 () -> "Entity type " + entityType.getSimpleName() + " not present in EntityService" );
    }
    assert null != typeMap;
    final EntityEntry removed = typeMap.remove( id );
    if ( Replicant.shouldCheckInvariants() )
    {
      invariant( () -> null != removed, () -> "Entity instance " + entity + " not present in EntityService" );
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
   * @param name the name of the entity if any. Must be null unless {@link Replicant#areNamesEnabled()} returns true.
   * @param type the type of the entity.
   * @param id   the id of the entity.
   * @return the existing Entity if it exists, otherwise the newly created entity.
   */
  @Nonnull
  Entity findOrCreateEntity( @Nullable final String name, @Nonnull final Class<?> type, final int id )
  {
    final Map<Integer, EntityEntry> typeMap = _entities.get( type );
    if ( null == typeMap )
    {
      final HashMap<Integer, EntityEntry> newTypeMap = new HashMap<>();
      _entities.put( type, newTypeMap );
      return createEntity( newTypeMap, name, type, id );
    }
    else
    {
      final EntityEntry entry = typeMap.get( id );
      if ( null == entry || Disposable.isDisposed( entry ) || Disposable.isDisposed( entry.getEntity() ) )
      {
        return createEntity( typeMap, name, type, id );
      }
      else
      {
        final Entity entity = entry.getEntity();
        ComponentObservable.observe( entity );
        return entity;
      }
    }
  }

  @Nonnull
  private Entity createEntity( @Nonnull final Map<Integer, EntityEntry> typeMap,
                               @Nullable final String name,
                               @Nonnull final Class<?> type,
                               final int id )
  {
    getEntitiesObservable().preReportChanged();
    final Entity entity = Entity.create( Replicant.areZonesEnabled() ? getReplicantContext() : null, name, type, id );
    final EntityEntry entry = EntityEntry.create( entity );
    final String monitorName =
      Arez.areNamesEnabled() ?
      getComponentName() + ".EntityWatcher." + ( Replicant.areNamesEnabled() ? entity.getName() : entity.getId() ) :
      null;
    final Observer monitor =
      getContext().when( Arez.areNativeComponentsEnabled() ? component() : null,
                         monitorName,
                         true,
                         () -> !ComponentObservable.observe( entity ),
                         () -> destroy( entity ),
                         true,
                         true );
    entry.setMonitor( monitor );

    typeMap.put( id, entry );
    getEntitiesObservable().reportChanged();
    ComponentObservable.observe( entity );
    return entity;
  }

  private void destroy( @Nonnull final Entity entity )
  {
    entity.delinkEntityFromAllSubscriptions();
    unlinkEntity( entity );
  }

  /**
   * Return the component associated with the service if native components enabled.
   *
   * @return the component associated with the service if native components enabled.
   */
  @ComponentRef
  @Nonnull
  abstract Component component();

  /**
   * Return the context associated with the service.
   *
   * @return the context associated with the service.
   */
  @ContextRef
  @Nonnull
  abstract ArezContext getContext();

  /**
   * Return the name associated with the service.
   *
   * @return the name associated with the service.
   */
  @ComponentNameRef
  @Nonnull
  abstract String getComponentName();
}
