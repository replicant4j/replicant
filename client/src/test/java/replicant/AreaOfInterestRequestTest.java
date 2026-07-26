package replicant;

import static org.testng.Assert.*;

import org.testng.annotations.Test;

public class AreaOfInterestRequestTest extends AbstractReplicantTest {
    @Test
    public void basicOperation() {
        final DatasetAddress datasetAddress = new DatasetAddress(1, 2);
        final Object filterParameter = null;
        final AreaOfInterestRequest.Type action = AreaOfInterestRequest.Type.ADD;

        final AreaOfInterestRequest entry = new AreaOfInterestRequest(datasetAddress, action, filterParameter);

        assertEquals(entry.getDatasetAddress(), datasetAddress);
        assertEquals(entry.getType(), action);
        assertEquals(entry.toString(), "AreaOfInterestRequest[Type=ADD Address=1.2]");
        assertEquals(entry.getFilterParameter(), filterParameter);
        assertTrue(entry.match(action, datasetAddress, filterParameter));
        assertFalse(entry.match(action, datasetAddress, "OtherFilter"));
        assertFalse(entry.match(AreaOfInterestRequest.Type.REMOVE, datasetAddress, filterParameter));
        assertFalse(entry.match(action, new DatasetAddress(1, 3, ValueUtil.randomInt()), filterParameter));

        assertFalse(entry.isInProgress());
        entry.markAsInProgress(1);
        assertTrue(entry.isInProgress());
        entry.markAsComplete();
        assertFalse(entry.isInProgress());
    }

    @Test
    public void construct_withNonNullFilterParameterAndRemoveAction() {
        final IllegalStateException exception = expectThrows(
                IllegalStateException.class,
                () -> new AreaOfInterestRequest(new DatasetAddress(1, 2), AreaOfInterestRequest.Type.REMOVE, "XXX"));

        assertEquals(
                exception.getMessage(),
                "Replicant-0027: AreaOfInterestRequest constructor passed a REMOVE request for Dataset Address '1.2'"
                        + " with a non-null Filter Parameter 'XXX'.");
    }

    @Test
    public void toString_WithFilterParameter() {
        final AreaOfInterestRequest entry =
                new AreaOfInterestRequest(new DatasetAddress(1, 2), AreaOfInterestRequest.Type.UPDATE, "XXX");

        assertEquals(entry.toString(), "AreaOfInterestRequest[Type=UPDATE Address=1.2 Filter Parameter=XXX]");
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
    public void removeActionIgnoredFilterParameterDuringMatch() {
        final DatasetAddress datasetAddress = new DatasetAddress(1, 2);
        final AreaOfInterestRequest.Type action = AreaOfInterestRequest.Type.REMOVE;

        final AreaOfInterestRequest entry = new AreaOfInterestRequest(datasetAddress, action, null);

        assertTrue(entry.match(action, datasetAddress, null));
        assertTrue(entry.match(action, datasetAddress, "OtherFilter"));
        assertFalse(entry.match(AreaOfInterestRequest.Type.ADD, datasetAddress, null));
    }
}
