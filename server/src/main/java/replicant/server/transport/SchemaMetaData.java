package replicant.server.transport;

import java.util.Objects;
import java.util.stream.Stream;
import org.jspecify.annotations.NonNull;
import replicant.server.ChannelAddress;

public final class SchemaMetaData
{
  @NonNull
  private final String _name;
  @NonNull
  private final ChannelMetaData[] _channels;
  @NonNull
  private final ChannelMetaData[] _instanceChannels;

  public SchemaMetaData( @NonNull final String name, @NonNull final ChannelMetaData... channels )
  {
    for ( var i = 0; i < channels.length; i++ )
    {
      final var channel = channels[ i ];
      if ( null != channel && i != channel.getChannelId() )
      {
        final var message = "Channel at index " + i + " does not have channel id matching index: " + channel;
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

  @NonNull
  public String getName()
  {
    return _name;
  }

  public int getChannelCount()
  {
    return _channels.length;
  }

  @NonNull
  public ChannelMetaData getChannelMetaData( @NonNull final ChannelAddress address )
  {
    return getChannelMetaData( address.channelId() );
  }

  /**
   * @return the channel metadata.
   */
  @NonNull
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

  @NonNull
  public ChannelMetaData getInstanceChannelByIndex( final int index )
  {
    return _instanceChannels[ index ];
  }
}
