package replicant;

import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import replicant.messages.ChannelChange;
import replicant.shared.Messages;

final class ChannelChangeDescriptor
{
  enum Type
  {
    ADD, REMOVE, UPDATE, DELETE
  }

  @Nonnull
  private final Type _type;
  @Nonnull
  private final ChannelAddress _address;
  @Nullable
  private final Object _filter;

  @Nonnull
  static ChannelChangeDescriptor from( final int schema, @Nonnull final String channelAction )
  {
    try
    {
      final String descriptor = channelAction.substring( 1 );
      final ChannelAddress address = ChannelAddress.parse( schema, descriptor );
      return new ChannelChangeDescriptor( actionToType( channelAction ), address, null );
    }
    catch ( final Throwable t )
    {
      throw new IllegalStateException( "Failed to parse channel action '" + channelAction + "'", t );
    }
  }

  @Nonnull
  static ChannelChangeDescriptor from( final int schema, @Nonnull final ChannelChange channelChange )
  {
    return from( schema, channelChange.getChannel() );
  }

  @Nonnull
  private static Type actionToType( @Nonnull final String channelAction )
  {
    assert !channelAction.isEmpty();
    final char commandCode = channelAction.charAt( 0 );
    final Type type =
      Messages.Update.CHANNEL_ACTION_ADD == commandCode ? Type.ADD :
      Messages.Update.CHANNEL_ACTION_REMOVE == commandCode ? Type.REMOVE :
      Messages.Update.CHANNEL_ACTION_UPDATE == commandCode ? Type.UPDATE :
      Messages.Update.CHANNEL_ACTION_DELETE == commandCode ? Type.DELETE :
      null;
    if ( null == type )
    {
      throw new IllegalArgumentException( "Unknown channel action '" + channelAction + "'" );
    }
    return type;
  }

  private ChannelChangeDescriptor( @Nonnull final Type type,
                                   @Nonnull final ChannelAddress address,
                                   @Nullable final Object filter )
  {
    _type = Objects.requireNonNull( type );
    _address = Objects.requireNonNull( address );
    _filter = filter;
  }

  @Nonnull
  Type getType()
  {
    return _type;
  }

  @Nonnull
  ChannelAddress getAddress()
  {
    return _address;
  }

  @Nullable
  Object getFilter()
  {
    return _filter;
  }
}
