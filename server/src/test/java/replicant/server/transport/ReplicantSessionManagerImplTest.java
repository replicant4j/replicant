package replicant.server.transport;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.json.Json;
import javax.json.JsonObject;
import javax.websocket.RemoteEndpoint;
import javax.websocket.Session;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import replicant.server.Change;
import replicant.server.ChangeSet;
import replicant.server.DatasetAddress;
import replicant.server.EntityMessage;
import replicant.server.ServerConstants;
import replicant.server.SubscriptionAction;
import replicant.server.SubscriptionDependency;
import replicant.server.ee.RegistryUtil;
import replicant.server.runtime.EntityMessageCacheUtil;
import replicant.server.runtime.TransactionSynchronizationRegistryUtil;

public class ReplicantSessionManagerImplTest {
    @BeforeMethod
    public void setup() {
        RegistryUtil.bind();
    }

    @AfterMethod
    public void clearContext() {
        RegistryUtil.unbind();
    }

    @Test
    public void sendChangeMessage_invalidAuthorizationDoesNotProcessPacket() throws Exception {
        final var authorization = mock(ReplicantSessionAuthorization.class);
        when(authorization.runIfValid(any())).thenReturn(false);
        final var session = new ReplicantSession(mock(Session.class), authorization);
        final var manager = new ReplicantSessionManagerImpl();
        final var packet = new Packet(false, null, null, null, List.of(), new ChangeSet());

        assertFalse(manager.sendChangeMessage(session, packet));

        verify(authorization).runIfValid(any());
    }

    @Test
    public void sendChangeMessage_staticKeyedLinkFollow_usesTargetDatasetKey() {
        final var sourceDataset = new DatasetMetadata(
                0, "Source", null, DatasetMetadata.FilterType.NONE, false, DatasetMetadata.CacheType.NONE, true);
        final var targetDataset = new DatasetMetadata(
                1, "Target", 1, DatasetMetadata.FilterType.STATIC, true, DatasetMetadata.CacheType.NONE, true);
        final var schema = new SchemaMetaData("Test", sourceDataset, targetDataset);

        final var context = new TestSessionContext(schema);
        final var manager = new ReplicantSessionManagerImpl();
        setField(manager, "_context", context);
        setField(manager, "_broker", mock(ReplicantMessageBroker.class));
        setField(manager, "_registry", TransactionSynchronizationRegistryUtil.lookup());

        final var webSocketSession = mock(Session.class);
        final var remote = mock(RemoteEndpoint.Basic.class);
        when(webSocketSession.getId()).thenReturn("session-1");
        when(webSocketSession.isOpen()).thenReturn(true);
        when(webSocketSession.getBasicRemote()).thenReturn(remote);

        final var session = new ReplicantSession(webSocketSession);

        final var sourceDatasetAddress = DatasetAddress.of(0);
        final var targetDatasetAddress = DatasetAddress.of(1, 7, "fi-7");
        final var subscriptionDependency =
                new SubscriptionDependency(sourceDatasetAddress, targetDatasetAddress, null, true);

        final var routingKeys = new HashMap<String, Serializable>();
        routingKeys.put("Source", "present");
        final var attributes = new HashMap<String, Serializable>();
        attributes.put("ID", 1);
        final var message = new EntityMessage(1, 1, 0L, routingKeys, attributes, Set.of(subscriptionDependency));

        final var newFilter = Json.createObjectBuilder().add("k", "v").build();
        final var packet = new Packet(false, null, null, null, List.of(message), new ChangeSet());

        session.getLock().lock();
        try {
            final var sourceEntry = session.createSubscriptionEntry(sourceDatasetAddress);
            final var originalFilter =
                    Json.createObjectBuilder().add("old", "value").build();
            sourceEntry.setFilter(originalFilter);

            manager.sendChangeMessage(session, packet);

            assertEquals(context.getPreSendChangeMessages(), List.of(packet));
            final var targetEntry = Objects.requireNonNull(session.findSubscriptionEntry(targetDatasetAddress));
            assertEquals(targetEntry.getFilter(), newFilter);
            assertTrue(sourceEntry.getOutwardSubscriptionDependencies().contains(targetDatasetAddress));
            assertTrue(targetEntry.getInwardSubscriptionDependencies().contains(sourceDatasetAddress));
        } finally {
            session.getLock().unlock();
        }

        final var collectCalls = context.getBulkCollectCalls();
        assertEquals(collectCalls.size(), 1);
        final var call = collectCalls.get(0);
        assertEquals(call.datasetAddresses(), List.of(targetDatasetAddress));
        assertEquals(call.filter(), newFilter);
        assertFalse(call.isExplicitSubscribe());
    }

