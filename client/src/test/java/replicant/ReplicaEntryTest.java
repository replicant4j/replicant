package replicant;

import static org.testng.Assert.*;

import arez.Disposable;
import java.util.concurrent.atomic.AtomicInteger;
import org.jspecify.annotations.NonNull;
import org.testng.annotations.Test;

public class ReplicaEntryTest extends AbstractReplicantTest {
    @Test
    public void basicConstruction() {
        final ReplicaRegistry replicaRegistry = Replicant.context().getReplicaRegistry();

        final Class<A> type = A.class;
        final int id = ValueUtil.randomInt();
        final String name = "A/" + id;
        final ReplicaEntry replicaEntry = safeAction(() -> replicaRegistry.findOrCreateReplicaEntry(name, type, id));

        assertEquals(replicaEntry.getName(), name);
        assertEquals(replicaEntry.getType(), type);
        assertEquals(replicaEntry.getId(), id);

        safeAction(() -> assertNull(replicaEntry.maybeReplica()));
        safeAction(() -> assertEquals(replicaEntry.getSubscriptions().size(), 0));
    }

    @Test
    public void toStringTest() {
        final ReplicaRegistry replicaRegistry = Replicant.context().getReplicaRegistry();

        final Class<A> type = A.class;
        final int id = 123;
        final String name = "A/123";
        final ReplicaEntry replicaEntry = safeAction(() -> replicaRegistry.findOrCreateReplicaEntry(name, type, id));

        assertEquals(replicaEntry.toString(), name);
        ReplicantTestUtil.disableNames();

        assertEquals(
                replicaEntry.toString(),
                replicaEntry.getClass().getName() + "@" + Integer.toHexString(replicaEntry.hashCode()));
    }

    @Test
    public void namePassedToConstructorWhenNamesDisabled() {
        final ReplicaRegistry replicaRegistry = Replicant.context().getReplicaRegistry();

        ReplicantTestUtil.disableNames();

        final IllegalStateException exception = expectThrows(
                IllegalStateException.class,
                () -> safeAction(() -> replicaRegistry.findOrCreateReplicaEntry("A/123", A.class, 123)));

        assertEquals(
                exception.getMessage(),
                "Replicant-0032: ReplicaEntry passed a name 'A/123' but Replicant.areNamesEnabled() is false");
    }

    @Test
    public void getNameInvokedWhenNamesDisabled() {
        final ReplicaRegistry replicaRegistry = Replicant.context().getReplicaRegistry();

        ReplicantTestUtil.disableNames();

        final ReplicaEntry replicaEntry =
                safeAction(() -> replicaRegistry.findOrCreateReplicaEntry(null, A.class, 123));

        final IllegalStateException exception =
                expectThrows(IllegalStateException.class, () -> safeAction(replicaEntry::getName));

        assertEquals(
                exception.getMessage(),
                "Replicant-0009: ReplicaEntry.getName() invoked when Replicant.areNamesEnabled() is false");
    }

    @Test
    public void replica() {
        final ReplicaRegistry replicaRegistry = Replicant.context().getReplicaRegistry();

        final ReplicaEntry replicaEntry = safeAction(() ->
                replicaRegistry.findOrCreateReplicaEntry(ValueUtil.randomString(), A.class, ValueUtil.randomInt()));

        safeAction(() -> assertNull(replicaEntry.maybeReplica()));

        final A replica = new A();
        safeAction(() -> replicaEntry.setReplica(replica));
        safeAction(() -> assertEquals(replicaEntry.maybeReplica(), replica));
        safeAction(() -> assertEquals(replicaEntry.getReplica(), replica));
    }

    @Test
    public void getReplica_whenNull() {
        final ReplicaRegistry replicaRegistry = Replicant.context().getReplicaRegistry();

        final ReplicaEntry replicaEntry = safeAction(() ->
                replicaRegistry.findOrCreateReplicaEntry(ValueUtil.randomString(), A.class, ValueUtil.randomInt()));

        final IllegalStateException exception = expectThrows(
                IllegalStateException.class, () -> safeAction(() -> assertNull(replicaEntry.getReplica())));

        assertEquals(
                exception.getMessage(), "Replicant-0071: ReplicaEntry.getReplica() invoked when no replica present");
    }

