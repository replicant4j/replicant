package replicant;

import static org.testng.Assert.*;

import org.testng.annotations.Test;

public class DatasetCacheEntryTest extends AbstractReplicantTest {
    @Test
    public void basicOperation() {
        final DatasetAddress datasetAddress =
                new DatasetAddress(ValueUtil.randomInt(), ValueUtil.randomInt(), ValueUtil.randomInt());
        final String datasetCacheVersion = ValueUtil.randomString();
        final String changeSet = ValueUtil.randomString();
        final DatasetCacheEntry entry = new DatasetCacheEntry(datasetAddress, datasetCacheVersion, changeSet);

        assertEquals(entry.getDatasetAddress(), datasetAddress);
        assertEquals(entry.getDatasetCacheVersion(), datasetCacheVersion);
        assertEquals(entry.getChangeSet(), changeSet);
    }
}