    @Test
    public void sendChangeMessage_deleteRemovesOnlyDeletedEntityOwnershipForSharedTarget() {
        final var sourceDataset = new DatasetMetadata(
                0, "Source", 1, DatasetMetadata.FilterType.NONE, false, DatasetMetadata.CacheType.NONE, true);
        final var targetDataset = new DatasetMetadata(
                1, "Target", null, DatasetMetadata.FilterType.NONE, false, DatasetMetadata.CacheType.NONE, true);
        final var schema = new SchemaMetaData("Test", sourceDataset, targetDataset);
        final var context = createManagerContext(schema);
        final var manager = createManager(context, mock(ReplicantMessageBroker.class));

        final var webSocketSession = mock(Session.class);
        final var remote = mock(RemoteEndpoint.Basic.class);
        when(webSocketSession.getId()).thenReturn("session-1");
        when(webSocketSession.isOpen()).thenReturn(true);
        when(webSocketSession.getBasicRemote()).thenReturn(remote);
        final var session = new ReplicantSession(webSocketSession);

        final var sourceDatasetAddress = DatasetAddress.of(0, 10);
        final var targetDatasetAddress = DatasetAddress.of(1);
        final var subscriptionDependency = new SubscriptionDependency(sourceDatasetAddress, targetDatasetAddress);
        final var routingKeys = new HashMap<String, Serializable>();
        routingKeys.put("Source", new ArrayList<>(List.of(10)));
        final var attributesA = new HashMap<String, Serializable>();
        final var attributesB = new HashMap<String, Serializable>();
        attributesA.put("ID", 100);
        attributesB.put("ID", 101);

        final var updateA = new EntityMessage(100, 2, 0L, routingKeys, attributesA, Set.of(subscriptionDependency));
        final var updateB = new EntityMessage(101, 2, 0L, routingKeys, attributesB, Set.of(subscriptionDependency));
        final var deleteA = new EntityMessage(100, 2, 1L, routingKeys, null, null);

        session.getLock().lock();
        try {
            final var sourceEntry = session.createSubscriptionEntry(sourceDatasetAddress);
            sourceEntry.setExplicitlySubscribed(true);

            manager.sendChangeMessage(
                    session, new Packet(false, null, null, null, List.of(updateA, updateB), new ChangeSet()));

            assertNotNull(session.findSubscriptionEntry(targetDatasetAddress));
            assertEquals(
                    sourceEntry.getOwnedOutwardSubscriptionDependencies(SubscriptionDependencyOwner.entity(2, 100)),
                    Set.of(targetDatasetAddress));
            assertEquals(
                    sourceEntry.getOwnedOutwardSubscriptionDependencies(SubscriptionDependencyOwner.entity(2, 101)),
                    Set.of(targetDatasetAddress));

            manager.sendChangeMessage(session, new Packet(false, null, null, null, List.of(deleteA), new ChangeSet()));

            final var targetEntry = Objects.requireNonNull(session.findSubscriptionEntry(targetDatasetAddress));
            assertTrue(sourceEntry.getOutwardSubscriptionDependencies().contains(targetDatasetAddress));
            assertTrue(targetEntry.getInwardSubscriptionDependencies().contains(sourceDatasetAddress));
            assertTrue(sourceEntry
                    .getOwnedOutwardSubscriptionDependencies(SubscriptionDependencyOwner.entity(2, 100))
                    .isEmpty());
            assertEquals(
                    sourceEntry.getOwnedOutwardSubscriptionDependencies(SubscriptionDependencyOwner.entity(2, 101)),
                    Set.of(targetDatasetAddress));
        } finally {
            session.getLock().unlock();
        }
    }

    @Test
    public void sendChangeMessage_sameTargetReplacementFromPacketMessage_preservesWithoutTargetReload() {
        final var sourceDataset = new DatasetMetadata(
                0, "Source", 1, DatasetMetadata.FilterType.NONE, false, DatasetMetadata.CacheType.NONE, true);
        final var targetDataset = new DatasetMetadata(
                1, "Target", null, DatasetMetadata.FilterType.NONE, false, DatasetMetadata.CacheType.NONE, true);
        final var schema = new SchemaMetaData("Test", sourceDataset, targetDataset);
        final var context = createManagerContext(schema);
        final var manager = createManager(context, mock(ReplicantMessageBroker.class));
        final var session = createOpenSession();

        final var sourceDatasetAddress = DatasetAddress.of(0, 10);
        final var targetDatasetAddress = DatasetAddress.of(1);
        final var oldOwner = SubscriptionDependencyOwner.entity(2, 100);
        final var newOwner = SubscriptionDependencyOwner.entity(2, 101);
        final var subscriptionDependency = new SubscriptionDependency(sourceDatasetAddress, targetDatasetAddress);
        final var updateNew = new EntityMessage(
                101, 2, 0L, instanceRouting("Source", 10), attributes(101), Set.of(subscriptionDependency));
        final var deleteOld = new EntityMessage(100, 2, 1L, instanceRouting("Source", 10), null, null);

        session.getLock().lock();
        try {
            final var sourceEntry = session.createSubscriptionEntry(sourceDatasetAddress);
            sourceEntry.setExplicitlySubscribed(true);
            session.createSubscriptionEntry(targetDatasetAddress);
            session.recordEntityScopedSubscriptionDependency(sourceDatasetAddress, targetDatasetAddress, 2, 100);

            manager.sendChangeMessage(
                    session, new Packet(false, null, null, null, List.of(deleteOld, updateNew), new ChangeSet()));

            final var targetEntry = Objects.requireNonNull(session.findSubscriptionEntry(targetDatasetAddress));
            assertTrue(sourceEntry
                    .getOwnedOutwardSubscriptionDependencies(oldOwner)
                    .isEmpty());
            assertEquals(sourceEntry.getOwnedOutwardSubscriptionDependencies(newOwner), Set.of(targetDatasetAddress));
            assertTrue(sourceEntry.getOutwardSubscriptionDependencies().contains(targetDatasetAddress));
            assertTrue(targetEntry.getInwardSubscriptionDependencies().contains(sourceDatasetAddress));
        } finally {
            session.getLock().unlock();
        }

        assertTrue(context.getBulkCollectCalls().isEmpty());
    }

