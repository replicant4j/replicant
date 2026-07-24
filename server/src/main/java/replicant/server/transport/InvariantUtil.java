package replicant.server.transport;

import org.jspecify.annotations.NonNull;
import org.jetbrains.annotations.VisibleForTesting;
import replicant.server.ChannelAddress;
import replicant.server.ChannelLink;

final class InvariantUtil
{
  private static final boolean ASSERTIONS_ENABLED = InvariantUtil.class.desiredAssertionStatus();

  private InvariantUtil()
  {
  }

  static boolean isInvariantCheckingEnabled()
  {
    return ASSERTIONS_ENABLED;
  }

  static void assertConcreteAddress( @NonNull final ChannelAddress address )
  {
    assert address.concrete();
  }

  static void assertConcreteAddress( @NonNull final SchemaMetaData schema, @NonNull final ChannelAddress address )
  {
    if ( isInvariantCheckingEnabled() )
    {
      assertConcreteAddress( address );
      assertAddressMatchesChannelMetaData( schema, address );
    }
  }

  @VisibleForTesting
  static void assertAddressMatchesChannelMetaData( @NonNull final SchemaMetaData schema,
                                                   @NonNull final ChannelAddress address )
  {
    if ( isInvariantCheckingEnabled() )
    {
      assertAddressMatchesChannelMetaData( schema.getChannelMetaData( address.channelId() ), address );
    }
  }

  private static void assertAddressMatchesChannelMetaData( @NonNull final ChannelMetaData channel,
                                                           @NonNull final ChannelAddress address )
  {
    if ( channel.isTypeGraph() )
    {
      assert !address.hasRootId();
    }
    else
    {
      assert address.hasRootId();
    }

    if ( address.partial() )
    {
      assert channel.requiresFilterInstanceId();
      assert null == address.filterInstanceId();
    }
    else if ( channel.requiresFilterInstanceId() )
    {
      assert null != address.filterInstanceId();
    }
    else
    {
      assert null == address.filterInstanceId();
    }
  }

  static void assertLink( @NonNull final SchemaMetaData schema, @NonNull final ChannelLink link )
  {
    if ( isInvariantCheckingEnabled() )
    {
      assertAddressMatchesChannelMetaData( schema, link.source() );
      final var targetChannel = schema.getChannelMetaData( link.target().channelId() );
      assertAddressMatchesChannelMetaData( targetChannel, link.target() );

      if ( link.partial() )
      {
        assert link.source().partial() || link.target().partial() ||
               ( targetChannel.requiresFilterParameter() && null == link.targetFilter() );
      }
      else
      {
        assert link.source().concrete();
        assert link.target().concrete();
        assert !targetChannel.requiresFilterParameter() || null != link.targetFilter();
      }
    }
  }
}
