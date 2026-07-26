package replicant;

import static org.testng.Assert.*;

import arez.Disposable;
import java.util.Collection;
import org.testng.annotations.Test;
import replicant.spy.AreaOfInterestCreatedEvent;
import replicant.spy.AreaOfInterestDisposedEvent;
import replicant.spy.AreaOfInterestFilterParameterUpdatedEvent;

public class AreaOfInterestServiceTest extends AbstractReplicantTest {
    @Test
    public void constructPassingContextWhenZonesDisabled() {
        final IllegalStateException exception =
                expectThrows(IllegalStateException.class, () -> AreaOfInterestService.create(Replicant.context()));

        assertEquals(
                exception.getMessage(),
                "Replicant-0037: ReplicantService passed a context but Replicant.areZonesEnabled() is false");
    }

    @Test
    public void basicSubscriptionManagement() {
        final AreaOfInterestService service = AreaOfInterestService.create(null);

        final DatasetAddress datasetAddress1 = new DatasetAddress(1, 0, null);
        final DatasetAddress datasetAddress2 = new DatasetAddress(1, 1, 1);
        final DatasetAddress datasetAddress3 = new DatasetAddress(1, 1, 2);

        safeAction(() -> {
            final AreaOfInterest areaOfInterest1 = service.createOrUpdateAreaOfInterest(datasetAddress1, null);
            assertNotNull(areaOfInterest1);

            assertEquals(areaOfInterest1.getDatasetAddress(), datasetAddress1);
            assertTrue(Disposable.isNotDisposed(areaOfInterest1));

            final Collection<AreaOfInterest> subscriptions = service.getAreasOfInterest();
            assertEquals(subscriptions.size(), 1);
            assertTrue(
                    subscriptions.stream().anyMatch(n -> n.getDatasetAddress().equals(datasetAddress1)));
            assertFalse(
                    subscriptions.stream().anyMatch(n -> n.getDatasetAddress().equals(datasetAddress2)));
            assertFalse(
                    subscriptions.stream().anyMatch(n -> n.getDatasetAddress().equals(datasetAddress3)));

            final Object newFilterParameter = new Object();
            areaOfInterest1.setFilterParameter(newFilterParameter);

            assertEquals(areaOfInterest1.getFilterParameter(), newFilterParameter);

            Disposable.dispose(areaOfInterest1);
            assertTrue(Disposable.isDisposed(areaOfInterest1));

            assertEquals(service.getAreasOfInterest().size(), 0);
        });
    }

    @Test
    public void createAreaOfInterestGeneratesSpyEvent() {
        final AreaOfInterestService service = AreaOfInterestService.create(null);
        final DatasetAddress datasetAddress1 = new DatasetAddress(1, 0, null);

        safeAction(() -> {
            final TestSpyEventHandler handler = registerTestSpyEventHandler();

            final AreaOfInterest areaOfInterest = service.createOrUpdateAreaOfInterest(datasetAddress1, null);
            assertNotNull(areaOfInterest);

            handler.assertEventCount(1);
            handler.assertNextEvent(
                    AreaOfInterestCreatedEvent.class, e -> assertEquals(e.getAreaOfInterest(), areaOfInterest));
        });
    }

    @Test
    public void updateAreaOfInterestGeneratesSpyEvent() {
        final AreaOfInterestService service = AreaOfInterestService.create(null);
        final DatasetAddress datasetAddress1 = new DatasetAddress(1, 0, null);

        safeAction(() -> {
            service.createOrUpdateAreaOfInterest(datasetAddress1, "Filter1");

            final TestSpyEventHandler handler = registerTestSpyEventHandler();

            final AreaOfInterest areaOfInterest = service.createOrUpdateAreaOfInterest(datasetAddress1, "Filter2");

            handler.assertEventCount(1);

            handler.assertNextEvent(
                    AreaOfInterestFilterParameterUpdatedEvent.class,
                    e -> assertEquals(e.getAreaOfInterest(), areaOfInterest));
        });
    }

