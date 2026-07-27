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
import replicant.server.ChangeSet;
import replicant.server.DatasetAddress;
import replicant.server.DatasetAddressCandidate;
import replicant.server.DatasetAddressTemplate;
import replicant.server.EntityChange;
import replicant.server.EntityChangeCandidate;
import replicant.server.ServerConstants;
import replicant.server.SubscriptionChange;
import replicant.server.SubscriptionDependencyCandidate;
import replicant.server.ee.RegistryUtil;
import replicant.server.runtime.EntityChangeCandidateCacheUtil;
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
    public void sendChangeMessage_fixedKeyedLinkFollow_usesTargetDatasetKey() {
        final var sourceDataset = new DatasetMetadata(
                0,
                "Source",
                null,
                DatasetMetadata.FilterMode.UNFILTERED,
                null,
                false,
                DatasetMetadata.CacheType.NONE,
                true);
        final var targetDataset = new DatasetMetadata(
                1,
                "Target",
                1,
                DatasetMetadata.FilterMode.PARAMETER_FILTERED,
                DatasetMetadata.FilterParameterMode.FIXED,
                true,
                DatasetMetadata.CacheType.NONE,
                true);
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
                new SubscriptionDependencyCandidate(sourceDatasetAddress, targetDatasetAddress);

        final var routingKeys = new HashMap<String, Serializable>();
        routingKeys.put("Source", "present");
        final var attributes = new HashMap<String, Serializable>();
        attributes.put("ID", 1);
        final var message =
                new EntityChangeCandidate(1, 1, 0L, routingKeys, attributes, Set.of(subscriptionDependency));

        final var newFilterParameter = Json.createObjectBuilder().add("k", "v").build();
        final var packet = new Packet(false, null, null, null, List.of(message), new ChangeSet());

        session.getLock().lock();
        try {
            final var sourceEntry = session.createSubscriptionEntry(sourceDatasetAddress, SubscriptionMode.IMPLICIT);
            final var originalFilter =
                    Json.createObjectBuilder().add("old", "value").build();
            sourceEntry.setFilterParameter(originalFilter);

            manager.sendChangeMessage(session, packet);

            assertEquals(context.getPreSendChangeMessages(), List.of(packet));
            final var targetEntry = Objects.requireNonNull(session.findSubscriptionEntry(targetDatasetAddress));
            assertEquals(targetEntry.getFilterParameter(), newFilterParameter);
            assertTrue(sourceEntry.getOutwardSubscriptionDependencies().contains(targetDatasetAddress));
            assertTrue(targetEntry.getInwardSubscriptionDependencies().contains(sourceDatasetAddress));
        } finally {
            session.getLock().unlock();
        }

        final var collectCalls = context.getBulkCollectCalls();
        assertEquals(collectCalls.size(), 1);
        final var call = collectCalls.get(0);
        assertEquals(call.datasetAddresses(), List.of(targetDatasetAddress));
        assertEquals(call.filterParameter(), newFilterParameter);
        assertEquals(call.mode(), SubscriptionMode.IMPLICIT);
    }

    @Test
    public void sendChangeMessage_deleteRemovesOnlyDeletedEntityOwnershipForSharedTarget() {
        final var sourceDataset = new DatasetMetadata(
                0,
                "Source",
                1,
                DatasetMetadata.FilterMode.UNFILTERED,
                null,
                false,
                DatasetMetadata.CacheType.NONE,
                true);
        final var targetDataset = new DatasetMetadata(
                1,
                "Target",
                null,
                DatasetMetadata.FilterMode.UNFILTERED,
                null,
                false,
                DatasetMetadata.CacheType.NONE,
                true);
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
        final var subscriptionDependency =
                new SubscriptionDependencyCandidate(sourceDatasetAddress, targetDatasetAddress);
        final var routingKeys = new HashMap<String, Serializable>();
        routingKeys.put("Source", new ArrayList<>(List.of(10)));
        final var attributesA = new HashMap<String, Serializable>();
        final var attributesB = new HashMap<String, Serializable>();
        attributesA.put("ID", 100);
        attributesB.put("ID", 101);

        final var updateA =
                new EntityChangeCandidate(100, 2, 0L, routingKeys, attributesA, Set.of(subscriptionDependency));
        final var updateB =
                new EntityChangeCandidate(101, 2, 0L, routingKeys, attributesB, Set.of(subscriptionDependency));
        final var deleteA = new EntityChangeCandidate(100, 2, 1L, routingKeys, null, null);

        session.getLock().lock();
        try {
            final var sourceEntry = session.createSubscriptionEntry(sourceDatasetAddress, SubscriptionMode.IMPLICIT);
            sourceEntry.setMode(SubscriptionMode.EXPLICIT);

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
                0,
                "Source",
                1,
                DatasetMetadata.FilterMode.UNFILTERED,
                null,
                false,
                DatasetMetadata.CacheType.NONE,
                true);
        final var targetDataset = new DatasetMetadata(
                1,
                "Target",
                null,
                DatasetMetadata.FilterMode.UNFILTERED,
                null,
                false,
                DatasetMetadata.CacheType.NONE,
                true);
        final var schema = new SchemaMetaData("Test", sourceDataset, targetDataset);
        final var context = createManagerContext(schema);
        final var manager = createManager(context, mock(ReplicantMessageBroker.class));
        final var session = createOpenSession();

        final var sourceDatasetAddress = DatasetAddress.of(0, 10);
        final var targetDatasetAddress = DatasetAddress.of(1);
        final var oldOwner = SubscriptionDependencyOwner.entity(2, 100);
        final var newOwner = SubscriptionDependencyOwner.entity(2, 101);
        final var subscriptionDependency =
                new SubscriptionDependencyCandidate(sourceDatasetAddress, targetDatasetAddress);
        final var updateNew = new EntityChangeCandidate(
                101, 2, 0L, instanceRouting("Source", 10), attributes(101), Set.of(subscriptionDependency));
        final var deleteOld = new EntityChangeCandidate(100, 2, 1L, instanceRouting("Source", 10), null, null);

        session.getLock().lock();
        try {
            final var sourceEntry = session.createSubscriptionEntry(sourceDatasetAddress, SubscriptionMode.IMPLICIT);
            sourceEntry.setMode(SubscriptionMode.EXPLICIT);
            session.createSubscriptionEntry(targetDatasetAddress, SubscriptionMode.IMPLICIT);
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
                0,
                "Source",
                1,
                DatasetMetadata.FilterMode.UNFILTERED,
                null,
                false,
                DatasetMetadata.CacheType.NONE,
                true);
        final var targetDataset = new DatasetMetadata(
                1,
                "Target",
                null,
                DatasetMetadata.FilterMode.UNFILTERED,
                null,
                false,
                DatasetMetadata.CacheType.NONE,
                true);
        final var schema = new SchemaMetaData("Test", sourceDataset, targetDataset);
        final var context = createManagerContext(schema);
        final var manager = createManager(context, mock(ReplicantMessageBroker.class));
        final var session = createOpenSession();

        final var sourceDatasetAddress = DatasetAddress.of(0, 10);
        final var targetDatasetAddress = DatasetAddress.of(1);
        final var oldOwner = SubscriptionDependencyOwner.entity(2, 100);
        final var newOwner = SubscriptionDependencyOwner.entity(2, 101);
        final var subscriptionDependency =
                new SubscriptionDependencyCandidate(sourceDatasetAddress, targetDatasetAddress);
        final var updateNew = new EntityChangeCandidate(
                101, 2, 0L, instanceRouting("Source", 10), attributes(101), Set.of(subscriptionDependency));
        final var deleteOld = new EntityChangeCandidate(100, 2, 1L, instanceRouting("Source", 10), null, null);
        final var changeSet = new ChangeSet();
        changeSet.merge(new EntityChange(updateNew, sourceDatasetAddress));

        session.getLock().lock();
        try {
            final var sourceEntry = session.createSubscriptionEntry(sourceDatasetAddress, SubscriptionMode.IMPLICIT);
            sourceEntry.setMode(SubscriptionMode.EXPLICIT);
            session.createSubscriptionEntry(targetDatasetAddress, SubscriptionMode.IMPLICIT);
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
                0,
                "Source",
                1,
                DatasetMetadata.FilterMode.UNFILTERED,
                null,
                false,
                DatasetMetadata.CacheType.NONE,
                true);
        final var targetDataset = new DatasetMetadata(
                1,
                "Target",
                3,
                DatasetMetadata.FilterMode.UNFILTERED,
                null,
                false,
                DatasetMetadata.CacheType.NONE,
                true);
        final var schema = new SchemaMetaData("Test", sourceDataset, targetDataset);
        final var context = createManagerContext(schema);
        final var manager = createManager(context, mock(ReplicantMessageBroker.class));
        final var session = createOpenSession();

        final var sourceDatasetAddress = DatasetAddress.of(0, 10);
        final var oldTargetDatasetAddress = DatasetAddress.of(1, 20);
        final var newTargetDatasetAddress = DatasetAddress.of(1, 21);
        final var newOwner = SubscriptionDependencyOwner.entity(2, 101);
        final var subscriptionDependency =
                new SubscriptionDependencyCandidate(sourceDatasetAddress, newTargetDatasetAddress);
        final var updateNew = new EntityChangeCandidate(
                101, 2, 0L, instanceRouting("Source", 10), attributes(101), Set.of(subscriptionDependency));
        final var deleteOld = new EntityChangeCandidate(100, 2, 1L, instanceRouting("Source", 10), null, null);

        session.getLock().lock();
        try {
            final var sourceEntry = session.createSubscriptionEntry(sourceDatasetAddress, SubscriptionMode.IMPLICIT);
            sourceEntry.setMode(SubscriptionMode.EXPLICIT);
            session.createSubscriptionEntry(oldTargetDatasetAddress, SubscriptionMode.IMPLICIT);
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
        assertNull(collectCalls.get(0).filterParameter());
        assertEquals(collectCalls.get(0).mode(), SubscriptionMode.IMPLICIT);
    }

    @Test
    public void sendChangeMessage_filterMismatchReplacement_isCollectedWithNewFilterByNormalExpansion() {
        final var sourceDataset = new DatasetMetadata(
                0,
                "Source",
                1,
                DatasetMetadata.FilterMode.UNFILTERED,
                null,
                false,
                DatasetMetadata.CacheType.NONE,
                true);
        final var targetDataset = new DatasetMetadata(
                1,
                "Target",
                null,
                DatasetMetadata.FilterMode.PARAMETER_FILTERED,
                DatasetMetadata.FilterParameterMode.UPDATABLE,
                false,
                DatasetMetadata.CacheType.NONE,
                true);
        final var schema = new SchemaMetaData("Test", sourceDataset, targetDataset);
        final var context = createManagerContext(schema);
        final var manager = createManager(context, mock(ReplicantMessageBroker.class));
        final var session = createOpenSession();

        final var sourceDatasetAddress = DatasetAddress.of(0, 10);
        final var targetDatasetAddress = DatasetAddress.of(1);
        final var oldFilterParameter =
                Json.createObjectBuilder().add("parameter", "old").build();
        final var newFilterParameter =
                Json.createObjectBuilder().add("parameter", "new").build();
        final var newOwner = SubscriptionDependencyOwner.entity(2, 101);
        final var subscriptionDependency =
                new SubscriptionDependencyCandidate(sourceDatasetAddress, targetDatasetAddress, newFilterParameter);
        final var updateNew = new EntityChangeCandidate(
                101, 2, 0L, instanceRouting("Source", 10), attributes(101), Set.of(subscriptionDependency));
        final var deleteOld = new EntityChangeCandidate(100, 2, 1L, instanceRouting("Source", 10), null, null);

        session.getLock().lock();
        try {
            final var sourceEntry = session.createSubscriptionEntry(sourceDatasetAddress, SubscriptionMode.IMPLICIT);
            sourceEntry.setMode(SubscriptionMode.EXPLICIT);
            final var targetEntry = session.createSubscriptionEntry(targetDatasetAddress, SubscriptionMode.IMPLICIT);
            targetEntry.setFilterParameter(oldFilterParameter);
            session.recordEntityScopedSubscriptionDependency(sourceDatasetAddress, targetDatasetAddress, 2, 100);

            manager.sendChangeMessage(
                    session, new Packet(false, null, null, null, List.of(deleteOld, updateNew), new ChangeSet()));

            final var reloadedTargetEntry = Objects.requireNonNull(session.findSubscriptionEntry(targetDatasetAddress));
            assertEquals(reloadedTargetEntry.getFilterParameter(), newFilterParameter);
            assertEquals(sourceEntry.getOwnedOutwardSubscriptionDependencies(newOwner), Set.of(targetDatasetAddress));
        } finally {
            session.getLock().unlock();
        }

        final var collectCalls = context.getBulkCollectCalls();
        assertEquals(collectCalls.size(), 1);
        assertEquals(collectCalls.get(0).datasetAddresses(), List.of(targetDatasetAddress));
        assertEquals(collectCalls.get(0).filterParameter(), newFilterParameter);
        assertEquals(collectCalls.get(0).mode(), SubscriptionMode.IMPLICIT);
    }

    @Test
    public void sendChangeMessage_existingLinkedUpdatableFilterParameterRetainsTargetSubscription() {
        final var sourceDataset = new DatasetMetadata(
                0,
                "Source",
                1,
                DatasetMetadata.FilterMode.UNFILTERED,
                null,
                false,
                DatasetMetadata.CacheType.NONE,
                true);
        final var targetDataset = new DatasetMetadata(
                1,
                "Target",
                null,
                DatasetMetadata.FilterMode.PARAMETER_FILTERED,
                DatasetMetadata.FilterParameterMode.UPDATABLE,
                false,
                DatasetMetadata.CacheType.NONE,
                true);
        final var context = createManagerContext(new SchemaMetaData("Test", sourceDataset, targetDataset));
        final var manager = createManager(context, mock(ReplicantMessageBroker.class));
        final var session = createOpenSession();
        final var sourceDatasetAddress = DatasetAddress.of(0, 10);
        final var targetDatasetAddress = DatasetAddress.of(1);
        final var originalFilterParameter =
                Json.createObjectBuilder().add("parameter", "old").build();
        final var newFilterParameter =
                Json.createObjectBuilder().add("parameter", "new").build();
        final var owner = SubscriptionDependencyOwner.entity(2, 101);
        final var subscriptionDependency =
                new SubscriptionDependencyCandidate(sourceDatasetAddress, targetDatasetAddress, newFilterParameter);
        final var message = new EntityChangeCandidate(
                101, 2, 0L, instanceRouting("Source", 10), attributes(101), Set.of(subscriptionDependency));

        final SubscriptionEntry originalTargetEntry;
        session.getLock().lock();
        try {
            final var sourceEntry = session.createSubscriptionEntry(sourceDatasetAddress, SubscriptionMode.IMPLICIT);
            sourceEntry.setMode(SubscriptionMode.EXPLICIT);
            originalTargetEntry = session.createSubscriptionEntry(targetDatasetAddress, SubscriptionMode.IMPLICIT);
            originalTargetEntry.setFilterParameter(originalFilterParameter);
            session.recordEntityScopedSubscriptionDependency(sourceDatasetAddress, targetDatasetAddress, 2, 101);

            manager.sendChangeMessage(session, new Packet(false, null, null, null, List.of(message), new ChangeSet()));

            assertSame(session.getSubscriptionEntry(targetDatasetAddress), originalTargetEntry);
            assertEquals(originalTargetEntry.getFilterParameter(), newFilterParameter);
            assertEquals(sourceEntry.getOwnedOutwardSubscriptionDependencies(owner), Set.of(targetDatasetAddress));
        } finally {
            session.getLock().unlock();
        }

        assertEquals(
                context.getFilterParameterChangeCalls(),
                List.of(new FilterParameterChangeCall(
                        List.of(targetDatasetAddress), originalFilterParameter, newFilterParameter)));
    }

    @Test
    public void sendChangeMessage_existingLinkedFixedFilterParameterReplacesSubscription() throws Exception {
        final var sourceDataset = new DatasetMetadata(
                0,
                "Source",
                1,
                DatasetMetadata.FilterMode.UNFILTERED,
                null,
                false,
                DatasetMetadata.CacheType.NONE,
                true);
        final var targetDataset = new DatasetMetadata(
                1,
                "Target",
                null,
                DatasetMetadata.FilterMode.PARAMETER_FILTERED,
                DatasetMetadata.FilterParameterMode.FIXED,
                false,
                DatasetMetadata.CacheType.NONE,
                true);
        final var context = createManagerContext(new SchemaMetaData("Test", sourceDataset, targetDataset));
        final var manager = createManager(context, mock(ReplicantMessageBroker.class));
        final var session = createOpenSession();
        final var sourceDatasetAddress = DatasetAddress.of(0, 10);
        final var targetDatasetAddress = DatasetAddress.of(1);
        final var originalFilterParameter =
                Json.createObjectBuilder().add("parameter", "old").build();
        final var newFilterParameter =
                Json.createObjectBuilder().add("parameter", "new").build();
        final var subscriptionDependency =
                new SubscriptionDependencyCandidate(sourceDatasetAddress, targetDatasetAddress, newFilterParameter);
        final var message = new EntityChangeCandidate(
                101, 2, 0L, instanceRouting("Source", 10), attributes(101), Set.of(subscriptionDependency));

        session.getLock().lock();
        try {
            final var sourceEntry = session.createSubscriptionEntry(sourceDatasetAddress, SubscriptionMode.IMPLICIT);
            sourceEntry.setMode(SubscriptionMode.EXPLICIT);
            final var originalTargetEntry =
                    session.createSubscriptionEntry(targetDatasetAddress, SubscriptionMode.IMPLICIT);
            originalTargetEntry.setFilterParameter(originalFilterParameter);
            session.recordEntityScopedSubscriptionDependency(sourceDatasetAddress, targetDatasetAddress, 2, 101);

            manager.sendChangeMessage(session, new Packet(false, null, null, null, List.of(message), new ChangeSet()));

            final var replacementTargetEntry = session.getSubscriptionEntry(targetDatasetAddress);
            assertNotSame(replacementTargetEntry, originalTargetEntry);
            assertEquals(replacementTargetEntry.getFilterParameter(), newFilterParameter);
            assertEquals(
                    sourceEntry.getOwnedOutwardSubscriptionDependencies(SubscriptionDependencyOwner.entity(2, 101)),
                    Set.of(targetDatasetAddress));
        } finally {
            session.getLock().unlock();
        }
        verify(session.getWebSocketSession(), never()).close(any(javax.websocket.CloseReason.class));
        assertEquals(
                context.getBulkCollectCalls(),
                List.of(new BulkCollectCall(
                        List.of(targetDatasetAddress), newFilterParameter, SubscriptionMode.IMPLICIT)));
        assertTrue(context.getFilterParameterChangeCalls().isEmpty());
    }

    @Test
    public void sendChangeMessage_existingLinkedFixedFilterParameterRejectsReplacementWhenTargetIsRetained()
            throws Exception {
        final var sourceDataset = new DatasetMetadata(
                0,
                "Source",
                1,
                DatasetMetadata.FilterMode.UNFILTERED,
                null,
                false,
                DatasetMetadata.CacheType.NONE,
                true);
        final var targetDataset = new DatasetMetadata(
                1,
                "Target",
                null,
                DatasetMetadata.FilterMode.PARAMETER_FILTERED,
                DatasetMetadata.FilterParameterMode.FIXED,
                false,
                DatasetMetadata.CacheType.NONE,
                true);
        final var context = createManagerContext(new SchemaMetaData("Test", sourceDataset, targetDataset));
        final var manager = createManager(context, mock(ReplicantMessageBroker.class));
        final var session = createOpenSession();
        final var sourceDatasetAddress = DatasetAddress.of(0, 10);
        final var targetDatasetAddress = DatasetAddress.of(1);
        final var originalFilterParameter =
                Json.createObjectBuilder().add("parameter", "old").build();
        final var newFilterParameter =
                Json.createObjectBuilder().add("parameter", "new").build();
        final var subscriptionDependency =
                new SubscriptionDependencyCandidate(sourceDatasetAddress, targetDatasetAddress, newFilterParameter);
        final var message = new EntityChangeCandidate(
                101, 2, 0L, instanceRouting("Source", 10), attributes(101), Set.of(subscriptionDependency));

        session.getLock().lock();
        try {
            final var sourceEntry = session.createSubscriptionEntry(sourceDatasetAddress, SubscriptionMode.IMPLICIT);
            sourceEntry.setMode(SubscriptionMode.EXPLICIT);
            final var targetEntry = session.createSubscriptionEntry(targetDatasetAddress, SubscriptionMode.IMPLICIT);
            targetEntry.setMode(SubscriptionMode.EXPLICIT);
            targetEntry.setFilterParameter(originalFilterParameter);
            session.recordEntityScopedSubscriptionDependency(sourceDatasetAddress, targetDatasetAddress, 2, 101);

            manager.sendChangeMessage(session, new Packet(false, null, null, null, List.of(message), new ChangeSet()));

            assertSame(session.getSubscriptionEntry(targetDatasetAddress), targetEntry);
            assertEquals(targetEntry.getFilterParameter(), originalFilterParameter);
        } finally {
            session.getLock().unlock();
        }
        verify(session.getWebSocketSession()).close(any(javax.websocket.CloseReason.class));
        assertTrue(context.getBulkCollectCalls().isEmpty());
        assertTrue(context.getFilterParameterChangeCalls().isEmpty());
    }

    @Test
    public void sendChangeMessage_filteredOutSourceRoute_isNotPreserved() {
        final var sourceDataset = new DatasetMetadata(
                0,
                "Source",
                1,
                DatasetMetadata.FilterMode.PARAMETER_FILTERED,
                DatasetMetadata.FilterParameterMode.FIXED,
                true,
                DatasetMetadata.CacheType.NONE,
                true);
        final var targetDataset = new DatasetMetadata(
                1,
                "Target",
                null,
                DatasetMetadata.FilterMode.UNFILTERED,
                null,
                false,
                DatasetMetadata.CacheType.NONE,
                true);
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
                new SubscriptionDependencyCandidate(DatasetAddressTemplate.of(0, 10), targetDatasetAddress);
        final var updateNew = new EntityChangeCandidate(
                101, 2, 0L, instanceRouting("Source", 10), attributes(101), Set.of(subscriptionDependency));
        final var deleteOld = new EntityChangeCandidate(100, 2, 1L, instanceRouting("Source", 10), null, null);
        context.excludeFilterEntityChangeCandidateDatasetAddress(excludedSourceDatasetAddress);

        session.getLock().lock();
        try {
            final var includedSourceEntry =
                    session.createSubscriptionEntry(includedSourceDatasetAddress, SubscriptionMode.IMPLICIT);
            final var excludedSourceEntry =
                    session.createSubscriptionEntry(excludedSourceDatasetAddress, SubscriptionMode.IMPLICIT);
            includedSourceEntry.setMode(SubscriptionMode.EXPLICIT);
            excludedSourceEntry.setMode(SubscriptionMode.EXPLICIT);
            session.createSubscriptionEntry(targetDatasetAddress, SubscriptionMode.IMPLICIT);
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
    public void sendChangeMessage_implicitlyFilteredDatasetAppliesMembershipFilter() {
        final var dataset = new DatasetMetadata(
                0, "Source", 1, DatasetMetadata.FilterMode.IMPLICIT, null, false, DatasetMetadata.CacheType.NONE, true);
        final var context = createManagerContext(new SchemaMetaData("Test", dataset));
        final var manager = createManager(context, mock(ReplicantMessageBroker.class));
        final var session = createOpenSession();
        final var datasetAddress = DatasetAddress.of(0, 10);
        context.excludeFilterEntityChangeCandidateDatasetAddress(datasetAddress);
        final var message = new EntityChangeCandidate(101, 2, 0L, instanceRouting("Source", 10), attributes(101), null);
        final var changeSet = new ChangeSet();

        session.getLock().lock();
        try {
            final var entry = session.createSubscriptionEntry(datasetAddress, SubscriptionMode.IMPLICIT);
            entry.setMode(SubscriptionMode.EXPLICIT);

            manager.sendChangeMessage(session, new Packet(false, null, null, null, List.of(message), changeSet));
        } finally {
            session.getLock().unlock();
        }

        assertTrue(changeSet.getEntityChanges().isEmpty());
    }

    @Test
    public void sendChangeMessage_shouldFollowDatasetLinkFalse_isNotPreserved() {
        final var sourceDataset = new DatasetMetadata(
                0,
                "Source",
                1,
                DatasetMetadata.FilterMode.UNFILTERED,
                null,
                false,
                DatasetMetadata.CacheType.NONE,
                true);
        final var targetDataset = new DatasetMetadata(
                1,
                "Target",
                null,
                DatasetMetadata.FilterMode.PARAMETER_FILTERED,
                DatasetMetadata.FilterParameterMode.UPDATABLE,
                false,
                DatasetMetadata.CacheType.NONE,
                true);
        final var schema = new SchemaMetaData("Test", sourceDataset, targetDataset);
        final var context = createManagerContext(schema);
        context.setShouldFollowDatasetLink(false);
        final var manager = createManager(context, mock(ReplicantMessageBroker.class));
        final var session = createOpenSession();

        final var sourceDatasetAddress = DatasetAddress.of(0, 10);
        final var targetDatasetAddress = DatasetAddress.of(1);
        final var targetFilterParameter =
                Json.createObjectBuilder().add("parameter", "current").build();
        final var newOwner = SubscriptionDependencyOwner.entity(2, 101);
        final var subscriptionDependency =
                new SubscriptionDependencyCandidate(sourceDatasetAddress, targetDatasetAddress, targetFilterParameter);
        final var updateNew = new EntityChangeCandidate(
                101, 2, 0L, instanceRouting("Source", 10), attributes(101), Set.of(subscriptionDependency));
        final var deleteOld = new EntityChangeCandidate(100, 2, 1L, instanceRouting("Source", 10), null, null);

        session.getLock().lock();
        try {
            final var sourceEntry = session.createSubscriptionEntry(sourceDatasetAddress, SubscriptionMode.IMPLICIT);
            sourceEntry.setMode(SubscriptionMode.EXPLICIT);
            final var targetEntry = session.createSubscriptionEntry(targetDatasetAddress, SubscriptionMode.IMPLICIT);
            targetEntry.setFilterParameter(targetFilterParameter);
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
                0,
                "Source",
                1,
                DatasetMetadata.FilterMode.UNFILTERED,
                null,
                false,
                DatasetMetadata.CacheType.NONE,
                true);
        final var targetDataset = new DatasetMetadata(
                1,
                "Target",
                null,
                DatasetMetadata.FilterMode.UNFILTERED,
                null,
                false,
                DatasetMetadata.CacheType.NONE,
                true);
        final var schema = new SchemaMetaData("Test", sourceDataset, targetDataset);
        final var context = createManagerContext(schema);
        final var manager = createManager(context, mock(ReplicantMessageBroker.class));
        final var session = createOpenSession();

        final var sourceDatasetAddress = DatasetAddress.of(0, 10);
        final var targetDatasetAddress = DatasetAddress.of(1);
        final var subscriptionDependency =
                new SubscriptionDependencyCandidate(sourceDatasetAddress, targetDatasetAddress);
        final var updateNew = new EntityChangeCandidate(
                101, 2, 0L, instanceRouting("Source", 10), attributes(101), Set.of(subscriptionDependency));
        final var deleteSourceRoot = new EntityChangeCandidate(10, 1, 1L, instanceRouting("Source", 10), null, null);

        session.getLock().lock();
        try {
            final var sourceEntry = session.createSubscriptionEntry(sourceDatasetAddress, SubscriptionMode.IMPLICIT);
            sourceEntry.setMode(SubscriptionMode.EXPLICIT);
            session.createSubscriptionEntry(targetDatasetAddress, SubscriptionMode.IMPLICIT);
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
                0,
                "Source",
                1,
                DatasetMetadata.FilterMode.PARAMETER_FILTERED,
                DatasetMetadata.FilterParameterMode.FIXED,
                true,
                DatasetMetadata.CacheType.NONE,
                true);
        final var targetDataset = new DatasetMetadata(
                1,
                "Target",
                3,
                DatasetMetadata.FilterMode.UNFILTERED,
                null,
                false,
                DatasetMetadata.CacheType.NONE,
                true);
        final var schema = new SchemaMetaData("Test", sourceDataset, targetDataset);
        final var context = createManagerContext(schema);
        final var manager = createManager(context, mock(ReplicantMessageBroker.class));
        final var session = createOpenSession();

        final var sourceDatasetAddress = DatasetAddress.of(0, 10, "fi-source");
        final var targetDatasetAddress = DatasetAddress.of(1, 20);
        final var newOwner = SubscriptionDependencyOwner.entity(2, 101);
        final var subscriptionDependency =
                new SubscriptionDependencyCandidate(sourceDatasetAddress, targetDatasetAddress);
        final var updateNew = new EntityChangeCandidate(
                101, 2, 0L, instanceRouting("Source", 10), attributes(101), Set.of(subscriptionDependency));
        final var deleteOld = new EntityChangeCandidate(100, 2, 1L, instanceRouting("Source", 10), null, null);
        final var deleteTargetRoot = new EntityChangeCandidate(20, 3, 1L, instanceRouting("Target", 20), null, null);
        session.getLock().lock();
        try {
            final var sourceEntry = session.createSubscriptionEntry(sourceDatasetAddress, SubscriptionMode.IMPLICIT);
            sourceEntry.setMode(SubscriptionMode.EXPLICIT);
            session.createSubscriptionEntry(targetDatasetAddress, SubscriptionMode.IMPLICIT);
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
                0,
                "MetaData",
                null,
                DatasetMetadata.FilterMode.UNFILTERED,
                null,
                false,
                DatasetMetadata.CacheType.NONE,
                false);
        final var requiringDataset = new DatasetMetadata(
                1,
                "Event",
                7,
                DatasetMetadata.FilterMode.UNFILTERED,
                null,
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
    public void subscribe_updatableFilterParameterChangeTransitionsImplicitToExplicitWithoutReplacingSubscription() {
        final var dataset = new DatasetMetadata(
                0,
                "Dataset",
                null,
                DatasetMetadata.FilterMode.PARAMETER_FILTERED,
                DatasetMetadata.FilterParameterMode.UPDATABLE,
                false,
                DatasetMetadata.CacheType.NONE,
                true);
        final var context = new TestSessionContext(new SchemaMetaData("Test", dataset));
        final var manager = createManager(context, mock(ReplicantMessageBroker.class));
        final var session = createOpenSession();
        final var datasetAddress = DatasetAddress.of(0);
        final var sourceDatasetAddress = DatasetAddress.of(1);
        final var originalFilterParameter =
                Json.createObjectBuilder().add("value", "original").build();
        final var newFilterParameter =
                Json.createObjectBuilder().add("value", "new").build();

        final SubscriptionEntry originalEntry;
        session.getLock().lock();
        try {
            session.createSubscriptionEntry(sourceDatasetAddress, SubscriptionMode.EXPLICIT);
            originalEntry = session.createSubscriptionEntry(datasetAddress, SubscriptionMode.IMPLICIT);
            originalEntry.setFilterParameter(originalFilterParameter);
            session.recordDatasetScopedSubscriptionDependency(sourceDatasetAddress, datasetAddress);
        } finally {
            session.getLock().unlock();
        }
        TransactionSynchronizationRegistryUtil.lookup().putResource(ServerConstants.REPLICATION_INVOCATION_KEY, null);

        manager.subscribe(session, 1, List.of(datasetAddress), newFilterParameter);

        session.getLock().lock();
        try {
            assertSame(session.getSubscriptionEntry(datasetAddress), originalEntry);
            assertEquals(originalEntry.getMode(), SubscriptionMode.EXPLICIT);
            assertEquals(originalEntry.getFilterParameter(), newFilterParameter);
            assertEquals(originalEntry.getInwardSubscriptionDependencies(), Set.of(sourceDatasetAddress));
        } finally {
            session.getLock().unlock();
        }
        assertEquals(
                context.getFilterParameterChangeCalls(),
                List.of(new FilterParameterChangeCall(
                        List.of(datasetAddress), originalFilterParameter, newFilterParameter)));
    }

    @Test
    public void subscribe_fixedFilterParameterChangeRejectsUpdateWhileSubscriptionPersists() {
        final var dataset = new DatasetMetadata(
                0,
                "Dataset",
                null,
                DatasetMetadata.FilterMode.PARAMETER_FILTERED,
                DatasetMetadata.FilterParameterMode.FIXED,
                false,
                DatasetMetadata.CacheType.NONE,
                true);
        final var context = new TestSessionContext(new SchemaMetaData("Test", dataset));
        final var manager = createManager(context, mock(ReplicantMessageBroker.class));
        final var session = createOpenSession();
        final var datasetAddress = DatasetAddress.of(0);
        final var originalFilterParameter =
                Json.createObjectBuilder().add("value", "original").build();
        final var newFilterParameter =
                Json.createObjectBuilder().add("value", "new").build();

        TransactionSynchronizationRegistryUtil.lookup().putResource(ServerConstants.REPLICATION_INVOCATION_KEY, null);
        manager.subscribe(session, 1, List.of(datasetAddress), originalFilterParameter);
        TransactionSynchronizationRegistryUtil.lookup().putResource(ServerConstants.REPLICATION_INVOCATION_KEY, null);

        final var error = expectThrows(
                AttemptedToUpdateFixedFilterParameterException.class,
                () -> manager.subscribe(session, 2, List.of(datasetAddress), newFilterParameter));

        assertTrue(Objects.requireNonNull(error.getMessage()).contains("Fixed Filter Parameter"));
        session.getLock().lock();
        try {
            assertEquals(session.getSubscriptionEntry(datasetAddress).getFilterParameter(), originalFilterParameter);
        } finally {
            session.getLock().unlock();
        }
        assertTrue(context.getFilterParameterChangeCalls().isEmpty());
    }

    @Test
    public void subscribe_fixedFilterParameterReplacementUsesNewSubscriptionAfterUnsubscribe() {
        final var dataset = new DatasetMetadata(
                0,
                "Dataset",
                null,
                DatasetMetadata.FilterMode.PARAMETER_FILTERED,
                DatasetMetadata.FilterParameterMode.FIXED,
                false,
                DatasetMetadata.CacheType.NONE,
                true);
        final var context = new TestSessionContext(new SchemaMetaData("Test", dataset));
        final var manager = createManager(context, mock(ReplicantMessageBroker.class));
        final var session = createOpenSession();
        final var datasetAddress = DatasetAddress.of(0);
        final var originalFilterParameter =
                Json.createObjectBuilder().add("value", "original").build();
        final var newFilterParameter =
                Json.createObjectBuilder().add("value", "new").build();

        TransactionSynchronizationRegistryUtil.lookup().putResource(ServerConstants.REPLICATION_INVOCATION_KEY, null);
        manager.subscribe(session, 1, List.of(datasetAddress), originalFilterParameter);
        final SubscriptionEntry originalEntry;
        session.getLock().lock();
        try {
            originalEntry = session.getSubscriptionEntry(datasetAddress);
        } finally {
            session.getLock().unlock();
        }
        final var registry = TransactionSynchronizationRegistryUtil.lookup();
        registry.putResource(ServerConstants.REPLICATION_INVOCATION_KEY, null);
        manager.unsubscribe(session, 2, List.of(datasetAddress));
        registry.putResource(ServerConstants.REPLICATION_INVOCATION_KEY, null);

        manager.subscribe(session, 3, List.of(datasetAddress), newFilterParameter);

        session.getLock().lock();
        try {
            final var replacementEntry = session.getSubscriptionEntry(datasetAddress);
            assertNotSame(replacementEntry, originalEntry);
            assertEquals(replacementEntry.getFilterParameter(), newFilterParameter);
        } finally {
            session.getLock().unlock();
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
                        DatasetMetadata.FilterMode.UNFILTERED,
                        null,
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
                        DatasetMetadata.FilterMode.UNFILTERED,
                        null,
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
                0,
                "Dataset",
                1,
                DatasetMetadata.FilterMode.UNFILTERED,
                null,
                false,
                DatasetMetadata.CacheType.NONE,
                true);
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
            final var entry1 = session.createSubscriptionEntry(datasetAddress1, SubscriptionMode.IMPLICIT);
            final var entry2 = session.createSubscriptionEntry(datasetAddress2, SubscriptionMode.IMPLICIT);
            entry1.setMode(SubscriptionMode.EXPLICIT);
            entry2.setMode(SubscriptionMode.EXPLICIT);
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

        final var sessionChanges = Objects.requireNonNull(EntityChangeCandidateCacheUtil.lookupSessionChanges());
        assertEquals(sessionChanges.getSubscriptionChanges().size(), 2);
        assertEquals(sessionChanges.getSubscriptionChanges().get(0).type(), SubscriptionChange.Type.UNSUBSCRIBE);
        assertEquals(sessionChanges.getSubscriptionChanges().get(1).type(), SubscriptionChange.Type.UNSUBSCRIBE);
    }

    @Test
    public void sendChangeMessage_deleteRootUnsubscribesRootAndDownstream() {
        final var sourceDataset = new DatasetMetadata(
                0,
                "Source",
                1,
                DatasetMetadata.FilterMode.UNFILTERED,
                null,
                false,
                DatasetMetadata.CacheType.NONE,
                true);
        final var targetDataset = new DatasetMetadata(
                1,
                "Target",
                null,
                DatasetMetadata.FilterMode.UNFILTERED,
                null,
                false,
                DatasetMetadata.CacheType.NONE,
                true);
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
        final var deleteMessage = new EntityChangeCandidate(10, 1, 0, routingKeys, null, null);
        final var changeSet = new ChangeSet();
        final var packet = new Packet(false, null, null, null, List.of(deleteMessage), changeSet);

        session.getLock().lock();
        try {
            final var sourceEntry = session.createSubscriptionEntry(sourceDatasetAddress, SubscriptionMode.IMPLICIT);
            sourceEntry.setMode(SubscriptionMode.EXPLICIT);
            session.createSubscriptionEntry(targetDatasetAddress, SubscriptionMode.IMPLICIT);
            session.recordDatasetScopedSubscriptionDependency(sourceDatasetAddress, targetDatasetAddress);

            manager.sendChangeMessage(session, packet);

            assertNull(session.findSubscriptionEntry(sourceDatasetAddress));
            assertNull(session.findSubscriptionEntry(targetDatasetAddress));
        } finally {
            session.getLock().unlock();
        }

        assertEquals(changeSet.getSubscriptionChanges().size(), 2);
        final var actionByDatasetAddress = changeSet.getSubscriptionChanges().stream()
                .collect(java.util.stream.Collectors.toMap(
                        SubscriptionChange::datasetAddress, SubscriptionChange::type));
        assertEquals(
                actionByDatasetAddress.get(sourceDatasetAddress), SubscriptionChange.Type.INVALIDATE_DATASET_ADDRESS);
        assertEquals(actionByDatasetAddress.get(targetDatasetAddress), SubscriptionChange.Type.UNSUBSCRIBE);
    }

    @Test
    public void sendChangeMessage_deleteWithKeyedSubscriptions_unsubscribesConcreteTargetsWithoutMessageLinks() {
        final var sourceDataset = new DatasetMetadata(
                0,
                "Source",
                1,
                DatasetMetadata.FilterMode.PARAMETER_FILTERED,
                DatasetMetadata.FilterParameterMode.FIXED,
                true,
                DatasetMetadata.CacheType.NONE,
                true);
        final var targetDataset = new DatasetMetadata(
                1,
                "Target",
                null,
                DatasetMetadata.FilterMode.PARAMETER_FILTERED,
                DatasetMetadata.FilterParameterMode.FIXED,
                true,
                DatasetMetadata.CacheType.NONE,
                true);
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
        final var deleteMessage = new EntityChangeCandidate(10, 1, 0, routingKeys, null, null);
        final var changeSet = new ChangeSet();
        final var packet = new Packet(false, null, null, null, List.of(deleteMessage), changeSet);

        session.getLock().lock();
        try {
            final var sourceEntryA = session.createSubscriptionEntry(sourceAddressA, SubscriptionMode.IMPLICIT);
            final var sourceEntryB = session.createSubscriptionEntry(sourceAddressB, SubscriptionMode.IMPLICIT);
            sourceEntryA.setMode(SubscriptionMode.EXPLICIT);
            sourceEntryB.setMode(SubscriptionMode.EXPLICIT);
            session.createSubscriptionEntry(targetDatasetAddressA, SubscriptionMode.IMPLICIT);
            session.createSubscriptionEntry(targetDatasetAddressB, SubscriptionMode.IMPLICIT);
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
    public void tryGetCacheEntry_rejectsImplicitlyFilteredDataset() throws Exception {
        final var dataset = new DatasetMetadata(
                0,
                "Source",
                null,
                DatasetMetadata.FilterMode.IMPLICIT,
                null,
                false,
                DatasetMetadata.CacheType.INTERNAL,
                true);
        final var manager = createManager(
                new TestSessionContext(new SchemaMetaData("Test", dataset)), mock(ReplicantMessageBroker.class));
        final var method =
                ReplicantSessionManagerImpl.class.getDeclaredMethod("tryGetCacheEntry", DatasetAddress.class);
        method.setAccessible(true);

        final var exception =
                expectThrows(InvocationTargetException.class, () -> method.invoke(manager, DatasetAddress.of(0)));

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
        private final List<FilterParameterChangeCall> _filterParameterChangeCalls = new ArrayList<>();

        @NonNull
        private final List<Packet> _preSendChangeMessages = new ArrayList<>();

        @NonNull
        private final Set<DatasetAddress> _excludedFilterEntityChangeCandidateAddresses = new HashSet<>();

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
                @Nullable final JsonObject filterParameter) {}

        @Override
        public void preSendChangeMessage(@NonNull final ReplicantSession session, @NonNull final Packet packet) {
            _preSendChangeMessages.add(packet);
        }

        @NonNull
        @Override
        public JsonObject deriveTargetFilterParameter(
                @NonNull final EntityChangeCandidate entityChangeCandidate,
                @NonNull final DatasetAddress sourceDatasetAddress,
                @Nullable final JsonObject sourceFilterParameter,
                @NonNull final DatasetAddressCandidate targetDatasetAddressCandidate) {
            return Json.createObjectBuilder().add("k", "v").build();
        }

        @NonNull
        @Override
        public String deriveTargetDatasetKey(
                @NonNull final EntityChangeCandidate entityChangeCandidate,
                @NonNull final DatasetAddress sourceDatasetAddress,
                @Nullable final JsonObject sourceFilterParameter,
                @NonNull final DatasetAddressTemplate targetDatasetAddressTemplate,
                @Nullable final JsonObject targetFilterParameter) {
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
                @Nullable final JsonObject filterParameter,
                @NonNull final ChangeSet changeSet,
                @NonNull final SubscriptionMode mode) {
            _bulkCollectCalls.add(new BulkCollectCall(datasetAddresses, filterParameter, mode));
            if (null != session) {
                for (final var datasetAddress : datasetAddresses) {
                    final var existing = session.findSubscriptionEntry(datasetAddress);
                    final var entry =
                            null == existing ? session.createSubscriptionEntry(datasetAddress, mode) : existing;
                    entry.setFilterParameter(filterParameter);
                    if (SubscriptionMode.EXPLICIT == mode) {
                        entry.setMode(SubscriptionMode.EXPLICIT);
                    }
                    changeSet.mergeSubscriptionChange(
                            datasetAddress,
                            null == existing ? SubscriptionChange.Type.SUBSCRIBE : SubscriptionChange.Type.UPDATE,
                            filterParameter);
                }
            }
        }

        @Override
        public void collectSubscriptionDataForFilterParameterChange(
                @NonNull final ReplicantSession session,
                @NonNull final List<DatasetAddress> datasetAddresses,
                @NonNull final JsonObject originalFilterParameter,
                @NonNull final JsonObject newFilterParameter,
                @NonNull final ChangeSet changeSet) {
            _filterParameterChangeCalls.add(
                    new FilterParameterChangeCall(datasetAddresses, originalFilterParameter, newFilterParameter));
            for (final var datasetAddress : datasetAddresses) {
                session.setFilterParameter(datasetAddress, newFilterParameter);
                changeSet.mergeSubscriptionChange(datasetAddress, SubscriptionChange.Type.UPDATE, newFilterParameter);
            }
        }

        @Nullable
        @Override
        public EntityChangeCandidate filterEntityChangeCandidate(
                @NonNull final ReplicantSession session,
                @NonNull final DatasetAddress datasetAddress,
                @NonNull final EntityChangeCandidate message) {
            if (_excludedFilterEntityChangeCandidateAddresses.contains(datasetAddress)) {
                return null;
            }
            return message;
        }

        @Override
        public boolean shouldFollowDatasetLink(
                @NonNull final DatasetAddress sourceDatasetAddress,
                @Nullable final JsonObject sourceFilterParameter,
                @NonNull final DatasetAddress targetDatasetAddress,
                @Nullable final JsonObject targetFilterParameter) {
            return _shouldFollowDatasetLink;
        }

        @NonNull
        List<BulkCollectCall> getBulkCollectCalls() {
            return _bulkCollectCalls;
        }

        @NonNull
        List<FilterParameterChangeCall> getFilterParameterChangeCalls() {
            return _filterParameterChangeCalls;
        }

        @NonNull
        List<Packet> getPreSendChangeMessages() {
            return _preSendChangeMessages;
        }

        void excludeFilterEntityChangeCandidateDatasetAddress(@NonNull final DatasetAddress datasetAddress) {
            _excludedFilterEntityChangeCandidateAddresses.add(datasetAddress);
        }

        void setShouldFollowDatasetLink(final boolean shouldFollowDatasetLink) {
            _shouldFollowDatasetLink = shouldFollowDatasetLink;
        }
    }

    private record BulkCollectCall(
            @NonNull List<DatasetAddress> datasetAddresses,
            @Nullable JsonObject filterParameter,
            @NonNull SubscriptionMode mode) {}

    private record FilterParameterChangeCall(
            @NonNull List<DatasetAddress> datasetAddresses,
            @NonNull JsonObject originalFilterParameter,
            @NonNull JsonObject newFilterParameter) {}

    private record DeriveTargetDatasetKeyCall(
            @NonNull EntityChangeCandidate entityChangeCandidate,
            @NonNull DatasetAddress sourceDatasetAddress,
            @Nullable JsonObject sourceFilterParameter,
            @NonNull DatasetAddress targetDatasetAddress,
            @Nullable JsonObject targetFilterParameter) {}
}
