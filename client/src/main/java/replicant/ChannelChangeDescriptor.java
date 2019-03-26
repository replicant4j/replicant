package replicant;

import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.replicant.shared.SharedConstants;
import replicant.messages.ChannelChange;

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
    final char commandCode = channelAction.charAt( 0 );
    final Type type =
      SharedConstants.CHANNEL_ACTION_ADD == commandCode ?
      Type.ADD :
      SharedConstants.CHANNEL_ACTION_REMOVE == commandCode ? Type.REMOVE : Type.DELETE;
    final ChannelAddress address = ChannelAddress.parse( schema, channelAction.substring( 1 ) );
    return new ChannelChangeDescriptor( type, address, null );
  }

  @Nonnull
  static ChannelChangeDescriptor from( final int schema, @Nonnull final ChannelChange channelChange )
  {
    final String channelAction = channelChange.getChannel();
    final char commandCode = channelAction.charAt( 0 );
    final Type type =
      SharedConstants.CHANNEL_ACTION_ADD == commandCode ?
      Type.ADD :
      SharedConstants.CHANNEL_ACTION_REMOVE == commandCode ?
      Type.REMOVE :
      SharedConstants.CHANNEL_ACTION_DELETE == commandCode ? Type.DELETE : Type.UPDATE;
    final ChannelAddress address = ChannelAddress.parse( schema, channelAction.substring( 1 ) );
    return new ChannelChangeDescriptor( type, address, channelChange.getFilter() );
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
