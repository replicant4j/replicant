package replicant.server.transport;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.json.JsonObject;
import replicant.server.ChannelAddress;

record ChannelLinkEntry(@Nonnull LinkOwner owner,
                        @Nonnull ChannelAddress source,
                        @Nonnull ChannelAddress target,
                        @Nullable JsonObject filter)
{
  ChannelLinkEntry
  {
    assert source.concrete();
    assert target.concrete();
  }
}