    @Test
    public void sendChangeMessage_sameTargetReplacementFromChangeSet_preservesWithoutTargetReload() {
        final var sourceDataset = new DatasetMetadata(
                0, "Source", 1, DatasetMetadata.FilterType.NONE, false, DatasetMetadata.CacheType.NONE, true);
        final var targetDataset = new DatasetMetadata(
                1, "Target", null, DatasetMetadata.FilterType.NONE, false, DatasetMetadata.CacheType.NONE, true);
        final var schema = new SchemaMetaData("Test", sourceDataset, targetDataset);
        final var context = createManagerContext(schema);
        final var manager = createManager(context, mock(ReplicantMessageBroker.class));
        final var session = createOpenSession();

        final var sourceDatasetAddress = DatasetAddress.of(0, 10);
        final var targetDatasetAddress = DatasetAddress.of(1);
        final var oldOwner = SubscriptionDependencyOwner.entity(2, 100);
        final var newOwner = SubscriptionDependencyOwner.entity(2, 101);
        final var subscriptionDependency = new SubscriptionDependency(sourceDatasetAddress, targetDatasetAddress);
        final var updateNew = new EntityMessage(
                101, 2, 0L, instanceRouting("Source", 10), attributes(101), Set.of(subscriptionDependency));
        final var deleteOld = new EntityMessage(100, 2, 1L, instanceRouting("Source", 10), null, null);
        final var changeSet = new ChangeSet();
        changeSet.merge(new Change(updateNew, sourceDatasetAddress));

        session.getLock().lock();
        try {
            final var sourceEntry = session.createSubscriptionEntry(sourceDatasetAddress);
            sourceEntry.setExplicitlySubscribed(true);
            session.createSubscriptionEntry(targetDatasetAddress);
            session.recordEntityScopedSubscriptionDependency(sourceDatasetAddress, targetDatasetAddress, 2, 100);

            manager.sendChangeMessage(session, new Packet(false, null, null, null, List.of(deleteOld), changeSet));

            final var targetEntry = Objects.requireNonNull(session.findSubscriptionEntry(targetDatasetAddress));
            assertTrue(sourceEntry
                    .getOwnedOutwardSubscriptionDependencies(oldOwner)
                    .isEmpty());
            assertEquals(sourceEntry.getOwnedOutwardSubscriptionDependencies(newOwner), Set.of(targetDatasetAddress));
            assertTrue(targetEntry.getInwardSubscriptionDependencies().contains(sourceDatasetAddress));
        } finally {
            session.getLock().unlock();
        }

        assertTrue(context.getBulkCollectCalls().isEmpty());
    }

    @Test
    public void sendChangeMessage_newTargetReplacement_isCollectedByNormalExpansion() {
        final var sourceDataset = new DatasetMetadata(
                0, "Source", 1, DatasetMetadata.FilterType.NONE, false, DatasetMetadata.CacheType.NONE, true);
        final var targetDataset = new DatasetMetadata(
                1, "Target", 3, DatasetMetadata.FilterType.NONE, false, DatasetMetadata.CacheType.NONE, true);
        final var schema = new SchemaMetaData("Test", sourceDataset, targetDataset);
        final var context = createManagerContext(schema);
        final var manager = createManager(context, mock(ReplicantMessageBroker.class));
        final var session = createOpenSession();

        final var sourceDatasetAddress = DatasetAddress.of(0, 10);
        final var oldTargetDatasetAddress = DatasetAddress.of(1, 20);
        final var newTargetDatasetAddress = DatasetAddress.of(1, 21);
        final var newOwner = SubscriptionDependencyOwner.entity(2, 101);
        final var subscriptionDependency = new SubscriptionDependency(sourceDatasetAddress, newTargetDatasetAddress);
        final var updateNew = new EntityMessage(
                101, 2, 0L, instanceRouting("Source", 10), attributes(101), Set.of(subscriptionDependency));
        final var deleteOld = new EntityMessage(100, 2, 1L, instanceRouting("Source", 10), null, null);

        session.getLock().lock();
        try {
            final var sourceEntry = session.createSubscriptionEntry(sourceDatasetAddress);
            sourceEntry.setExplicitlySubscribed(true);
            session.createSubscriptionEntry(oldTargetDatasetAddress);
            session.recordEntityScopedSubscriptionDependency(sourceDatasetAddress, oldTargetDatasetAddress, 2, 100);

            manager.sendChangeMessage(
                    session, new Packet(false, null, null, null, List.of(deleteOld, updateNew), new ChangeSet()));

            assertNull(session.findSubscriptionEntry(oldTargetDatasetAddress));
            final var newTargetEntry = Objects.requireNonNull(session.findSubscriptionEntry(newTargetDatasetAddress));
            assertEquals(
                    sourceEntry.getOwnedOutwardSubscriptionDependencies(newOwner), Set.of(newTargetDatasetAddress));
            assertTrue(newTargetEntry.getInwardSubscriptionDependencies().contains(sourceDatasetAddress));
        } finally {
            session.getLock().unlock();
        }

        final var collectCalls = context.getBulkCollectCalls();
        assertEquals(collectCalls.size(), 1);
        assertEquals(collectCalls.get(0).datasetAddresses(), List.of(newTargetDatasetAddress));
        assertNull(collectCalls.get(0).filter());
        assertFalse(collectCalls.get(0).isExplicitSubscribe());
    }

    @Test
    public void sendChangeMessage_filterMismatchReplacement_isCollectedWithNewFilterByNormalExpansion() {
        final var sourceDataset = new DatasetMetadata(
                0, "Source", 1, DatasetMetadata.FilterType.NONE, false, DatasetMetadata.CacheType.NONE, true);
        final var targetDataset = new DatasetMetadata(
                1, "Target", null, DatasetMetadata.FilterType.DYNAMIC, false, DatasetMetadata.CacheType.NONE, true);
        final var schema = new SchemaMetaData("Test", sourceDataset, targetDataset);
        final var context = createManagerContext(schema);
        final var manager = createManager(context, mock(ReplicantMessageBroker.class));
        final var session = createOpenSession();

        final var sourceDatasetAddress = DatasetAddress.of(0, 10);
        final var targetDatasetAddress = DatasetAddress.of(1);
        final var oldFilter = Json.createObjectBuilder().add("filter", "old").build();
        final var newFilter = Json.createObjectBuilder().add("filter", "new").build();
        final var newOwner = SubscriptionDependencyOwner.entity(2, 101);
        final var subscriptionDependency =
                new SubscriptionDependency(sourceDatasetAddress, targetDatasetAddress, newFilter);
        final var updateNew = new EntityMessage(
                101, 2, 0L, instanceRouting("Source", 10), attributes(101), Set.of(subscriptionDependency));
        final var deleteOld = new EntityMessage(100, 2, 1L, instanceRouting("Source", 10), null, null);

        session.getLock().lock();
        try {
            final var sourceEntry = session.createSubscriptionEntry(sourceDatasetAddress);
            sourceEntry.setExplicitlySubscribed(true);
            final var targetEntry = session.createSubscriptionEntry(targetDatasetAddress);
            targetEntry.setFilter(oldFilter);
            session.recordEntityScopedSubscriptionDependency(sourceDatasetAddress, targetDatasetAddress, 2, 100);

            manager.sendChangeMessage(
                    session, new Packet(false, null, null, null, List.of(deleteOld, updateNew), new ChangeSet()));

            final var reloadedTargetEntry = Objects.requireNonNull(session.findSubscriptionEntry(targetDatasetAddress));
            assertEquals(reloadedTargetEntry.getFilter(), newFilter);
            assertEquals(sourceEntry.getOwnedOutwardSubscriptionDependencies(newOwner), Set.of(targetDatasetAddress));
        } finally {
            session.getLock().unlock();
        }

        final var collectCalls = context.getBulkCollectCalls();
        assertEquals(collectCalls.size(), 1);
        assertEquals(collectCalls.get(0).datasetAddresses(), List.of(targetDatasetAddress));
        assertEquals(collectCalls.get(0).filter(), newFilter);
        assertFalse(collectCalls.get(0).isExplicitSubscribe());
    }

