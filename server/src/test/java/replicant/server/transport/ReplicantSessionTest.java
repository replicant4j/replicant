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
import replicant.server.Change;
import replicant.server.ChangeSet;
import replicant.server.ChannelAction;
import replicant.server.DatasetAddress;
import replicant.server.MessageTestUtil;
import replicant.server.ValueUtil;
import replicant.shared.Messages;

public class ReplicantSessionTest {
    @SuppressWarnings("ConstantConditions")
    @Test
    public void basicOperation() {
        final var webSocketSession = mock(Session.class);
        final var sessionId = ValueUtil.randomString();
        when(webSocketSession.getId()).thenReturn(sessionId);
        final var session = new ReplicantSession(webSocketSession);
        session.getLock().lock();

        assertEquals(session.getId(), sessionId);

        assertEquals(getSubscriptions(session).size(), 0);

        final var cd1 = DatasetAddress.of(1, null);

        assertNull(session.findSubscriptionEntry(cd1));
        assertFalse(session.isSubscriptionEntryPresent(cd1));

        try {
            session.getSubscriptionEntry(cd1);
            fail("Expected to be unable to get non existent entry");
        } catch (final IllegalStateException ise) {
            assertEquals(ise.getMessage(), "Unable to locate subscription entry for Dataset Address 1");
        }

        final var entry = session.createSubscriptionEntry(cd1);

        assertEquals(entry.datasetAddress(), cd1);
        assertEquals(getSubscriptions(session).size(), 1);
        assertEquals(session.findSubscriptionEntry(cd1), entry);
        assertEquals(session.getSubscriptionEntry(cd1), entry);
        assertTrue(getSubscriptions(session).containsKey(cd1));
        assertTrue(getSubscriptions(session).containsValue(entry));

        assertTrue(session.deleteSubscriptionEntry(entry));
        assertFalse(session.deleteSubscriptionEntry(entry));

        assertNull(session.findSubscriptionEntry(cd1));
        assertFalse(session.isSubscriptionEntryPresent(cd1));
        assertEquals(getSubscriptions(session).size(), 0);
    }

    @Test
    public void subscriptionIndexesByChannelAndRoot() {
        final var session = new ReplicantSession(mock(Session.class));
        session.getLock().lock();
        try {
            final var typeA = DatasetAddress.of(1, null);
            final var typeB = DatasetAddress.of(1, null, "fi");
            final var instA = DatasetAddress.of(1, 5);
            final var instB = DatasetAddress.of(1, 5, "fi2");
            final var other = DatasetAddress.of(2, null);

            session.createSubscriptionEntry(typeA);
            session.createSubscriptionEntry(typeB);
            session.createSubscriptionEntry(instA);
            session.createSubscriptionEntry(instB);
            session.createSubscriptionEntry(other);

            assertEquals(session.findSubscriptionEntries(1, null).size(), 2);
            assertEquals(session.findSubscriptionEntries(1, 5).size(), 2);
            assertEquals(session.findSubscriptionEntries(2, null).size(), 1);
            assertTrue(session.findSubscriptionEntries(9, null).isEmpty());
        } finally {
            session.getLock().unlock();
        }
    }

    @Test
    public void requiresLockForSubscriptionAccess() {
        final var session = new ReplicantSession(mock(Session.class));

        final var exception =
                expectThrows(IllegalStateException.class, () -> session.findSubscriptionEntry(DatasetAddress.of(1)));
        assertEquals(exception.getMessage(), "Expected session to be locked by the current thread");
    }

    @Test
    public void deleteSubscriptionEntry_updatesIndexes() {
        final var session = new ReplicantSession(mock(Session.class));
        session.getLock().lock();
        try {
            final var entryA = session.createSubscriptionEntry(DatasetAddress.of(1, 5));
            final var entryB = session.createSubscriptionEntry(DatasetAddress.of(1, 5, "fi"));

            assertEquals(session.findSubscriptionEntries(1, 5).size(), 2);

            assertTrue(session.deleteSubscriptionEntry(entryA));
            assertEquals(session.findSubscriptionEntries(1, 5).size(), 1);

            assertTrue(session.deleteSubscriptionEntry(entryB));
            assertTrue(session.findSubscriptionEntries(1, 5).isEmpty());
        } finally {
            session.getLock().unlock();
        }
    }

