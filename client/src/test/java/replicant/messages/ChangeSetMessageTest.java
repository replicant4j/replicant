package replicant.messages;

import static org.testng.Assert.*;

import org.testng.annotations.Test;
import replicant.AbstractReplicantTest;
import replicant.ValueUtil;

public class ChangeSetMessageTest extends AbstractReplicantTest {
    @Test
    public void construct() {
        assertEquals(ChangeSetMessage.TYPE, "change-set");

        final EntityChange[] entityChanges = {};
        final String[] subscriptionChanges = {};
        final SubscriptionChangeMessage[] filterParameterSubscriptionChanges = new SubscriptionChangeMessage[0];

        final int requestId = ValueUtil.randomInt();
        final String datasetCacheVersion = ValueUtil.randomString();

        final ChangeSetMessage changeSet = ChangeSetMessage.create(
                requestId,
                datasetCacheVersion,
                subscriptionChanges,
                filterParameterSubscriptionChanges,
                entityChanges,
                null);

        assertEquals(changeSet.getRequestId(), (Integer) requestId);
        assertEquals(changeSet.getDatasetCacheVersion(), datasetCacheVersion);
        assertEquals(changeSet.getEntityChanges(), entityChanges);
        assertTrue(changeSet.hasEntityChanges());
        assertTrue(changeSet.hasSubscriptionChanges());
        assertTrue(changeSet.hasFilterParameterSubscriptionChanges());
        assertEquals(changeSet.getSubscriptionChanges(), subscriptionChanges);
        assertEquals(changeSet.getFilterParameterSubscriptionChanges(), filterParameterSubscriptionChanges);

        changeSet.validate();
    }

    @Test
    public void construct_NoChanges() {
        final ChangeSetMessage changeSet = ChangeSetMessage.create(null, null, null, null, null, null);

        assertNull(changeSet.getRequestId());
        assertNull(changeSet.getDatasetCacheVersion());
        assertFalse(changeSet.hasEntityChanges());
        assertFalse(changeSet.hasSubscriptionChanges());
        assertFalse(changeSet.hasFilterParameterSubscriptionChanges());

        changeSet.validate();
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

        final ChangeSetMessage changeSet =
                ChangeSetMessage.create(null, null, subscriptionChanges, null, entityChanges, null);

        changeSet.validate();
    }

    @Test
    public void validate_duplicateSubscriptionChanges_typeDataset() {
        final String[] subscriptionChanges = new String[] {"+1", "+2.50", "+3.50", "+4.23", "+1"};

        final ChangeSetMessage changeSet = ChangeSetMessage.create(null, null, subscriptionChanges, null, null, null);

        final IllegalStateException exception = expectThrows(IllegalStateException.class, changeSet::validate);
        assertEquals(
                exception.getMessage(),
                "Replicant-0022: ChangeSetMessage contains multiple Subscription Changes for Dataset Address 1.");
    }

    @Test
    public void validate_duplicateSubscriptionChanges_instanceDataset() {
        final SubscriptionChangeMessage[] subscriptionChanges = new SubscriptionChangeMessage[] {
            SubscriptionChangeMessage.create("+2.50", "XX"), SubscriptionChangeMessage.create("=2.50", "XY")
        };

        final ChangeSetMessage changeSet =
                ChangeSetMessage.create(null, null, new String[] {"+1", "+4.23"}, subscriptionChanges, null, null);

        final IllegalStateException exception = expectThrows(IllegalStateException.class, changeSet::validate);
        assertEquals(
                exception.getMessage(),
                "Replicant-0028: ChangeSetMessage contains multiple Subscription Changes for Dataset Address 2.50.");
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

        final ChangeSetMessage changeSet = ChangeSetMessage.create(null, null, null, null, entityChanges, null);

        final IllegalStateException exception = expectThrows(IllegalStateException.class, changeSet::validate);
        assertEquals(
                exception.getMessage(),
                "Replicant-0014: ChangeSetMessage contains multiple Entity Changes with the id '1.1'.");
    }

    @Test
    public void getSubscriptionChanges_WhenNone() {
        final ChangeSetMessage changeSet = ChangeSetMessage.create(null, null, null, null, null, null);

        final IllegalStateException exception =
                expectThrows(IllegalStateException.class, changeSet::getSubscriptionChanges);
        assertEquals(
                exception.getMessage(),
                "Replicant-0013: ChangeSetMessage.getSubscriptionChanges() invoked when no changes are present. Should"
                        + " guard call with ChangeSetMessage.hasSubscriptionChanges().");
    }

    @Test
    public void getFilterParameterSubscriptionChanges_WhenNone() {
        final ChangeSetMessage changeSet = ChangeSetMessage.create(null, null, null, null, null, null);

        final IllegalStateException exception =
                expectThrows(IllegalStateException.class, changeSet::getFilterParameterSubscriptionChanges);
        assertEquals(
                exception.getMessage(),
                "Replicant-0030: ChangeSetMessage.getFilterParameterSubscriptionChanges() invoked when no changes are"
                        + " present. Should guard call with ChangeSetMessage.hasFilterParameterSubscriptionChanges().");
    }

    @Test
    public void getEntityChanges_WhenNone() {
        final ChangeSetMessage changeSet = ChangeSetMessage.create(null, null, null, null, null, null);

        final IllegalStateException exception = expectThrows(IllegalStateException.class, changeSet::getEntityChanges);
        assertEquals(
                exception.getMessage(),
                "Replicant-0012: ChangeSetMessage.getEntityChanges() invoked when no changes are present. Should guard"
                        + " call with ChangeSetMessage.hasEntityChanges().");
    }
}
