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

  void activate();

  void deactivate();

  void attributeChanged( @Nonnull Object entity, @Nonnull String name, @Nonnull Object value );

  void entityRemoved( @Nonnull Object entity );
}
