package replicant;

import static org.testng.Assert.*;

import org.testng.annotations.Test;

public class AreaOfInterestRequestTest extends AbstractReplicantTest {
    @Test
    public void basicOperation() {
        final DatasetAddress datasetAddress = new DatasetAddress(1, 2);
        final Object filter = null;
        final AreaOfInterestRequest.Type action = AreaOfInterestRequest.Type.ADD;

        final AreaOfInterestRequest entry = new AreaOfInterestRequest(datasetAddress, action, filter);

        assertEquals(entry.getDatasetAddress(), datasetAddress);
        assertEquals(entry.getType(), action);
        assertEquals(entry.toString(), "AreaOfInterestRequest[Type=ADD Address=1.2]");
        assertEquals(entry.getFilter(), filter);
        assertTrue(entry.match(action, datasetAddress, filter));
        assertFalse(entry.match(action, datasetAddress, "OtherFilter"));
        assertFalse(entry.match(AreaOfInterestRequest.Type.REMOVE, datasetAddress, filter));
        assertFalse(entry.match(action, new DatasetAddress(1, 3, ValueUtil.randomInt()), filter));

        assertFalse(entry.isInProgress());
        entry.markAsInProgress(1);
        assertTrue(entry.isInProgress());
        entry.markAsComplete();
        assertFalse(entry.isInProgress());
    }

    @Test
    public void construct_withNOnNullFIlterAndRemoveAction() {
        final IllegalStateException exception = expectThrows(
                IllegalStateException.class,
                () -> new AreaOfInterestRequest(new DatasetAddress(1, 2), AreaOfInterestRequest.Type.REMOVE, "XXX"));

        assertEquals(
                exception.getMessage(),
                "Replicant-0027: AreaOfInterestRequest constructor passed a REMOVE request for Dataset Address '1.2'"
                        + " with a non-null filter 'XXX'.");
    }

    @Test
    public void toString_WithFilter() {
        final AreaOfInterestRequest entry =
                new AreaOfInterestRequest(new DatasetAddress(1, 2), AreaOfInterestRequest.Type.UPDATE, "XXX");

        assertEquals(entry.toString(), "AreaOfInterestRequest[Type=UPDATE Address=1.2 Filter=XXX]");
    }

    @Test
    public void toString_NamingDisabled() {
        ReplicantTestUtil.disableNames();
        final AreaOfInterestRequest entry =
                new AreaOfInterestRequest(new DatasetAddress(1, 2), AreaOfInterestRequest.Type.UPDATE, "XXX");

        assertEquals(
                entry.toString(),
                "replicant.AreaOfInterestRequest@" + Integer.toHexString(System.identityHashCode(entry)));
    }

    @Test
    public void removeActionIgnoredFilterDuringMatch() {
        final DatasetAddress datasetAddress = new DatasetAddress(1, 2);
        final AreaOfInterestRequest.Type action = AreaOfInterestRequest.Type.REMOVE;

        final AreaOfInterestRequest entry = new AreaOfInterestRequest(datasetAddress, action, null);

        assertTrue(entry.match(action, datasetAddress, null));
        assertTrue(entry.match(action, datasetAddress, "OtherFilter"));
        assertFalse(entry.match(AreaOfInterestRequest.Type.ADD, datasetAddress, null));
    }
}
