package org.realityforge.replicant.client;

import arez.Arez;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A channel address is immutable reference that defines the channel address.
 * A "type" graph is addressed by the type while an "instance" graph is addressed
 * by the "type" and the instance "id"
 */
public final class ChannelAddress
{
  @Nonnull
  private final Enum _graph;
  @Nullable
  private final Object _id;

  public ChannelAddress( @Nonnull final Enum graph )
  {
    this( graph, null );
  }

  public ChannelAddress( @Nonnull final Enum graph, @Nullable final Object id )
  {
    _graph = Objects.requireNonNull( graph );
    _id = id;
  }

  @Nonnull
  public Class getSystem()
  {
    return _graph.getDeclaringClass();
  }

  @Nonnull
  public Enum getGraph()
  {
    return _graph;
  }

  @Nullable
  public Object getId()
  {
    return _id;
  }

  @Override
  public String toString()
  {
    if ( Arez.areNamesEnabled() )
    {
      return getSystem().getSimpleName() + "." + _graph.toString() + ( null != _id ? ":" + _id : "" );
    }
    else
    {
      return super.toString();
    }
  }

  @Override
  public boolean equals( final Object o )
  {
    if ( this == o )
    {
      return true;
    }
    if ( o == null || getClass() != o.getClass() )
    {
      return false;
    }

    final ChannelAddress that = (ChannelAddress) o;
    return Objects.equals( _graph, that._graph ) && Objects.equals( _id, that._id );
  }

  @Override
  public int hashCode()
  {
    int result = _graph.hashCode();
    result = 31 * result + ( _id != null ? _id.hashCode() : 0 );
    return result;
  }
}
