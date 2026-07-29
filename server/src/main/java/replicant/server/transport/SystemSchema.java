package replicant.server.transport;

import java.util.Objects;
import java.util.stream.Stream;
import org.jspecify.annotations.NonNull;
import replicant.server.DatasetAddress;

/**
 * The server-side catalog of Dataset definitions belonging to one isolated replicated system.
 */
public final class SystemSchema {
    @NonNull
    private final String _name;

    @NonNull
    private final Dataset[] _datasets;

    @NonNull
    private final Dataset[] _instanceDatasets;

    public SystemSchema(@NonNull final String name, @NonNull final Dataset... datasets) {
        for (var i = 0; i < datasets.length; i++) {
            final var dataset = datasets[i];
            if (null != dataset && i != dataset.getId()) {
                final var message =
                        "Dataset at index " + i + " does not have a Dataset ID matching the index: " + dataset;
                throw new IllegalArgumentException(message);
            }
        }
        _name = Objects.requireNonNull(name);
        _datasets = datasets;
        _instanceDatasets = Stream.of(datasets)
                .filter(Objects::nonNull)
                .filter(Dataset::isInstanceDataset)
                .toArray(Dataset[]::new);
    }

    @NonNull
    public String getName() {
        return _name;
    }

    public int getDatasetCount() {
        return _datasets.length;
    }

    @NonNull
    public Dataset getDataset(@NonNull final DatasetAddress datasetAddress) {
        return getDataset(datasetAddress.datasetId());
    }

    /**
     * @return the Dataset definition.
     */
    @NonNull
    public Dataset getDataset(final int datasetId) {
        if (!hasDataset(datasetId)) {
            throw new IllegalArgumentException("Dataset with Dataset ID " + datasetId + " does not exist");
        }
        return _datasets[datasetId];
    }

    public boolean hasDataset(final int datasetId) {
        return null != _datasets[datasetId];
    }

    public int getInstanceDatasetCount() {
        return _instanceDatasets.length;
    }

    @NonNull
    public Dataset getInstanceDatasetByIndex(final int index) {
        return _instanceDatasets[index];
    }
}