    @Test
    public void disposeAreaOfInterestGeneratesSpyEvent() {
        final AreaOfInterestService service = AreaOfInterestService.create(null);
        final DatasetAddress datasetAddress1 = new DatasetAddress(1, 0, null);

        final AreaOfInterest areaOfInterest =
                safeAction(() -> service.createOrUpdateAreaOfInterest(datasetAddress1, "Filter1"));

        final TestSpyEventHandler handler = registerTestSpyEventHandler();

        Disposable.dispose(areaOfInterest);
        handler.assertEventCount(1);

        handler.assertNextEvent(
                AreaOfInterestDisposedEvent.class, e -> assertEquals(e.getAreaOfInterest(), areaOfInterest));
    }

    @Test
    public void createSubscription() {
        safeAction(() -> {
            final AreaOfInterestService service = AreaOfInterestService.create(null);

            final DatasetAddress datasetAddress1 = new DatasetAddress(1, 0);
            final DatasetAddress datasetAddress2 = new DatasetAddress(1, 1);

            final String filterParameter1 = "Filer1";
            final String filterParameter2 = null;

            final AreaOfInterest areaOfInterest1 =
                    service.createOrUpdateAreaOfInterest(datasetAddress1, filterParameter1);

            assertEquals(areaOfInterest1.getDatasetAddress(), datasetAddress1);
            assertEquals(areaOfInterest1.getFilterParameter(), filterParameter1);

            final AreaOfInterest areaOfInterest2 =
                    service.createOrUpdateAreaOfInterest(datasetAddress2, filterParameter2);

            assertEquals(areaOfInterest2.getDatasetAddress(), datasetAddress2);
            assertEquals(areaOfInterest2.getFilterParameter(), filterParameter2);
        });
    }

    @Test
    public void createOrUpdateAreaOfInterest() {
        safeAction(() -> {
            final DatasetAddress datasetAddress = new DatasetAddress(1, 0);
            final String filterParameter1 = ValueUtil.randomString();
            final String filterParameter2 = ValueUtil.randomString();

            final AreaOfInterestService service = AreaOfInterestService.create(null);

            // No existing subscription
            final AreaOfInterest areaOfInterest1 =
                    service.createOrUpdateAreaOfInterest(datasetAddress, filterParameter1);
            assertEquals(areaOfInterest1.getDatasetAddress(), datasetAddress);
            assertEquals(areaOfInterest1.getFilterParameter(), filterParameter1);
            assertEquals(service.findAreaOfInterestByDatasetAddress(datasetAddress), areaOfInterest1);
            assertEquals(service.getAreasOfInterest().size(), 1);

            // Existing subscription, same filterParameter
            final AreaOfInterest areaOfInterest2 =
                    service.createOrUpdateAreaOfInterest(datasetAddress, filterParameter1);
            assertEquals(areaOfInterest2.getDatasetAddress(), datasetAddress);
            assertEquals(areaOfInterest2.getFilterParameter(), filterParameter1);
            assertEquals(areaOfInterest1, areaOfInterest2);
            assertEquals(service.findAreaOfInterestByDatasetAddress(datasetAddress), areaOfInterest2);
            assertEquals(service.getAreasOfInterest().size(), 1);

            // Existing subscription, different filterParameter
            final AreaOfInterest subscription3 = service.createOrUpdateAreaOfInterest(datasetAddress, filterParameter2);
            assertEquals(subscription3.getDatasetAddress(), datasetAddress);
            assertEquals(subscription3.getFilterParameter(), filterParameter2);
            assertEquals(areaOfInterest1, subscription3);
            assertEquals(service.findAreaOfInterestByDatasetAddress(datasetAddress), subscription3);
            assertEquals(service.getAreasOfInterest().size(), 1);
        });
    }
}