    @Test
    public void sendChangeMessage_filteredOutSourceRoute_isNotPreserved() {
        final var sourceDataset = new DatasetMetadata(
                0, "Source", 1, DatasetMetadata.FilterType.STATIC, true, DatasetMetadata.CacheType.NONE, true);
        final var targetDataset = new DatasetMetadata(
                1, "Target", null, DatasetMetadata.FilterType.NONE, false, DatasetMetadata.CacheType.NONE, true);
        final var schema = new SchemaMetaData("Test", sourceDataset, targetDataset);
        final var context = createManagerContext(schema);
        final var manager = createManager(context, mock(ReplicantMessageBroker.class));
        final var session = createOpenSession();

        final var includedSourceDatasetAddress = DatasetAddress.of(0, 10, "included");
        final var excludedSourceDatasetAddress = DatasetAddress.of(0, 10, "excluded");
        final var targetDatasetAddress = DatasetAddress.of(1);
        final var oldOwner = SubscriptionDependencyOwner.entity(2, 100);
        final var newOwner = SubscriptionDependencyOwner.entity(2, 101);
        final var subscriptionDependency =
                new SubscriptionDependency(DatasetAddress.partial(0, 10), targetDatasetAddress, null, true);
        final var updateNew = new EntityMessage(
                101, 2, 0L, instanceRouting("Source", 10), attributes(101), Set.of(subscriptionDependency));
        final var deleteOld = new EntityMessage(100, 2, 1L, instanceRouting("Source", 10), null, null);
        context.excludeFilterEntityMessageDatasetAddress(excludedSourceDatasetAddress);

        session.getLock().lock();
        try {
            final var includedSourceEntry = session.createSubscriptionEntry(includedSourceDatasetAddress);
            final var excludedSourceEntry = session.createSubscriptionEntry(excludedSourceDatasetAddress);
            includedSourceEntry.setExplicitlySubscribed(true);
            excludedSourceEntry.setExplicitlySubscribed(true);
            session.createSubscriptionEntry(targetDatasetAddress);
            session.recordEntityScopedSubscriptionDependency(
                    includedSourceDatasetAddress, targetDatasetAddress, 2, 100);
            session.recordEntityScopedSubscriptionDependency(
                    excludedSourceDatasetAddress, targetDatasetAddress, 2, 100);

            manager.sendChangeMessage(
                    session, new Packet(false, null, null, null, List.of(deleteOld, updateNew), new ChangeSet()));

            assertTrue(includedSourceEntry
                    .getOwnedOutwardSubscriptionDependencies(oldOwner)
                    .isEmpty());
            assertEquals(
                    includedSourceEntry.getOwnedOutwardSubscriptionDependencies(newOwner),
                    Set.of(targetDatasetAddress));
            assertEquals(
                    excludedSourceEntry.getOwnedOutwardSubscriptionDependencies(oldOwner),
                    Set.of(targetDatasetAddress));
            assertTrue(excludedSourceEntry
                    .getOwnedOutwardSubscriptionDependencies(newOwner)
                    .isEmpty());
        } finally {
            session.getLock().unlock();
        }

        assertTrue(context.getBulkCollectCalls().isEmpty());
    }

    @Test
    public void sendChangeMessage_shouldFollowDatasetLinkFalse_isNotPreserved() {
        final var sourceDataset = new DatasetMetadata(
                0, "Source", 1, DatasetMetadata.FilterType.NONE, false, DatasetMetadata.CacheType.NONE, true);
        final var targetDataset = new DatasetMetadata(
                1, "Target", null, DatasetMetadata.FilterType.DYNAMIC, false, DatasetMetadata.CacheType.NONE, true);
        final var schema = new SchemaMetaData("Test", sourceDataset, targetDataset);
        final var context = createManagerContext(schema);
        context.setShouldFollowDatasetLink(false);
        final var manager = createManager(context, mock(ReplicantMessageBroker.class));
        final var session = createOpenSession();

        final var sourceDatasetAddress = DatasetAddress.of(0, 10);
        final var targetDatasetAddress = DatasetAddress.of(1);
        final var targetFilter =
                Json.createObjectBuilder().add("filter", "current").build();
        final var newOwner = SubscriptionDependencyOwner.entity(2, 101);
        final var subscriptionDependency =
                new SubscriptionDependency(sourceDatasetAddress, targetDatasetAddress, targetFilter);
        final var updateNew = new EntityMessage(
                101, 2, 0L, instanceRouting("Source", 10), attributes(101), Set.of(subscriptionDependency));
        final var deleteOld = new EntityMessage(100, 2, 1L, instanceRouting("Source", 10), null, null);

        session.getLock().lock();
        try {
            final var sourceEntry = session.createSubscriptionEntry(sourceDatasetAddress);
            sourceEntry.setExplicitlySubscribed(true);
            final var targetEntry = session.createSubscriptionEntry(targetDatasetAddress);
            targetEntry.setFilter(targetFilter);
            session.recordEntityScopedSubscriptionDependency(sourceDatasetAddress, targetDatasetAddress, 2, 100);

            manager.sendChangeMessage(
                    session, new Packet(false, null, null, null, List.of(deleteOld, updateNew), new ChangeSet()));

            assertNull(session.findSubscriptionEntry(targetDatasetAddress));
            assertTrue(sourceEntry
                    .getOwnedOutwardSubscriptionDependencies(newOwner)
                    .isEmpty());
        } finally {
            session.getLock().unlock();
        }

        assertTrue(context.getBulkCollectCalls().isEmpty());
    }

