package replicant;

import arez.annotations.ArezComponent;
import arez.annotations.ComponentDependency;
import java.util.Objects;
import javax.annotation.Nonnull;

/**
 * An observable structure that contains reference to Entity used from within Subscription.
 *
 * <p>This is used so we can observe it in finder and thus finder will be rescheduled once the entry
 * is removed from subscription, even if entity is not removed altogether.</p>
 */
@ArezComponent
abstract class EntitySubscriptionEntry
{
  /**
   * The underlying entity.
   */
  @Nonnull
  @ComponentDependency
  final Entity _entity;

  @Nonnull
  static EntitySubscriptionEntry create( @Nonnull final Entity entity )
  {
    return new Arez_EntitySubscriptionEntry( entity );
  }

  /**
   * Create entry for entity.
   *
   * @param entity the entity.
   */
  EntitySubscriptionEntry( @Nonnull final Entity entity )
  {
    _entity = Objects.requireNonNull( entity );
  }

  /**
   * Return the entity the entry represents.
   *
   * @return the entity the entry represents.
   */
  @Nonnull
  final Entity getEntity()
  {
    return _entity;
  }
}