    @Test
    public void setETags_resetsAndRemovesNulls() {
        final var session = new ReplicantSession(mock(Session.class));
        session.getLock().lock();
        try {
            final var datasetAddress1 = DatasetAddress.of(1, null);
            final var datasetAddress2 = DatasetAddress.of(2, 5);

            session.setETag(datasetAddress1, "v1");
            assertEquals(session.getETag(datasetAddress1), "v1");

            final var eTags = new HashMap<DatasetAddress, String>();
            eTags.put(datasetAddress1, null);
            eTags.put(datasetAddress2, "v2");
            session.setETags(eTags);

            assertNull(session.getETag(datasetAddress1));
            assertEquals(session.getETag(datasetAddress2), "v2");
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
            final var message = MessageTestUtil.createMessage(1, 2, 0, "r1", "r2", "a1", "a2");
            final var change = new Change(message, DatasetAddress.of(5, null));
            final var changeSet = new ChangeSet();
            changeSet.merge(change);

            session.sendPacket(7, null, null, changeSet);

            final var captor = org.mockito.ArgumentCaptor.forClass(String.class);
            verify(remote).sendText(captor.capture());

            final var payload =
                    Json.createReader(new StringReader(captor.getValue())).readObject();
            assertEquals(payload.getString(Messages.Common.TYPE), Messages.S2C_Type.UPDATE);
            assertEquals(payload.getInt(Messages.Common.REQUEST_ID), 7);
            assertEquals(payload.getJsonArray(Messages.Update.CHANGES).size(), 1);
        } finally {
            session.getLock().unlock();
        }
    }

