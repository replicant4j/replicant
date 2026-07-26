package replicant.server;

import static org.testng.Assert.*;

import java.util.Collections;
import java.util.List;
import javax.json.Json;
import org.jspecify.annotations.NonNull;
import org.testng.annotations.Test;
import replicant.server.SubscriptionAction.Action;

public class ChangeSetTest {
    @Test
    public void basicOperation() {
        final var id = 17;
        final var typeID = 42;

        final var message1 = MessageTestUtil.createMessage(id, typeID, 0, "r1", "r2", "a1", "a2");
        final var message2 = MessageTestUtil.createMessage(id, typeID, 0, "r1", "r3", "aZ", "a2");
        final var message3 = MessageTestUtil.createMessage(18, 42, 0, "X", "X", "X", "X");

        final var change1 = new Change(message1);
        final var change2 = new Change(message2);
        final var change3 = new Change(message3);

        change1.getDatasetAddresses().add(DatasetAddress.of(1, 1));
        change2.getDatasetAddresses().add(DatasetAddress.of(2, 3));
        change3.getDatasetAddresses().add(DatasetAddress.of(3, 42));

        final var changeSet = new ChangeSet();

        assertEquals(changeSet.getChanges().size(), 0);

        changeSet.merge(Collections.singletonList(change1));

        assertEquals(changeSet.getChanges().size(), 1);
        assertEquals(change1.getDatasetAddresses().size(), 1);

        changeSet.merge(change2);

        assertEquals(changeSet.getChanges().size(), 1);
        assertEquals(change1.getDatasetAddresses().size(), 2);

        // Re-merge same
        changeSet.merge(change2);

        assertEquals(changeSet.getChanges().size(), 1);
        assertEquals(change1.getDatasetAddresses().size(), 2);

        changeSet.merge(change3);

        assertEquals(changeSet.getChanges().size(), 2);
    }

    @Test
    public void actions() {
        final var changeSet = new ChangeSet();

        assertEquals(changeSet.getSubscriptionActions().size(), 0);

        final var filter = Json.createBuilderFactory(null).createObjectBuilder().build();
        changeSet.mergeSubscriptionAction(SubscriptionAction.of(DatasetAddress.of(1, 2), Action.SUBSCRIBE, filter));

        assertEquals(changeSet.getSubscriptionActions().size(), 1);

        final var action = changeSet.getSubscriptionActions().get(0);
        assertEquals(action.datasetAddress().datasetId(), 1);
        assertEquals(action.datasetAddress().datasetRootId(), (Integer) 2);
        assertEquals(action.action(), Action.SUBSCRIBE);
        assertEquals(action.filter(), filter);
    }

    @Test
    public void mergeSubscriptionAction_basic() {
        final var changeSet = new ChangeSet();

        assertEquals(changeSet.getSubscriptionActions().size(), 0);

        final var myFilter = Json.createObjectBuilder().add("k", "v").build();
        changeSet.mergeSubscriptionAction(DatasetAddress.of(1, 2), Action.SUBSCRIBE, myFilter);

        assertEquals(changeSet.getSubscriptionActions().size(), 1);

        final var action = changeSet.getSubscriptionActions().get(0);
        assertEquals(action.datasetAddress().datasetId(), 1);
        assertEquals(action.datasetAddress().datasetRootId(), (Integer) 2);
        assertEquals(action.action(), Action.SUBSCRIBE);

        assertEquals(action.filter(), myFilter);
    }

    @Test
    public void merge_with_Copy() {
        final var id = 17;
        final var typeID = 42;

        final var message1 = MessageTestUtil.createMessage(id, typeID, 0, "r1", "r2", "a1", "a2");
        final var change1 = new Change(message1);

        final var changeSet = new ChangeSet();

        assertEquals(changeSet.getChanges().size(), 0);

        changeSet.merge(change1, true);

        final var changes = changeSet.getChanges();
        assertEquals(changes.size(), 1);
        final var change = changes.iterator().next();
        assertEquals(change.getEntityMessage().getId(), id);
        assertNotSame(change, change1);
    }

