package replicant.spy;

import static org.testng.Assert.*;

import java.util.HashMap;
import org.testng.annotations.Test;
import replicant.AbstractReplicantTest;
import replicant.AreaOfInterest;
import replicant.DatasetAddress;
import replicant.Replicant;
import replicant.ValueUtil;

public class AreaOfInterestFilterUpdatedEventTest extends AbstractReplicantTest {
    @Test
    public void basicOperation() {
        // Pause scheduler to prevent automatic subscription reconciliation
        pauseScheduler();

        final String filter = ValueUtil.randomString();
        final DatasetAddress datasetAddress = new DatasetAddress(1, 2);
        final AreaOfInterest areaOfInterest =
                safeAction(() -> Replicant.context().createOrUpdateAreaOfInterest(datasetAddress, filter));

        final AreaOfInterestFilterUpdatedEvent event = new AreaOfInterestFilterUpdatedEvent(areaOfInterest);

        assertEquals(event.getAreaOfInterest(), areaOfInterest);

        final HashMap<String, Object> data = new HashMap<>();
        safeAction(() -> event.toMap(data));

        assertEquals(data.get("type"), "AreaOfInterest.Updated");
        assertEquals(data.get("datasetAddress.schemaId"), 1);
        assertEquals(data.get("datasetAddress.datasetId"), 2);
        assertNull(data.get("datasetAddress.datasetRootId"));
        assertEquals(data.get("channel.filter"), filter);
        assertEquals(data.size(), 5);
    }
}
