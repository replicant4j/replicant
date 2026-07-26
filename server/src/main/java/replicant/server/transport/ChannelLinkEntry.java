package replicant.server.transport;

import javax.json.JsonObject;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import replicant.server.DatasetAddress;

record ChannelLinkEntry(
        @NonNull LinkOwner owner,
        @NonNull DatasetAddress sourceDatasetAddress,
        @NonNull DatasetAddress targetDatasetAddress,
        @Nullable JsonObject filter) {
    ChannelLinkEntry {
        assert sourceDatasetAddress.concrete();
        assert targetDatasetAddress.concrete();
    }
}
