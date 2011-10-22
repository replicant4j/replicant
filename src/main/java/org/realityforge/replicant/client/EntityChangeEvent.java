package org.realityforge.replicant.client;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * An event indicating that an imitation has changed.
 */
public final class EntityChangeEvent
{
  private final EntityChangeType _type;
  private final Object _object;
  private final String _name;
  private final Object _value;

  public EntityChangeEvent( @Nonnull final EntityChangeType type, @Nonnull final Object object )
  {
    this( type, object, null, null );
  }

  @SuppressWarnings( { "ConstantConditions" } )
  public EntityChangeEvent( @Nonnull final EntityChangeType type,
                            @Nonnull final Object object,
                            @Nullable final String name,
                            @Nullable final Object value )
  {
    if ( null == type )
    {
      throw new NullPointerException( "type" );
    }
    if ( null == object )
    {
      throw new NullPointerException( "object" );
    }
    _type = type;
    _object = object;
    _name = name;
    _value = value;
  }

  @Nonnull
  public final EntityChangeType getType()
  {
    return _type;
  }

  @Nonnull
  public final Object getObject()
  {
    return _object;
  }

  @Nullable
  public final String getName()
  {
    return _name;
  }

  @Nullable
  public final Object getValue()
  {
    return _value;
  }

  public final String toString()
  {
    final StringBuilder sb = new StringBuilder();
    sb.append( "EntityChange[type=" );
    sb.append( getType().name() );
    sb.append( ",name=" );
    sb.append( getName() );
    sb.append( ",value=" );
    sb.append( getValue() );
    sb.append( ",object=" );
    sb.append( getObject() );
    sb.append( "]" );
    return sb.toString();
  }
}
