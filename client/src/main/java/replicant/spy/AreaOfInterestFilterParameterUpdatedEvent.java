package replicant.spy;

import arez.spy.SerializableEvent;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.NonNull;
import replicant.AreaOfInterest;
import replicant.DatasetAddress;

/**
 * Notification when an AreaOfInterest Filter Parameter has been updated.
 */
public final class AreaOfInterestFilterParameterUpdatedEvent implements SerializableEvent {
    @NonNull
    private final AreaOfInterest _areaOfInterest;

    public AreaOfInterestFilterParameterUpdatedEvent(@NonNull final AreaOfInterest areaOfInterest) {
        _areaOfInterest = Objects.requireNonNull(areaOfInterest);
    }

    @NonNull
    public AreaOfInterest getAreaOfInterest() {
        return _areaOfInterest;
    }

    @Override
    public void toMap(@NonNull final Map<String, Object> map) {
        map.put("type", "AreaOfInterest.Updated");
        final AreaOfInterest areaOfInterest = getAreaOfInterest();
        final DatasetAddress datasetAddress = areaOfInterest.getDatasetAddress();
        map.put("datasetAddress.systemSchemaId", datasetAddress.systemSchemaId());
        map.put("datasetAddress.datasetId", datasetAddress.datasetId());
        map.put("datasetAddress.datasetRootId", datasetAddress.datasetRootId());
        map.put("areaOfInterest.filterParameter", areaOfInterest.getFilterParameter());
    }
}
