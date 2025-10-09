package replicant.server.transport;

import java.util.Objects;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import replicant.server.ChannelAddress;

public final class SchemaMetaData
{
  @Nonnull
  private final String _name;
  @Nonnull
  private final ChannelMetaData[] _channels;
  @Nonnull
  private final ChannelMetaData[] _instanceChannels;

  public SchemaMetaData( @Nonnull final String name, @Nonnull final ChannelMetaData... channels )
  {
    for ( int i = 0; i < channels.length; i++ )
    {
      final ChannelMetaData channel = channels[ i ];
      if ( null != channel && i != channel.getChannelId() )
      {
        final String message = "Channel at index " + i + " does not have channel id matching index: " + channel;
        throw new IllegalArgumentException( message );
      }
    }
    _name = Objects.requireNonNull( name );
    _channels = channels;
    _instanceChannels =
      Stream.of( channels )
        .filter( Objects::nonNull )
        .filter( ChannelMetaData::isInstanceGraph )
        .toArray( ChannelMetaData[]::new );
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
    return getChannelMetaData( address.channelId() );
  }

  /**
   * @return the channel metadata.
   */
  @Nonnull
  public ChannelMetaData getChannelMetaData( final int channelId )
  {
    if ( !hasChannelMetaData( channelId ) )
    {
      throw new IllegalArgumentException( "Channel with id " + channelId + " does not exist" );
    }
    return _channels[ channelId ];
  }

  public boolean hasChannelMetaData( final int channelId )
  {
    return null != _channels[ channelId ];
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
