package org.realityforge.replicant.server.transport;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import org.realityforge.replicant.server.ChannelDescriptor;

public final class SystemMetaData
{
  @Nonnull
  private final String _name;
  private List<ChannelMetaData> _channels;

  public SystemMetaData( @Nonnull final String name, @Nonnull final ChannelMetaData... channels )
  {
    assert Arrays.stream( channels ).allMatch( Objects::nonNull );
    for ( int i = 0; i < channels.length; i++ )
    {
      final ChannelMetaData channel = channels[ i ];
      if ( i != channel.getChannelID() )
      {
        final String message = "Channel at index " + i + " does not have channel id matching index: " + channel;
        throw new IllegalArgumentException( message );
      }
    }
    _name = Objects.requireNonNull( name );
    _channels = Collections.unmodifiableList( Arrays.asList( Objects.requireNonNull( channels ) ) );
  }

  @Nonnull
  public String getName()
  {
    return _name;
  }

  @Nonnull
  public List<ChannelMetaData> getChannels()
  {
    return _channels;
  }

  @Nonnull
  public ChannelMetaData getChannelMetaData( @Nonnull final ChannelDescriptor descriptor )
  {
    return getChannelMetaData( descriptor.getChannelID() );
  }

  /**
   * @return the channel metadata.
   */
  @Nonnull
  public ChannelMetaData getChannelMetaData( @Nonnegative final int channelID )
  {
    if ( channelID >= _channels.size() || channelID < 0 )
    {
      final String message = "Channel " + channelID + " not part of declared metadata: " + _channels;
      throw new IllegalStateException( message );
    }
    return _channels.get( channelID );
  }
}
