package replicant.server;

import static org.testng.Assert.*;

import java.util.Collections;
import java.util.List;
import javax.json.Json;
import org.jspecify.annotations.NonNull;
import org.testng.annotations.Test;

public class ChangeSetTest {
    @Test
    public void basicOperation() {
        final var id = 17;
        final var typeID = 42;

        final var candidate1 =
                EntityChangeCandidateTestUtil.createEntityChangeCandidate(id, typeID, 0, "r1", "r2", "a1", "a2");
        final var candidate2 =
                EntityChangeCandidateTestUtil.createEntityChangeCandidate(id, typeID, 0, "r1", "r3", "aZ", "a2");
        final var candidate3 = EntityChangeCandidateTestUtil.createEntityChangeCandidate(18, 42, 0, "X", "X", "X", "X");

        final var change1 = new EntityChange(candidate1);
        final var change2 = new EntityChange(candidate2);
        final var change3 = new EntityChange(candidate3);

        change1.getDatasetAddresses().add(DatasetAddress.of(1, 1));
        change2.getDatasetAddresses().add(DatasetAddress.of(2, 3));
        change3.getDatasetAddresses().add(DatasetAddress.of(3, 42));

        final var changeSet = new ChangeSet();

        assertEquals(changeSet.getEntityChanges().size(), 0);

        changeSet.merge(Collections.singletonList(change1));

        assertEquals(changeSet.getEntityChanges().size(), 1);
        assertEquals(change1.getDatasetAddresses().size(), 1);

        changeSet.merge(change2);

        assertEquals(changeSet.getEntityChanges().size(), 1);
        assertEquals(change1.getDatasetAddresses().size(), 2);

        // Re-merge same
        changeSet.merge(change2);

        assertEquals(changeSet.getEntityChanges().size(), 1);
        assertEquals(change1.getDatasetAddresses().size(), 2);

        changeSet.merge(change3);

        assertEquals(changeSet.getEntityChanges().size(), 2);
    }

    @Test
    public void subscriptionChanges() {
        final var changeSet = new ChangeSet();

        assertEquals(changeSet.getSubscriptionChanges().size(), 0);

        final var filterParameter =
                Json.createBuilderFactory(null).createObjectBuilder().build();
        changeSet.mergeSubscriptionChange(
                SubscriptionChange.of(DatasetAddress.of(1, 2), SubscriptionChange.Type.SUBSCRIBE, filterParameter));

        assertEquals(changeSet.getSubscriptionChanges().size(), 1);

        final var subscriptionChange = changeSet.getSubscriptionChanges().get(0);
        assertEquals(subscriptionChange.datasetAddress().datasetId(), 1);
        assertEquals(subscriptionChange.datasetAddress().datasetRootId(), (Integer) 2);
        assertEquals(subscriptionChange.type(), SubscriptionChange.Type.SUBSCRIBE);
        assertEquals(subscriptionChange.filterParameter(), filterParameter);
    }

    @Test
    public void mergeSubscriptionChange_basic() {
        final var changeSet = new ChangeSet();

        assertEquals(changeSet.getSubscriptionChanges().size(), 0);

        final var filterParameter = Json.createObjectBuilder().add("k", "v").build();
        changeSet.mergeSubscriptionChange(DatasetAddress.of(1, 2), SubscriptionChange.Type.SUBSCRIBE, filterParameter);

        assertEquals(changeSet.getSubscriptionChanges().size(), 1);

        final var subscriptionChange = changeSet.getSubscriptionChanges().get(0);
        assertEquals(subscriptionChange.datasetAddress().datasetId(), 1);
        assertEquals(subscriptionChange.datasetAddress().datasetRootId(), (Integer) 2);
        assertEquals(subscriptionChange.type(), SubscriptionChange.Type.SUBSCRIBE);

        assertEquals(subscriptionChange.filterParameter(), filterParameter);
    }