    @Test
    public void fullMerge() {
        final var changeSet = new ChangeSet();

        final var id = 17;
        final var typeID = 42;

        final var message1 = MessageTestUtil.createMessage(id, typeID, 0, "r1", "r2", "a1", "a2");
        final var change1 = new Change(message1);
        changeSet.merge(change1);

        final var filter = Json.createBuilderFactory(null).createObjectBuilder().build();
        changeSet.mergeSubscriptionAction(SubscriptionAction.of(DatasetAddress.of(1, 2), Action.SUBSCRIBE, filter));

        final var changeSet2 = new ChangeSet();
        changeSet2.merge(changeSet);

        final var changes = changeSet2.getChanges();
        assertEquals(changes.size(), 1);
        final var change = changes.iterator().next();
        assertEquals(change.getEntityMessage().getId(), id);
        assertNotSame(change, change1);

        final var actions = changeSet2.getSubscriptionActions();
        assertEquals(actions.size(), 1);

        final var action = actions.get(0);
        assertEquals(action.datasetAddress().datasetId(), 1);
        assertEquals(action.datasetAddress().datasetRootId(), (Integer) 2);
        assertEquals(action.action(), Action.SUBSCRIBE);
        assertEquals(action.filter(), filter);
    }

    @Test
    public void merge_copiesETag() {
        final var eTag = "etag-1";
        final var source = new ChangeSet();
        source.setETag(eTag);

        final var copyTarget = new ChangeSet();
        assertNull(copyTarget.getETag());
        copyTarget.merge(source);
        assertEquals(copyTarget.getETag(), eTag);
    }

    @Test
    public void mergeEntityMessageSet() {
        final var changeSet = new ChangeSet();

        final var id = 17;
        final var typeID = 42;

        final var message1 = MessageTestUtil.createMessage(id, typeID, 0, "r1", "r2", "a1", "a2");
        final var messageSet = new EntityMessageSet();
        messageSet.merge(message1);

        final var datasetAddress = DatasetAddress.of(1);
        changeSet.merge(List.of(new Change(message1, datasetAddress)));

        final var changes = changeSet.getChanges();
        assertEquals(changes.size(), 1);
        final var change = changes.iterator().next();
        assertEquals(change.getEntityMessage().getId(), id);
        assertEquals(change.getDatasetAddresses().size(), 1);
        assertTrue(change.getDatasetAddresses().contains(datasetAddress));
    }

    @Test
    public void mergeSubscriptionActionDelete() {
        final var changeSet = new ChangeSet();

        assertEquals(changeSet.getSubscriptionActions().size(), 0);

        final var datasetAddress1 = DatasetAddress.of(1, 2);
        final var datasetAddress2 = DatasetAddress.of(1, 3);
        final var datasetAddress3 = DatasetAddress.of(1, 4);

        final var filter = Json.createObjectBuilder().add("k", "v").build();

        changeSet.mergeSubscriptionAction(datasetAddress1, Action.SUBSCRIBE, filter);
        changeSet.mergeSubscriptionAction(datasetAddress2, Action.UNSUBSCRIBE);
        changeSet.mergeSubscriptionAction(datasetAddress3, Action.UPDATE, filter);

        assertEquals(changeSet.getSubscriptionActions().size(), 3);

        assertAction(changeSet, Action.SUBSCRIBE, datasetAddress1);
        assertAction(changeSet, Action.UNSUBSCRIBE, datasetAddress2);
        assertAction(changeSet, Action.UPDATE, datasetAddress3);

        changeSet.mergeSubscriptionAction(datasetAddress3, Action.DELETE);

        assertEquals(changeSet.getSubscriptionActions().size(), 3);

        assertAction(changeSet, Action.SUBSCRIBE, datasetAddress1);
        assertAction(changeSet, Action.UNSUBSCRIBE, datasetAddress2);
        assertAction(changeSet, Action.DELETE, datasetAddress3);

        changeSet.mergeSubscriptionAction(datasetAddress2, Action.DELETE);

        assertEquals(changeSet.getSubscriptionActions().size(), 3);

        assertAction(changeSet, Action.SUBSCRIBE, datasetAddress1);
        assertAction(changeSet, Action.DELETE, datasetAddress2);
        assertAction(changeSet, Action.DELETE, datasetAddress3);

        changeSet.mergeSubscriptionAction(datasetAddress1, Action.DELETE);

        assertEquals(changeSet.getSubscriptionActions().size(), 2);

        assertAction(changeSet, Action.DELETE, datasetAddress2);
        assertAction(changeSet, Action.DELETE, datasetAddress3);
    }

