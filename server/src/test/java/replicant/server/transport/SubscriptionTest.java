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

public class SubscriptionTest {
    @Test
    public void basicFlow() {
        final var cd1 = DatasetAddress.of(ValueUtil.randomInt(), ValueUtil.randomInt());
        final var cd2 = DatasetAddress.of(ValueUtil.randomInt(), ValueUtil.randomInt());
        final var cd3 = DatasetAddress.of(ValueUtil.randomInt(), ValueUtil.randomInt());
        final var cd4 = DatasetAddress.of(ValueUtil.randomInt(), ValueUtil.randomInt());
        final var cd5 = DatasetAddress.of(ValueUtil.randomInt(), ValueUtil.randomInt());

        final var session = newSession();
        session.getLock().lock();
        final var subscription = new Subscription(session, cd1, SubscriptionMode.IMPLICIT);

        assertEquals(subscription.datasetAddress(), cd1);
        assertEquals(subscription.getMode(), SubscriptionMode.IMPLICIT);
        assertEquals(subscription.getInwardSubscriptionDependencies().size(), 0);
        assertEquals(subscription.getOutwardSubscriptionDependencies().size(), 0);
        assertTrue(subscription.canUnsubscribe());
        assertNull(subscription.getFilterParameter());

        subscription.setMode(SubscriptionMode.EXPLICIT);
        assertEquals(subscription.getMode(), SubscriptionMode.EXPLICIT);
        assertFalse(subscription.canUnsubscribe());
        subscription.setMode(SubscriptionMode.IMPLICIT);
        assertEquals(subscription.getMode(), SubscriptionMode.IMPLICIT);
        assertTrue(subscription.canUnsubscribe());

        final var filterParameter = Json.createObjectBuilder().build();

        subscription.setFilterParameter(filterParameter);
        assertEquals(subscription.getFilterParameter(), filterParameter);
        subscription.setFilterParameter(null);
        assertNull(subscription.getFilterParameter());

        // Deregister when there is none subscribed
        assertEquals(
                subscription.deregisterOutwardSubscriptionDependencies(SubscriptionDependencyOwner.dataset(), cd2),
                new DatasetAddress[0]);
        assertEquals(subscription.deregisterInwardSubscriptionDependencies(cd2), new DatasetAddress[0]);

        // Register inward Subscription Dependencies
        assertEquals(
                subscription.registerInwardSubscriptionDependencies(cd2, cd3, cd4),
                new DatasetAddress[] {cd2, cd3, cd4});
        assertFalse(subscription.canUnsubscribe());
        assertEquals(subscription.getInwardSubscriptionDependencies().size(), 3);
        assertTrue(subscription.getInwardSubscriptionDependencies().contains(cd2));
        assertTrue(subscription.getInwardSubscriptionDependencies().contains(cd3));
        assertTrue(subscription.getInwardSubscriptionDependencies().contains(cd4));
        assertFalse(subscription.getInwardSubscriptionDependencies().contains(cd5));
        assertEquals(subscription.getOutwardSubscriptionDependencies().size(), 0);

        assertEquals(subscription.registerInwardSubscriptionDependencies(cd2, cd3, cd4), new DatasetAddress[0]);

        // Deregister some of those incoming
        assertEquals(subscription.deregisterInwardSubscriptionDependencies(cd2, cd3), new DatasetAddress[] {cd2, cd3});
        assertFalse(subscription.canUnsubscribe());
        assertEquals(subscription.getInwardSubscriptionDependencies().size(), 1);
        assertFalse(subscription.getInwardSubscriptionDependencies().contains(cd2));
        assertFalse(subscription.getInwardSubscriptionDependencies().contains(cd3));
        assertTrue(subscription.getInwardSubscriptionDependencies().contains(cd4));
        assertFalse(subscription.getInwardSubscriptionDependencies().contains(cd5));
        assertEquals(subscription.getOutwardSubscriptionDependencies().size(), 0);

        // Deregister the remaining
        assertEquals(subscription.deregisterInwardSubscriptionDependencies(cd2, cd3, cd4), new DatasetAddress[] {cd4});
        assertTrue(subscription.canUnsubscribe());
        assertEquals(subscription.getInwardSubscriptionDependencies().size(), 0);
        assertEquals(subscription.getOutwardSubscriptionDependencies().size(), 0);

        // Register outward Subscription Dependencies
        assertEquals(
                subscription.registerOutwardSubscriptionDependencies(
                        SubscriptionDependencyOwner.dataset(), cd2, cd3, cd3, cd4),
                new DatasetAddress[] {cd2, cd3, cd4});
        assertTrue(subscription.canUnsubscribe());
        assertEquals(subscription.getInwardSubscriptionDependencies().size(), 0);
        assertEquals(subscription.getOutwardSubscriptionDependencies().size(), 3);
        assertTrue(subscription.getOutwardSubscriptionDependencies().contains(cd2));
        assertTrue(subscription.getOutwardSubscriptionDependencies().contains(cd3));
        assertTrue(subscription.getOutwardSubscriptionDependencies().contains(cd4));
        assertFalse(subscription.getOutwardSubscriptionDependencies().contains(cd5));

        assertEquals(
                subscription.registerOutwardSubscriptionDependencies(
                        SubscriptionDependencyOwner.dataset(), cd2, cd3, cd3, cd4),
                new DatasetAddress[0]);

        // Deregister some outgoing
        assertEquals(
                subscription.deregisterOutwardSubscriptionDependencies(
                        SubscriptionDependencyOwner.dataset(), cd2, cd3, cd3),
                new DatasetAddress[] {cd2, cd3});
        assertTrue(subscription.canUnsubscribe());
        assertEquals(subscription.getInwardSubscriptionDependencies().size(), 0);
        assertEquals(subscription.getOutwardSubscriptionDependencies().size(), 1);
        assertFalse(subscription.getOutwardSubscriptionDependencies().contains(cd2));
        assertFalse(subscription.getOutwardSubscriptionDependencies().contains(cd3));
        assertTrue(subscription.getOutwardSubscriptionDependencies().contains(cd4));
        assertFalse(subscription.getOutwardSubscriptionDependencies().contains(cd5));
    }

