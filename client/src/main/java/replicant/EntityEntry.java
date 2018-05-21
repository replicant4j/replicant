package replicant;

import arez.Disposable;
import arez.Observer;
import arez.annotations.ArezComponent;
import arez.annotations.PreDispose;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * An observable structure that contains reference to Entity used from within Subscription.
 *
 * <p>This is used so we can observe it in finder and thus finder will be rescheduled once the entry
 * is removed from subscription, even if entity is not removed altogether. It is also used when the
 * entity is partially disposed.</p>
 */
@ArezComponent( allowEmpty = true )
abstract class EntityEntry
{
  /**
   * The underlying entity.
   */
  @Nonnull
  private final Entity _entity;
  /**
   * The monitor observer that will remove entity from repository if it is independent disposed.
   */
  @Nullable
  private Observer _monitor;

  static EntityEntry create( @Nonnull final Entity entity )
  {
    return new Arez_EntityEntry( entity );
  }

  /**
   * Create entry for entity.
   *
   * @param entity the entity.
   */
  EntityEntry( @Nonnull final Entity entity )
  {
    _entity = Objects.requireNonNull( entity );
  }

  /**
   * Return the entity the entry represents.
   *
   * @return the entity the entry represents.
   */
  @Nonnull
  Entity getEntity()
  {
    return _entity;
  }

  /**
   * Set the monitor that will remove entity from repository when entity is disposed.
   *
   * @param monitor the monitor that will remove entity from repository when entity is disposed.
   */
  void setMonitor( @Nonnull final Observer monitor )
  {
    _monitor = Objects.requireNonNull( monitor );
  }

  @PreDispose
  final void preDispose()
  {
    if ( null != _monitor )
    {
      _monitor.dispose();
      _monitor = null;
    }
    Disposable.dispose( _entity );
  }
}
