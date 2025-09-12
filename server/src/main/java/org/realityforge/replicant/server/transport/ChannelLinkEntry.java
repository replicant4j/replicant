package org.realityforge.replicant.server.transport;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.replicant.server.ChannelAddress;

record ChannelLinkEntry(@Nonnull ChannelAddress source, @Nonnull ChannelAddress target, @Nullable Object filter)
{
}
