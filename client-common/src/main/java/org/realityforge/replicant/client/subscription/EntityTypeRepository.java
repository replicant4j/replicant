package org.realityforge.replicant.client.subscription;

import arez.annotations.Action;
import arez.annotations.ArezComponent;
import arez.component.AbstractContainer;
import arez.component.RepositoryUtil;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * The EntityTypeRepository contains all entity instances with a specific type.
 */
@ArezComponent
abstract class EntityTypeRepository
  // TODO: Rather than extend ABstractContainer extend something else that can contain non-Arez components
  extends AbstractContainer<Object, Entity>
{
  private final Class<?> _type;

  @Nonnull
  static EntityTypeRepository create( @Nonnull final Class<?> type )
  {
    return new Arez_EntityTypeRepository( type );
  }

  EntityTypeRepository( @Nonnull final Class<?> type )
  {
    _type = Objects.requireNonNull( type );
  }

  @Nonnull
  List<Entity> getEntities()
  {
    return RepositoryUtil.asList( entities() );
  }

  @Nullable
  Entity findEntityById( @Nonnull final Object id )
  {
    return super.findByArezId( id );
  }

  @Nonnull
  public Entity findOrCreateEntity( @Nonnull final Object id )
  {
    final Entity entity = findEntityById( id );
    if ( null != entity )
    {
      return entity;
    }
    else
    {
      final Entity newEntity = Entity.create( _type, id );
      registerEntity( newEntity );
      return newEntity;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean contains( @Nonnull final Entity entity )
  {
    return super.contains( entity );
  }

  /**
   * {@inheritDoc}
   */
  @Action
  @Override
  public void destroy( @Nonnull final Entity entity )
  {
    super.destroy( entity );
  }
}
