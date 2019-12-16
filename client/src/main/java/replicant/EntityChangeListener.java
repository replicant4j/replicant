package replicant;

import javax.annotation.Nonnull;

/**
 * The interface through which listeners receive events.
 */
public interface EntityChangeListener
{
  void entityAdded( @Nonnull EntityChangeEvent event );

  void entityRemoved( @Nonnull EntityChangeEvent event );

  void attributeChanged( @Nonnull EntityChangeEvent event );

  void relatedAdded( @Nonnull EntityChangeEvent event );

  void relatedRemoved( @Nonnull EntityChangeEvent event );
}
