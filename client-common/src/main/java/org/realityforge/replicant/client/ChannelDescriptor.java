package org.realityforge.replicant.client;

import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class ChannelDescriptor
{
  @Nonnull
  private final Enum _graph;
  @Nullable
  private final Object _id;

  public ChannelDescriptor( @Nonnull final Enum graph )
  {
    this( graph, null );
  }

  public ChannelDescriptor( @Nonnull final Enum graph, @Nullable final Object id )
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
  public Object getID()
  {
    return _id;
  }

  @Override
  public String toString()
  {
    return _graph.toString() + ( null != _id ? ":" + _id : "" );
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

    final ChannelDescriptor that = (ChannelDescriptor) o;
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
