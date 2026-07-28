package replicant.server;

import static org.testng.Assert.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class EntityChangeCandidateSorterTest {
    private int _nextEntityId;

    @BeforeMethod
    public void resetNextID() {
        _nextEntityId = 0;
    }

    @Test
    public void sort() {
        final var m1 = newEntityChangeCandidate(1, 1, 10L, false);
        final var m2 = newEntityChangeCandidate(2, 2, 100L, false);
        final var m3 = newEntityChangeCandidate(3, 2, 10L, false);
        final var m4 = newEntityChangeCandidate(4, 3, 10L, true);
        final var m5 = newEntityChangeCandidate(5, 3, 100L, true);

        {
            final var l1 = Arrays.asList(m1, m2, m3, m4, m5);
            final var r1 = EntityChangeCandidateSorter.sort(l1);
            assertIndex(r1, 0, 5);
            assertIndex(r1, 1, 4);
            assertIndex(r1, 2, 1);
            assertIndex(r1, 3, 3);
            assertIndex(r1, 4, 2);
        }

        {
            final var l2 = Arrays.asList(m5, m4, m3, m2, m1);
            final var r2 = EntityChangeCandidateSorter.sort(l2);
            assertIndex(r2, 0, 5);
            assertIndex(r2, 1, 4);
            assertIndex(r2, 2, 1);
            assertIndex(r2, 3, 3);
            assertIndex(r2, 4, 2);
        }

        {
            final var l2 = Arrays.asList(m1, m1, m2, m2, m1);
            final var r2 = EntityChangeCandidateSorter.sort(l2);
            assertIndex(r2, 0, 1);
            assertIndex(r2, 1, 1);
            assertIndex(r2, 2, 1);
            assertIndex(r2, 3, 2);
            assertIndex(r2, 4, 2);
        }

        {
            final var l2 = Arrays.asList(m4, m4, m5, m5, m4);
            final var r2 = EntityChangeCandidateSorter.sort(l2);
            assertIndex(r2, 0, 5);
            assertIndex(r2, 1, 5);
            assertIndex(r2, 2, 4);
            assertIndex(r2, 3, 4);
            assertIndex(r2, 4, 4);
        }
    }

    private void assertIndex(final List<EntityChangeCandidate> l1, final int index, final int value) {
        assertEquals(l1.get(index).getEntityId(), value);
    }

    private EntityChangeCandidate newEntityChangeCandidate(
            final int entityId, final int entityTypeId, final long timestamp, final boolean isDelete) {
        return new EntityChangeCandidate(
                entityId, entityTypeId, timestamp, new HashMap<>(), isDelete ? null : new HashMap<>(), null);
    }

    @Test
    public void deletionsShouldOrderBeforeChanges() {
        final var candidates = new EntityChangeCandidate[] {
            createDeletionCandidate(1), createUpdateCandidate(1), createDeletionCandidate(1)
        };

        final var sortedCandidates = EntityChangeCandidateSorter.sort(Arrays.asList(candidates));

        assertDeletion(sortedCandidates.get(0));
        assertDeletion(sortedCandidates.get(1));
        assertUpdate(sortedCandidates.get(2));
    }

    @Test
    public void typesShouldOrderDescendingWithinDeletions() {
        final var candidates = new EntityChangeCandidate[] {
            createUpdateCandidate(1),
            createDeletionCandidate(1),
            createDeletionCandidate(3),
            createDeletionCandidate(2),
            createDeletionCandidate(4)
        };

        final var sortedCandidates = EntityChangeCandidateSorter.sort(Arrays.asList(candidates));

        assertEquals(sortedCandidates.get(0).getEntityTypeId(), 4);
        assertEquals(sortedCandidates.get(1).getEntityTypeId(), 3);
        assertEquals(sortedCandidates.get(2).getEntityTypeId(), 2);
        assertEquals(sortedCandidates.get(3).getEntityTypeId(), 1);
        assertUpdate(sortedCandidates.get(4));
    }

    @Test
    public void typesShouldOrderAscendingWithinUpdates() {
        final var candidates = new EntityChangeCandidate[] {
            createUpdateCandidate(1),
            createUpdateCandidate(3),
            createUpdateCandidate(2),
            createUpdateCandidate(4),
            createDeletionCandidate(2)
        };

        final var sortedCandidates = EntityChangeCandidateSorter.sort(Arrays.asList(candidates));

        assertDeletion(sortedCandidates.get(0));
        assertEquals(sortedCandidates.get(1).getEntityTypeId(), 1);
        assertEquals(sortedCandidates.get(2).getEntityTypeId(), 2);
        assertEquals(sortedCandidates.get(3).getEntityTypeId(), 3);
        assertEquals(sortedCandidates.get(4).getEntityTypeId(), 4);
    }

    @Test
    public void deletionForSameTypeShouldOrderByReverseTime() {
        final var candidates = new EntityChangeCandidate[] {
            createDeletionCandidate(2, 10),
            createDeletionCandidate(1, 15),
            createDeletionCandidate(2, 20),
            createDeletionCandidate(2, 15)
        };

        final var sortedCandidates = EntityChangeCandidateSorter.sort(Arrays.asList(candidates));

        assertEquals(sortedCandidates.get(0).getTimestamp(), 20);
        assertEquals(sortedCandidates.get(1).getTimestamp(), 15);
        assertEquals(sortedCandidates.get(2).getTimestamp(), 10);
        assertEquals(sortedCandidates.get(3).getEntityTypeId(), 1);
    }

    @Test
    public void updateForSameTypeShouldOrderByTime() {
        final var candidates = new EntityChangeCandidate[] {
            createUpdateCandidate(2, 10),
            createUpdateCandidate(1, 15),
            createUpdateCandidate(2, 20),
            createUpdateCandidate(2, 15)
        };

        final var sortedCandidates = EntityChangeCandidateSorter.sort(Arrays.asList(candidates));

        assertEquals(sortedCandidates.get(0).getEntityTypeId(), 1);
        assertEquals(sortedCandidates.get(1).getTimestamp(), 10);
        assertEquals(sortedCandidates.get(2).getTimestamp(), 15);
        assertEquals(sortedCandidates.get(3).getTimestamp(), 20);
    }

    private EntityChangeCandidate createUpdateCandidate(final int entityTypeId) {
        return createUpdateCandidate(entityTypeId, 0);
    }

    private EntityChangeCandidate createUpdateCandidate(final int entityTypeId, final long time) {
        return new EntityChangeCandidate(_nextEntityId++, entityTypeId, time, new HashMap<>(), new HashMap<>(), null);
    }

    private EntityChangeCandidate createDeletionCandidate(final int entityTypeId) {
        return createDeletionCandidate(entityTypeId, 0);
    }

    private EntityChangeCandidate createDeletionCandidate(final int entityTypeId, final long time) {
        return new EntityChangeCandidate(_nextEntityId++, entityTypeId, time, new HashMap<>(), null, null);
    }

    private void assertDeletion(final EntityChangeCandidate candidate) {
        assertTrue(candidate.isDelete(), "Expected " + candidate + " to be a deletion");
    }

    private void assertUpdate(final EntityChangeCandidate candidate) {
        assertTrue(candidate.isUpdate(), "Expected " + candidate + " to be an update");
    }
}
