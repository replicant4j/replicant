package org.realityforge.replicant.client;

import javax.annotation.Nonnull;

/**
 * The ChangeMapper is responsible for applying changes to the world.
 * <p/>
 * <p>The mapper will be invoked for each change message and is responsible for;</p>
 * <p/>
 * <ul>
 * <li>creating the entity if it does not already exist</li>
 * <li>registering or de-registering the entity in the {@link EntityRepository}</li>
 * <li>applying the state changes to the entity</li>
 * <li>generating change messages and propagating them through the {@link EntityChangeBroker}</li>
 * </ul>
 * <p/>
 * <p>As the ChangeMapper is very specific to the domain model that is being replicated, the ChangeMapper
 * is typically generated from a description via a tool such as Domgen.</p>
 */
public interface ChangeMapper
{
  /**
   * Apply a single change to the world.
   *
   * @param change the Change.
   * @return the entity that was created, updated or removed.
   */
  @Nonnull
  Object applyChange( @Nonnull Change change );
}
