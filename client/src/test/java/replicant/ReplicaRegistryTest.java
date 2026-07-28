package replicant;

import static org.testng.Assert.*;

import arez.Disposable;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import org.testng.annotations.Test;

public class ReplicaRegistryTest extends AbstractReplicantTest {
    @Test
    public void basicEntityLifecycle() {
        final ReplicaRegistry registry = Replicant.context().getReplicaRegistry();

        final AtomicInteger findAllEntityTypesCallCount = new AtomicInteger();
        observer(() -> {
            if (Disposable.isNotDisposed(registry)) {
                // Access observable next line
                registry.findAllReplicaTypes();
            }

            findAllEntityTypesCallCount.incrementAndGet();
        });

        final AtomicInteger findAllReplicaEntriesByTypeACallCount = new AtomicInteger();
        observer(() -> {
            if (Disposable.isNotDisposed(registry)) {
                // Access observable next line
                registry.findAllReplicaEntriesByType(A.class);
            }

            findAllReplicaEntriesByTypeACallCount.incrementAndGet();
        });

        final AtomicInteger findAllReplicaEntriesByTypeBCallCount = new AtomicInteger();
        observer(() -> {
            if (Disposable.isNotDisposed(registry)) {
                // Access observable next line
                registry.findAllReplicaEntriesByType(B.class);
            }

            findAllReplicaEntriesByTypeBCallCount.incrementAndGet();
        });

        final AtomicInteger findReplicaEntryByTypeAndEntityId1CallCount = new AtomicInteger();
        observer(() -> {
            if (Disposable.isNotDisposed(registry)) {
                // Access observable next line
                registry.findReplicaEntryByTypeAndEntityId(A.class, 1);
            }

            findReplicaEntryByTypeAndEntityId1CallCount.incrementAndGet();
        });

        final AtomicInteger findReplicaEntryByTypeAndEntityId2CallCount = new AtomicInteger();
        observer(() -> {
            if (Disposable.isNotDisposed(registry)) {
                // Access observable next line
                registry.findReplicaEntryByTypeAndEntityId(A.class, 2);
            }

            findReplicaEntryByTypeAndEntityId2CallCount.incrementAndGet();
        });

        assertEquals(findAllEntityTypesCallCount.get(), 1);
        assertEquals(findAllReplicaEntriesByTypeACallCount.get(), 1);
        assertEquals(findAllReplicaEntriesByTypeBCallCount.get(), 1);
        assertEquals(findReplicaEntryByTypeAndEntityId1CallCount.get(), 1);
        assertEquals(findReplicaEntryByTypeAndEntityId2CallCount.get(), 1);

        safeAction(() -> assertEquals(registry.findAllReplicaTypes().size(), 0));
        safeAction(
                () -> assertEquals(registry.findAllReplicaEntriesByType(A.class).size(), 0));
        safeAction(
                () -> assertEquals(registry.findAllReplicaEntriesByType(B.class).size(), 0));
        safeAction(() -> assertNull(registry.findReplicaEntryByTypeAndEntityId(A.class, 1)));
        safeAction(() -> assertNull(registry.findReplicaEntryByTypeAndEntityId(A.class, 2)));

        // add first replicaEntry
        {
            safeAction(() -> registry.findOrCreateReplicaEntry("A/1", A.class, 1));

            assertEquals(findAllEntityTypesCallCount.get(), 2);
            assertEquals(findAllReplicaEntriesByTypeACallCount.get(), 2);
            assertEquals(findAllReplicaEntriesByTypeBCallCount.get(), 2);
            assertEquals(findReplicaEntryByTypeAndEntityId1CallCount.get(), 2);
            assertEquals(findReplicaEntryByTypeAndEntityId2CallCount.get(), 2);

            safeAction(() -> assertEquals(registry.findAllReplicaTypes().size(), 1));
            safeAction(() ->
                    assertEquals(registry.findAllReplicaEntriesByType(A.class).size(), 1));
            safeAction(() ->
                    assertEquals(registry.findAllReplicaEntriesByType(B.class).size(), 0));
            safeAction(() -> assertNotNull(registry.findReplicaEntryByTypeAndEntityId(A.class, 1)));
            safeAction(() -> assertNull(registry.findReplicaEntryByTypeAndEntityId(A.class, 2)));
        }

        // Attempt to add same replicaEntry
        {
            safeAction(() -> registry.findOrCreateReplicaEntry("A/1", A.class, 1));

            assertEquals(findAllEntityTypesCallCount.get(), 2);
            assertEquals(findAllReplicaEntriesByTypeACallCount.get(), 2);
            assertEquals(findAllReplicaEntriesByTypeBCallCount.get(), 2);
            assertEquals(findReplicaEntryByTypeAndEntityId1CallCount.get(), 2);
            assertEquals(findReplicaEntryByTypeAndEntityId2CallCount.get(), 2);

            safeAction(() -> assertEquals(registry.findAllReplicaTypes().size(), 1));
            safeAction(() ->
                    assertEquals(registry.findAllReplicaEntriesByType(A.class).size(), 1));
            safeAction(() ->
                    assertEquals(registry.findAllReplicaEntriesByType(B.class).size(), 0));
            safeAction(() -> assertNotNull(registry.findReplicaEntryByTypeAndEntityId(A.class, 1)));
            safeAction(() -> assertNull(registry.findReplicaEntryByTypeAndEntityId(A.class, 2)));
        }

        // add an replicaEntry of the same type
        {
            safeAction(() -> registry.findOrCreateReplicaEntry("A/2", A.class, 2));

            assertEquals(findAllEntityTypesCallCount.get(), 3);
            assertEquals(findAllReplicaEntriesByTypeACallCount.get(), 3);
            assertEquals(findAllReplicaEntriesByTypeBCallCount.get(), 3);
            assertEquals(findReplicaEntryByTypeAndEntityId1CallCount.get(), 2);
            assertEquals(findReplicaEntryByTypeAndEntityId2CallCount.get(), 3);

            safeAction(() -> assertEquals(registry.findAllReplicaTypes().size(), 1));
            safeAction(() ->
                    assertEquals(registry.findAllReplicaEntriesByType(A.class).size(), 2));
            safeAction(() ->
                    assertEquals(registry.findAllReplicaEntriesByType(B.class).size(), 0));
            safeAction(() -> assertNotNull(registry.findReplicaEntryByTypeAndEntityId(A.class, 1)));
            safeAction(() -> assertNotNull(registry.findReplicaEntryByTypeAndEntityId(A.class, 2)));
        }

        // Add an replicaEntry of a different type
        {
            safeAction(() -> registry.findOrCreateReplicaEntry("B/-53", B.class, -53));

            assertEquals(findAllEntityTypesCallCount.get(), 4);
            assertEquals(findAllReplicaEntriesByTypeACallCount.get(), 4);
            assertEquals(findAllReplicaEntriesByTypeBCallCount.get(), 4);
            assertEquals(findReplicaEntryByTypeAndEntityId1CallCount.get(), 2);
            assertEquals(findReplicaEntryByTypeAndEntityId2CallCount.get(), 3);

            safeAction(() -> assertEquals(registry.findAllReplicaTypes().size(), 2));
            safeAction(() ->
                    assertEquals(registry.findAllReplicaEntriesByType(A.class).size(), 2));
            safeAction(() ->
                    assertEquals(registry.findAllReplicaEntriesByType(B.class).size(), 1));
            safeAction(() -> assertNotNull(registry.findReplicaEntryByTypeAndEntityId(A.class, 1)));
            safeAction(() -> assertNotNull(registry.findReplicaEntryByTypeAndEntityId(A.class, 2)));
        }

        // Dispose replicaEntry of different type
        {
            safeAction(() -> {
                final ReplicaEntry replicaEntry = registry.findReplicaEntryByTypeAndEntityId(B.class, -53);
                assertNotNull(replicaEntry);
                Disposable.dispose(replicaEntry);
            });

            assertEquals(findAllEntityTypesCallCount.get(), 5);
            assertEquals(findAllReplicaEntriesByTypeACallCount.get(), 5);
            assertEquals(findAllReplicaEntriesByTypeBCallCount.get(), 5);
            assertEquals(findReplicaEntryByTypeAndEntityId1CallCount.get(), 2);
            assertEquals(findReplicaEntryByTypeAndEntityId2CallCount.get(), 3);

            safeAction(() -> assertEquals(registry.findAllReplicaTypes().size(), 1));
            safeAction(() ->
                    assertEquals(registry.findAllReplicaEntriesByType(A.class).size(), 2));
            safeAction(() ->
                    assertEquals(registry.findAllReplicaEntriesByType(B.class).size(), 0));
            safeAction(() -> assertNotNull(registry.findReplicaEntryByTypeAndEntityId(A.class, 1)));
            safeAction(() -> assertNotNull(registry.findReplicaEntryByTypeAndEntityId(A.class, 2)));
        }

        // Dispose replicaEntry of A type
        {
            safeAction(() -> {
                final ReplicaEntry replicaEntry = registry.findReplicaEntryByTypeAndEntityId(A.class, 1);
                assertNotNull(replicaEntry);
                Disposable.dispose(replicaEntry);
            });

            assertEquals(findAllEntityTypesCallCount.get(), 6);
            assertEquals(findAllReplicaEntriesByTypeACallCount.get(), 6);
            assertEquals(findAllReplicaEntriesByTypeBCallCount.get(), 6);
            assertEquals(findReplicaEntryByTypeAndEntityId1CallCount.get(), 3);
            assertEquals(findReplicaEntryByTypeAndEntityId2CallCount.get(), 3);

            safeAction(() -> assertEquals(registry.findAllReplicaTypes().size(), 1));
            safeAction(() ->
                    assertEquals(registry.findAllReplicaEntriesByType(A.class).size(), 1));
            safeAction(() ->
                    assertEquals(registry.findAllReplicaEntriesByType(B.class).size(), 0));
            safeAction(() -> assertNull(registry.findReplicaEntryByTypeAndEntityId(A.class, 1)));
            safeAction(() -> assertNotNull(registry.findReplicaEntryByTypeAndEntityId(A.class, 2)));
        }
    }

