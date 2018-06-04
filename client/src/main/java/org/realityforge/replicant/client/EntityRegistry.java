package org.realityforge.replicant.client;

import javax.annotation.Nonnull;

/**
 * The service interface for registering entities by type+id.
 */
public interface EntityRegistry
{
  /**
   * Register specified entity with the specified type and id.
   * No entity should be registered with specified type+id pair.
   *
   * @param type   the type of the entity.
   * @param id     the id of the entity.
   * @param entity the entity.
   * @param <T>    the entity type.
   */
  <T> void registerEntity( @Nonnull Class<T> type, @Nonnull Object id, @Nonnull T entity );
}
