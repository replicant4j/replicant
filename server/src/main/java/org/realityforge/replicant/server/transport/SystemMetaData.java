package org.realityforge.replicant.server.transport;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.realityforge.replicant.server.ChannelAddress;

public final class SystemMetaData
{
  @Nonnull
  private final String _name;
  @Nonnull
  private final ChannelMetaData[] _channels;
  @Nonnull
  private final ChannelMetaData[] _instanceChannels;

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
    _instanceChannels =
      Stream.of( channels ).filter( ChannelMetaData::isInstanceGraph ).toArray( ChannelMetaData[]::new );
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
    return _channels[ channelId ];
  }

  public int getInstanceChannelCount()
  {
    return _instanceChannels.length;
  }

  @Nonnull
  public ChannelMetaData getInstanceChannelByIndex( final int index )
  {
    return _instanceChannels[ index ];
  }
}
