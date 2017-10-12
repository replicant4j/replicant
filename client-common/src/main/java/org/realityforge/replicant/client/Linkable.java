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
   * @return true if link() has been invoked and invalidate has not been invoked, false otherwise.
   */
  boolean isLinked();
}
