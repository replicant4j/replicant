package replicant.server.transport;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import replicant.server.ChangeSet;
import replicant.server.DatasetAddress;

final class DatasetCacheEntry {
    @NonNull
    private final ReadWriteLock _lock = new ReentrantReadWriteLock();

    @NonNull
    private final DatasetAddress _descriptor;

    @Nullable
    private String _datasetCacheVersion;

    @Nullable
    private ChangeSet _changeSet;

    DatasetCacheEntry(@NonNull final DatasetAddress datasetAddress) {
        _descriptor = Objects.requireNonNull(datasetAddress);
    }

    @NonNull
    ReadWriteLock getLock() {
        return _lock;
    }

    @NonNull
    DatasetAddress getDescriptor() {
        return _descriptor;
    }

    void init(@NonNull final ChangeSet changeSet) {
        _datasetCacheVersion = UUID.randomUUID().toString();
        _changeSet = Objects.requireNonNull(changeSet);
    }

    boolean isInitialized() {
        return null != _datasetCacheVersion;
    }

    @NonNull
    String getDatasetCacheVersion() {
        return Objects.requireNonNull(_datasetCacheVersion);
    }

    @NonNull
    ChangeSet getChangeSet() {
        return Objects.requireNonNull(_changeSet);
    }
}
