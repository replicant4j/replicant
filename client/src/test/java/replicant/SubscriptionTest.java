package replicant;

import static org.testng.Assert.*;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import org.testng.annotations.Test;

public class SubscriptionTest extends AbstractReplicantTest {
    @Test
    public void basicConstruction() {
        final DatasetAddress datasetAddress = new DatasetAddress(1, 0);
        final Object filterParameter = ValueUtil.randomString();
        final Subscription subscription =
                Subscription.create(null, datasetAddress, filterParameter, SubscriptionMode.EXPLICIT);

        assertEquals(subscription.datasetAddress(), datasetAddress);

        safeAction(() -> {
            assertEquals(subscription.getMode(), SubscriptionMode.EXPLICIT);
            subscription.setMode(SubscriptionMode.IMPLICIT);
            assertEquals(subscription.getMode(), SubscriptionMode.IMPLICIT);
        });
        safeAction(() -> assertEquals(subscription.getReplicaEntries().size(), 0));
        safeAction(() -> assertEquals(subscription.getFilterParameter(), filterParameter));
    }

    @Test
    public void filterParameter() {
        final Object filterParameter1 = ValueUtil.randomString();
        final Object filterParameter2 = ValueUtil.randomString();

        final Subscription subscription =
                Subscription.create(null, new DatasetAddress(1, 0), filterParameter1, SubscriptionMode.IMPLICIT);

        safeAction(() -> assertEquals(subscription.getFilterParameter(), filterParameter1));
        safeAction(() -> subscription.setFilterParameter(filterParameter2));
        safeAction(() -> assertEquals(subscription.getFilterParameter(), filterParameter2));
    }

