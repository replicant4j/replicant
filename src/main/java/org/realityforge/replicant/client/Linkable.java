package org.realityforge.replicant.client;

/**
 * Interface implemented by imitations that need to be linked.
 */
public interface Linkable
{
  /**
   * Resolve any references to related entities.
   */
  void link();

  /**
   * Remove direct references to related entities and mark as unresolved.
   */
  void delink();

  /**
   * Invalidating the entity will remove the references to this entity from the repository and change broker.
   * This makes this entity no longer usable.
   */
  void invalidate();
}
