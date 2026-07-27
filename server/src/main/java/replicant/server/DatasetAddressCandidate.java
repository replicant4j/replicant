package replicant.server;

import org.jspecify.annotations.Nullable;

/**
 * A concrete Dataset Address or an incomplete Dataset Address Template encountered while evaluating a Dataset Link.
 */
public sealed interface DatasetAddressCandidate permits DatasetAddress, DatasetAddressTemplate {
    int datasetId();

    @Nullable
    Integer datasetRootId();

    @Nullable
    String datasetKey();
}
