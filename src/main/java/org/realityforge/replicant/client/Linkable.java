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
}
