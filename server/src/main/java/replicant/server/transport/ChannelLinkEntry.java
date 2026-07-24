package replicant.server.transport;

import javax.json.JsonObject;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import replicant.server.ChannelAddress;

record ChannelLinkEntry(
        @NonNull LinkOwner owner,
        @NonNull ChannelAddress source,
        @NonNull ChannelAddress target,
        @Nullable JsonObject filter) {
    ChannelLinkEntry {
        assert source.concrete();
        assert target.concrete();
    }
}