    @Test
    public void merge_with_Copy() {
        final var id = 17;
        final var typeID = 42;

        final var candidate1 =
                EntityChangeCandidateTestUtil.createEntityChangeCandidate(id, typeID, 0, "r1", "r2", "a1", "a2");
        final var change1 = new EntityChange(candidate1);

        final var changeSet = new ChangeSet();

        assertEquals(changeSet.getEntityChanges().size(), 0);

        changeSet.merge(change1, true);

        final var changes = changeSet.getEntityChanges();
        assertEquals(changes.size(), 1);
        final var change = changes.iterator().next();
        assertEquals(change.getEntityChangeCandidate().getEntityId(), id);
        assertNotSame(change, change1);
    }

    @Test
    public void fullMerge() {
        final var changeSet = new ChangeSet();

        final var id = 17;
        final var typeID = 42;

        final var candidate1 =
                EntityChangeCandidateTestUtil.createEntityChangeCandidate(id, typeID, 0, "r1", "r2", "a1", "a2");
        final var change1 = new EntityChange(candidate1);
        changeSet.merge(change1);

        final var filterParameter =
                Json.createBuilderFactory(null).createObjectBuilder().build();
        changeSet.mergeSubscriptionChange(
                SubscriptionChange.of(DatasetAddress.of(1, 2), SubscriptionChange.Type.SUBSCRIBE, filterParameter));

        final var changeSet2 = new ChangeSet();
        changeSet2.merge(changeSet);

        final var changes = changeSet2.getEntityChanges();
        assertEquals(changes.size(), 1);
        final var change = changes.iterator().next();
        assertEquals(change.getEntityChangeCandidate().getEntityId(), id);
        assertNotSame(change, change1);

        final var subscriptionChanges = changeSet2.getSubscriptionChanges();
        assertEquals(subscriptionChanges.size(), 1);

        final var subscriptionChange = subscriptionChanges.get(0);
        assertEquals(subscriptionChange.datasetAddress().datasetId(), 1);
        assertEquals(subscriptionChange.datasetAddress().datasetRootId(), (Integer) 2);
        assertEquals(subscriptionChange.type(), SubscriptionChange.Type.SUBSCRIBE);
        assertEquals(subscriptionChange.filterParameter(), filterParameter);
    }

    @Test
    public void mergeEntityChangeCandidateSet() {
        final var changeSet = new ChangeSet();

        final var id = 17;
        final var typeID = 42;

        final var candidate1 =
                EntityChangeCandidateTestUtil.createEntityChangeCandidate(id, typeID, 0, "r1", "r2", "a1", "a2");
        final var candidateSet = new EntityChangeCandidateSet();
        candidateSet.merge(candidate1);

        final var datasetAddress = DatasetAddress.of(1);
        changeSet.merge(List.of(new EntityChange(candidate1, datasetAddress)));

        final var changes = changeSet.getEntityChanges();
        assertEquals(changes.size(), 1);
        final var change = changes.iterator().next();
        assertEquals(change.getEntityChangeCandidate().getEntityId(), id);
        assertEquals(change.getDatasetAddresses().size(), 1);
        assertTrue(change.getDatasetAddresses().contains(datasetAddress));
    }

