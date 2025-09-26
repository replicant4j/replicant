package replicant;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import replicant.messages.ChannelChange;
import replicant.shared.Messages;

record ChannelChangeDescriptor(@Nonnull Type type, @Nonnull ChannelAddress address, @Nullable Object filter)
{
  enum Type
  {
    ADD, REMOVE, UPDATE, DELETE
  }

  @Nonnull
  static ChannelChangeDescriptor from( final int schema, @Nonnull final String channelAction )
  {
    final Type type = actionToType( channelAction );
    final ChannelAddress address = ChannelAddress.parse( schema, channelAction.substring( 1 ) );
    return new ChannelChangeDescriptor( type, address, null );
  }

  @Nonnull
  static ChannelChangeDescriptor from( final int schema, @Nonnull final ChannelChange channelChange )
  {
    final String channelAction = channelChange.getChannel();
    final Type type = actionToType( channelAction );
    final ChannelAddress address = ChannelAddress.parse( schema, channelAction.substring( 1 ) );
    return new ChannelChangeDescriptor( type, address, channelChange.getFilter() );
  }

  @Nonnull
  private static Type actionToType( @Nonnull final String channelAction )
  {
    final char commandCode = channelAction.charAt( 0 );
    return Messages.Update.CHANNEL_ACTION_ADD == commandCode ? Type.ADD :
           Messages.Update.CHANNEL_ACTION_REMOVE == commandCode ? Type.REMOVE :
           Messages.Update.CHANNEL_ACTION_UPDATE == commandCode ? Type.UPDATE :
           Type.DELETE;
  }
}
