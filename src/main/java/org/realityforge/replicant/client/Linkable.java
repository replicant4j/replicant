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

  /**
   * Invalidating the entity will delink the entity from related entities and remove the references to the
   * repository and change broker. After invoking this method the entity is no longer valid and should not
   * be used. This is called by the repository during delinking.
   */
  void invalidate();

  /**
   * @return false if invalidate has been invoked on entity, true otherwise.
   */
  boolean isValid();
}