    @Test
    public void mergeSubscriptionChangeDatasetAddressInvalidation() {
        final var changeSet = new ChangeSet();

        assertEquals(changeSet.getSubscriptionChanges().size(), 0);

        final var datasetAddress1 = DatasetAddress.of(1, 2);
        final var datasetAddress2 = DatasetAddress.of(1, 3);
        final var datasetAddress3 = DatasetAddress.of(1, 4);

        final var filterParameter = Json.createObjectBuilder().add("k", "v").build();

        changeSet.mergeSubscriptionChange(datasetAddress1, SubscriptionChange.Type.SUBSCRIBE, filterParameter);
        changeSet.mergeSubscriptionChange(datasetAddress2, SubscriptionChange.Type.UNSUBSCRIBE);
        changeSet.mergeSubscriptionChange(datasetAddress3, SubscriptionChange.Type.UPDATE, filterParameter);

        assertEquals(changeSet.getSubscriptionChanges().size(), 3);

        assertSubscriptionChange(changeSet, SubscriptionChange.Type.SUBSCRIBE, datasetAddress1);
        assertSubscriptionChange(changeSet, SubscriptionChange.Type.UNSUBSCRIBE, datasetAddress2);
        assertSubscriptionChange(changeSet, SubscriptionChange.Type.UPDATE, datasetAddress3);

        changeSet.mergeSubscriptionChange(datasetAddress3, SubscriptionChange.Type.INVALIDATE_DATASET_ADDRESS);

        assertEquals(changeSet.getSubscriptionChanges().size(), 3);

        assertSubscriptionChange(changeSet, SubscriptionChange.Type.SUBSCRIBE, datasetAddress1);
        assertSubscriptionChange(changeSet, SubscriptionChange.Type.UNSUBSCRIBE, datasetAddress2);
        assertSubscriptionChange(changeSet, SubscriptionChange.Type.INVALIDATE_DATASET_ADDRESS, datasetAddress3);

        changeSet.mergeSubscriptionChange(datasetAddress2, SubscriptionChange.Type.INVALIDATE_DATASET_ADDRESS);

        assertEquals(changeSet.getSubscriptionChanges().size(), 3);

        assertSubscriptionChange(changeSet, SubscriptionChange.Type.SUBSCRIBE, datasetAddress1);
        assertSubscriptionChange(changeSet, SubscriptionChange.Type.INVALIDATE_DATASET_ADDRESS, datasetAddress2);
        assertSubscriptionChange(changeSet, SubscriptionChange.Type.INVALIDATE_DATASET_ADDRESS, datasetAddress3);

        changeSet.mergeSubscriptionChange(datasetAddress1, SubscriptionChange.Type.INVALIDATE_DATASET_ADDRESS);

        assertEquals(changeSet.getSubscriptionChanges().size(), 2);

        assertSubscriptionChange(changeSet, SubscriptionChange.Type.INVALIDATE_DATASET_ADDRESS, datasetAddress2);
        assertSubscriptionChange(changeSet, SubscriptionChange.Type.INVALIDATE_DATASET_ADDRESS, datasetAddress3);
    }

    @Test
    public void mergeSubscriptionChange_unfilteredSubscribeAndUnsubscribeCancel() {
        final var datasetAddress = DatasetAddress.of(1, 2);

        final var subscribeThenUnsubscribe = new ChangeSet();
        subscribeThenUnsubscribe.mergeSubscriptionChange(datasetAddress, SubscriptionChange.Type.SUBSCRIBE);
        subscribeThenUnsubscribe.mergeSubscriptionChange(datasetAddress, SubscriptionChange.Type.UNSUBSCRIBE);

        assertTrue(subscribeThenUnsubscribe.getSubscriptionChanges().isEmpty());

        final var unsubscribeThenSubscribe = new ChangeSet();
        unsubscribeThenSubscribe.mergeSubscriptionChange(datasetAddress, SubscriptionChange.Type.UNSUBSCRIBE);
        unsubscribeThenSubscribe.mergeSubscriptionChange(datasetAddress, SubscriptionChange.Type.SUBSCRIBE);

        assertTrue(unsubscribeThenSubscribe.getSubscriptionChanges().isEmpty());
    }