    @Test
    public void sendPacket_requiresLock() {
        final var session = new ReplicantSession(mock(Session.class));
        final var changeSet = new ChangeSet();

        final var exception =
                expectThrows(IllegalStateException.class, () -> session.sendPacket(null, null, null, changeSet));
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
    public void cacheKeys() {
        final var session = new ReplicantSession(mock(Session.class));
        session.getLock().lock();
        try {
            final var cd1 = DatasetAddress.of(1, null);

            assertNull(session.getETag(cd1));

            session.setETag(cd1, "X");

            assertEquals(session.getETag(cd1), "X");
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
            final JsonObject filter = Json.createObjectBuilder().add("k", "v").build();
            session.recordSubscription(changeSet, datasetAddress, filter, true);

            final var entry = session.getSubscriptionEntry(datasetAddress);
            assertTrue(entry.isExplicitlySubscribed());
            assertEquals(entry.getFilter(), filter);
        } finally {
            session.getLock().unlock();
        }

        assertEquals(changeSet.getChannelActions().size(), 1);
        final var action = changeSet.getChannelActions().get(0);
        assertEquals(action.datasetAddress(), datasetAddress);
        assertEquals(action.action(), ChannelAction.Action.ADD);
        assertNotNull(action.filter());
        assertEquals(Objects.requireNonNull(action.filter()).getString("k"), "v");
    }

    @Test
    public void recordSubscription_updatesEntryAndCanPromoteExplicitSubscribe() {
        final var session = new ReplicantSession(mock(Session.class));
        final var changeSet = new ChangeSet();
        final var datasetAddress = DatasetAddress.of(1, 2);

        session.getLock().lock();
        try {
            final var entry = session.createSubscriptionEntry(datasetAddress);
            entry.setFilter(Json.createObjectBuilder().add("old", "value").build());
            assertFalse(entry.isExplicitlySubscribed());

            final var newFilter = Json.createObjectBuilder().add("k", "v").build();
            session.recordSubscription(changeSet, datasetAddress, newFilter, true);

            assertTrue(entry.isExplicitlySubscribed());
            assertEquals(entry.getFilter(), newFilter);
        } finally {
            session.getLock().unlock();
        }

        assertEquals(changeSet.getChannelActions().size(), 1);
        assertEquals(changeSet.getChannelActions().get(0).action(), ChannelAction.Action.UPDATE);
    }

    @Test
    public void recordSubscriptions_recordsEachAddress() {
        final var session = new ReplicantSession(mock(Session.class));
        final var changeSet = new ChangeSet();
        final var datasetAddresses = List.of(DatasetAddress.of(1, 2), DatasetAddress.of(1, 3));

        session.getLock().lock();
        try {
            session.recordSubscriptions(changeSet, datasetAddresses, null, false);

            assertEquals(session.findSubscriptionEntries(1, 2).size(), 1);
            assertEquals(session.findSubscriptionEntries(1, 3).size(), 1);
        } finally {
            session.getLock().unlock();
        }

        assertEquals(changeSet.getChannelActions().size(), 2);
        assertEquals(changeSet.getChannelActions().get(0).action(), ChannelAction.Action.ADD);
        assertEquals(changeSet.getChannelActions().get(1).action(), ChannelAction.Action.ADD);
    }

    @Test
    public void recordSubscription_rejectsPartialAddress() {
        final var session = new ReplicantSession(mock(Session.class));
        final var changeSet = new ChangeSet();

        session.getLock().lock();
        try {
            expectThrows(
                    AssertionError.class,
                    () -> session.recordSubscription(changeSet, DatasetAddress.partial(1, 2), null, false));
        } finally {
            session.getLock().unlock();
        }
    }

    @Test
    public void recordGraphScopedGraphLink_linksEntries() {
        final var session = new ReplicantSession(mock(Session.class));
        final var sourceDatasetAddress = DatasetAddress.of(1, 2);
        final var targetDatasetAddress = DatasetAddress.of(2);

        session.getLock().lock();
        try {
            session.createSubscriptionEntry(sourceDatasetAddress);
            session.createSubscriptionEntry(targetDatasetAddress);

            session.recordGraphScopedGraphLink(sourceDatasetAddress, targetDatasetAddress);

            assertTrue(session.getSubscriptionEntry(sourceDatasetAddress)
                    .getOutwardSubscriptions()
                    .contains(targetDatasetAddress));
            assertTrue(session.getSubscriptionEntry(targetDatasetAddress)
                    .getInwardSubscriptions()
                    .contains(sourceDatasetAddress));
        } finally {
            session.getLock().unlock();
        }
    }

    @Test
    public void recordGraphScopedGraphLink_rejectsPartialAddress() {
        final var session = new ReplicantSession(mock(Session.class));

        session.getLock().lock();
        try {
            session.createSubscriptionEntry(DatasetAddress.of(1, 2, "fi"));
            session.createSubscriptionEntry(DatasetAddress.of(2));

            expectThrows(
                    AssertionError.class,
                    () -> session.recordGraphScopedGraphLink(DatasetAddress.partial(1, 2), DatasetAddress.of(2)));
        } finally {
            session.getLock().unlock();
        }
    }

    @Test
    public void recordGraphScopedGraphLink_rejectsInstanceGraphTarget() {
        final var session = new ReplicantSession(mock(Session.class));

        session.getLock().lock();
        try {
            session.createSubscriptionEntry(DatasetAddress.of(1, 2));
            session.createSubscriptionEntry(DatasetAddress.of(2, 3));

            expectThrows(
                    AssertionError.class,
                    () -> session.recordGraphScopedGraphLink(DatasetAddress.of(1, 2), DatasetAddress.of(2, 3)));
        } finally {
            session.getLock().unlock();
        }
    }

    @Test
    public void recordEntityScopedGraphLink_linksEntries() {
        final var session = new ReplicantSession(mock(Session.class));
        final var sourceDatasetAddress = DatasetAddress.of(1, 2);
        final var targetDatasetAddress = DatasetAddress.of(2);

        session.getLock().lock();
        try {
            session.createSubscriptionEntry(sourceDatasetAddress);
            session.createSubscriptionEntry(targetDatasetAddress);

            session.recordEntityScopedGraphLink(sourceDatasetAddress, targetDatasetAddress, 7, 11);

            final var sourceEntry = session.getSubscriptionEntry(sourceDatasetAddress);
            assertTrue(sourceEntry.getOutwardSubscriptions().contains(targetDatasetAddress));
            assertEquals(
                    sourceEntry.getOwnedOutwardSubscriptions(LinkOwner.entity(7, 11)), Set.of(targetDatasetAddress));
            assertTrue(session.getSubscriptionEntry(targetDatasetAddress)
                    .getInwardSubscriptions()
                    .contains(sourceDatasetAddress));
        } finally {
            session.getLock().unlock();
        }
    }

    @Test
    public void recordEntityScopedGraphLink_allowsInstanceGraphTarget() {
        final var session = new ReplicantSession(mock(Session.class));
        final var sourceDatasetAddress = DatasetAddress.of(1, 2);
        final var targetDatasetAddress = DatasetAddress.of(2, 3);

        session.getLock().lock();
        try {
            session.createSubscriptionEntry(sourceDatasetAddress);
            session.createSubscriptionEntry(targetDatasetAddress);

            session.recordEntityScopedGraphLink(sourceDatasetAddress, targetDatasetAddress, 7, 11);

            assertTrue(session.getSubscriptionEntry(sourceDatasetAddress)
                    .getOutwardSubscriptions()
                    .contains(targetDatasetAddress));
            assertEquals(
                    session.getSubscriptionEntry(sourceDatasetAddress)
                            .getOwnedOutwardSubscriptions(LinkOwner.entity(7, 11)),
                    Set.of(targetDatasetAddress));
            assertTrue(session.getSubscriptionEntry(targetDatasetAddress)
                    .getInwardSubscriptions()
                    .contains(sourceDatasetAddress));
        } finally {
            session.getLock().unlock();
        }
    }

    @Test
    public void recordEntityScopedGraphLink_rejectsPartialAddress() {
        final var session = new ReplicantSession(mock(Session.class));

        session.getLock().lock();
        try {
            session.createSubscriptionEntry(DatasetAddress.of(1, 2, "fi"));
            session.createSubscriptionEntry(DatasetAddress.of(2));

            expectThrows(
                    AssertionError.class,
                    () -> session.recordEntityScopedGraphLink(
                            DatasetAddress.partial(1, 2), DatasetAddress.of(2), 7, 11));
        } finally {
            session.getLock().unlock();
        }
    }

    @Test
    public void entityOwnedGraphLinks_requireLastOwnerBeforeDelink() {
        final var session = new ReplicantSession(mock(Session.class));
        final var changeSet = new ChangeSet();
        final var sourceDatasetAddress = DatasetAddress.of(1, 2);
        final var targetDatasetAddress = DatasetAddress.of(2, 3);

        session.getLock().lock();
        try {
            final var sourceEntry = session.createSubscriptionEntry(sourceDatasetAddress);
            final var targetEntry = session.createSubscriptionEntry(targetDatasetAddress);
            targetEntry.setExplicitlySubscribed(true);

            session.recordGraphLink(sourceEntry, targetEntry, LinkOwner.entity(7, 11));
            session.recordGraphLink(sourceEntry, targetEntry, LinkOwner.entity(7, 12));

            assertTrue(sourceEntry.getOutwardSubscriptions().contains(targetDatasetAddress));
            assertEquals(
                    sourceEntry.getOwnedOutwardSubscriptions(LinkOwner.entity(7, 11)), Set.of(targetDatasetAddress));
            assertEquals(
                    sourceEntry.getOwnedOutwardSubscriptions(LinkOwner.entity(7, 12)), Set.of(targetDatasetAddress));

            session.delinkDownstreamSubscription(sourceEntry, LinkOwner.entity(7, 11), targetDatasetAddress, changeSet);

            assertTrue(sourceEntry.getOutwardSubscriptions().contains(targetDatasetAddress));
            assertTrue(targetEntry.getInwardSubscriptions().contains(sourceDatasetAddress));
            assertNotNull(session.findSubscriptionEntry(targetDatasetAddress));

            session.delinkDownstreamSubscription(sourceEntry, LinkOwner.entity(7, 12), targetDatasetAddress, changeSet);

            assertTrue(sourceEntry.getOutwardSubscriptions().isEmpty());
            assertTrue(targetEntry.getInwardSubscriptions().isEmpty());
            assertNotNull(session.findSubscriptionEntry(targetDatasetAddress));
        } finally {
            session.getLock().unlock();
        }

        assertTrue(changeSet.getChannelActions().isEmpty());
    }

    @Test
    public void getFilterAndSetFilter_roundTrip() {
        final var session = new ReplicantSession(mock(Session.class));
        final var datasetAddress = DatasetAddress.of(1, 2);

        session.getLock().lock();
        try {
            session.createSubscriptionEntry(datasetAddress);
            final JsonObject filter = Json.createObjectBuilder().add("k", "v").build();
            session.setFilter(datasetAddress, filter);
            assertEquals(session.getFilter(datasetAddress), filter);

            session.setFilter(datasetAddress, null);
            assertNull(session.getFilter(datasetAddress));
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
            final var entry = session.createSubscriptionEntry(datasetAddress);
            entry.setExplicitlySubscribed(true);

            session.bulkUnsubscribe(Collections.singletonList(datasetAddress), changeSet);
            assertNull(session.findSubscriptionEntry(datasetAddress));

            session.bulkUnsubscribe(Collections.singletonList(datasetAddress), changeSet);
        } finally {
            session.getLock().unlock();
        }

        assertEquals(changeSet.getChannelActions().size(), 1);
        assertEquals(changeSet.getChannelActions().get(0).action(), ChannelAction.Action.REMOVE);
    }

    @Test
    public void bulkUnsubscribe_removesEachSubscribedAddress() {
        final var session = new ReplicantSession(mock(Session.class));
        final var changeSet = new ChangeSet();
        final var datasetAddress1 = DatasetAddress.of(1, 1);
        final var datasetAddress2 = DatasetAddress.of(1, 2);

        session.getLock().lock();
        try {
            final var entry1 = session.createSubscriptionEntry(datasetAddress1);
            final var entry2 = session.createSubscriptionEntry(datasetAddress2);
            entry1.setExplicitlySubscribed(true);
            entry2.setExplicitlySubscribed(true);

            session.bulkUnsubscribe(List.of(datasetAddress1, datasetAddress2, DatasetAddress.of(1, 3)), changeSet);

            assertNull(session.findSubscriptionEntry(datasetAddress1));
            assertNull(session.findSubscriptionEntry(datasetAddress2));
        } finally {
            session.getLock().unlock();
        }

        assertEquals(changeSet.getChannelActions().size(), 2);
        assertEquals(changeSet.getChannelActions().get(0).action(), ChannelAction.Action.REMOVE);
        assertEquals(changeSet.getChannelActions().get(1).action(), ChannelAction.Action.REMOVE);
    }

    @Test
    public void bulkUnsubscribe_rejectsPartialAddress() {
        final var session = new ReplicantSession(mock(Session.class));
        final var changeSet = new ChangeSet();

        session.getLock().lock();
        try {
            expectThrows(
                    AssertionError.class,
                    () -> session.bulkUnsubscribe(Collections.singletonList(DatasetAddress.partial(1, 2)), changeSet));
        } finally {
            session.getLock().unlock();
        }
    }

    @Test
    public void performUnsubscribe_onlyRemovesWhenEntryCanUnsubscribe() {
        final var session = new ReplicantSession(mock(Session.class));
        final var changeSet = new ChangeSet();
        final var datasetAddress = DatasetAddress.of(1, 2);

        session.getLock().lock();
        try {
            final var entry = session.createSubscriptionEntry(datasetAddress);
            entry.setExplicitlySubscribed(true);

            session.performUnsubscribe(entry, false, false, changeSet);
            assertNotNull(session.findSubscriptionEntry(datasetAddress));

            session.performUnsubscribe(entry, true, false, changeSet);
            assertNull(session.findSubscriptionEntry(datasetAddress));
        } finally {
            session.getLock().unlock();
        }

        assertEquals(changeSet.getChannelActions().size(), 1);
        assertEquals(changeSet.getChannelActions().get(0).action(), ChannelAction.Action.REMOVE);
    }

    @Test
    public void performUnsubscribe_deleteUsesDeleteAction() {
        final var session = new ReplicantSession(mock(Session.class));
        final var changeSet = new ChangeSet();
        final var datasetAddress = DatasetAddress.of(1, 2);

        session.getLock().lock();
        try {
            final var entry = session.createSubscriptionEntry(datasetAddress);
            entry.setExplicitlySubscribed(true);

            session.performUnsubscribe(entry, true, true, changeSet);
            assertNull(session.findSubscriptionEntry(datasetAddress));
        } finally {
            session.getLock().unlock();
        }

        assertEquals(changeSet.getChannelActions().size(), 1);
        assertEquals(changeSet.getChannelActions().get(0).action(), ChannelAction.Action.DELETE);
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
            final var entryA = session.createSubscriptionEntry(a);
            session.createSubscriptionEntry(b);
            session.createSubscriptionEntry(c);
            session.recordGraphScopedGraphLink(a, b);
            session.recordGraphScopedGraphLink(b, c);

            session.performUnsubscribe(entryA, false, false, changeSet);

            assertNull(session.findSubscriptionEntry(a));
            assertNull(session.findSubscriptionEntry(b));
            assertNull(session.findSubscriptionEntry(c));
        } finally {
            session.getLock().unlock();
        }

        assertEquals(changeSet.getChannelActions().size(), 3);
        final var actions = changeSet.getChannelActions().stream()
                .map(ChannelAction::datasetAddress)
                .collect(java.util.stream.Collectors.toSet());
        assertEquals(actions, Set.of(a, b, c));
    }

