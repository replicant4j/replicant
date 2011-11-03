package org.realityforge.replicant.client;

import javax.annotation.Nonnull;

/**
 * Interface for interacting with the local entity broker.
 */
public interface EntityChangeBroker
{
  void addChangeListener( @Nonnull EntityChangeListener listener );

  void addChangeListener( @Nonnull Class clazz, @Nonnull EntityChangeListener listener );

  void addChangeListener( @Nonnull Object object, @Nonnull EntityChangeListener listener );

  void removeChangeListener( @Nonnull EntityChangeListener listener );

  void removeChangeListener( @Nonnull Object object, @Nonnull EntityChangeListener listener );

  void removeChangeListener( @Nonnull Class clazz, @Nonnull EntityChangeListener listener );

  /**
   * Pause the broker.
   *
   * <p>Changes sent to the broker while it is paused will be cached and transmitted when it is resumed.</p>
   */
  void pause();

  /**
   * Resume the broker.
   *
   * <p>Any changes that have been delivered since pause has been invoked will be delivered on resume.</p>
   */
  void resume();

  /**
   * @return true if the broker is paused.
   */
  boolean isPaused();

  /**
   * Disable the transmission of changes to listeners.
   *
   * <p>Changes sent to the broker while it is disabled will be discarded.</p>
   *
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
   * @param name the key used to identify the property that changed.
   * @param value the value that the property changed to.
   */
  void attributeChanged( @Nonnull Object entity, @Nonnull String name, @Nonnull Object value );

  /**
   * Notify listeners that an entity was removed.
   *
   * @param entity the entity removed.
   */
  void entityRemoved( @Nonnull Object entity );
}