    @Test
    public void mergeSubscriptionChange_parameterFilteredReplacementCollapsesToSubscribe() {
        final var datasetAddress = DatasetAddress.of(1, 2);
        final var filterParameter1 = Json.createObjectBuilder().add("k", "v").build();
        final var filterParameter2 = Json.createObjectBuilder().add("k", "v").build();

        final var subscribeThenUnsubscribe = new ChangeSet();
        subscribeThenUnsubscribe.mergeSubscriptionChange(
                datasetAddress, SubscriptionChange.Type.SUBSCRIBE, filterParameter1);
        subscribeThenUnsubscribe.mergeSubscriptionChange(datasetAddress, SubscriptionChange.Type.UNSUBSCRIBE);

        assertTrue(subscribeThenUnsubscribe.getSubscriptionChanges().isEmpty());

        final var unsubscribeThenSubscribe = new ChangeSet();
        unsubscribeThenSubscribe.mergeSubscriptionChange(datasetAddress, SubscriptionChange.Type.UNSUBSCRIBE);
        unsubscribeThenSubscribe.mergeSubscriptionChange(
                datasetAddress, SubscriptionChange.Type.SUBSCRIBE, filterParameter2);

        assertEquals(
                unsubscribeThenSubscribe.getSubscriptionChanges(),
                List.of(SubscriptionChange.of(datasetAddress, SubscriptionChange.Type.SUBSCRIBE, filterParameter2)));
    }

    @Test
    public void mergeSubscriptionChange_unfilteredUpdateAfterSubscribeIsIgnored() {
        final var datasetAddress = DatasetAddress.of(1, 2);
        final var changeSet = new ChangeSet();

        changeSet.mergeSubscriptionChange(datasetAddress, SubscriptionChange.Type.SUBSCRIBE);
        changeSet.mergeSubscriptionChange(datasetAddress, SubscriptionChange.Type.UPDATE);

        assertEquals(
                changeSet.getSubscriptionChanges(),
                List.of(SubscriptionChange.of(datasetAddress, SubscriptionChange.Type.SUBSCRIBE)));
    }

    @Test
    public void mergeSubscriptionChange_withFilterParameterUpdateAfterSubscribeWithSameFilterIsIgnored() {
        final var datasetAddress = DatasetAddress.of(1, 2);
        final var filterParameter1 = Json.createObjectBuilder().add("k", "v").build();
        final var filterParameter2 = Json.createObjectBuilder().add("k", "v").build();
        final var changeSet = new ChangeSet();

        changeSet.mergeSubscriptionChange(datasetAddress, SubscriptionChange.Type.SUBSCRIBE, filterParameter1);
        changeSet.mergeSubscriptionChange(datasetAddress, SubscriptionChange.Type.UPDATE, filterParameter2);

        assertEquals(
                changeSet.getSubscriptionChanges(),
                List.of(SubscriptionChange.of(datasetAddress, SubscriptionChange.Type.SUBSCRIBE, filterParameter1)));
    }

    @Test
    public void mergeSubscriptionChange_withFilterParameterUpdateAfterSubscribeWithDifferentFilterIsMerged() {
        final var datasetAddress = DatasetAddress.of(1, 2);
        final var filterParameter1 = Json.createObjectBuilder().add("k", "v1").build();
        final var filterParameter2 = Json.createObjectBuilder().add("k", "v2").build();
        final var changeSet = new ChangeSet();

        changeSet.mergeSubscriptionChange(datasetAddress, SubscriptionChange.Type.SUBSCRIBE, filterParameter1);
        changeSet.mergeSubscriptionChange(datasetAddress, SubscriptionChange.Type.UPDATE, filterParameter2);

        assertEquals(
                changeSet.getSubscriptionChanges(),
                List.of(SubscriptionChange.of(datasetAddress, SubscriptionChange.Type.SUBSCRIBE, filterParameter2)));
    }

    private void assertSubscriptionChange(
            @NonNull final ChangeSet changeSet,
            final SubscriptionChange.@NonNull Type type,
            @NonNull final DatasetAddress datasetAddress) {
        assertTrue(changeSet.getSubscriptionChanges().stream()
                .anyMatch(change -> change.datasetAddress().equals(datasetAddress) && change.type() == type));
    }
}
