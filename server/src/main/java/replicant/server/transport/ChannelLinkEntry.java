package replicant.server.transport;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import replicant.server.ChannelAddress;

record ChannelLinkEntry(@Nonnull LinkOwner owner,
                        @Nonnull ChannelAddress source,
                        @Nonnull ChannelAddress target,
                        @Nullable Object filter)
{
  ChannelLinkEntry
  {
    assert source.concrete();
    assert target.concrete();
  }
}
