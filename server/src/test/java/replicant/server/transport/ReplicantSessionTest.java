package replicant.server.transport;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

import java.io.IOException;
import java.io.StringReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.json.Json;
import javax.json.JsonObject;
import javax.websocket.CloseReason;
import javax.websocket.RemoteEndpoint;
import javax.websocket.Session;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.testng.annotations.Test;
import replicant.server.ChangeSet;
import replicant.server.DatasetAddress;
import replicant.server.EntityChange;
import replicant.server.EntityChangeCandidateTestUtil;
import replicant.server.SubscriptionChange;
import replicant.server.ValueUtil;
import replicant.shared.Messages;

public class ReplicantSessionTest {
    @Test
    public void principal() {
        final var authorization = mock(ReplicantSessionAuthorization.class);
        final var initialPrincipal = new Object();
        when(authorization.getPrincipal()).thenReturn(initialPrincipal);

        final var session = new ReplicantSession(mock(Session.class), authorization);
        assertSame(session.getPrincipal(), initialPrincipal);

        final var replacementPrincipal = new Object();
        session.setPrincipal(replacementPrincipal);
        assertSame(session.getPrincipal(), replacementPrincipal);

        session.setPrincipal(null);
        assertNull(session.getPrincipal());
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void basicOperation() {
        final var webSocketSession = mock(Session.class);
        final var replicantSessionId = ValueUtil.randomString();
        when(webSocketSession.getId()).thenReturn(replicantSessionId);
        final var session = new ReplicantSession(webSocketSession);
        session.getLock().lock();

        assertEquals(session.getReplicantSessionId(), replicantSessionId);

        assertEquals(getSubscriptions(session).size(), 0);

        final var cd1 = DatasetAddress.of(1, null);

        assertNull(session.findSubscription(cd1));
        assertFalse(session.isSubscriptionPresent(cd1));

        try {
            session.getSubscription(cd1);
            fail("Expected to be unable to get non existent entry");
        } catch (final IllegalStateException ise) {
            assertEquals(ise.getMessage(), "Unable to locate Subscription for Dataset Address 1");
        }

        final var entry = session.createSubscription(cd1, SubscriptionMode.IMPLICIT);

        assertEquals(entry.datasetAddress(), cd1);
        assertEquals(getSubscriptions(session).size(), 1);
        assertEquals(session.findSubscription(cd1), entry);
        assertEquals(session.getSubscription(cd1), entry);
        assertTrue(getSubscriptions(session).containsKey(cd1));
        assertTrue(getSubscriptions(session).containsValue(entry));

        assertTrue(session.deleteSubscription(entry));
        assertFalse(session.deleteSubscription(entry));

        assertNull(session.findSubscription(cd1));
        assertFalse(session.isSubscriptionPresent(cd1));
        assertEquals(getSubscriptions(session).size(), 0);
    }

    @Test
    public void subscriptionIndexesByDatasetAndRoot() {
        final var session = new ReplicantSession(mock(Session.class));
        session.getLock().lock();
        try {
            final var typeA = DatasetAddress.of(1, null);
            final var typeB = DatasetAddress.of(1, null, "fi");
            final var instA = DatasetAddress.of(1, 5);
            final var instB = DatasetAddress.of(1, 5, "fi2");
            final var other = DatasetAddress.of(2, null);

            session.createSubscription(typeA, SubscriptionMode.IMPLICIT);
            session.createSubscription(typeB, SubscriptionMode.IMPLICIT);
            session.createSubscription(instA, SubscriptionMode.IMPLICIT);
            session.createSubscription(instB, SubscriptionMode.IMPLICIT);
            session.createSubscription(other, SubscriptionMode.IMPLICIT);

            assertEquals(session.findSubscriptions(1, null).size(), 2);
            assertEquals(session.findSubscriptions(1, 5).size(), 2);
            assertEquals(session.findSubscriptions(2, null).size(), 1);
            assertTrue(session.findSubscriptions(9, null).isEmpty());
        } finally {
            session.getLock().unlock();
        }
    }

    @Test
    public void requiresLockForSubscriptionAccess() {
        final var session = new ReplicantSession(mock(Session.class));

        final var exception =
                expectThrows(IllegalStateException.class, () -> session.findSubscription(DatasetAddress.of(1)));
        assertEquals(exception.getMessage(), "Expected session to be locked by the current thread");
    }

    @Test
    public void deleteSubscription_updatesIndexes() {
        final var session = new ReplicantSession(mock(Session.class));
        session.getLock().lock();
        try {
            final var entryA = session.createSubscription(DatasetAddress.of(1, 5), SubscriptionMode.IMPLICIT);
            final var entryB = session.createSubscription(DatasetAddress.of(1, 5, "fi"), SubscriptionMode.IMPLICIT);

            assertEquals(session.findSubscriptions(1, 5).size(), 2);

            assertTrue(session.deleteSubscription(entryA));
            assertEquals(session.findSubscriptions(1, 5).size(), 1);

            assertTrue(session.deleteSubscription(entryB));
            assertTrue(session.findSubscriptions(1, 5).isEmpty());
        } finally {
            session.getLock().unlock();
        }
    }

    @Test
    public void setDatasetCacheVersions_resetsAndRemovesNulls() {
        final var session = new ReplicantSession(mock(Session.class));
        session.getLock().lock();
        try {
            final var datasetAddress1 = DatasetAddress.of(1, null);
            final var datasetAddress2 = DatasetAddress.of(2, 5);

            session.setDatasetCacheVersion(datasetAddress1, "v1");
            assertEquals(session.getDatasetCacheVersion(datasetAddress1), "v1");

            final var datasetCacheVersions = new HashMap<DatasetAddress, String>();
            datasetCacheVersions.put(datasetAddress1, null);
            datasetCacheVersions.put(datasetAddress2, "v2");
            session.setDatasetCacheVersions(datasetCacheVersions);

            assertNull(session.getDatasetCacheVersion(datasetAddress1));
            assertEquals(session.getDatasetCacheVersion(datasetAddress2), "v2");
        } finally {
            session.getLock().unlock();
        }
    }

    @Test
    public void queuePacket_prioritizesSubscriptionPackets() {
        final var session = new ReplicantSession(mock(Session.class));
        final var normal = new Packet(false, null, null, null, Collections.emptyList(), new ChangeSet());
        final var sub1 = new Packet(true, null, null, null, Collections.emptyList(), new ChangeSet());
        final var sub2 = new Packet(true, null, null, null, Collections.emptyList(), new ChangeSet());

        session.queuePacket(normal);
        session.queuePacket(sub1);
        session.queuePacket(sub2);

        assertSame(session.popPendingPacket(), sub1);
        assertSame(session.popPendingPacket(), sub2);
        assertSame(session.popPendingPacket(), normal);
        assertNull(session.popPendingPacket());
    }

    @Test
    public void sendPacket_emitsChangeSet() throws IOException {
        final var webSocketSession = mock(Session.class);
        final var remote = mock(RemoteEndpoint.Basic.class);
        when(webSocketSession.isOpen()).thenReturn(true);
        when(webSocketSession.getBasicRemote()).thenReturn(remote);

        final var session = new ReplicantSession(webSocketSession);
        session.getLock().lock();
        try {
            final var entityChangeCandidate =
                    EntityChangeCandidateTestUtil.createEntityChangeCandidate(1, 2, 0, "r1", "r2", "a1", "a2");
            final var change = new EntityChange(entityChangeCandidate, DatasetAddress.of(5, null));
            final var changeSet = new ChangeSet();
            changeSet.merge(change);

            session.sendChangeSet(7, null, null, changeSet);

            final var captor = org.mockito.ArgumentCaptor.forClass(String.class);
            verify(remote).sendText(captor.capture());

            final var payload =
                    Json.createReader(new StringReader(captor.getValue())).readObject();
            assertEquals(payload.getString(Messages.Common.TYPE), Messages.S2C_Type.CHANGE_SET);
            assertEquals(payload.getInt(Messages.Common.REQUEST_ID), 7);
            assertEquals(payload.getJsonArray(Messages.ChangeSet.ENTITY_CHANGES).size(), 1);
        } finally {
            session.getLock().unlock();
        }
    }

    @Test
    public void sendPacket_requiresLock() {
        final var session = new ReplicantSession(mock(Session.class));
        final var changeSet = new ChangeSet();

        final var exception =
                expectThrows(IllegalStateException.class, () -> session.sendChangeSet(null, null, null, changeSet));
        assertEquals(exception.getMessage(), "Expected session to be locked by the current thread");
    }

    @Test
    public void closeDueToInterrupt_closesSession() throws IOException {
        final var webSocketSession = mock(Session.class);
        when(webSocketSession.isOpen()).thenReturn(true);
        final var session = new ReplicantSession(webSocketSession);

        session.closeDueToInterrupt();

        final var captor = org.mockito.ArgumentCaptor.forClass(CloseReason.class);
        verify(webSocketSession).close(captor.capture());
        assertEquals(captor.getValue().getReasonPhrase(), "Action interrupted");
    }

    @Test
    public void close_noopWhenAlreadyClosed() throws IOException {
        final var webSocketSession = mock(Session.class);
        when(webSocketSession.isOpen()).thenReturn(false);
        final var session = new ReplicantSession(webSocketSession);

        session.close();

        verify(webSocketSession, never()).close();
    }

    @Test
    public void pingTransport_sendsPingWhenOpen() throws IOException {
        final var webSocketSession = mock(Session.class);
        final var remote = mock(RemoteEndpoint.Basic.class);
        when(webSocketSession.isOpen()).thenReturn(true);
        when(webSocketSession.getBasicRemote()).thenReturn(remote);

        final var session = new ReplicantSession(webSocketSession);

        session.pingTransport();

        verify(remote).sendPing(null);
    }

    @Test
    public void pingTransport_noopWhenClosed() {
        final var webSocketSession = mock(Session.class);
        when(webSocketSession.isOpen()).thenReturn(false);

        final var session = new ReplicantSession(webSocketSession);

        session.pingTransport();

        verify(webSocketSession, never()).getBasicRemote();
    }

    @Test
    public void datasetCacheVersions() {
        final var session = new ReplicantSession(mock(Session.class));
        session.getLock().lock();
        try {
            final var cd1 = DatasetAddress.of(1, null);

            assertNull(session.getDatasetCacheVersion(cd1));

            session.setDatasetCacheVersion(cd1, "X");

            assertEquals(session.getDatasetCacheVersion(cd1), "X");
        } finally {
            session.getLock().unlock();
        }
    }

    @Test
    public void recordSubscription_addsEntryAndAction() {
        final var session = new ReplicantSession(mock(Session.class));
        final var changeSet = new ChangeSet();
        final var datasetAddress = DatasetAddress.of(1, 2);

        session.getLock().lock();
        try {
            final JsonObject filterParameter =
                    Json.createObjectBuilder().add("k", "v").build();
            session.recordSubscription(changeSet, datasetAddress, filterParameter, SubscriptionMode.EXPLICIT);

            final var entry = session.getSubscription(datasetAddress);
            assertEquals(entry.getMode(), SubscriptionMode.EXPLICIT);
            assertEquals(entry.getFilterParameter(), filterParameter);
        } finally {
            session.getLock().unlock();
        }

        assertEquals(changeSet.getSubscriptionChanges().size(), 1);
        final var action = changeSet.getSubscriptionChanges().get(0);
        assertEquals(action.datasetAddress(), datasetAddress);
        assertEquals(action.type(), SubscriptionChange.Type.SUBSCRIBE);
        assertNotNull(action.filterParameter());
        assertEquals(Objects.requireNonNull(action.filterParameter()).getString("k"), "v");
    }

    @Test
    public void recordSubscription_updatesEntryAndCanPromoteToExplicitMode() {
        final var session = new ReplicantSession(mock(Session.class));
        final var changeSet = new ChangeSet();
        final var datasetAddress = DatasetAddress.of(1, 2);

        session.getLock().lock();
        try {
            final var entry = session.createSubscription(datasetAddress, SubscriptionMode.IMPLICIT);
            entry.setFilterParameter(
                    Json.createObjectBuilder().add("old", "value").build());
            assertEquals(entry.getMode(), SubscriptionMode.IMPLICIT);

            final var newFilterParameter =
                    Json.createObjectBuilder().add("k", "v").build();
            session.recordSubscription(changeSet, datasetAddress, newFilterParameter, SubscriptionMode.EXPLICIT);

            assertEquals(entry.getMode(), SubscriptionMode.EXPLICIT);
            assertEquals(entry.getFilterParameter(), newFilterParameter);
        } finally {
            session.getLock().unlock();
        }

        assertEquals(changeSet.getSubscriptionChanges().size(), 1);
        assertEquals(changeSet.getSubscriptionChanges().get(0).type(), SubscriptionChange.Type.UPDATE);
    }

    @Test
    public void recordSubscriptions_recordsEachAddress() {
        final var session = new ReplicantSession(mock(Session.class));
        final var changeSet = new ChangeSet();
        final var datasetAddresses = List.of(DatasetAddress.of(1, 2), DatasetAddress.of(1, 3));

        session.getLock().lock();
        try {
            session.recordSubscriptions(changeSet, datasetAddresses, null, SubscriptionMode.IMPLICIT);

            assertEquals(session.findSubscriptions(1, 2).size(), 1);
            assertEquals(session.findSubscriptions(1, 3).size(), 1);
        } finally {
            session.getLock().unlock();
        }

        assertEquals(changeSet.getSubscriptionChanges().size(), 2);
        assertEquals(changeSet.getSubscriptionChanges().get(0).type(), SubscriptionChange.Type.SUBSCRIBE);
        assertEquals(changeSet.getSubscriptionChanges().get(1).type(), SubscriptionChange.Type.SUBSCRIBE);
    }

    @Test
    public void recordDatasetScopedSubscriptionDependency_recordsDependency() {
        final var session = new ReplicantSession(mock(Session.class));
        final var sourceDatasetAddress = DatasetAddress.of(1, 2);
        final var targetDatasetAddress = DatasetAddress.of(2);

        session.getLock().lock();
        try {
            session.createSubscription(sourceDatasetAddress, SubscriptionMode.IMPLICIT);
            session.createSubscription(targetDatasetAddress, SubscriptionMode.IMPLICIT);

            session.recordDatasetScopedSubscriptionDependency(sourceDatasetAddress, targetDatasetAddress);

            assertTrue(session.getSubscription(sourceDatasetAddress)
                    .getOutwardSubscriptionDependencies()
                    .contains(targetDatasetAddress));
            assertTrue(session.getSubscription(targetDatasetAddress)
                    .getInwardSubscriptionDependencies()
                    .contains(sourceDatasetAddress));
        } finally {
            session.getLock().unlock();
        }
    }

    @Test
    public void recordDatasetScopedSubscriptionDependency_rejectsInstanceDatasetTarget() {
        final var session = new ReplicantSession(mock(Session.class));

        session.getLock().lock();
        try {
            session.createSubscription(DatasetAddress.of(1, 2), SubscriptionMode.IMPLICIT);
            session.createSubscription(DatasetAddress.of(2, 3), SubscriptionMode.IMPLICIT);

            expectThrows(
                    AssertionError.class,
                    () -> session.recordDatasetScopedSubscriptionDependency(
                            DatasetAddress.of(1, 2), DatasetAddress.of(2, 3)));
        } finally {
            session.getLock().unlock();
        }
    }

    @Test
    public void recordEntityScopedSubscriptionDependency_recordsDependency() {
        final var session = new ReplicantSession(mock(Session.class));
        final var sourceDatasetAddress = DatasetAddress.of(1, 2);
        final var targetDatasetAddress = DatasetAddress.of(2);

        session.getLock().lock();
        try {
            session.createSubscription(sourceDatasetAddress, SubscriptionMode.IMPLICIT);
            session.createSubscription(targetDatasetAddress, SubscriptionMode.IMPLICIT);

            session.recordEntityScopedSubscriptionDependency(sourceDatasetAddress, targetDatasetAddress, 7, 11);

            final var sourceSubscription = session.getSubscription(sourceDatasetAddress);
            assertTrue(sourceSubscription.getOutwardSubscriptionDependencies().contains(targetDatasetAddress));
            assertEquals(
                    sourceSubscription.getOwnedOutwardSubscriptionDependencies(
                            SubscriptionDependencyOwner.entity(7, 11)),
                    Set.of(targetDatasetAddress));
            assertTrue(session.getSubscription(targetDatasetAddress)
                    .getInwardSubscriptionDependencies()
                    .contains(sourceDatasetAddress));
        } finally {
            session.getLock().unlock();
        }
    }

    @Test
    public void recordEntityScopedSubscriptionDependency_allowsInstanceDatasetTarget() {
        final var session = new ReplicantSession(mock(Session.class));
        final var sourceDatasetAddress = DatasetAddress.of(1, 2);
        final var targetDatasetAddress = DatasetAddress.of(2, 3);

        session.getLock().lock();
        try {
            session.createSubscription(sourceDatasetAddress, SubscriptionMode.IMPLICIT);
            session.createSubscription(targetDatasetAddress, SubscriptionMode.IMPLICIT);

            session.recordEntityScopedSubscriptionDependency(sourceDatasetAddress, targetDatasetAddress, 7, 11);

            assertTrue(session.getSubscription(sourceDatasetAddress)
                    .getOutwardSubscriptionDependencies()
                    .contains(targetDatasetAddress));
            assertEquals(
                    session.getSubscription(sourceDatasetAddress)
                            .getOwnedOutwardSubscriptionDependencies(SubscriptionDependencyOwner.entity(7, 11)),
                    Set.of(targetDatasetAddress));
            assertTrue(session.getSubscription(targetDatasetAddress)
                    .getInwardSubscriptionDependencies()
                    .contains(sourceDatasetAddress));
        } finally {
            session.getLock().unlock();
        }
    }

    @Test
    public void entityOwnedSubscriptionDependencies_requireLastOwnerBeforeDependencyRemoval() {
        final var session = new ReplicantSession(mock(Session.class));
        final var changeSet = new ChangeSet();
        final var sourceDatasetAddress = DatasetAddress.of(1, 2);
        final var targetDatasetAddress = DatasetAddress.of(2, 3);

        session.getLock().lock();
        try {
            final var sourceSubscription = session.createSubscription(sourceDatasetAddress, SubscriptionMode.IMPLICIT);
            final var targetSubscription = session.createSubscription(targetDatasetAddress, SubscriptionMode.IMPLICIT);
            targetSubscription.setMode(SubscriptionMode.EXPLICIT);

            session.recordSubscriptionDependency(
                    sourceSubscription, targetSubscription, SubscriptionDependencyOwner.entity(7, 11));
            session.recordSubscriptionDependency(
                    sourceSubscription, targetSubscription, SubscriptionDependencyOwner.entity(7, 12));

            assertTrue(sourceSubscription.getOutwardSubscriptionDependencies().contains(targetDatasetAddress));
            assertEquals(
                    sourceSubscription.getOwnedOutwardSubscriptionDependencies(
                            SubscriptionDependencyOwner.entity(7, 11)),
                    Set.of(targetDatasetAddress));
            assertEquals(
                    sourceSubscription.getOwnedOutwardSubscriptionDependencies(
                            SubscriptionDependencyOwner.entity(7, 12)),
                    Set.of(targetDatasetAddress));

            session.removeDownstreamSubscriptionDependency(
                    sourceSubscription, SubscriptionDependencyOwner.entity(7, 11), targetDatasetAddress, changeSet);

            assertTrue(sourceSubscription.getOutwardSubscriptionDependencies().contains(targetDatasetAddress));
            assertTrue(targetSubscription.getInwardSubscriptionDependencies().contains(sourceDatasetAddress));
            assertNotNull(session.findSubscription(targetDatasetAddress));

            session.removeDownstreamSubscriptionDependency(
                    sourceSubscription, SubscriptionDependencyOwner.entity(7, 12), targetDatasetAddress, changeSet);

            assertTrue(sourceSubscription.getOutwardSubscriptionDependencies().isEmpty());
            assertTrue(targetSubscription.getInwardSubscriptionDependencies().isEmpty());
            assertNotNull(session.findSubscription(targetDatasetAddress));
        } finally {
            session.getLock().unlock();
        }

        assertTrue(changeSet.getSubscriptionChanges().isEmpty());
    }

    @Test
    public void getFilterAndSetFilter_roundTrip() {
        final var session = new ReplicantSession(mock(Session.class));
        final var datasetAddress = DatasetAddress.of(1, 2);

        session.getLock().lock();
        try {
            session.createSubscription(datasetAddress, SubscriptionMode.IMPLICIT);
            final JsonObject filterParameter =
                    Json.createObjectBuilder().add("k", "v").build();
            session.setFilterParameter(datasetAddress, filterParameter);
            assertEquals(session.getFilterParameter(datasetAddress), filterParameter);

            session.setFilterParameter(datasetAddress, null);
            assertNull(session.getFilterParameter(datasetAddress));
        } finally {
            session.getLock().unlock();
        }
    }

    @Test
    public void unsubscribe_removesExistingAndIgnoresMissing() {
        final var session = new ReplicantSession(mock(Session.class));
        final var changeSet = new ChangeSet();
        final var datasetAddress = DatasetAddress.of(1, 2);

        session.getLock().lock();
        try {
            final var entry = session.createSubscription(datasetAddress, SubscriptionMode.IMPLICIT);
            entry.setMode(SubscriptionMode.EXPLICIT);

            session.bulkUnsubscribe(Collections.singletonList(datasetAddress), changeSet);
            assertNull(session.findSubscription(datasetAddress));

            session.bulkUnsubscribe(Collections.singletonList(datasetAddress), changeSet);
        } finally {
            session.getLock().unlock();
        }

        assertEquals(changeSet.getSubscriptionChanges().size(), 1);
        assertEquals(changeSet.getSubscriptionChanges().get(0).type(), SubscriptionChange.Type.UNSUBSCRIBE);
    }

    @Test
    public void bulkUnsubscribe_removesEachSubscribedAddress() {
        final var session = new ReplicantSession(mock(Session.class));
        final var changeSet = new ChangeSet();
        final var datasetAddress1 = DatasetAddress.of(1, 1);
        final var datasetAddress2 = DatasetAddress.of(1, 2);

        session.getLock().lock();
        try {
            final var entry1 = session.createSubscription(datasetAddress1, SubscriptionMode.IMPLICIT);
            final var entry2 = session.createSubscription(datasetAddress2, SubscriptionMode.IMPLICIT);
            entry1.setMode(SubscriptionMode.EXPLICIT);
            entry2.setMode(SubscriptionMode.EXPLICIT);

            session.bulkUnsubscribe(List.of(datasetAddress1, datasetAddress2, DatasetAddress.of(1, 3)), changeSet);

            assertNull(session.findSubscription(datasetAddress1));
            assertNull(session.findSubscription(datasetAddress2));
        } finally {
            session.getLock().unlock();
        }

        assertEquals(changeSet.getSubscriptionChanges().size(), 2);
        assertEquals(changeSet.getSubscriptionChanges().get(0).type(), SubscriptionChange.Type.UNSUBSCRIBE);
        assertEquals(changeSet.getSubscriptionChanges().get(1).type(), SubscriptionChange.Type.UNSUBSCRIBE);
    }

    @Test
    public void performUnsubscribe_onlyRemovesWhenEntryCanUnsubscribe() {
        final var session = new ReplicantSession(mock(Session.class));
        final var changeSet = new ChangeSet();
        final var datasetAddress = DatasetAddress.of(1, 2);

        session.getLock().lock();
        try {
            final var entry = session.createSubscription(datasetAddress, SubscriptionMode.IMPLICIT);
            entry.setMode(SubscriptionMode.EXPLICIT);

            session.performUnsubscribe(entry, false, false, changeSet);
            assertNotNull(session.findSubscription(datasetAddress));

            session.performUnsubscribe(entry, true, false, changeSet);
            assertNull(session.findSubscription(datasetAddress));
        } finally {
            session.getLock().unlock();
        }

        assertEquals(changeSet.getSubscriptionChanges().size(), 1);
        assertEquals(changeSet.getSubscriptionChanges().get(0).type(), SubscriptionChange.Type.UNSUBSCRIBE);
    }

    @Test
    public void performUnsubscribe_transitionsExplicitToImplicitUntilFinalDependencyIsRemoved() {
        final var session = new ReplicantSession(mock(Session.class));
        final var changeSet = new ChangeSet();
        final var sourceDatasetAddress = DatasetAddress.of(1, 2);
        final var targetDatasetAddress = DatasetAddress.of(2);
        final var filterParameter =
                Json.createObjectBuilder().add("filter", "value").build();

        session.getLock().lock();
        try {
            session.createSubscription(sourceDatasetAddress, SubscriptionMode.EXPLICIT);
            final var targetSubscription = session.createSubscription(targetDatasetAddress, SubscriptionMode.EXPLICIT);
            targetSubscription.setFilterParameter(filterParameter);
            session.recordDatasetScopedSubscriptionDependency(sourceDatasetAddress, targetDatasetAddress);

            session.performUnsubscribe(targetSubscription, true, false, changeSet);

            assertSame(session.findSubscription(targetDatasetAddress), targetSubscription);
            assertEquals(targetSubscription.getMode(), SubscriptionMode.IMPLICIT);
            assertEquals(targetSubscription.getFilterParameter(), filterParameter);
            assertEquals(targetSubscription.getInwardSubscriptionDependencies(), Set.of(sourceDatasetAddress));
            assertTrue(changeSet.getSubscriptionChanges().isEmpty());

            session.removeDownstreamSubscriptionDependency(sourceDatasetAddress, targetDatasetAddress, changeSet);

            assertNull(session.findSubscription(targetDatasetAddress));
        } finally {
            session.getLock().unlock();
        }

        assertEquals(changeSet.getSubscriptionChanges().size(), 1);
        final var action = changeSet.getSubscriptionChanges().get(0);
        assertEquals(action.datasetAddress(), targetDatasetAddress);
        assertEquals(action.type(), SubscriptionChange.Type.UNSUBSCRIBE);
    }

    @Test
    public void performUnsubscribe_removesOrphanedImplicitSubscription() {
        final var session = new ReplicantSession(mock(Session.class));
        final var changeSet = new ChangeSet();
        final var datasetAddress = DatasetAddress.of(1, 2);

        session.getLock().lock();
        try {
            final var entry = session.createSubscription(datasetAddress, SubscriptionMode.IMPLICIT);
            assertTrue(entry.canUnsubscribe());

            session.performUnsubscribe(entry, false, false, changeSet);

            assertNull(session.findSubscription(datasetAddress));
        } finally {
            session.getLock().unlock();
        }

        assertEquals(changeSet.getSubscriptionChanges().size(), 1);
        assertEquals(changeSet.getSubscriptionChanges().get(0).type(), SubscriptionChange.Type.UNSUBSCRIBE);
    }

    @Test
    public void performUnsubscribe_deleteUsesDeleteAction() {
        final var session = new ReplicantSession(mock(Session.class));
        final var changeSet = new ChangeSet();
        final var datasetAddress = DatasetAddress.of(1, 2);

        session.getLock().lock();
        try {
            final var entry = session.createSubscription(datasetAddress, SubscriptionMode.IMPLICIT);
            entry.setMode(SubscriptionMode.EXPLICIT);

            session.performUnsubscribe(entry, true, true, changeSet);
            assertNull(session.findSubscription(datasetAddress));
        } finally {
            session.getLock().unlock();
        }

        assertEquals(changeSet.getSubscriptionChanges().size(), 1);
        assertEquals(
                changeSet.getSubscriptionChanges().get(0).type(), SubscriptionChange.Type.INVALIDATE_DATASET_ADDRESS);
    }

    @Test
    public void performUnsubscribe_cascadesDownstreamSubscriptions() {
        final var session = new ReplicantSession(mock(Session.class));
        final var changeSet = new ChangeSet();
        final var a = DatasetAddress.of(1, 1);
        final var b = DatasetAddress.of(2);
        final var c = DatasetAddress.of(3);

        session.getLock().lock();
        try {
            final var entryA = session.createSubscription(a, SubscriptionMode.IMPLICIT);
            session.createSubscription(b, SubscriptionMode.IMPLICIT);
            session.createSubscription(c, SubscriptionMode.IMPLICIT);
            session.recordDatasetScopedSubscriptionDependency(a, b);
            session.recordDatasetScopedSubscriptionDependency(b, c);

            session.performUnsubscribe(entryA, false, false, changeSet);

            assertNull(session.findSubscription(a));
            assertNull(session.findSubscription(b));
            assertNull(session.findSubscription(c));
        } finally {
            session.getLock().unlock();
        }

        assertEquals(changeSet.getSubscriptionChanges().size(), 3);
        final var actions = changeSet.getSubscriptionChanges().stream()
                .map(SubscriptionChange::datasetAddress)
                .collect(java.util.stream.Collectors.toSet());
        assertEquals(actions, Set.of(a, b, c));
    }

    @Test
    public void removeDownstreamSubscriptionDependency_keepsExplicitModeDownstream() {
        final var session = new ReplicantSession(mock(Session.class));
        final var changeSet = new ChangeSet();
        final var upstream = DatasetAddress.of(1, 1);
        final var downstream = DatasetAddress.of(2);

        session.getLock().lock();
        try {
            session.createSubscription(upstream, SubscriptionMode.IMPLICIT);
            final var downstreamEntry = session.createSubscription(downstream, SubscriptionMode.IMPLICIT);
            downstreamEntry.setMode(SubscriptionMode.EXPLICIT);
            session.recordDatasetScopedSubscriptionDependency(upstream, downstream);

            session.removeDownstreamSubscriptionDependency(upstream, downstream, changeSet);

            assertTrue(session.getSubscription(upstream)
                    .getOutwardSubscriptionDependencies()
                    .isEmpty());
            assertTrue(session.getSubscription(downstream)
                    .getInwardSubscriptionDependencies()
                    .isEmpty());
            assertNotNull(session.findSubscription(downstream));
        } finally {
            session.getLock().unlock();
        }

        assertTrue(changeSet.getSubscriptionChanges().isEmpty());
    }

    @SuppressWarnings("DataFlowIssue")
    @NonNull
    private Map<DatasetAddress, Subscription> getSubscriptions(final ReplicantSession session) {
        return Objects.requireNonNull(getField(session, "_subscriptions"));
    }

    @SuppressWarnings({"SameParameterValue", "unchecked"})
    @Nullable
    private <T> T getField(@NonNull final ReplicantSession session, @NonNull final String fieldName) {
        try {
            final var field = ReplicantSession.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            return (T) field.get(session);
        } catch (final Throwable t) {
            throw new AssertionError(t);
        }
    }
}
