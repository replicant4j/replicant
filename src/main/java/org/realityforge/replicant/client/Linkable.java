package org.realityforge.replicant.client;

/**
 * Interface implemented by imitations that need to be linked. This is a contract between an entity and the
 * entity repository. Note: the methods on this interface should only be invoked by the repository and not by users
 * of the entity repository.
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