    @Test
    public void delinkDownstreamSubscription_keepsExplicitDownstream() {
        final var session = new ReplicantSession(mock(Session.class));
        final var changeSet = new ChangeSet();
        final var upstream = DatasetAddress.of(1, 1);
        final var downstream = DatasetAddress.of(2);

        session.getLock().lock();
        try {
            session.createSubscriptionEntry(upstream);
            final var downstreamEntry = session.createSubscriptionEntry(downstream);
            downstreamEntry.setExplicitlySubscribed(true);
            session.recordGraphScopedGraphLink(upstream, downstream);

            session.delinkDownstreamSubscription(upstream, downstream, changeSet);

            assertTrue(session.getSubscriptionEntry(upstream)
                    .getOutwardSubscriptions()
                    .isEmpty());
            assertTrue(session.getSubscriptionEntry(downstream)
                    .getInwardSubscriptions()
                    .isEmpty());
            assertNotNull(session.findSubscriptionEntry(downstream));
        } finally {
            session.getLock().unlock();
        }

        assertTrue(changeSet.getChannelActions().isEmpty());
    }

    @Test
    public void delinkDownstreamSubscription_rejectsPartialAddress() {
        final var session = new ReplicantSession(mock(Session.class));
        final var changeSet = new ChangeSet();

        session.getLock().lock();
        try {
            session.createSubscriptionEntry(DatasetAddress.of(1, 1, "fi"));
            session.createSubscriptionEntry(DatasetAddress.of(1, 2));

            expectThrows(
                    AssertionError.class,
                    () -> session.delinkDownstreamSubscription(
                            DatasetAddress.partial(1, 1), DatasetAddress.of(1, 2), changeSet));
        } finally {
            session.getLock().unlock();
        }
    }

    @SuppressWarnings("DataFlowIssue")
    @NonNull
    private Map<DatasetAddress, SubscriptionEntry> getSubscriptions(final ReplicantSession session) {
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
