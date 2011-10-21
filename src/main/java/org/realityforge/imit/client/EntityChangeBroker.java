package org.realityforge.imit.client;

import javax.annotation.Nonnull;

/**
 * Interface for interacting with the local entity broker.
 */
public interface EntityChangeBroker
{
  void addChangeListener( @Nonnull EntityChangeListener listener );

  void addChangeListener( @Nonnull Class clazz, @Nonnull EntityChangeListener listener );

  void addChangeListener( @Nonnull Object object, @Nonnull EntityChangeListener listener );

  void addAttributeChangeListener( @Nonnull Object object,
                                   @Nonnull String feature,
                                   @Nonnull EntityChangeListener listener );

  /**
   * Add a listener if you want to listen to changes in an attribute on the specified class.
   */
  void addAttributeChangeListener( @Nonnull Class clazz,
                                   @Nonnull String feature,
                                   @Nonnull EntityChangeListener listener );

  void removeChangeListener( @Nonnull EntityChangeListener listener );

  void removeChangeListener( @Nonnull Object object, @Nonnull EntityChangeListener listener );

  void removeChangeListener( @Nonnull Class clazz, @Nonnull EntityChangeListener listener );

  void removeAttributeChangeListener( @Nonnull Class clazz,
                                      @Nonnull String name,
                                      @Nonnull EntityChangeListener listener );

  void removeAttributeChangeListener( @Nonnull Object object,
                                      @Nonnull String name,
                                      @Nonnull EntityChangeListener listener );

  void activate();

  void deactivate();

  void attributeChanged( @Nonnull Object entity, @Nonnull String name, @Nonnull Object value );

  void entityRemoved( @Nonnull Object entity );
}
