package org.realityforge.replicant.client;

import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * An event indicating that an imitation has changed.
 */
public final class EntityChangeEvent
{
  @Nonnull
  private final EntityChangeType _type;
  private final Object _object;
  private final String _name;
  private final Object _value;

  EntityChangeEvent( @Nonnull final EntityChangeType type,
                     @Nonnull final Object object,
                     @Nullable final String name,
                     @Nullable final Object value )
  {
    _type = Objects.requireNonNull( type );
    _object = Objects.requireNonNull( object );
    _name = name;
    _value = value;
  }

  @Nonnull
  public EntityChangeType getType()
  {
    return _type;
  }

  @Nonnull
  public Object getObject()
  {
    return _object;
  }

  @Nullable
  public String getName()
  {
    return _name;
  }

  @Nullable
  public Object getValue()
  {
    return _value;
  }

  public String toString()
  {
    return "EntityChange[" +
           "type=" + getType().name() + "," +
           "name=" + getName() + "," +
           "value=" + getValue() + "," +
           "object=" + getObject() +
           "]";
  }
}
