package replicant.spy;

import static org.testng.Assert.*;

import java.util.HashMap;
import org.testng.annotations.Test;
import replicant.AbstractReplicantTest;
import replicant.DatasetAddress;
import replicant.Subscription;
import replicant.ValueUtil;

public class SubscriptionDisposedEventTest extends AbstractReplicantTest {
    @Test
    public void basicOperation() {
        // Pause scheduler to prevent automatic subscription reconciliation
        pauseScheduler();

        final String filterParameter = ValueUtil.randomString();
        final Subscription subscription = createSubscription(new DatasetAddress(1, 2), filterParameter, true);

        final SubscriptionDisposedEvent event = new SubscriptionDisposedEvent(subscription);

        assertEquals(event.getSubscription(), subscription);

        final HashMap<String, Object> data = new HashMap<>();
        safeAction(() -> event.toMap(data));

        assertEquals(data.get("type"), "Subscription.Disposed");
        assertEquals(data.get("datasetAddress.schemaId"), 1);
        assertEquals(data.get("datasetAddress.datasetId"), 2);
        assertNull(data.get("datasetAddress.datasetRootId"));
        assertEquals(data.get("subscription.filterParameter"), filterParameter);
        assertEquals(data.get("explicitSubscription"), true);
        assertEquals(data.size(), 6);
    }
}