    @Test
    public void sendChangeMessage_sourceRootDeleteWinsOverPreservation() {
        final var sourceDataset = new DatasetMetadata(
                0, "Source", 1, DatasetMetadata.FilterType.NONE, false, DatasetMetadata.CacheType.NONE, true);
        final var targetDataset = new DatasetMetadata(
                1, "Target", null, DatasetMetadata.FilterType.NONE, false, DatasetMetadata.CacheType.NONE, true);
        final var schema = new SchemaMetaData("Test", sourceDataset, targetDataset);
        final var context = createManagerContext(schema);
        final var manager = createManager(context, mock(ReplicantMessageBroker.class));
        final var session = createOpenSession();

        final var sourceDatasetAddress = DatasetAddress.of(0, 10);
        final var targetDatasetAddress = DatasetAddress.of(1);
        final var subscriptionDependency = new SubscriptionDependency(sourceDatasetAddress, targetDatasetAddress);
        final var updateNew = new EntityMessage(
                101, 2, 0L, instanceRouting("Source", 10), attributes(101), Set.of(subscriptionDependency));
        final var deleteSourceRoot = new EntityMessage(10, 1, 1L, instanceRouting("Source", 10), null, null);

        session.getLock().lock();
        try {
            final var sourceEntry = session.createSubscriptionEntry(sourceDatasetAddress);
            sourceEntry.setExplicitlySubscribed(true);
            session.createSubscriptionEntry(targetDatasetAddress);
            session.recordEntityScopedSubscriptionDependency(sourceDatasetAddress, targetDatasetAddress, 1, 10);

            manager.sendChangeMessage(
                    session,
                    new Packet(false, null, null, null, List.of(deleteSourceRoot, updateNew), new ChangeSet()));

            assertNull(session.findSubscriptionEntry(sourceDatasetAddress));
            assertNull(session.findSubscriptionEntry(targetDatasetAddress));
        } finally {
            session.getLock().unlock();
        }

        assertTrue(context.getBulkCollectCalls().isEmpty());
    }

    @Test
    public void sendChangeMessage_targetRootDeleteWinsOverPreservation() {
        final var sourceDataset = new DatasetMetadata(
                0, "Source", 1, DatasetMetadata.FilterType.STATIC, true, DatasetMetadata.CacheType.NONE, true);
        final var targetDataset = new DatasetMetadata(
                1, "Target", 3, DatasetMetadata.FilterType.NONE, false, DatasetMetadata.CacheType.NONE, true);
        final var schema = new SchemaMetaData("Test", sourceDataset, targetDataset);
        final var context = createManagerContext(schema);
        final var manager = createManager(context, mock(ReplicantMessageBroker.class));
        final var session = createOpenSession();

        final var sourceDatasetAddress = DatasetAddress.of(0, 10, "fi-source");
        final var targetDatasetAddress = DatasetAddress.of(1, 20);
        final var newOwner = SubscriptionDependencyOwner.entity(2, 101);
        final var subscriptionDependency = new SubscriptionDependency(sourceDatasetAddress, targetDatasetAddress);
        final var updateNew = new EntityMessage(
                101, 2, 0L, instanceRouting("Source", 10), attributes(101), Set.of(subscriptionDependency));
        final var deleteOld = new EntityMessage(100, 2, 1L, instanceRouting("Source", 10), null, null);
        final var deleteTargetRoot = new EntityMessage(20, 3, 1L, instanceRouting("Target", 20), null, null);
        session.getLock().lock();
        try {
            final var sourceEntry = session.createSubscriptionEntry(sourceDatasetAddress);
            sourceEntry.setExplicitlySubscribed(true);
            session.createSubscriptionEntry(targetDatasetAddress);
            session.recordEntityScopedSubscriptionDependency(sourceDatasetAddress, targetDatasetAddress, 2, 100);

            manager.sendChangeMessage(
                    session,
                    new Packet(
                            false, null, null, null, List.of(deleteOld, deleteTargetRoot, updateNew), new ChangeSet()));

            assertNull(session.findSubscriptionEntry(targetDatasetAddress));
            assertTrue(sourceEntry
                    .getOwnedOutwardSubscriptionDependencies(newOwner)
                    .isEmpty());
        } finally {
            session.getLock().unlock();
        }

        assertTrue(context.getBulkCollectCalls().isEmpty());
    }

    @Test
    public void subscribe_requiredTypeDatasetPrecedesRequiringDatasetAndCleansUpWithDependency() {
        final var requiredTypeDataset = new DatasetMetadata(
                0, "MetaData", null, DatasetMetadata.FilterType.NONE, false, DatasetMetadata.CacheType.NONE, false);
        final var requiringDataset = new DatasetMetadata(
                1,
                "Event",
                7,
                DatasetMetadata.FilterType.NONE,
                false,
                DatasetMetadata.CacheType.NONE,
                true,
                requiredTypeDataset);
        final var schema = new SchemaMetaData("Test", requiredTypeDataset, requiringDataset);
        final var context = new TestSessionContext(schema);
        final var manager = createManager(context, mock(ReplicantMessageBroker.class));
        final var session = createOpenSession();
        final var requiredTypeDatasetAddress = DatasetAddress.of(0);
        final var requiringDatasetAddress = DatasetAddress.of(1, 42);
        TransactionSynchronizationRegistryUtil.lookup().putResource(ServerConstants.REPLICATION_INVOCATION_KEY, null);

        manager.subscribe(session, 7, List.of(requiringDatasetAddress), null);

        final var collectCalls = context.getBulkCollectCalls();
        assertEquals(collectCalls.size(), 2);
        assertEquals(collectCalls.get(0).datasetAddresses(), List.of(requiredTypeDatasetAddress));
        assertEquals(collectCalls.get(1).datasetAddresses(), List.of(requiringDatasetAddress));

        final var lock = session.getLock();
        lock.lock();
        try {
            session.recordDatasetScopedSubscriptionDependency(requiringDatasetAddress, requiredTypeDatasetAddress);
        } finally {
            lock.unlock();
        }

        TransactionSynchronizationRegistryUtil.lookup().putResource(ServerConstants.REPLICATION_INVOCATION_KEY, null);
        manager.unsubscribe(session, 8, List.of(requiringDatasetAddress));

        lock.lock();
        try {
            assertFalse(session.isSubscriptionEntryPresent(requiringDatasetAddress));
            assertFalse(session.isSubscriptionEntryPresent(requiredTypeDatasetAddress));
        } finally {
            lock.unlock();
        }
    }

