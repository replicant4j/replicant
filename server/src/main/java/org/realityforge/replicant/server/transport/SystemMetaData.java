package org.realityforge.replicant.server.transport;

import java.util.Arrays;
import java.util.Objects;
import javax.annotation.Nonnull;
import org.realityforge.replicant.server.ChannelAddress;

public final class SystemMetaData
{
  @Nonnull
  private final String _name;
  @Nonnull
  private final ChannelMetaData[] _channels;

  public SystemMetaData( @Nonnull final String name, @Nonnull final ChannelMetaData... channels )
  {
    assert Arrays.stream( channels ).allMatch( Objects::nonNull );
    for ( int i = 0; i < channels.length; i++ )
    {
      final ChannelMetaData channel = channels[ i ];
      if ( i != channel.getChannelId() )
      {
        final String message = "Channel at index " + i + " does not have channel id matching index: " + channel;
        throw new IllegalArgumentException( message );
      }
    }
    _name = Objects.requireNonNull( name );
    _channels = channels;
  }

  @Nonnull
  public String getName()
  {
    return _name;
  }

  public int getChannelCount()
  {
    return _channels.length;
  }

  @Nonnull
  public ChannelMetaData getChannelMetaData( @Nonnull final ChannelAddress address )
  {
    return getChannelMetaData( address.getChannelId() );
  }

  /**
   * @return the channel metadata.
   */
  @Nonnull
  public ChannelMetaData getChannelMetaData( final int channelId )
  {
    if ( channelId >= _channels.length || channelId < 0 )
    {
      throw new NoSuchChannelException( channelId );
    }
    return _channels[ channelId ];
  }
}