    @Test
    public void mergeSubscriptionAction_unfilteredSubscribeAndUnsubscribeCancel() {
        final var datasetAddress = DatasetAddress.of(1, 2);

        final var subscribeThenUnsubscribe = new ChangeSet();
        subscribeThenUnsubscribe.mergeSubscriptionAction(datasetAddress, Action.SUBSCRIBE);
        subscribeThenUnsubscribe.mergeSubscriptionAction(datasetAddress, Action.UNSUBSCRIBE);

        assertTrue(subscribeThenUnsubscribe.getSubscriptionActions().isEmpty());

        final var unsubscribeThenSubscribe = new ChangeSet();
        unsubscribeThenSubscribe.mergeSubscriptionAction(datasetAddress, Action.UNSUBSCRIBE);
        unsubscribeThenSubscribe.mergeSubscriptionAction(datasetAddress, Action.SUBSCRIBE);

        assertTrue(unsubscribeThenSubscribe.getSubscriptionActions().isEmpty());
    }

    @Test
    public void mergeSubscriptionAction_filteredSubscribeAndUnsubscribeWithSameFilterCancel() {
        final var datasetAddress = DatasetAddress.of(1, 2);
        final var filter1 = Json.createObjectBuilder().add("k", "v").build();
        final var filter2 = Json.createObjectBuilder().add("k", "v").build();

        final var subscribeThenUnsubscribe = new ChangeSet();
        subscribeThenUnsubscribe.mergeSubscriptionAction(datasetAddress, Action.SUBSCRIBE, filter1);
        subscribeThenUnsubscribe.mergeSubscriptionAction(datasetAddress, Action.UNSUBSCRIBE);

        assertTrue(subscribeThenUnsubscribe.getSubscriptionActions().isEmpty());

        final var unsubscribeThenSubscribe = new ChangeSet();
        unsubscribeThenSubscribe.mergeSubscriptionAction(datasetAddress, Action.UNSUBSCRIBE);
        unsubscribeThenSubscribe.mergeSubscriptionAction(datasetAddress, Action.SUBSCRIBE, filter2);

        assertTrue(unsubscribeThenSubscribe.getSubscriptionActions().isEmpty());
    }

    @Test
    public void mergeSubscriptionAction_unfilteredUpdateAfterSubscribeIsIgnored() {
        final var datasetAddress = DatasetAddress.of(1, 2);
        final var changeSet = new ChangeSet();

        changeSet.mergeSubscriptionAction(datasetAddress, Action.SUBSCRIBE);
        changeSet.mergeSubscriptionAction(datasetAddress, Action.UPDATE);

        assertEquals(
                changeSet.getSubscriptionActions(), List.of(SubscriptionAction.of(datasetAddress, Action.SUBSCRIBE)));
    }

    @Test
    public void mergeSubscriptionAction_filteredUpdateAfterSubscribeWithSameFilterIsIgnored() {
        final var datasetAddress = DatasetAddress.of(1, 2);
        final var filter1 = Json.createObjectBuilder().add("k", "v").build();
        final var filter2 = Json.createObjectBuilder().add("k", "v").build();
        final var changeSet = new ChangeSet();

        changeSet.mergeSubscriptionAction(datasetAddress, Action.SUBSCRIBE, filter1);
        changeSet.mergeSubscriptionAction(datasetAddress, Action.UPDATE, filter2);

        assertEquals(
                changeSet.getSubscriptionActions(),
                List.of(SubscriptionAction.of(datasetAddress, Action.SUBSCRIBE, filter1)));
    }

    @Test
    public void mergeSubscriptionAction_filteredUpdateAfterSubscribeWithDifferentFilterIsMerged() {
        final var datasetAddress = DatasetAddress.of(1, 2);
        final var filter1 = Json.createObjectBuilder().add("k", "v1").build();
        final var filter2 = Json.createObjectBuilder().add("k", "v2").build();
        final var changeSet = new ChangeSet();

        changeSet.mergeSubscriptionAction(datasetAddress, Action.SUBSCRIBE, filter1);
        changeSet.mergeSubscriptionAction(datasetAddress, Action.UPDATE, filter2);

        assertEquals(
                changeSet.getSubscriptionActions(),
                List.of(SubscriptionAction.of(datasetAddress, Action.SUBSCRIBE, filter2)));
    }

    private void assertAction(
            @NonNull final ChangeSet changeSet,
            @NonNull final Action action,
            @NonNull final DatasetAddress datasetAddress) {
        assertTrue(changeSet.getSubscriptionActions().stream()
                .anyMatch(a -> a.datasetAddress().equals(datasetAddress) && a.action() == action));
    }
}
