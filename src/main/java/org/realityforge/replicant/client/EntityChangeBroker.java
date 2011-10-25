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

  void attributeChanged( @Nonnull Object entity, @Nonnull String name, @Nonnull Object value );

  void entityRemoved( @Nonnull Object entity );
}
