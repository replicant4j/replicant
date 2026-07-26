package replicant;

import static org.testng.Assert.*;

import org.testng.annotations.Test;

public class CacheEntryTest extends AbstractReplicantTest {
    @Test
    public void basicOperation() {
        final DatasetAddress datasetAddress =
                new DatasetAddress(ValueUtil.randomInt(), ValueUtil.randomInt(), ValueUtil.randomInt());
        final String eTag = ValueUtil.randomString();
        final String content = ValueUtil.randomString();
        final CacheEntry entry = new CacheEntry(datasetAddress, eTag, content);

        assertEquals(entry.getDatasetAddress(), datasetAddress);
        assertEquals(entry.getETag(), eTag);
        assertEquals(entry.getContent(), content);
    }
}