    @Test
    public void replicaEntries() {
        final ReplicaRegistry replicaRegistry = Replicant.context().getReplicaRegistry();
        final ReplicaEntry replicaEntry1 =
                safeAction(() -> replicaRegistry.findOrCreateReplicaEntry("A/1", A.class, 1));
        final ReplicaEntry replicaEntry2 =
                safeAction(() -> replicaRegistry.findOrCreateReplicaEntry("A/2", A.class, 2));

        final DatasetAddress datasetAddress = new DatasetAddress(1, 0, 1);
        final Subscription subscription = Subscription.create(null, datasetAddress, null, SubscriptionMode.EXPLICIT);

        final AtomicInteger callCount = new AtomicInteger();
        observer(() -> {
            // Just invoke method to get observing
            //noinspection ResultOfMethodCallIgnored
            subscription.getReplicaEntries();
            callCount.incrementAndGet();
        });

        final AtomicInteger findCallCount = new AtomicInteger();
        observer(() -> {
            // Just invoke method to get observing
            subscription.findReplicaEntryByTypeAndId(A.class, 1);
            findCallCount.incrementAndGet();
        });

        assertEquals(callCount.get(), 1);
        assertEquals(findCallCount.get(), 1);
        safeAction(() -> assertEquals(subscription.findAllReplicaTypes().size(), 0));
        safeAction(() -> assertFalse(subscription.findAllReplicaTypes().contains(A.class)));
        safeAction(() ->
                assertEquals(subscription.findAllReplicaEntriesByType(A.class).size(), 0));
        safeAction(() -> assertEquals(
                subscription.findAllReplicaEntriesByType(String.class).size(), 0));
        safeAction(() -> assertNull(subscription.findReplicaEntryByTypeAndId(A.class, 1)));
        safeAction(() -> assertNull(subscription.findReplicaEntryByTypeAndId(A.class, 2)));

        safeAction(() -> subscription.linkSubscriptionToReplicaEntry(replicaEntry1));

        assertEquals(callCount.get(), 2);
        assertEquals(findCallCount.get(), 2);
        safeAction(() -> assertEquals(subscription.findAllReplicaTypes().size(), 1));
        safeAction(() -> assertTrue(subscription.findAllReplicaTypes().contains(A.class)));
        safeAction(() ->
                assertEquals(subscription.findAllReplicaEntriesByType(A.class).size(), 1));
        safeAction(() -> assertEquals(
                subscription.findAllReplicaEntriesByType(String.class).size(), 0));
        safeAction(() -> assertEquals(subscription.findReplicaEntryByTypeAndId(A.class, 1), replicaEntry1));
        safeAction(() -> assertNull(subscription.findReplicaEntryByTypeAndId(A.class, 2)));

        // Add second replicaEntry, finder no need to re-find
        safeAction(() -> subscription.linkSubscriptionToReplicaEntry(replicaEntry2));

        assertEquals(callCount.get(), 3);
        assertEquals(findCallCount.get(), 2);
        safeAction(() -> assertEquals(subscription.findAllReplicaTypes().size(), 1));
        safeAction(() -> assertTrue(subscription.findAllReplicaTypes().contains(A.class)));
        safeAction(() ->
                assertEquals(subscription.findAllReplicaEntriesByType(A.class).size(), 2));
        safeAction(() -> assertEquals(
                subscription.findAllReplicaEntriesByType(String.class).size(), 0));
        safeAction(() -> assertEquals(subscription.findReplicaEntryByTypeAndId(A.class, 1), replicaEntry1));
        safeAction(() -> assertEquals(subscription.findReplicaEntryByTypeAndId(A.class, 2), replicaEntry2));

        // Duplicate link ... ignored as no change
        safeAction(() -> subscription.linkSubscriptionToReplicaEntry(replicaEntry2));

        assertEquals(callCount.get(), 3);
        assertEquals(findCallCount.get(), 2);
        safeAction(() -> assertEquals(subscription.findAllReplicaTypes().size(), 1));
        safeAction(() ->
                assertEquals(subscription.findAllReplicaEntriesByType(A.class).size(), 2));
        safeAction(() -> assertEquals(
                subscription.findAllReplicaEntriesByType(String.class).size(), 0));
        safeAction(() -> assertEquals(subscription.findReplicaEntryByTypeAndId(A.class, 1), replicaEntry1));
        safeAction(() -> assertEquals(subscription.findReplicaEntryByTypeAndId(A.class, 2), replicaEntry2));

        // Removing replicaEntry 1, finder will react
        safeAction(() -> subscription.delinkReplicaEntryFromSubscription(replicaEntry1, true));

        assertEquals(callCount.get(), 4);
        assertEquals(findCallCount.get(), 3);
        safeAction(() -> assertEquals(subscription.findAllReplicaTypes().size(), 1));
        safeAction(() ->
                assertEquals(subscription.findAllReplicaEntriesByType(A.class).size(), 1));
        safeAction(() -> assertEquals(
                subscription.findAllReplicaEntriesByType(String.class).size(), 0));
        safeAction(() -> assertNull(subscription.findReplicaEntryByTypeAndId(A.class, 1)));
        safeAction(() -> assertEquals(subscription.findReplicaEntryByTypeAndId(A.class, 2), replicaEntry2));

        // Removing replicaEntry 2, state is reset
        safeAction(() -> subscription.delinkReplicaEntryFromSubscription(replicaEntry2, true));

        assertEquals(callCount.get(), 5);
        assertEquals(findCallCount.get(), 4);
        safeAction(() -> assertEquals(subscription.findAllReplicaTypes().size(), 0));
        safeAction(() ->
                assertEquals(subscription.findAllReplicaEntriesByType(A.class).size(), 0));
        safeAction(() -> assertEquals(
                subscription.findAllReplicaEntriesByType(String.class).size(), 0));
        safeAction(() -> assertNull(subscription.findReplicaEntryByTypeAndId(A.class, 1)));
        safeAction(() -> assertNull(subscription.findReplicaEntryByTypeAndId(A.class, 2)));
    }

