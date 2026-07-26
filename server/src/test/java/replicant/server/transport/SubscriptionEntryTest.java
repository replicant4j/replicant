package replicant.server.transport;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import javax.json.Json;
import javax.websocket.Session;
import org.jspecify.annotations.NonNull;
import org.testng.annotations.Test;
import replicant.server.DatasetAddress;
import replicant.server.ValueUtil;

public class SubscriptionEntryTest {
    @Test
    public void basicFlow() {
        final var cd1 = DatasetAddress.of(ValueUtil.randomInt(), ValueUtil.randomInt());
        final var cd2 = DatasetAddress.of(ValueUtil.randomInt(), ValueUtil.randomInt());
        final var cd3 = DatasetAddress.of(ValueUtil.randomInt(), ValueUtil.randomInt());
        final var cd4 = DatasetAddress.of(ValueUtil.randomInt(), ValueUtil.randomInt());
        final var cd5 = DatasetAddress.of(ValueUtil.randomInt(), ValueUtil.randomInt());

        final var session = newSession();
        session.getLock().lock();
        final var entry = new SubscriptionEntry(session, cd1);

        assertEquals(entry.datasetAddress(), cd1);
        assertFalse(entry.isExplicitlySubscribed());
        assertEquals(entry.getInwardSubscriptionDependencies().size(), 0);
        assertEquals(entry.getOutwardSubscriptionDependencies().size(), 0);
        assertTrue(entry.canUnsubscribe());
        assertNull(entry.getFilterParameter());

        entry.setExplicitlySubscribed(true);
        assertTrue(entry.isExplicitlySubscribed());
        assertFalse(entry.canUnsubscribe());
        entry.setExplicitlySubscribed(false);
        assertFalse(entry.isExplicitlySubscribed());
        assertTrue(entry.canUnsubscribe());

        final var filterParameter = Json.createObjectBuilder().build();

        entry.setFilterParameter(filterParameter);
        assertEquals(entry.getFilterParameter(), filterParameter);
        entry.setFilterParameter(null);
        assertNull(entry.getFilterParameter());

        // Deregister when there is none subscribed
        assertEquals(
                entry.deregisterOutwardSubscriptionDependencies(SubscriptionDependencyOwner.dataset(), cd2),
                new DatasetAddress[0]);
        assertEquals(entry.deregisterInwardSubscriptionDependencies(cd2), new DatasetAddress[0]);

        // Register inward Subscription Dependencies
        assertEquals(entry.registerInwardSubscriptionDependencies(cd2, cd3, cd4), new DatasetAddress[] {cd2, cd3, cd4});
        assertFalse(entry.canUnsubscribe());
        assertEquals(entry.getInwardSubscriptionDependencies().size(), 3);
        assertTrue(entry.getInwardSubscriptionDependencies().contains(cd2));
        assertTrue(entry.getInwardSubscriptionDependencies().contains(cd3));
        assertTrue(entry.getInwardSubscriptionDependencies().contains(cd4));
        assertFalse(entry.getInwardSubscriptionDependencies().contains(cd5));
        assertEquals(entry.getOutwardSubscriptionDependencies().size(), 0);

        assertEquals(entry.registerInwardSubscriptionDependencies(cd2, cd3, cd4), new DatasetAddress[0]);

        // Deregister some of those incoming
        assertEquals(entry.deregisterInwardSubscriptionDependencies(cd2, cd3), new DatasetAddress[] {cd2, cd3});
        assertFalse(entry.canUnsubscribe());
        assertEquals(entry.getInwardSubscriptionDependencies().size(), 1);
        assertFalse(entry.getInwardSubscriptionDependencies().contains(cd2));
        assertFalse(entry.getInwardSubscriptionDependencies().contains(cd3));
        assertTrue(entry.getInwardSubscriptionDependencies().contains(cd4));
        assertFalse(entry.getInwardSubscriptionDependencies().contains(cd5));
        assertEquals(entry.getOutwardSubscriptionDependencies().size(), 0);

        // Deregister the remaining
        assertEquals(entry.deregisterInwardSubscriptionDependencies(cd2, cd3, cd4), new DatasetAddress[] {cd4});
        assertTrue(entry.canUnsubscribe());
        assertEquals(entry.getInwardSubscriptionDependencies().size(), 0);
        assertEquals(entry.getOutwardSubscriptionDependencies().size(), 0);

        // Register outward Subscription Dependencies
        assertEquals(
                entry.registerOutwardSubscriptionDependencies(
                        SubscriptionDependencyOwner.dataset(), cd2, cd3, cd3, cd4),
                new DatasetAddress[] {cd2, cd3, cd4});
        assertTrue(entry.canUnsubscribe());
        assertEquals(entry.getInwardSubscriptionDependencies().size(), 0);
        assertEquals(entry.getOutwardSubscriptionDependencies().size(), 3);
        assertTrue(entry.getOutwardSubscriptionDependencies().contains(cd2));
        assertTrue(entry.getOutwardSubscriptionDependencies().contains(cd3));
        assertTrue(entry.getOutwardSubscriptionDependencies().contains(cd4));
        assertFalse(entry.getOutwardSubscriptionDependencies().contains(cd5));

        assertEquals(
                entry.registerOutwardSubscriptionDependencies(
                        SubscriptionDependencyOwner.dataset(), cd2, cd3, cd3, cd4),
                new DatasetAddress[0]);

        // Deregister some outgoing
        assertEquals(
                entry.deregisterOutwardSubscriptionDependencies(SubscriptionDependencyOwner.dataset(), cd2, cd3, cd3),
                new DatasetAddress[] {cd2, cd3});
        assertTrue(entry.canUnsubscribe());
        assertEquals(entry.getInwardSubscriptionDependencies().size(), 0);
        assertEquals(entry.getOutwardSubscriptionDependencies().size(), 1);
        assertFalse(entry.getOutwardSubscriptionDependencies().contains(cd2));
        assertFalse(entry.getOutwardSubscriptionDependencies().contains(cd3));
        assertTrue(entry.getOutwardSubscriptionDependencies().contains(cd4));
        assertFalse(entry.getOutwardSubscriptionDependencies().contains(cd5));
    }

