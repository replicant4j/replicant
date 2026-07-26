package replicant.spy;

import arez.spy.SerializableEvent;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.NonNull;
import replicant.AreaOfInterest;
import replicant.DatasetAddress;

/**
 * Notification when an AreaOfInterest has been created.
 */
public final class AreaOfInterestCreatedEvent implements SerializableEvent {
    @NonNull
    private final AreaOfInterest _areaOfInterest;

    public AreaOfInterestCreatedEvent(@NonNull final AreaOfInterest areaOfInterest) {
        _areaOfInterest = Objects.requireNonNull(areaOfInterest);
    }

    @NonNull
    public AreaOfInterest getAreaOfInterest() {
        return _areaOfInterest;
    }

    @Override
    public void toMap(@NonNull final Map<String, Object> map) {
        map.put("type", "AreaOfInterest.Created");
        final DatasetAddress datasetAddress = getAreaOfInterest().getDatasetAddress();
        map.put("datasetAddress.schemaId", datasetAddress.schemaId());
        map.put("datasetAddress.datasetId", datasetAddress.datasetId());
        map.put("datasetAddress.datasetRootId", datasetAddress.datasetRootId());
        map.put("channel.filter", getAreaOfInterest().getFilter());
    }
}