    @Test
    public void invalidateSession_removesAndClosesExistingSession() throws Exception {
        final var schema = new SchemaMetaData(
                "Test",
                new DatasetMetadata(
                        0,
                        "Source",
                        null,
                        DatasetMetadata.FilterType.NONE,
                        false,
                        DatasetMetadata.CacheType.NONE,
                        true));
        final var context = new TestSessionContext(schema);
        final var manager = createManager(context, mock(ReplicantMessageBroker.class));

        final var webSocketSession = mock(Session.class);
        when(webSocketSession.getId()).thenReturn("session-1");
        when(webSocketSession.isOpen()).thenReturn(true);

        final var session = manager.createSession(webSocketSession, mock(ReplicantSessionAuthorization.class));
        assertNotNull(manager.getSession("session-1"));

        manager.invalidateSession(session);

        assertNull(manager.getSession("session-1"));
        verify(webSocketSession).close();
    }

    @Test
    public void invalidateSession_ignoresUnknownSession() throws Exception {
        final var schema = new SchemaMetaData(
                "Test",
                new DatasetMetadata(
                        0,
                        "Source",
                        null,
                        DatasetMetadata.FilterType.NONE,
                        false,
                        DatasetMetadata.CacheType.NONE,
                        true));
        final var context = new TestSessionContext(schema);
        final var manager = createManager(context, mock(ReplicantMessageBroker.class));

        final var webSocketSession = mock(Session.class);
        when(webSocketSession.getId()).thenReturn("unknown-session");

        final var session = new ReplicantSession(webSocketSession);
        manager.invalidateSession(session);

        verify(webSocketSession, never()).close();
    }

    @Test
    public void unsubscribe_removesSubscriptionsViaSessionLogic() {
        final var dataset = new DatasetMetadata(
                0, "Dataset", 1, DatasetMetadata.FilterType.NONE, false, DatasetMetadata.CacheType.NONE, true);
        final var schema = new SchemaMetaData("Test", dataset);
        final var context = new TestSessionContext(schema);
        final var manager = createManager(context, mock(ReplicantMessageBroker.class));

        final var webSocketSession = mock(Session.class);
        when(webSocketSession.getId()).thenReturn("session-1");
        when(webSocketSession.isOpen()).thenReturn(true);
        final var session = new ReplicantSession(webSocketSession);

        final var datasetAddress1 = DatasetAddress.of(0, 1);
        final var datasetAddress2 = DatasetAddress.of(0, 2);
        final var datasetAddress3 = DatasetAddress.of(0, 3);

        session.getLock().lock();
        try {
            final var entry1 = session.createSubscriptionEntry(datasetAddress1);
            final var entry2 = session.createSubscriptionEntry(datasetAddress2);
            entry1.setExplicitlySubscribed(true);
            entry2.setExplicitlySubscribed(true);
        } finally {
            session.getLock().unlock();
        }

        final var registry = TransactionSynchronizationRegistryUtil.lookup();
        registry.putResource(ServerConstants.REPLICATION_INVOCATION_KEY, null);

        manager.unsubscribe(session, 99, List.of(datasetAddress1, datasetAddress2, datasetAddress3));

        session.getLock().lock();
        try {
            assertNull(session.findSubscriptionEntry(datasetAddress1));
            assertNull(session.findSubscriptionEntry(datasetAddress2));
        } finally {
            session.getLock().unlock();
        }

        final var sessionChanges = Objects.requireNonNull(EntityMessageCacheUtil.lookupSessionChanges());
        assertEquals(sessionChanges.getSubscriptionActions().size(), 2);
        assertEquals(sessionChanges.getSubscriptionActions().get(0).action(), SubscriptionAction.Action.UNSUBSCRIBE);
        assertEquals(sessionChanges.getSubscriptionActions().get(1).action(), SubscriptionAction.Action.UNSUBSCRIBE);
    }

