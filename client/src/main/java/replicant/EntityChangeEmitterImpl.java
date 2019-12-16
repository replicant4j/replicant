package replicant;

import java.util.Objects;
import javax.annotation.Nonnull;

final class EntityChangeEmitterImpl
  implements EntityChangeEmitter
{
  @Nonnull
  private final EntityChangeBroker _broker;

  EntityChangeEmitterImpl( @Nonnull final EntityChangeBroker broker )
  {
    _broker = Objects.requireNonNull( broker );
  }

  @Override
  public boolean isEnabled()
  {
    return _broker.isEnabled();
  }

  @Override
  public void attributeChanged( @Nonnull final Object entity, @Nonnull final String name, @Nonnull final Object value )
  {
    _broker.sendEvent( new EntityChangeEvent( EntityChangeEvent.Type.ATTRIBUTE_CHANGED, entity, name, value ) );
  }

  @Override
  public void relatedAdded( @Nonnull final Object entity, @Nonnull final String name, @Nonnull final Object other )
  {
    _broker.sendEvent( new EntityChangeEvent( EntityChangeEvent.Type.RELATED_ADDED, entity, name, other ) );
  }

  @Override
  public void relatedRemoved( @Nonnull final Object entity, @Nonnull final String name, @Nonnull final Object other )
  {
    _broker.sendEvent( new EntityChangeEvent( EntityChangeEvent.Type.RELATED_REMOVED, entity, name, other ) );
  }

  @Override
  public void entityAdded( @Nonnull final Object entity )
  {
    _broker.sendEvent( new EntityChangeEvent( EntityChangeEvent.Type.ENTITY_ADDED, entity, null, null ) );
  }

  @Override
  public void entityRemoved( @Nonnull final Object entity )
  {
    _broker.sendEvent( new EntityChangeEvent( EntityChangeEvent.Type.ENTITY_REMOVED, entity, null, null ) );
  }
}
