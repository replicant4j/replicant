package org.realityforge.replicant.server;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public record ChannelAddress(int channelId, @Nullable Integer rootId)
  implements Comparable<ChannelAddress>
{
  @Nonnull
  public static ChannelAddress parse( @Nonnull final String name )
  {
    final int offset = name.indexOf( "." );
    final int channelId = Integer.parseInt( -1 == offset ? name : name.substring( 0, offset ) );
    final Integer rootId = -1 == offset ? null : Integer.parseInt( name.substring( offset + 1 ) );
    return new ChannelAddress( channelId, rootId );
  }

  public ChannelAddress( final int channelId )
  {
    this( channelId, null );
  }

  public boolean hasRootId()
  {
    return null != rootId;
  }

  @Override
  public int compareTo( @Nonnull final ChannelAddress other )
  {
    final int channelDiff = Integer.compare( channelId(), other.channelId() );
    if ( 0 != channelDiff )
    {
      return channelDiff;
    }
    else
    {
      final Integer otherRootId = other.rootId();
      final Integer rootId = rootId();
      if ( null == otherRootId && null == rootId )
      {
        return 0;
      }
      else if ( null == otherRootId )
      {
        return -1;
      }
      else if ( null == rootId )
      {
        return 1;
      }
      else
      {
        return rootId.compareTo( otherRootId );
      }
    }
  }

  @Nonnull
  @Override
  public String toString()
  {
    return channelId + ( null == rootId ? "" : "." + rootId );
  }
}
