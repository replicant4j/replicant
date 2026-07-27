package replicant;

import static org.testng.Assert.*;

import arez.Disposable;
import java.util.concurrent.atomic.AtomicInteger;
import org.jspecify.annotations.NonNull;
import org.testng.annotations.Test;
import replicant.spy.AreaOfInterestDisposedEvent;
import zemeckis.ZemeckisTestUtil;

public class AreaOfInterestTest extends AbstractReplicantTest {
    @Test
    public void statusValues() {
        assertEquals(AreaOfInterest.Status.values(), new AreaOfInterest.Status[] {
            AreaOfInterest.Status.PENDING, AreaOfInterest.Status.SATISFIED, AreaOfInterest.Status.INVALIDATED
        });
    }

    @Test
    public void statusAndDataAvailabilityAreIndependent() {
        final AreaOfInterest areaOfInterest = createAreaOfInterest(new DatasetAddress(1, 0));

        assertEquals(areaOfInterest.getStatus(), AreaOfInterest.Status.PENDING);
        assertFalse(areaOfInterest.isDataAvailable());

        final Subscription implicit =
                createSubscription(areaOfInterest.getDatasetAddress(), null, SubscriptionMode.IMPLICIT);

        assertEquals(areaOfInterest.getStatus(), AreaOfInterest.Status.PENDING);
        assertTrue(areaOfInterest.isDataAvailable());

        Disposable.dispose(implicit);
        final Subscription explicit =
                createSubscription(areaOfInterest.getDatasetAddress(), "Other", SubscriptionMode.EXPLICIT);

        assertEquals(areaOfInterest.getStatus(), AreaOfInterest.Status.PENDING);
        assertTrue(areaOfInterest.isDataAvailable());

        safeAction(() -> explicit.setFilterParameter(null));

        assertEquals(areaOfInterest.getStatus(), AreaOfInterest.Status.SATISFIED);
        assertTrue(areaOfInterest.isDataAvailable());
    }

    @Test
    public void onConstruct() {
        final AreaOfInterest areaOfInterest = createAreaOfInterest(new DatasetAddress(1, 0));

        safeAction(() -> {
            assertEquals(areaOfInterest.getStatus(), AreaOfInterest.Status.PENDING);
            assertEquals(areaOfInterest.getDatasetAddress(), new DatasetAddress(1, 0));
            assertNull(areaOfInterest.getFilterParameter());
            assertNull(areaOfInterest.getSubscription());
            assertFalse(areaOfInterest.isDataAvailable());
        });
    }

    @Test
    public void disposeAreaOfInterestGeneratesSpyEvent() {
        final AreaOfInterest areaOfInterest = createAreaOfInterest(new DatasetAddress(1, 0));

        final TestSpyEventHandler handler = registerTestSpyEventHandler();

        Disposable.dispose(areaOfInterest);
        handler.assertEventCount(1);

        final AreaOfInterestDisposedEvent event = handler.assertNextEvent(AreaOfInterestDisposedEvent.class);
        assertEquals(event.getAreaOfInterest(), areaOfInterest);
    }

    @Test
    public void notifications() {
        createConnector();
        final AreaOfInterest areaOfInterest = createAreaOfInterest(new DatasetAddress(1, 0));

        final AtomicInteger getStatusCallCount = new AtomicInteger();
        observer(() -> {
            if (Disposable.isNotDisposed(areaOfInterest)) {
                areaOfInterest.getStatus();
            }
            getStatusCallCount.incrementAndGet();
        });

        final AtomicInteger isDataAvailableCallCount = new AtomicInteger();
        observer(() -> {
            if (Disposable.isNotDisposed(areaOfInterest)) {
                areaOfInterest.isDataAvailable();
            }
            isDataAvailableCallCount.incrementAndGet();
        });

        assertEquals(getStatusCallCount.get(), 1);
        assertEquals(isDataAvailableCallCount.get(), 1);

        final Subscription subscription =
                createSubscription(areaOfInterest.getDatasetAddress(), null, SubscriptionMode.EXPLICIT);

        assertEquals(getStatusCallCount.get(), 2);
        assertEquals(isDataAvailableCallCount.get(), 2);

        safeAction(() -> subscription.setFilterParameter("Other"));

        assertEquals(getStatusCallCount.get(), 3);
        assertEquals(isDataAvailableCallCount.get(), 2);

        Disposable.dispose(subscription);

        assertEquals(getStatusCallCount.get(), 3);
        assertEquals(isDataAvailableCallCount.get(), 3);
    }

    @Test
    public void testToString() {
        final AreaOfInterest areaOfInterest = createAreaOfInterest(new DatasetAddress(1, 0));
        assertEquals(areaOfInterest.toString(), "AreaOfInterest[1.0 Status: PENDING]");
    }

    @Test
    public void testToStringWithFilterParameter() {
        final AreaOfInterest areaOfInterest = AreaOfInterest.create(null, new DatasetAddress(1, 0), "MyFilter", false);
        assertEquals(areaOfInterest.toString(), "AreaOfInterest[1.0 Filter Parameter: MyFilter Status: PENDING]");
    }

    @Test
    public void testToString_namesDisabled() {
        ReplicantTestUtil.disableNames();

        final AreaOfInterest areaOfInterest = createAreaOfInterest(new DatasetAddress(1, 0));

        assertEquals(
                areaOfInterest.toString(),
                "replicant.Arez_AreaOfInterest@" + Integer.toHexString(areaOfInterest.hashCode()));
    }

    @Test
    public void refCounting() {
        final AreaOfInterest areaOfInterest =
                createAreaOfInterest(new DatasetAddress(ValueUtil.randomInt(), ValueUtil.randomInt()));

        assertEquals(areaOfInterest.getRefCount(), 0);

        areaOfInterest.incRefCount();
        areaOfInterest.incRefCount();

        assertEquals(areaOfInterest.getRefCount(), 2);

        areaOfInterest.decRefCount();

        assertEquals(areaOfInterest.getRefCount(), 1);

        areaOfInterest.decRefCount();
        assertEquals(areaOfInterest.getRefCount(), 0);
        areaOfInterest.incRefCount();
        assertEquals(areaOfInterest.getRefCount(), 1);

        assertTrue(ZemeckisTestUtil.pumpNext());

        assertEquals(areaOfInterest.getRefCount(), 1);
        assertTrue(Disposable.isNotDisposed(areaOfInterest));

        areaOfInterest.decRefCount();
        assertEquals(areaOfInterest.getRefCount(), 0);

        assertTrue(ZemeckisTestUtil.pumpNext());

        assertTrue(Disposable.isDisposed(areaOfInterest));
    }

    @NonNull
    private AreaOfInterest createAreaOfInterest(@NonNull final DatasetAddress datasetAddress) {
        return AreaOfInterest.create(
                Replicant.areZonesEnabled() ? Replicant.context() : null, datasetAddress, null, false);
    }
}