    @Test
    public void sorting() {
        final var cd1 = DatasetAddress.of(1, 42);
        final var cd3 = DatasetAddress.of(1, 43);
        final var cd4 = DatasetAddress.of(2, null);
        final var cd5 = DatasetAddress.of(3, null);

        final var session = newSession();
        final var entry1 = new SubscriptionEntry(session, cd1);
        final var entry3 = new SubscriptionEntry(session, cd3);
        final var entry4 = new SubscriptionEntry(session, cd4);
        final var entry5 = new SubscriptionEntry(session, cd5);

        final var list = new ArrayList<>(Arrays.asList(entry5, entry4, entry3, entry1));

        Collections.sort(list);

        final var expected = new SubscriptionEntry[] {entry1, entry3, entry4, entry5};
        assertEquals(list.toArray(new SubscriptionEntry[0]), expected);
    }

    @Test
    public void ownerAwareOutwardSubscriptions_referenceCountSharedTargets() {
        final var sourceDatasetAddress = DatasetAddress.of(1, 1);
        final var targetDatasetAddress = DatasetAddress.of(2, 2);

        final var session = newSession();
        session.getLock().lock();
        try {
            final var entry = new SubscriptionEntry(session, sourceDatasetAddress);
            final var ownerA = SubscriptionDependencyOwner.entity(7, 11);
            final var ownerB = SubscriptionDependencyOwner.entity(7, 12);

            assertEquals(
                    entry.registerOutwardSubscriptionDependencies(ownerA, targetDatasetAddress),
                    new DatasetAddress[] {targetDatasetAddress});
            assertTrue(entry.getOutwardSubscriptionDependencies().contains(targetDatasetAddress));
            assertEquals(entry.getOwnedOutwardSubscriptionDependencies(ownerA), Set.of(targetDatasetAddress));

            assertEquals(
                    entry.registerOutwardSubscriptionDependencies(ownerB, targetDatasetAddress), new DatasetAddress[0]);
            assertTrue(entry.getOutwardSubscriptionDependencies().contains(targetDatasetAddress));
            assertEquals(entry.getOwnedOutwardSubscriptionDependencies(ownerB), Set.of(targetDatasetAddress));

            assertEquals(
                    entry.deregisterOutwardSubscriptionDependencies(ownerA, targetDatasetAddress),
                    new DatasetAddress[0]);
            assertTrue(entry.getOutwardSubscriptionDependencies().contains(targetDatasetAddress));
            assertTrue(entry.getOwnedOutwardSubscriptionDependencies(ownerA).isEmpty());

            assertEquals(
                    entry.deregisterOutwardSubscriptionDependencies(ownerB, targetDatasetAddress),
                    new DatasetAddress[] {targetDatasetAddress});
            assertTrue(entry.getOutwardSubscriptionDependencies().isEmpty());
            assertTrue(entry.getOwnedOutwardSubscriptionDependencies(ownerB).isEmpty());
        } finally {
            session.getLock().unlock();
        }
    }

    @NonNull
    private ReplicantSession newSession() {
        return new ReplicantSession(mock(Session.class));
    }
}
