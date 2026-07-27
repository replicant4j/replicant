package replicant.messages;

import static org.testng.Assert.*;

import org.testng.annotations.Test;
import replicant.AbstractReplicantTest;
import replicant.ValueUtil;

public class UpdateMessageTest extends AbstractReplicantTest {
    @Test
    public void construct() {
        final EntityChange[] entityChanges = {};
        final String[] subscriptionChanges = {};
        final SubscriptionChangeMessage[] filterParameterSubscriptionChanges = new SubscriptionChangeMessage[0];

        final int requestId = ValueUtil.randomInt();
        final String datasetCacheVersion = ValueUtil.randomString();

        final UpdateMessage updateMessage = UpdateMessage.create(
                requestId,
                datasetCacheVersion,
                subscriptionChanges,
                filterParameterSubscriptionChanges,
                entityChanges,
                null);

        assertEquals(updateMessage.getRequestId(), (Integer) requestId);
        assertEquals(updateMessage.getDatasetCacheVersion(), datasetCacheVersion);
        assertEquals(updateMessage.getEntityChanges(), entityChanges);
        assertTrue(updateMessage.hasEntityChanges());
        assertTrue(updateMessage.hasSubscriptionChanges());
        assertTrue(updateMessage.hasFilterParameterSubscriptionChanges());
        assertEquals(updateMessage.getSubscriptionChanges(), subscriptionChanges);
        assertEquals(updateMessage.getFilterParameterSubscriptionChanges(), filterParameterSubscriptionChanges);

        updateMessage.validate();
    }

    @Test
    public void construct_NoChanges() {
        final UpdateMessage updateMessage = UpdateMessage.create(null, null, null, null, null, null);

        assertNull(updateMessage.getRequestId());
        assertNull(updateMessage.getDatasetCacheVersion());
        assertFalse(updateMessage.hasEntityChanges());
        assertFalse(updateMessage.hasSubscriptionChanges());
        assertFalse(updateMessage.hasFilterParameterSubscriptionChanges());

        updateMessage.validate();
    }

    @Test
    public void validate_whereAllOK() {
        final String[] subscriptionChanges = new String[] {"+1", "+2.50", "+3.50", "+4.23", "+4.24", "+4.25", "+5.1"};
        final EntityChange[] entityChanges = new EntityChange[] {
            EntityChange.create(1, 1, new String[0]),
            EntityChange.create(1, 2, new String[0], new EntityChangeDataImpl()),
            EntityChange.create(1, 3, new String[0], new EntityChangeDataImpl()),
            EntityChange.create(1, 4, new String[0]),
            EntityChange.create(2, 33, new String[0], new EntityChangeDataImpl()),
            EntityChange.create(3, 34, new String[0], new EntityChangeDataImpl()),
            EntityChange.create(4, 1, new String[0])
        };

        final UpdateMessage updateMessage =
                UpdateMessage.create(null, null, subscriptionChanges, null, entityChanges, null);

        updateMessage.validate();
    }

    @Test
    public void validate_duplicateSubscriptionChanges_typeDataset() {
        final String[] subscriptionChanges = new String[] {"+1", "+2.50", "+3.50", "+4.23", "+1"};

        final UpdateMessage updateMessage = UpdateMessage.create(null, null, subscriptionChanges, null, null, null);

        final IllegalStateException exception = expectThrows(IllegalStateException.class, updateMessage::validate);
        assertEquals(
                exception.getMessage(),
                "Replicant-0022: UpdateMessage contains multiple Subscription changes for Dataset Address 1.");
    }

    @Test
    public void validate_duplicateSubscriptionChanges_instanceDataset() {
        final SubscriptionChangeMessage[] subscriptionChanges = new SubscriptionChangeMessage[] {
            SubscriptionChangeMessage.create("+2.50", "XX"), SubscriptionChangeMessage.create("=2.50", "XY")
        };

        final UpdateMessage updateMessage =
                UpdateMessage.create(null, null, new String[] {"+1", "+4.23"}, subscriptionChanges, null, null);

        final IllegalStateException exception = expectThrows(IllegalStateException.class, updateMessage::validate);
        assertEquals(
                exception.getMessage(),
                "Replicant-0028: UpdateMessage contains multiple Subscription changes for Dataset Address 2.50.");
    }

    @Test
    public void validate_duplicateEntityChanges() {
        final EntityChange[] entityChanges = new EntityChange[] {
            EntityChange.create(1, 1, new String[0], new EntityChangeDataImpl()),
            EntityChange.create(1, 2, new String[0], new EntityChangeDataImpl()),
            EntityChange.create(1, 3, new String[0], new EntityChangeDataImpl()),
            EntityChange.create(1, 4, new String[0]),
            EntityChange.create(2, 33, new String[0], new EntityChangeDataImpl()),
            EntityChange.create(3, 34, new String[0], new EntityChangeDataImpl()),
            EntityChange.create(1, 1, new String[0])
        };

        final UpdateMessage updateMessage = UpdateMessage.create(null, null, null, null, entityChanges, null);

        final IllegalStateException exception = expectThrows(IllegalStateException.class, updateMessage::validate);
        assertEquals(
                exception.getMessage(),
                "Replicant-0014: UpdateMessage contains multiple EntityChange messages with the id '1.1'.");
    }

    @Test
    public void getSubscriptionChanges_WhenNone() {
        final UpdateMessage updateMessage = UpdateMessage.create(null, null, null, null, null, null);

        final IllegalStateException exception =
                expectThrows(IllegalStateException.class, updateMessage::getSubscriptionChanges);
        assertEquals(
                exception.getMessage(),
                "Replicant-0013: UpdateMessage.getSubscriptionChanges() invoked when no changes are present. Should"
                        + " guard call with UpdateMessage.hasSubscriptionChanges().");
    }

    @Test
    public void getFilterParameterSubscriptionChanges_WhenNone() {
        final UpdateMessage updateMessage = UpdateMessage.create(null, null, null, null, null, null);

        final IllegalStateException exception =
                expectThrows(IllegalStateException.class, updateMessage::getFilterParameterSubscriptionChanges);
        assertEquals(
                exception.getMessage(),
                "Replicant-0030: UpdateMessage.getFilterParameterSubscriptionChanges() invoked when no changes are"
                        + " present. Should guard call with UpdateMessage.hasFilterParameterSubscriptionChanges().");
    }

    @Test
    public void getEntityChanges_WhenNone() {
        final UpdateMessage updateMessage = UpdateMessage.create(null, null, null, null, null, null);

        final IllegalStateException exception =
                expectThrows(IllegalStateException.class, updateMessage::getEntityChanges);
        assertEquals(
                exception.getMessage(),
                "Replicant-0012: UpdateMessage.getEntityChanges() invoked when no changes are present. Should guard"
                        + " call with UpdateMessage.hasEntityChanges().");
    }
}