    @Test
    public void unlinkReplicaEntry_missingType() {
        final ReplicaRegistry registry = Replicant.context().getReplicaRegistry();

        final ReplicaEntry replicaEntry = ReplicaEntry.create(null, "A", A.class, 1);

        final IllegalStateException exception = expectThrows(
                IllegalStateException.class, () -> safeAction(() -> registry.unlinkReplicaEntry(replicaEntry)));

        assertEquals(exception.getMessage(), "Replica type A not present in ReplicaRegistry");
    }

    @Test
    public void unlinkReplicaEntry_missingInstance() {
        final ReplicaRegistry registry = Replicant.context().getReplicaRegistry();

        safeAction(() -> registry.findOrCreateReplicaEntry("A/1", A.class, 1));
        final ReplicaEntry replicaEntry = ReplicaEntry.create(null, "A/2", A.class, 2);

        final IllegalStateException exception = expectThrows(
                IllegalStateException.class, () -> safeAction(() -> registry.unlinkReplicaEntry(replicaEntry)));

        assertEquals(exception.getMessage(), "Replica Entry A/2 not present in ReplicaRegistry");
    }

    @Test
    public void disposedEntityNeverReturned() {
        final ReplicaRegistry registry = Replicant.context().getReplicaRegistry();

        final AtomicInteger findAllEntityTypesCallCount = new AtomicInteger();
        observer(() -> {
            if (Disposable.isNotDisposed(registry)) {
                // Access observable next line
                registry.findAllReplicaTypes();
            }

            findAllEntityTypesCallCount.incrementAndGet();
        });

        final AtomicInteger findAllReplicaEntriesByTypeACallCount = new AtomicInteger();
        observer(() -> {
            if (Disposable.isNotDisposed(registry)) {
                // Access observable next line
                registry.findAllReplicaEntriesByType(A.class);
            }

            findAllReplicaEntriesByTypeACallCount.incrementAndGet();
        });

        final AtomicInteger findReplicaEntryByTypeAndEntityId1CallCount = new AtomicInteger();
        observer(() -> {
            if (Disposable.isNotDisposed(registry)) {
                // Access observable next line
                registry.findReplicaEntryByTypeAndEntityId(A.class, 1);
            }

            findReplicaEntryByTypeAndEntityId1CallCount.incrementAndGet();
        });

        assertEquals(findAllEntityTypesCallCount.get(), 1);
        assertEquals(findAllReplicaEntriesByTypeACallCount.get(), 1);
        assertEquals(findReplicaEntryByTypeAndEntityId1CallCount.get(), 1);

        safeAction(() -> assertEquals(registry.findAllReplicaTypes().size(), 0));
        safeAction(
                () -> assertEquals(registry.findAllReplicaEntriesByType(A.class).size(), 0));
        safeAction(() -> assertNull(registry.findReplicaEntryByTypeAndEntityId(A.class, 1)));

        {
            safeAction(() -> registry.findOrCreateReplicaEntry("A/1", A.class, 1));

            assertEquals(findAllEntityTypesCallCount.get(), 2);
            assertEquals(findAllReplicaEntriesByTypeACallCount.get(), 2);
            assertEquals(findReplicaEntryByTypeAndEntityId1CallCount.get(), 2);

            safeAction(() -> assertEquals(registry.findAllReplicaTypes().size(), 1));
            safeAction(() ->
                    assertEquals(registry.findAllReplicaEntriesByType(A.class).size(), 1));
            safeAction(() -> assertNotNull(registry.findReplicaEntryByTypeAndEntityId(A.class, 1)));
            safeAction(() -> assertEquals(
                    Objects.requireNonNull(registry.getReplicaEntries().get(A.class))
                            .size(),
                    1));
        }

        // Dispose replicaEntry
        {
            final Disposable schedulerLock = pauseScheduler();
            safeAction(() -> {
                final ReplicaEntry replicaEntry = registry.findReplicaEntryByTypeAndEntityId(A.class, 1);
                assertNotNull(replicaEntry);
                Disposable.dispose(replicaEntry);
            });

            assertEquals(findAllEntityTypesCallCount.get(), 2);
            assertEquals(findAllReplicaEntriesByTypeACallCount.get(), 2);
            assertEquals(findReplicaEntryByTypeAndEntityId1CallCount.get(), 2);

            safeAction(() -> assertNull(registry.getReplicaEntries().get(A.class)));
            // Oddity - we have a type with 0 members. Can happen during deletion
            safeAction(() -> assertEquals(registry.findAllReplicaTypes().size(), 0));
            safeAction(() ->
                    assertEquals(registry.findAllReplicaEntriesByType(A.class).size(), 0));
            safeAction(() -> assertNull(registry.findReplicaEntryByTypeAndEntityId(A.class, 1)));

            schedulerLock.dispose();

            assertEquals(findAllEntityTypesCallCount.get(), 3);
            assertEquals(findAllReplicaEntriesByTypeACallCount.get(), 3);
            assertEquals(findReplicaEntryByTypeAndEntityId1CallCount.get(), 3);

            safeAction(() -> assertEquals(registry.getReplicaEntries().size(), 0));
            safeAction(() -> assertEquals(registry.findAllReplicaTypes().size(), 0));
            safeAction(() ->
                    assertEquals(registry.findAllReplicaEntriesByType(A.class).size(), 0));
            safeAction(() -> assertNull(registry.findReplicaEntryByTypeAndEntityId(A.class, 1)));
        }
    }

    private static class A {}

    private static class B {}
}