    @Test
    public void sendChangeMessage_deleteRootUnsubscribesRootAndDownstream() {
        final var sourceDataset = new DatasetMetadata(
                0, "Source", 1, DatasetMetadata.FilterType.NONE, false, DatasetMetadata.CacheType.NONE, true);
        final var targetDataset = new DatasetMetadata(
                1, "Target", null, DatasetMetadata.FilterType.NONE, false, DatasetMetadata.CacheType.NONE, true);
        final var schema = new SchemaMetaData("Test", sourceDataset, targetDataset);
        final var context = new TestSessionContext(schema);
        final var manager = createManager(context, mock(ReplicantMessageBroker.class));

        final var webSocketSession = mock(Session.class);
        final var remote = mock(RemoteEndpoint.Basic.class);
        when(webSocketSession.getId()).thenReturn("session-1");
        when(webSocketSession.isOpen()).thenReturn(true);
        when(webSocketSession.getBasicRemote()).thenReturn(remote);
        final var session = new ReplicantSession(webSocketSession);

        final var sourceDatasetAddress = DatasetAddress.of(0, 10);
        final var targetDatasetAddress = DatasetAddress.of(1);

        final var routingKeys = new HashMap<String, Serializable>();
        routingKeys.put("Source", new ArrayList<>(List.of(10)));
        final var deleteMessage = new EntityMessage(10, 1, 0, routingKeys, null, null);
        final var changeSet = new ChangeSet();
        final var packet = new Packet(false, null, null, null, List.of(deleteMessage), changeSet);

        session.getLock().lock();
        try {
            final var sourceEntry = session.createSubscriptionEntry(sourceDatasetAddress);
            sourceEntry.setExplicitlySubscribed(true);
            session.createSubscriptionEntry(targetDatasetAddress);
            session.recordDatasetScopedSubscriptionDependency(sourceDatasetAddress, targetDatasetAddress);

            manager.sendChangeMessage(session, packet);

            assertNull(session.findSubscriptionEntry(sourceDatasetAddress));
            assertNull(session.findSubscriptionEntry(targetDatasetAddress));
        } finally {
            session.getLock().unlock();
        }

        assertEquals(changeSet.getSubscriptionActions().size(), 2);
        final var actionByDatasetAddress = changeSet.getSubscriptionActions().stream()
                .collect(java.util.stream.Collectors.toMap(
                        SubscriptionAction::datasetAddress, SubscriptionAction::action));
        assertEquals(actionByDatasetAddress.get(sourceDatasetAddress), SubscriptionAction.Action.DELETE);
        assertEquals(actionByDatasetAddress.get(targetDatasetAddress), SubscriptionAction.Action.UNSUBSCRIBE);
    }

    @Test
    public void sendChangeMessage_deleteWithKeyedSubscriptions_unsubscribesConcreteTargetsWithoutMessageLinks() {
        final var sourceDataset = new DatasetMetadata(
                0, "Source", 1, DatasetMetadata.FilterType.STATIC, true, DatasetMetadata.CacheType.NONE, true);
        final var targetDataset = new DatasetMetadata(
                1, "Target", null, DatasetMetadata.FilterType.STATIC, true, DatasetMetadata.CacheType.NONE, true);
        final var schema = new SchemaMetaData("Test", sourceDataset, targetDataset);
        final var context = new TestSessionContext(schema);
        final var manager = createManager(context, mock(ReplicantMessageBroker.class));

        final var webSocketSession = mock(Session.class);
        final var remote = mock(RemoteEndpoint.Basic.class);
        when(webSocketSession.getId()).thenReturn("session-1");
        when(webSocketSession.isOpen()).thenReturn(true);
        when(webSocketSession.getBasicRemote()).thenReturn(remote);
        final var session = new ReplicantSession(webSocketSession);

        final var sourceAddressA = DatasetAddress.of(0, 10, "fi-a");
        final var sourceAddressB = DatasetAddress.of(0, 10, "fi-b");
        final var targetDatasetAddressA = DatasetAddress.of(1, null, "fi-a");
        final var targetDatasetAddressB = DatasetAddress.of(1, null, "fi-b");
        final var routingKeys = new HashMap<String, Serializable>();
        routingKeys.put("Source", new ArrayList<>(List.of(10)));
        final var deleteMessage = new EntityMessage(10, 1, 0, routingKeys, null, null);
        final var changeSet = new ChangeSet();
        final var packet = new Packet(false, null, null, null, List.of(deleteMessage), changeSet);

        session.getLock().lock();
        try {
            final var sourceEntryA = session.createSubscriptionEntry(sourceAddressA);
            final var sourceEntryB = session.createSubscriptionEntry(sourceAddressB);
            sourceEntryA.setExplicitlySubscribed(true);
            sourceEntryB.setExplicitlySubscribed(true);
            session.createSubscriptionEntry(targetDatasetAddressA);
            session.createSubscriptionEntry(targetDatasetAddressB);
            session.recordDatasetScopedSubscriptionDependency(sourceAddressA, targetDatasetAddressA);
            session.recordDatasetScopedSubscriptionDependency(sourceAddressB, targetDatasetAddressB);

            manager.sendChangeMessage(session, packet);

            assertNull(session.findSubscriptionEntry(sourceAddressA));
            assertNull(session.findSubscriptionEntry(sourceAddressB));
            assertNull(session.findSubscriptionEntry(targetDatasetAddressA));
            assertNull(session.findSubscriptionEntry(targetDatasetAddressB));
        } finally {
            session.getLock().unlock();
        }
    }

    @Test
    public void tryGetCacheEntry_rejectsPartialAddresses() throws Exception {
        final var dataset = new DatasetMetadata(
                0, "Source", null, DatasetMetadata.FilterType.NONE, false, DatasetMetadata.CacheType.INTERNAL, true);
        final var schema = new SchemaMetaData("Test", dataset);
        final var context = new TestSessionContext(schema);
        final var manager = createManager(context, mock(ReplicantMessageBroker.class));

        final var method =
                ReplicantSessionManagerImpl.class.getDeclaredMethod("tryGetCacheEntry", DatasetAddress.class);
        method.setAccessible(true);

        final var exception =
                expectThrows(InvocationTargetException.class, () -> method.invoke(manager, DatasetAddress.partial(0)));
        assertTrue(exception.getCause() instanceof AssertionError);
    }

    @NonNull
    private ReplicantSessionManagerImpl createManager(
            @NonNull final TestSessionContext context, @NonNull final ReplicantMessageBroker broker) {
        final var manager = new ReplicantSessionManagerImpl();
        setField(manager, "_context", context);
        setField(manager, "_broker", broker);
        setField(manager, "_registry", TransactionSynchronizationRegistryUtil.lookup());
        return manager;
    }

    @NonNull
    private TestSessionContext createManagerContext(@NonNull final SchemaMetaData schema) {
        return new TestSessionContext(schema);
    }

