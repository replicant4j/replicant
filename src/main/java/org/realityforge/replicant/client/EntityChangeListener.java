package org.realityforge.replicant.client;

/**
 * The interface through which listeners receive events.
 */
public interface EntityChangeListener
{
  void entityRemoved( EntityChangeEvent event );

  void attributeChanged( EntityChangeEvent event );

  void relatedAdded( EntityChangeEvent event );

  void relatedRemoved( EntityChangeEvent event );
}
