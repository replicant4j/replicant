package org.realityforge.replicant.client;

import javax.annotation.Nonnull;

/**
 * The interface through which entities generate events.
 */
public interface EntityChangeEmitter
{
  /**
   * @return true if the emitter is enabled.
   */
  boolean isEnabled();

  /**
   * Notify listeners that an attribute has changed.
   *
   * @param entity the entity on which the change occurred.
   * @param name   the key used to identify the property that changed.
   * @param value  the value that the property changed to.
   */
  void attributeChanged( @Nonnull Object entity, @Nonnull String name, @Nonnull Object value );

  /**
   * Notify listeners that an entity was added.
   *
   * @param entity the entity added.
   */
  void entityAdded( @Nonnull Object entity );

  /**
   * Notify listeners that an entity was removed.
   *
   * @param entity the entity removed.
   */
  void entityRemoved( @Nonnull Object entity );

  /**
   * Notify listeners that the specified entity gained a relationship to the "other" entity.
   *
   * @param entity the entity on which the relationship was updated.
   * @param name   the key used to identify the relationship that changed.
   * @param other  the entity that updated to relate to this entity.
   */
  void relatedAdded( @Nonnull Object entity, @Nonnull String name, @Nonnull Object other );

  /**
   * Notify listeners that the specified entity lost a relationship to the "other" entity.
   *
   * @param entity the entity on which the relationship was updated.
   * @param name   the key used to identify the relationship that changed.
   * @param other  the entity that updated to no longer relate to this entity.
   */
  void relatedRemoved( @Nonnull Object entity, @Nonnull String name, @Nonnull Object other );
}
