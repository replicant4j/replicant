package replicant.server.transport;

import java.util.Objects;
import java.util.stream.Stream;
import org.jspecify.annotations.NonNull;
import replicant.server.DatasetAddress;

public final class SchemaMetaData {
    @NonNull
    private final String _name;

    @NonNull
    private final DatasetMetadata[] _datasets;

    @NonNull
    private final DatasetMetadata[] _instanceDatasets;

    public SchemaMetaData(@NonNull final String name, @NonNull final DatasetMetadata... datasets) {
        for (var i = 0; i < datasets.length; i++) {
            final var dataset = datasets[i];
            if (null != dataset && i != dataset.getDatasetId()) {
                final var message = "Dataset at index " + i + " does not have Dataset id matching index: " + dataset;
                throw new IllegalArgumentException(message);
            }
        }
        _name = Objects.requireNonNull(name);
        _datasets = datasets;
        _instanceDatasets = Stream.of(datasets)
                .filter(Objects::nonNull)
                .filter(DatasetMetadata::isInstanceDataset)
                .toArray(DatasetMetadata[]::new);
    }

    @NonNull
    public String getName() {
        return _name;
    }

    public int getDatasetCount() {
        return _datasets.length;
    }

    @NonNull
    public DatasetMetadata getDatasetMetadata(@NonNull final DatasetAddress datasetAddress) {
        return getDatasetMetadata(datasetAddress.datasetId());
    }

    /**
     * @return the Dataset metadata.
     */
    @NonNull
    public DatasetMetadata getDatasetMetadata(final int datasetId) {
        if (!hasDatasetMetadata(datasetId)) {
            throw new IllegalArgumentException("Dataset with id " + datasetId + " does not exist");
        }
        return _datasets[datasetId];
    }

    public boolean hasDatasetMetadata(final int datasetId) {
        return null != _datasets[datasetId];
    }

    public int getInstanceDatasetCount() {
        return _instanceDatasets.length;
    }

    @NonNull
    public DatasetMetadata getInstanceDatasetByIndex(final int index) {
        return _instanceDatasets[index];
    }
}