    @Test
    public void delinkReplicaEntryFromSubscription_noSuchType() {
        final ReplicaRegistry replicaRegistry = Replicant.context().getReplicaRegistry();
        final ReplicaEntry replicaEntry = safeAction(() ->
                replicaRegistry.findOrCreateReplicaEntry(ValueUtil.randomString(), A.class, ValueUtil.randomInt()));

        final Subscription subscription1 =
                Subscription.create(null, new DatasetAddress(1, 0, 1), null, SubscriptionMode.EXPLICIT);

        replicaEntry.subscriptions().put(subscription1.datasetAddress(), subscription1);
        safeAction(() -> assertEquals(replicaEntry.getSubscriptions().size(), 1));

        final IllegalStateException exception = expectThrows(
                IllegalStateException.class,
                () -> safeAction(() -> subscription1.delinkReplicaEntryFromSubscription(replicaEntry, true)));
        assertEquals(exception.getMessage(), "Replica type A not present in Subscription at 1.0.1");
    }

    @Test
    public void delinkReplicaEntryFromSubscription_noSuchInstance() {
        final ReplicaRegistry replicaRegistry = Replicant.context().getReplicaRegistry();
        final ReplicaEntry replicaEntry =
                safeAction(() -> replicaRegistry.findOrCreateReplicaEntry("A/123", A.class, 123));
        final ReplicaEntry replicaEntry2 = safeAction(() ->
                replicaRegistry.findOrCreateReplicaEntry(ValueUtil.randomString(), A.class, ValueUtil.randomInt()));

        final Subscription subscription1 =
                Subscription.create(null, new DatasetAddress(1, 0, 1), null, SubscriptionMode.EXPLICIT);

        safeAction(() -> replicaEntry2.linkToSubscription(subscription1));

        replicaEntry.subscriptions().put(subscription1.datasetAddress(), subscription1);
        safeAction(() -> assertEquals(replicaEntry.getSubscriptions().size(), 1));

        final IllegalStateException exception = expectThrows(
                IllegalStateException.class,
                () -> safeAction(() -> subscription1.delinkReplicaEntryFromSubscription(replicaEntry, true)));
        assertEquals(exception.getMessage(), "Replica Entry A/123 not present in Subscription at 1.0.1");
    }

    @SuppressWarnings({"EqualsWithItself", "SelfComparison"})
    @Test
    public void comparable() {
        final DatasetAddress datasetAddress1 = new DatasetAddress(1, 0);
        final DatasetAddress datasetAddress2 = new DatasetAddress(1, 1);

        final Subscription subscription1 = Subscription.create(null, datasetAddress1, null, SubscriptionMode.EXPLICIT);
        final Subscription subscription2 = Subscription.create(null, datasetAddress2, null, SubscriptionMode.EXPLICIT);

        assertEquals(subscription1.compareTo(subscription1), 0);
        assertEquals(subscription1.compareTo(subscription2), -1);
        assertEquals(subscription2.compareTo(subscription1), 1);
        assertEquals(subscription2.compareTo(subscription2), 0);
    }

    @Test
    public void getDataset() {
        final Dataset dataset = new Dataset(
                0,
                ValueUtil.randomString(),
                null,
                Dataset.FilterMode.UNFILTERED,
                null,
                false,
                null,
                false,
                Dataset.Visibility.UNIVERSAL,
                Collections.emptyList());
        createConnector(new SystemSchema(1, ValueUtil.randomString(), new Dataset[] {dataset}, new EntityType[0]));
        final DatasetAddress datasetAddress1 = new DatasetAddress(1, 0);

        final Subscription subscription1 = Subscription.create(null, datasetAddress1, null, SubscriptionMode.EXPLICIT);

        assertEquals(subscription1.getDataset(), dataset);
    }

