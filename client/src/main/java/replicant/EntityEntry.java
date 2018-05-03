package replicant;

import arez.annotations.ArezComponent;
import java.util.Objects;
import javax.annotation.Nonnull;

/**
 * An observable structure that contains reference to Entity used from within Subscription.
 *
 * <p>This is only used so we can observe it in finder and thus finder will be rescheduled once the entry
 * is removed from subscription, even if entity is not removed altogether.</p>
 */
@ArezComponent( allowEmpty = true )
abstract class EntityEntry
{
  private final Entity _entity;

  static EntityEntry create( @Nonnull final Entity entity )
  {
    return new Arez_EntityEntry( entity );
  }

  EntityEntry( @Nonnull final Entity entity )
  {
    _entity = Objects.requireNonNull( entity );
  }

  Entity getEntity()
  {
    return _entity;
  }
}
