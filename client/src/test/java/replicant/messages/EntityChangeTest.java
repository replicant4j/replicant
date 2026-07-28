package replicant.messages;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

import org.testng.annotations.Test;
import replicant.AbstractReplicantTest;

public class EntityChangeTest extends AbstractReplicantTest {
    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    public void construct_removal() {
        final String[] datasetAddresses = {"0", "3.4"};
        final EntityChange change = EntityChange.create(2, 1, datasetAddresses);

        assertEquals(change.getId(), "2.1");
        assertEquals(change.getDatasetAddresses(), datasetAddresses);
        assertTrue(change.isRemove());
        assertFalse(change.isUpdate());
        assertThrows(change::getPayload);
    }

    @Test
    public void construct_update() {
        final String[] datasetAddresses = {"0", "3.4"};
        final EntityChangePayload payload = mock(EntityChangePayload.class);
        final EntityChange change = EntityChange.create(2, 1, datasetAddresses, payload);

        assertEquals(change.getId(), "2.1");
        assertEquals(change.getDatasetAddresses(), datasetAddresses);
        assertFalse(change.isRemove());
        assertTrue(change.isUpdate());
        assertEquals(change.getPayload(), payload);
    }
}