    @Test
    public void getDatasetRoot() {
        final Dataset dataset = new Dataset(
                0,
                ValueUtil.randomString(),
                A.class,
                Dataset.FilterMode.UNFILTERED,
                null,
                false,
                null,
                false,
                Dataset.Visibility.UNIVERSAL,
                Collections.emptyList());
        createConnector(new SystemSchema(1, ValueUtil.randomString(), new Dataset[] {dataset}, new EntityType[0]));
        final DatasetAddress datasetAddress1 = new DatasetAddress(1, 0, 33);

        final Subscription subscription1 = Subscription.create(null, datasetAddress1, null, SubscriptionMode.EXPLICIT);

        final ReplicaRegistry replicaRegistry = Replicant.context().getReplicaRegistry();
        final ReplicaEntry replicaEntry1 =
                safeAction(() -> replicaRegistry.findOrCreateReplicaEntry("A/33", A.class, 33));
        final A replica = new A();
        safeAction(() -> replicaEntry1.setReplica(replica));

        safeAction(() -> subscription1.linkSubscriptionToReplicaEntry(replicaEntry1));

        safeAction(() -> assertEquals(subscription1.getDatasetRoot(), replica));
    }

    @Test
    public void getDatasetRoot_butReplicaNotPresent() {
        final Dataset dataset = new Dataset(
                0,
                ValueUtil.randomString(),
                A.class,
                Dataset.FilterMode.UNFILTERED,
                null,
                false,
                null,
                false,
                Dataset.Visibility.UNIVERSAL,
                Collections.emptyList());
        createConnector(new SystemSchema(1, ValueUtil.randomString(), new Dataset[] {dataset}, new EntityType[0]));
        final DatasetAddress datasetAddress1 = new DatasetAddress(1, 0, 33);

        final Subscription subscription1 = Subscription.create(null, datasetAddress1, null, SubscriptionMode.EXPLICIT);

        final IllegalStateException exception =
                expectThrows(IllegalStateException.class, () -> safeAction(subscription1::getDatasetRoot));
        assertEquals(
                exception.getMessage(),
                "Replicant-0088: Subscription.getDatasetRoot() invoked for Dataset Address 1.0.33 but Replica"
                        + " is not present.");
    }

    @Test
    public void getDatasetRoot_butDatasetAddressHasNoRootId() {
        final Dataset dataset = new Dataset(
                0,
                ValueUtil.randomString(),
                A.class,
                Dataset.FilterMode.UNFILTERED,
                null,
                false,
                null,
                false,
                Dataset.Visibility.UNIVERSAL,
                Collections.emptyList());
        createConnector(new SystemSchema(1, ValueUtil.randomString(), new Dataset[] {dataset}, new EntityType[0]));
        final DatasetAddress datasetAddress1 = new DatasetAddress(1, 0);

        final Subscription subscription1 = Subscription.create(null, datasetAddress1, null, SubscriptionMode.EXPLICIT);

        final IllegalStateException exception =
                expectThrows(IllegalStateException.class, () -> safeAction(subscription1::getDatasetRoot));
        assertEquals(
                exception.getMessage(),
                "Replicant-0087: Subscription.getDatasetRoot() invoked for Dataset Address 1.0 but the Dataset"
                        + " Address has no Dataset Root ID.");
    }

    @Test
    public void getDatasetRoot_butDatasetIsTypeDataset() {
        final Dataset dataset = new Dataset(
                0,
                ValueUtil.randomString(),
                null,
                Dataset.FilterMode.UNFILTERED,
                null,
                false,
                null,
                false,
                Dataset.Visibility.UNIVERSAL,
                Collections.emptyList());
        createConnector(new SystemSchema(1, ValueUtil.randomString(), new Dataset[] {dataset}, new EntityType[0]));
        final DatasetAddress datasetAddress1 = new DatasetAddress(1, 0, 44);

        final Subscription subscription1 = Subscription.create(null, datasetAddress1, null, SubscriptionMode.EXPLICIT);

        final IllegalStateException exception =
                expectThrows(IllegalStateException.class, () -> safeAction(subscription1::getDatasetRoot));
        assertEquals(
                exception.getMessage(),
                "Replicant-0029: Subscription.getDatasetRoot() invoked for Dataset Address 1.0.44 but the Dataset is"
                        + " not an Instance Dataset.");
    }

    static class A {}
}
