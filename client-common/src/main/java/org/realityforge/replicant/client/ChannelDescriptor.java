package org.realityforge.replicant.client;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ChannelDescriptor
{
  @Nonnull
  private final Enum _graph;
  @Nullable
  private final Object _id;

  public ChannelDescriptor( @Nonnull final Enum graph, @Nullable final Object id )
  {
    _graph = graph;
    _id = id;
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
    return _graph.equals( that._graph ) && !( _id != null ? !_id.equals( that._id ) : that._id != null );
  }

  @Override
  public int hashCode()
  {
    int result = _graph.hashCode();
    result = 31 * result + ( _id != null ? _id.hashCode() : 0 );
    return result;
  }
}