    @Test
    public void typeDatasetSubscriptions() {
        final ReplicaRegistry replicaRegistry = Replicant.context().getReplicaRegistry();

        final ReplicaEntry replicaEntry = safeAction(() -> replicaRegistry.findOrCreateReplicaEntry(
                ValueUtil.randomString(), String.class, ValueUtil.randomInt()));

        final Subscription subscription1 = createSubscription(new DatasetAddress(1, 0));
        final Subscription subscription2 = createSubscription(new DatasetAddress(1, 1));

        final AtomicInteger callCount = new AtomicInteger();
        observer(() -> {
            if (Disposable.isNotDisposed(replicaEntry)) {
                // Access observable next line
                replicaEntry.getSubscriptions();
            }
            callCount.incrementAndGet();
        });

        assertEquals(callCount.get(), 1);
        safeAction(() -> assertEquals(replicaEntry.getSubscriptions().size(), 0));

        // Add initial subscription
        {
            safeAction(() -> replicaEntry.linkToSubscription(subscription1));

            assertEquals(callCount.get(), 2);
            safeAction(() -> assertEquals(replicaEntry.getSubscriptions().size(), 1));
        }

        // Add second subscription and thus get notified
        {
            safeAction(() -> replicaEntry.linkToSubscription(subscription2));

            assertEquals(callCount.get(), 3);
            safeAction(() -> assertEquals(replicaEntry.getSubscriptions().size(), 2));
        }

        // Remove subscription and thus get notified
        {
            safeAction(() -> replicaEntry.delinkFromSubscription(subscription2));

            assertEquals(callCount.get(), 4);
            safeAction(() -> assertEquals(replicaEntry.getSubscriptions().size(), 1));
        }

        // Remove last subscription and thus get notified, also replicaEntry should now be disposed
        {
            safeAction(() -> replicaEntry.delinkFromSubscription(subscription1));

            assertEquals(callCount.get(), 5);
            assertTrue(Disposable.isDisposed(replicaEntry));
            // Have to access test-only method as replicaEntry is disposed
            assertEquals(replicaEntry.subscriptions().size(), 0);
        }
    }

    @Test
    public void instanceDatasetSubscriptions() {
        final ReplicaRegistry replicaRegistry = Replicant.context().getReplicaRegistry();

        final ReplicaEntry replicaEntry = safeAction(() -> replicaRegistry.findOrCreateReplicaEntry(
                ValueUtil.randomString(), String.class, ValueUtil.randomInt()));

        final Subscription subscription1 = createSubscription(new DatasetAddress(1, 0, 1));
        final Subscription subscription2 = createSubscription(new DatasetAddress(1, 0, 2));

        final AtomicInteger callCount = new AtomicInteger();
        observer(() -> {
            if (Disposable.isNotDisposed(replicaEntry)) {
                // Access observable next line
                replicaEntry.getSubscriptions();
            }
            callCount.incrementAndGet();
        });

        assertEquals(callCount.get(), 1);
        safeAction(() -> assertEquals(replicaEntry.getSubscriptions().size(), 0));

        // Add initial subscription
        {
            safeAction(() -> replicaEntry.linkToSubscription(subscription1));

            assertEquals(callCount.get(), 2);
            safeAction(() -> assertEquals(replicaEntry.getSubscriptions().size(), 1));
        }

        // Add second subscription and thus get notified
        // second subscription is of the same Dataset, so should go through second path
        {
            safeAction(() -> replicaEntry.linkToSubscription(subscription2));

            assertEquals(callCount.get(), 3);
            safeAction(() -> assertEquals(replicaEntry.getSubscriptions().size(), 2));
        }

        // Remove subscription and thus get notified
        {
            safeAction(() -> replicaEntry.delinkFromSubscription(subscription2));

            assertEquals(callCount.get(), 4);
            safeAction(() -> assertEquals(replicaEntry.getSubscriptions().size(), 1));
        }

        // Remove last subscription and thus get notified, also replicaEntry should now be disposed
        {
            safeAction(() -> replicaEntry.delinkFromSubscription(subscription1));

            assertEquals(callCount.get(), 5);
            assertTrue(Disposable.isDisposed(replicaEntry));
            // Have to access test-only method as replicaEntry is disposed
            assertEquals(replicaEntry.subscriptions().size(), 0);
        }
    }

    @Test
    public void delinkFromSubscription_whenNotLinked() {
        final ReplicaRegistry replicaRegistry = Replicant.context().getReplicaRegistry();

        final ReplicaEntry replicaEntry = safeAction(
                () -> replicaRegistry.findOrCreateReplicaEntry("MyEntity", String.class, ValueUtil.randomInt()));

        final Subscription subscription1 = createSubscription(new DatasetAddress(1, 0, 1));

        final IllegalStateException exception = expectThrows(
                IllegalStateException.class,
                () -> safeAction(() -> replicaEntry.delinkFromSubscription(subscription1)));
        assertEquals(
                exception.getMessage(),
                "Replicant-0081: ReplicaEntry.delinkFromSubscription invoked on Replica Entry MyEntity passing"
                        + " subscription 1.0.1 but Replica Entry is not linked to subscription.");
    }

    @Test
    public void linkToSubscription_whenNotLinked() {
        final ReplicaRegistry replicaRegistry = Replicant.context().getReplicaRegistry();

        final ReplicaEntry replicaEntry = safeAction(
                () -> replicaRegistry.findOrCreateReplicaEntry("MyEntity", String.class, ValueUtil.randomInt()));

        final Subscription subscription1 = createSubscription(new DatasetAddress(1, 0, 1));

        safeAction(() -> replicaEntry.linkToSubscription(subscription1));

        final IllegalStateException exception = expectThrows(
                IllegalStateException.class, () -> safeAction(() -> replicaEntry.linkToSubscription(subscription1)));
        assertEquals(
                exception.getMessage(),
                "Replicant-0080: ReplicaEntry.linkToSubscription invoked on Replica Entry MyEntity passing"
                        + " subscription 1.0.1 but Replica Entry is already linked to subscription.");
    }

