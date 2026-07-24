package replicant;

import arez.annotations.ArezComponent;
import arez.annotations.ComponentDependency;
import arez.annotations.Feature;
import java.util.Objects;
import org.jspecify.annotations.NonNull;

/**
 * An observable structure that contains reference to Entity used from within Subscription.
 *
 * <p>This is used so we can observe it in finder and thus finder will be rescheduled once the entry
 * is removed from subscription, even if entity is not removed altogether.</p>
 */
@ArezComponent( requireId = Feature.DISABLE )
abstract class EntitySubscriptionEntry
{
  /**
   * The underlying entity.
   */
  @NonNull
  @ComponentDependency
  final Entity _entity;

  @NonNull
  static EntitySubscriptionEntry create( @NonNull final Entity entity )
  {
    return new Arez_EntitySubscriptionEntry( entity );
  }

  /**
   * Create entry for entity.
   *
   * @param entity the entity.
   */
  EntitySubscriptionEntry( @NonNull final Entity entity )
  {
    _entity = Objects.requireNonNull( entity );
  }

  /**
   * Return the entity the entry represents.
   *
   * @return the entity the entry represents.
   */
  @NonNull
  Entity getEntity()
  {
    return _entity;
  }
}