    @Test
    public void sorting() {
        final var cd1 = DatasetAddress.of(1, 42);
        final var cd3 = DatasetAddress.of(1, 43);
        final var cd4 = DatasetAddress.of(2, null);
        final var cd5 = DatasetAddress.of(3, null);

        final var session = newSession();
        final var subscription1 = new Subscription(session, cd1, SubscriptionMode.IMPLICIT);
        final var subscription3 = new Subscription(session, cd3, SubscriptionMode.IMPLICIT);
        final var subscription4 = new Subscription(session, cd4, SubscriptionMode.IMPLICIT);
        final var subscription5 = new Subscription(session, cd5, SubscriptionMode.IMPLICIT);

        final var list = new ArrayList<>(Arrays.asList(subscription5, subscription4, subscription3, subscription1));

        Collections.sort(list);

        final var expected = new Subscription[] {subscription1, subscription3, subscription4, subscription5};
        assertEquals(list.toArray(new Subscription[0]), expected);
    }

    @Test
    public void ownerAwareOutwardSubscriptions_referenceCountSharedTargets() {
        final var sourceDatasetAddress = DatasetAddress.of(1, 1);
        final var targetDatasetAddress = DatasetAddress.of(2, 2);

        final var session = newSession();
        session.getLock().lock();
        try {
            final var subscription = new Subscription(session, sourceDatasetAddress, SubscriptionMode.IMPLICIT);
            final var ownerA = SubscriptionDependencyOwner.entity(7, 11);
            final var ownerB = SubscriptionDependencyOwner.entity(7, 12);

            assertEquals(
                    subscription.registerOutwardSubscriptionDependencies(ownerA, targetDatasetAddress),
                    new DatasetAddress[] {targetDatasetAddress});
            assertTrue(subscription.getOutwardSubscriptionDependencies().contains(targetDatasetAddress));
            assertEquals(subscription.getOwnedOutwardSubscriptionDependencies(ownerA), Set.of(targetDatasetAddress));

            assertEquals(
                    subscription.registerOutwardSubscriptionDependencies(ownerB, targetDatasetAddress),
                    new DatasetAddress[0]);
            assertTrue(subscription.getOutwardSubscriptionDependencies().contains(targetDatasetAddress));
            assertEquals(subscription.getOwnedOutwardSubscriptionDependencies(ownerB), Set.of(targetDatasetAddress));

            assertEquals(
                    subscription.deregisterOutwardSubscriptionDependencies(ownerA, targetDatasetAddress),
                    new DatasetAddress[0]);
            assertTrue(subscription.getOutwardSubscriptionDependencies().contains(targetDatasetAddress));
            assertTrue(
                    subscription.getOwnedOutwardSubscriptionDependencies(ownerA).isEmpty());

            assertEquals(
                    subscription.deregisterOutwardSubscriptionDependencies(ownerB, targetDatasetAddress),
                    new DatasetAddress[] {targetDatasetAddress});
            assertTrue(subscription.getOutwardSubscriptionDependencies().isEmpty());
            assertTrue(
                    subscription.getOwnedOutwardSubscriptionDependencies(ownerB).isEmpty());
        } finally {
            session.getLock().unlock();
        }
    }

    @NonNull
    private ReplicantSession newSession() {
        return new ReplicantSession(mock(Session.class));
    }
}
