package replicant.server;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public record ChannelAddress(int channelId, @Nullable Integer rootId, @Nullable String filterInstanceId)
  implements Comparable<ChannelAddress>
{
  @Nonnull
  public static ChannelAddress parse( @Nonnull final String name )
  {
    final int instanceOffset = name.indexOf( '#' );
    final String channelPart = -1 == instanceOffset ? name : name.substring( 0, instanceOffset );
    final String filterInstanceId = -1 == instanceOffset ? null : name.substring( instanceOffset + 1 );
    final int offset = channelPart.indexOf( "." );
    final int channelId = Integer.parseInt( -1 == offset ? channelPart : channelPart.substring( 0, offset ) );
    final Integer rootId = -1 == offset ? null : Integer.parseInt( channelPart.substring( offset + 1 ) );
    return new ChannelAddress( channelId, rootId, filterInstanceId );
  }

  public ChannelAddress( final int channelId )
  {
    this( channelId, null, null );
  }

  public ChannelAddress( final int channelId, @Nullable final Integer rootId )
  {
    this( channelId, rootId, null );
  }

  public ChannelAddress( final int channelId, @Nullable final Integer rootId, @Nullable String filterInstanceId )
  {
    this.channelId = channelId;
    this.rootId = rootId;
    this.filterInstanceId = filterInstanceId;
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
        // continue
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
        final int rootDiff = rootId.compareTo( otherRootId );
        if ( 0 != rootDiff )
        {
          return rootDiff;
        }
      }
    }
    final String f1 = filterInstanceId();
    final String f2 = other.filterInstanceId();
    if ( null == f1 && null == f2 )
    {
      return 0;
    }
    else if ( null == f1 )
    {
      return -1;
    }
    else if ( null == f2 )
    {
      return 1;
    }
    else
    {
      return f1.compareTo( f2 );
    }
  }

  @Nonnull
  @Override
  public String toString()
  {
    final String base = channelId + ( null == rootId ? "" : "." + rootId );
    return base + ( null == filterInstanceId ? "" : "#" + filterInstanceId );
  }
}