    private void setField(@NonNull final Object target, @NonNull final String name, @Nullable final Object value) {
        try {
            final var field = ReplicantSessionManagerImpl.class.getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, value);
        } catch (final Exception e) {
            throw new AssertionError(e);
        }
    }

    @NonNull
    private ReplicantSession createOpenSession() {
        final var webSocketSession = mock(Session.class);
        final var remote = mock(RemoteEndpoint.Basic.class);
        when(webSocketSession.getId()).thenReturn("session-1");
        when(webSocketSession.isOpen()).thenReturn(true);
        when(webSocketSession.getBasicRemote()).thenReturn(remote);
        return new ReplicantSession(webSocketSession);
    }

    @NonNull
    private HashMap<String, Serializable> instanceRouting(@NonNull final String routingKey, final int datasetRootId) {
        final var routingKeys = new HashMap<String, Serializable>();
        routingKeys.put(routingKey, new ArrayList<>(List.of(datasetRootId)));
        return routingKeys;
    }

    @NonNull
    private HashMap<String, Serializable> attributes(final int id) {
        final var attributes = new HashMap<String, Serializable>();
        attributes.put("ID", id);
        return attributes;
    }

    private static final class TestSessionContext implements ReplicantSessionContext {
        @NonNull
        private final SchemaMetaData _schema;

        @NonNull
        private final List<BulkCollectCall> _bulkCollectCalls = new ArrayList<>();

        @NonNull
        private final List<Packet> _preSendChangeMessages = new ArrayList<>();

        @NonNull
        private final Set<DatasetAddress> _excludedFilterEntityMessageAddresses = new HashSet<>();

        private boolean _shouldFollowDatasetLink = true;

        private TestSessionContext(@NonNull final SchemaMetaData schema) {
            _schema = schema;
        }

        @NonNull
        @Override
        public SchemaMetaData getSchemaMetaData() {
            return _schema;
        }

        @Override
        public boolean isAuthorized(@NonNull final ReplicantSession session) {
            return true;
        }

        @Override
        public void preSubscribe(
                @NonNull final ReplicantSession session,
                @NonNull final DatasetAddress datasetAddress,
                @Nullable final JsonObject filter) {}

        @Override
        public void preSendChangeMessage(@NonNull final ReplicantSession session, @NonNull final Packet packet) {
            _preSendChangeMessages.add(packet);
        }

        @NonNull
        @Override
        public JsonObject deriveTargetFilter(
                @NonNull final EntityMessage entityMessage,
                @NonNull final DatasetAddress sourceDatasetAddress,
                @Nullable final JsonObject sourceFilter,
                @NonNull final DatasetAddress targetDatasetAddress) {
            return Json.createObjectBuilder().add("k", "v").build();
        }

        @NonNull
        @Override
        public String deriveTargetDatasetKey(
                @NonNull final EntityMessage entityMessage,
                @NonNull final DatasetAddress sourceDatasetAddress,
                @Nullable final JsonObject sourceFilter,
                @NonNull final DatasetAddress targetDatasetAddress,
                @Nullable final JsonObject targetFilter) {
            final var sourceDatasetKey = sourceDatasetAddress.datasetKey();
            return null == sourceDatasetKey ? "fi-7" : sourceDatasetKey;
        }

        @Override
        public boolean flushOpenEntityManager() {
            return false;
        }

        @Override
        public void execCommand(
                @NonNull final ReplicantSession session,
                @NonNull final String command,
                final int requestId,
                @Nullable final JsonObject payload) {}

        @Override
        public void collectSubscriptionData(
                @Nullable final ReplicantSession session,
                @NonNull final List<DatasetAddress> datasetAddresses,
                @Nullable final JsonObject filter,
                @NonNull final ChangeSet changeSet,
                final boolean isExplicitSubscribe) {
            _bulkCollectCalls.add(new BulkCollectCall(datasetAddresses, filter, isExplicitSubscribe));
            if (null != session) {
                for (final var datasetAddress : datasetAddresses) {
                    final var existing = session.findSubscriptionEntry(datasetAddress);
                    final var entry = null == existing ? session.createSubscriptionEntry(datasetAddress) : existing;
                    entry.setFilter(filter);
                    changeSet.mergeSubscriptionAction(
                            datasetAddress,
                            null == existing ? SubscriptionAction.Action.SUBSCRIBE : SubscriptionAction.Action.UPDATE,
                            filter);
                }
            }
        }

        @Override
        public void collectSubscriptionDataForFilterChange(
                @NonNull final ReplicantSession session,
                @NonNull final List<DatasetAddress> datasetAddresses,
                @Nullable final JsonObject originalFilter,
                @Nullable final JsonObject newFilter,
                @NonNull final ChangeSet changeSet) {}

        @Nullable
        @Override
        public EntityMessage filterEntityMessage(
                @NonNull final ReplicantSession session,
                @NonNull final DatasetAddress datasetAddress,
                @NonNull final EntityMessage message) {
            if (_excludedFilterEntityMessageAddresses.contains(datasetAddress)) {
                return null;
            }
            return message;
        }

        @Override
        public boolean shouldFollowDatasetLink(
                @NonNull final DatasetAddress sourceDatasetAddress,
                @Nullable final JsonObject sourceFilter,
                @NonNull final DatasetAddress targetDatasetAddress,
                @Nullable final JsonObject targetFilter) {
            return _shouldFollowDatasetLink;
        }

        @NonNull
        List<BulkCollectCall> getBulkCollectCalls() {
            return _bulkCollectCalls;
        }

        @NonNull
        List<Packet> getPreSendChangeMessages() {
            return _preSendChangeMessages;
        }

        void excludeFilterEntityMessageDatasetAddress(@NonNull final DatasetAddress datasetAddress) {
            _excludedFilterEntityMessageAddresses.add(datasetAddress);
        }

        void setShouldFollowDatasetLink(final boolean shouldFollowDatasetLink) {
            _shouldFollowDatasetLink = shouldFollowDatasetLink;
        }
    }

    private record BulkCollectCall(
            @NonNull List<DatasetAddress> datasetAddresses,
            @Nullable JsonObject filter,
            boolean isExplicitSubscribe) {}

    private record DeriveTargetDatasetKeyCall(
            @NonNull EntityMessage entityMessage,
            @NonNull DatasetAddress sourceDatasetAddress,
            @Nullable JsonObject sourceFilter,
            @NonNull DatasetAddress targetDatasetAddress,
            @Nullable JsonObject targetFilter) {}
}
