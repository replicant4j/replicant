package replicant.server.transport;

import static org.testng.Assert.*;

import java.util.UUID;
import org.testng.annotations.Test;
import replicant.server.ChangeSet;
import replicant.server.DatasetAddress;

public class DatasetCacheEntryTest {
    @Test
    public void basicOperation() {
        final var datasetAddress = DatasetAddress.of(1, null);
        final var entry = new DatasetCacheEntry(datasetAddress);
        assertEquals(entry.getDatasetAddress(), datasetAddress);

        assertNotNull(entry.getLock());
        expectThrows(NullPointerException.class, entry::getDatasetCacheVersion);
        expectThrows(NullPointerException.class, entry::getChangeSet);

        final var changeSet = new ChangeSet();
        entry.init(changeSet);

        assertEquals(UUID.fromString(entry.getDatasetCacheVersion()).toString(), entry.getDatasetCacheVersion());
        assertEquals(entry.getChangeSet(), changeSet);

        final var other = new DatasetCacheEntry(datasetAddress);
        other.init(new ChangeSet());
        assertNotEquals(other.getDatasetCacheVersion(), entry.getDatasetCacheVersion());
    }
}
