package org.realityforge.replicant.client;

import java.util.ArrayList;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * The service interface for storing entities locally on the client.
 */
public interface EntityRepository
{
  <T> void registerEntity( @Nonnull Class<T> type, @Nonnull Object id, @Nonnull T entity );

  @Nonnull
  <T> T deregisterEntity( @Nonnull Class<T> type, @Nonnull Object id );

  @Nonnull
  default <T> T getByID( @Nonnull final Class<T> type, @Nonnull final Object id )
  {
    final T entity = findByID( type, id );
    if ( null == entity )
    {
      throw new NoSuchEntityException( type, id );
    }
    return entity;
  }

  @Nullable
  default <T> T findByID( @Nonnull final Class<T> type, @Nonnull final Object id )
  {
    return findByID( type, id, true );
  }

  /**
   * Lookup an entity of specified type with specified id, returning null if not present.
   * If forceLink is true then the entity will be linked prior to being returned.
   *
   * @param type      the type of the entity.
   * @param id        the id of the entity.
   * @param forceLink true if link() is to be called on entity before it is returned.
   * @param <T>       the entity type.
   * @return the entity or null if no such entity.
   */
  @Nullable
  <T> T findByID( @Nonnull Class<T> type, @Nonnull Object id, boolean forceLink );

  @Nonnull
  <T> ArrayList<T> findAll( @Nonnull Class<T> type );

  /**
   * Return the list of ids for entities of a particular type.
   *
   * @param type the entity type.
   * @return the list of ids.
   */
  @Nonnull
  <T> ArrayList<Object> findAllIDs( @Nonnull Class<T> type );

  /**
   * Return the list of types registered in repository.
   *
   * @return the list of types.
   */
  @Nonnull
  ArrayList<Class> getTypes();
}
