package org.realityforge.replicant.client;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Interface for interacting with the local entity broker.
 */
public interface EntityChangeBroker
{
  /**
   * Add an EntityChangeListener that receives all change messages.
   *
   * @param listener the EntityChangeListener
   */
  void addChangeListener( @Nonnull EntityChangeListener listener );

  /**
   * Add an EntityChangeListener that receives all change messages for models of a particular type and all sub-types.
   *
   * @param clazz    the type to subscribe to.
   * @param listener the EntityChangeListener
   */
  void addChangeListener( @Nonnull Class clazz, @Nonnull EntityChangeListener listener );

  /**
   * Add an EntityChangeListener that receives all change messages for a particular entity.
   *
   * @param entity   the entity to subscribe to.
   * @param listener the EntityChangeListener
   */
  void addChangeListener( @Nonnull Object entity, @Nonnull EntityChangeListener listener );

  /**
   * Remove an EntityChangeListener that receives all change messages.
   *
   * @param listener the EntityChangeListener
   */
  void removeChangeListener( @Nonnull EntityChangeListener listener );

  /**
   * Remove an EntityChangeListener that receives all change messages for models of a particular type and all sub-types.
   *
   * @param clazz    the type to subscribe to.
   * @param listener the EntityChangeListener
   */
  void removeChangeListener( @Nonnull Class clazz, @Nonnull EntityChangeListener listener );

  /**
   * Remove an EntityChangeListener that receives all change messages for a particular entity.
   *
   * @param entity   the entity to subscribe to.
   * @param listener the EntityChangeListener
   */
  void removeChangeListener( @Nonnull Object entity, @Nonnull EntityChangeListener listener );

  /**
   * Remove listener from listening to any changes.
   *
   * @param listener the EntityChangeListener
   */
  void purgeChangeListener( @Nonnull EntityChangeListener listener );

  /**
   * Pause the broker.
   * <p/>
   * <p>Changes sent to the broker while it is paused will be cached and transmitted when it is resumed.</p>
   */
  void pause();

  /**
   * Resume the broker.
   * <p/>
   * <p>Any changes that have been delivered since pause has been invoked will be delivered on resume.</p>
   */
  void resume();

  /**
   * @return true if the broker is paused.
   */
  boolean isPaused();

  /**
   * Disable the transmission of changes to listeners.
   * <p/>
   * <p>Changes sent to the broker while it is disabled will be discarded.</p>
   * <p/>
   * <p>Typically the broker is disabled before a bulk load of an EntityRepository and re-enabled
   * after the fact.</p>
   */
  void disable();

  /**
   * Re-enable the transmission of changes to listeners after a disable.
   */
  void enable();

  /**
   * @return true if the broker is enabled.
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

  /**
   * Return the listener entry for specified listener if it exists.
   *
   * @param listener the listener.
   * @return the associated entry or null.
   */
  @Nullable
  ListenerEntry findEntryForListener( @Nonnull EntityChangeListener listener );
}
