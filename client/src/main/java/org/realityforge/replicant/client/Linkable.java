package org.realityforge.replicant.client;

import javax.annotation.Nonnull;

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
   * Link specified object if it is linkable.
   *
   * @param object the object to link if linkable.
   */
  static void link( @Nonnull final Object object )
  {
    if ( object instanceof Linkable )
    {
      final Linkable linkable = (Linkable) object;
      linkable.link();
    }
  }
}
