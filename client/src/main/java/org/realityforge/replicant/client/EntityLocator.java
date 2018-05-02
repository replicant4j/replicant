package org.realityforge.replicant.client;

import arez.component.NoSuchEntityException;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * The service interface for looking up entities by type+id.
 * This is used from within the entities during linking phase.
 */
public interface EntityLocator
{
  @Nonnull
  default <T> T getByID( @Nonnull final Class<T> type, @Nonnull final Object id )
  {
    final T entity = findByID( type, id );
    if ( null == entity )
    {
      throw new NoSuchEntityException( id );
    }
    return entity;
  }

  /**
   * Lookup an entity of specified type with specified id, returning null if not present.
   *
   * @param type the type of the entity.
   * @param id   the id of the entity.
   * @param <T>  the entity type.
   * @return the entity or null if no such entity.
   */
  @Nullable
  <T> T findByID( @Nonnull Class<T> type, @Nonnull Object id );

  /**
   * Return all entities of a specific type.
   *
   * @param type the type of the entity.
   * @param <T>  the entity type.
   * @return the entities.
   */
  @Nonnull
  <T> List<T> findAll( @Nonnull Class<T> type );
}
