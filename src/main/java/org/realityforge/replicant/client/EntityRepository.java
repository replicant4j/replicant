package org.realityforge.replicant.client;

import java.util.ArrayList;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * The service interface for storing entities locally on the client.
 */
public interface EntityRepository
{
  <T> void registerEntity( Class<T> type, Object id, T entity );

  @Nonnull
  <T> T deregisterEntity( Class<T> type, Object id );

  @Nonnull
  <T> T getByID( Class<T> type, Object id );

  @Nullable
  <T> T findByID( Class<T> type, Object id );

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
  <T> T findByID( Class<T> type, Object id, boolean forceLink );

  @Nonnull
  <T> ArrayList<T> findAll( Class<T> type );

  /**
   * Iterate through all of th entities in the repository and raise an exception if any invalid entities are found.
   *
   * @throws Exception if an invalid entity is in the repository.
   */
  void validate()
    throws Exception;
}
