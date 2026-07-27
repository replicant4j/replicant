package replicant;

import static org.testng.Assert.*;

import org.testng.annotations.Test;

public class SubscriptionOperationTest extends AbstractReplicantTest {
    @Test
    public void basicOperation() {
        final DatasetAddress datasetAddress = new DatasetAddress(1, 2);
        final Object filterParameter = null;
        final SubscriptionOperation.Type type = SubscriptionOperation.Type.SUBSCRIBE;

        final SubscriptionOperation entry = new SubscriptionOperation(datasetAddress, type, filterParameter);

        assertEquals(entry.getDatasetAddress(), datasetAddress);
        assertEquals(entry.getType(), type);
        assertEquals(entry.toString(), "SubscriptionOperation[Type=SUBSCRIBE Address=1.2]");
        assertEquals(entry.getFilterParameter(), filterParameter);
        assertTrue(entry.match(type, datasetAddress, filterParameter));
        assertFalse(entry.match(type, datasetAddress, "OtherFilter"));
        assertFalse(entry.match(SubscriptionOperation.Type.UNSUBSCRIBE, datasetAddress, filterParameter));
        assertFalse(entry.match(type, new DatasetAddress(1, 3, ValueUtil.randomInt()), filterParameter));

        assertFalse(entry.isInProgress());
        entry.markAsInProgress(1);
        assertTrue(entry.isInProgress());
        entry.markAsComplete();
        assertFalse(entry.isInProgress());
    }

    @Test
    public void construct_withNonNullFilterParameterAndUnsubscribeOperation() {
        final IllegalStateException exception = expectThrows(
                IllegalStateException.class,
                () -> new SubscriptionOperation(
                        new DatasetAddress(1, 2), SubscriptionOperation.Type.UNSUBSCRIBE, "XXX"));

        assertEquals(
                exception.getMessage(),
                "Replicant-0027: SubscriptionOperation constructor passed an UNSUBSCRIBE operation for Dataset Address"
                        + " '1.2' with a non-null Filter Parameter 'XXX'.");
    }

    @Test
    public void toString_WithFilterParameter() {
        final SubscriptionOperation entry =
                new SubscriptionOperation(new DatasetAddress(1, 2), SubscriptionOperation.Type.UPDATE, "XXX");

        assertEquals(entry.toString(), "SubscriptionOperation[Type=UPDATE Address=1.2 Filter Parameter=XXX]");
    }

    @Test
    public void toString_NamingDisabled() {
        ReplicantTestUtil.disableNames();
        final SubscriptionOperation entry =
                new SubscriptionOperation(new DatasetAddress(1, 2), SubscriptionOperation.Type.UPDATE, "XXX");

        assertEquals(
                entry.toString(),
                "replicant.SubscriptionOperation@" + Integer.toHexString(System.identityHashCode(entry)));
    }

    @Test
    public void unsubscribeOperationIgnoresFilterParameterDuringMatch() {
        final DatasetAddress datasetAddress = new DatasetAddress(1, 2);
        final SubscriptionOperation.Type type = SubscriptionOperation.Type.UNSUBSCRIBE;

        final SubscriptionOperation entry = new SubscriptionOperation(datasetAddress, type, null);

        assertTrue(entry.match(type, datasetAddress, null));
        assertTrue(entry.match(type, datasetAddress, "OtherFilter"));
        assertFalse(entry.match(SubscriptionOperation.Type.SUBSCRIBE, datasetAddress, null));
    }
}