    @Test
    public void tryLinkToSubscription() {
        final ReplicaRegistry replicaRegistry = Replicant.context().getReplicaRegistry();

        final ReplicaEntry replicaEntry = safeAction(
                () -> replicaRegistry.findOrCreateReplicaEntry("MyEntity", String.class, ValueUtil.randomInt()));

        final Subscription subscription1 = createSubscription(new DatasetAddress(1, 0, 1));

        safeAction(() -> replicaEntry.tryLinkToSubscription(subscription1));

        safeAction(() -> assertEquals(replicaEntry.getSubscriptions().size(), 1));

        // Should perform no action
        replicaEntry.tryLinkToSubscription(subscription1);

        safeAction(() -> assertEquals(replicaEntry.getSubscriptions().size(), 1));
    }

    @Test
    public void disposeRemovesReplicaEntryFromSubscriptions() {
        final ReplicaRegistry replicaRegistry = Replicant.context().getReplicaRegistry();

        final Class<A> type = A.class;
        final int id = ValueUtil.randomInt();
        final String name = "A/" + id;
        final ReplicaEntry replicaEntry = safeAction(() -> replicaRegistry.findOrCreateReplicaEntry(name, type, id));

        final Subscription subscription1 = createSubscription(new DatasetAddress(1, 0, 1));
        final Subscription subscription2 = createSubscription(new DatasetAddress(1, 0, 2));

        safeAction(() -> assertEquals(replicaEntry.getSubscriptions().size(), 0));
        safeAction(() -> replicaEntry.linkToSubscription(subscription1));
        safeAction(() -> replicaEntry.linkToSubscription(subscription2));
        safeAction(() -> assertEquals(replicaEntry.getSubscriptions().size(), 2));

        safeAction(() -> assertEquals(subscription1.findReplicaEntryByTypeAndId(type, id), replicaEntry));
        safeAction(() -> assertEquals(subscription2.findReplicaEntryByTypeAndId(type, id), replicaEntry));

        Disposable.dispose(replicaEntry);

        safeAction(() ->
                assertEquals(subscription1.findAllReplicaEntriesByType(type).size(), 0));
        safeAction(() ->
                assertEquals(subscription2.findAllReplicaEntriesByType(type).size(), 0));
    }

    @Test
    public void disposeWillDisposeReplica() {
        final ReplicaRegistry replicaRegistry = Replicant.context().getReplicaRegistry();

        final ReplicaEntry replicaEntry = safeAction(() ->
                replicaRegistry.findOrCreateReplicaEntry(ValueUtil.randomString(), A.class, ValueUtil.randomInt()));
        final A replica = new A();
        safeAction(() -> replicaEntry.setReplica(replica));

        assertFalse(Disposable.isDisposed(replicaEntry));
        assertFalse(replica.isDisposed());

        Disposable.dispose(replicaEntry);

        assertTrue(Disposable.isDisposed(replicaEntry));
        assertTrue(replica.isDisposed());
    }

    @Test
    public void getSubscriptions_mutability() {
        final ReplicaRegistry replicaRegistry = Replicant.context().getReplicaRegistry();

        final ReplicaEntry replicaEntry = safeAction(() ->
                replicaRegistry.findOrCreateReplicaEntry(ValueUtil.randomString(), A.class, ValueUtil.randomInt()));

        final Subscription subscription1 = createSubscription();

        expectThrows(
                UnsupportedOperationException.class,
                () -> safeAction(() -> replicaEntry.getSubscriptions().add(subscription1)));
    }

    @Test
    public void delinkSubscriptionFromReplicaEntry_whenSubscriptionMissing() {
        final ReplicaRegistry replicaRegistry = Replicant.context().getReplicaRegistry();

        final ReplicaEntry replicaEntry =
                safeAction(() -> replicaRegistry.findOrCreateReplicaEntry("A/123", A.class, 123));

        final Subscription subscription1 = createSubscription(new DatasetAddress(1, 0, 1));

        safeAction(() -> assertEquals(replicaEntry.getSubscriptions().size(), 0));
        safeAction(() -> replicaEntry.linkToSubscription(subscription1));
        safeAction(() -> assertEquals(replicaEntry.getSubscriptions().size(), 1));

        replicaEntry.subscriptions().remove(subscription1.datasetAddress());

        final IllegalStateException exception = expectThrows(
                IllegalStateException.class,
                () -> safeAction(() -> replicaEntry.delinkSubscriptionFromReplicaEntry(subscription1)));
        assertEquals(
                exception.getMessage(),
                "Unable to locate subscription for Dataset Address 1.0.1 on Replica Entry A/123");
    }

    @NonNull
    private Subscription createSubscription() {
        return createSubscription(new DatasetAddress(1, 0));
    }

    @NonNull
    private Subscription createSubscription(@NonNull final DatasetAddress datasetAddress) {
        return Subscription.create(null, datasetAddress, null, true);
    }

    static class A implements Disposable {
        private boolean _disposed;

        @Override
        public void dispose() {
            _disposed = true;
        }

        @Override
        public boolean isDisposed() {
            return _disposed;
        }
    }
}
