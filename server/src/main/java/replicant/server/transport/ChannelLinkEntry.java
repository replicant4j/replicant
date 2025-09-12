package replicant.server.transport;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import replicant.server.ChannelAddress;

record ChannelLinkEntry(@Nonnull ChannelAddress source, @Nonnull ChannelAddress target, @Nullable Object filter)
{
}
