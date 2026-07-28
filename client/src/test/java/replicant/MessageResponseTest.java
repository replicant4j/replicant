package replicant;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

import arez.component.Linkable;
import org.testng.annotations.Test;
import replicant.messages.ChangeSetMessage;
import replicant.messages.EntityChange;
import replicant.messages.EntityChangePayloadImpl;
import replicant.messages.SubscriptionChangeMessage;
import replicant.spy.DataLoadStatus;

public class MessageResponseTest extends AbstractReplicantTest {
    @Test
    public void construct() {
        final MessageResponse action =
                new MessageResponse(1, ChangeSetMessage.create(null, null, null, null, null, null), null);

        assertFalse(action.areReplicaLinksPending());
        assertFalse(action.areEntityChangesPending());
        assertFalse(action.hasReplicaValidationStarted());

        assertEquals(action.getSubscriptionSubscribeCount(), 0);
        assertEquals(action.getSubscriptionUpdateCount(), 0);
        assertEquals(action.getSubscriptionUnsubscribeCount(), 0);
        assertEquals(action.getEntityUpdateCount(), 0);
        assertEquals(action.getEntityRemoveCount(), 0);
        assertEquals(action.getEntityLinkCount(), 0);
    }

    @Test
    public void toStatus() {
        final ChangeSetMessage changeSet =
                ChangeSetMessage.create(null, null, null, new SubscriptionChangeMessage[0], new EntityChange[0], null);

        final MessageResponse action = new MessageResponse(1, changeSet, null);

        action.incSubscriptionSubscribeCount();
        action.incSubscriptionSubscribeCount();
        action.incSubscriptionUnsubscribeCount();
        action.incSubscriptionUnsubscribeCount();
        action.incSubscriptionUnsubscribeCount();
        action.incSubscriptionUpdateCount();
        action.incEntityUpdateCount();
        action.incEntityRemoveCount();
        action.incEntityRemoveCount();
        action.incEntityLinkCount();

        final DataLoadStatus status = action.toStatus();

        assertNull(status.getRequestId());
        assertEquals(status.getSubscriptionSubscribeCount(), 2);
        assertEquals(status.getSubscriptionUpdateCount(), 1);
        assertEquals(status.getSubscriptionUnsubscribeCount(), 3);
        assertEquals(status.getEntityUpdateCount(), 1);
        assertEquals(status.getEntityRemoveCount(), 2);
        assertEquals(status.getEntityLinkCount(), 1);
    }

    @Test
    public void incIgnoredUnlessSpyEnabled() {
        ReplicantTestUtil.disableSpies();

        final MessageResponse action = new MessageResponse(1, new ChangeSetMessage(), null);

        assertEquals(action.getSubscriptionSubscribeCount(), 0);
        assertEquals(action.getSubscriptionUpdateCount(), 0);
        assertEquals(action.getSubscriptionUnsubscribeCount(), 0);
        assertEquals(action.getEntityUpdateCount(), 0);
        assertEquals(action.getEntityRemoveCount(), 0);
        assertEquals(action.getEntityLinkCount(), 0);

        // We enforce this to make it easier for DCE
        action.incSubscriptionSubscribeCount();
        action.incSubscriptionUnsubscribeCount();
        action.incSubscriptionUpdateCount();
        action.incEntityUpdateCount();
        action.incEntityRemoveCount();
        action.incEntityLinkCount();

        assertEquals(action.getSubscriptionSubscribeCount(), 0);
        assertEquals(action.getSubscriptionUpdateCount(), 0);
        assertEquals(action.getSubscriptionUnsubscribeCount(), 0);
        assertEquals(action.getEntityUpdateCount(), 0);
        assertEquals(action.getEntityRemoveCount(), 0);
        assertEquals(action.getEntityLinkCount(), 0);
    }

    @Test
    public void testToString() {
        final ChangeSetMessage changeSet =
                ChangeSetMessage.create(null, null, null, new SubscriptionChangeMessage[0], new EntityChange[0], null);
        final MessageResponse action = new MessageResponse(1, changeSet, null);
        assertEquals(
                action.toString(),
                "MessageResponse[Type=change-set,RequestId=null,ChangeIndex=0,ReplicasToLink.size=0]");

        // Null out Entities
        action.nextReplicaToLink();

        assertEquals(
                action.toString(),
                "MessageResponse[Type=change-set,RequestId=null,ChangeIndex=0,ReplicasToLink.size=0]");

        ReplicantTestUtil.disableNames();

        assertEquals(
                action.toString(), "replicant.MessageResponse@" + Integer.toHexString(System.identityHashCode(action)));
    }

    @Test
    public void lifeCycleWithNormallyCompletedRequest() {
        // ChangeSet details
        final int requestId = ValueUtil.randomInt();

        // Subscription changes
        final SubscriptionChangeMessage[] subscriptionChanges = new SubscriptionChangeMessage[0];

        // Entity Updates
        final int datasetId = 22;

        // Entity update
        final EntityChange change1 =
                EntityChange.create(100, 50, new String[] {String.valueOf(datasetId)}, new EntityChangePayloadImpl());
        // Entity Remove
        final EntityChange change2 = EntityChange.create(100, 51, new String[] {String.valueOf(datasetId)});
        // Entity update - non linkable
        final EntityChange change3 =
                EntityChange.create(100, 52, new String[] {String.valueOf(datasetId)}, new EntityChangePayloadImpl());
        final EntityChange[] entityChanges = new EntityChange[] {change1, change2, change3};

        final Object[] entities = new Object[] {mock(Linkable.class), new Object(), new Object()};

        final ChangeSetMessage changeSet =
                ChangeSetMessage.create(requestId, null, null, subscriptionChanges, entityChanges, null);

        final String requestKey = ValueUtil.randomString();
        final RequestEntry request = new RequestEntry(requestId, requestKey, false, null);

        final MessageResponse action = new MessageResponse(1, changeSet, request);

        assertEquals(action.getMessage(), changeSet);
        assertEquals(action.getRequest(), request);

        assertFalse(action.needsSubscriptionChangesProcessed());
        assertTrue(action.areEntityChangesPending());
        assertFalse(action.areReplicaLinksPending());
        assertFalse(action.hasReplicaValidationStarted());

        // Process entity changes
        {
            assertEquals(action.nextEntityChange(), entityChanges[0]);
            action.replicaProcessed(entities[0]);
            action.incEntityUpdateCount();

            assertTrue(action.areEntityChangesPending());

            assertEquals(action.nextEntityChange(), entityChanges[1]);
            action.incEntityRemoveCount();

            assertTrue(action.areEntityChangesPending());

            assertEquals(action.nextEntityChange(), entityChanges[2]);
            action.incEntityUpdateCount();
            action.replicaProcessed(entities[2]);

            assertFalse(action.areEntityChangesPending());

            assertNull(action.nextEntityChange());

            assertFalse(action.areEntityChangesPending());

            assertEquals(action.getEntityUpdateCount(), 2);
            assertEquals(action.getEntityRemoveCount(), 1);
        }

        assertFalse(action.needsSubscriptionChangesProcessed());
        assertFalse(action.areEntityChangesPending());
        assertTrue(action.areReplicaLinksPending());
        assertFalse(action.hasReplicaValidationStarted());

        // process links
        {
            assertEquals(action.nextReplicaToLink(), entities[0]);
            action.incEntityLinkCount();
            assertNull(action.nextReplicaToLink());
            assertEquals(action.getEntityLinkCount(), 1);
        }

        assertFalse(action.areReplicaLinksPending());

        assertFalse(action.areEntityChangesPending());
        assertFalse(action.areReplicaLinksPending());
        assertFalse(action.hasReplicaValidationStarted());

        action.markReplicaValidationStarted();

        assertFalse(action.areEntityChangesPending());
        assertFalse(action.areReplicaLinksPending());
        assertTrue(action.hasReplicaValidationStarted());
    }

    @Test
    public void lifeCycleWithSubscriptionChanges() {
        // ChangeSet details
        final int requestId = ValueUtil.randomInt();

        // Subscription changes
        final String filterParameter1 = ValueUtil.randomString();
        final String filterParameter2 = ValueUtil.randomString();
        final SubscriptionChangeMessage subscriptionChange1 = SubscriptionChangeMessage.create("+42", filterParameter1);
        final SubscriptionChangeMessage subscriptionChange2 =
                SubscriptionChangeMessage.create("=43.1", filterParameter2);
        final SubscriptionChangeMessage[] subscriptionChanges =
                new SubscriptionChangeMessage[] {subscriptionChange1, subscriptionChange2};

        final EntityChange[] entityChanges = new EntityChange[0];

        final ChangeSetMessage changeSet = ChangeSetMessage.create(
                requestId, null, new String[] {"-43.2"}, subscriptionChanges, entityChanges, null);
        final String requestKey = ValueUtil.randomString();
        final RequestEntry request = new RequestEntry(requestId, requestKey, false, null);

        final MessageResponse action = new MessageResponse(1, changeSet, request);

        assertEquals(action.getMessage(), changeSet);
        assertEquals(action.getRequest(), request);

        assertTrue(action.needsSubscriptionChangesProcessed());
        assertFalse(action.areEntityChangesPending());
        assertFalse(action.areReplicaLinksPending());
        assertFalse(action.hasReplicaValidationStarted());

        // processed as single block in caller
        action.markSubscriptionChangesProcessed();

        assertFalse(action.needsSubscriptionChangesProcessed());
        assertFalse(action.areEntityChangesPending());
        assertFalse(action.areReplicaLinksPending());
        assertFalse(action.hasReplicaValidationStarted());

        action.markReplicaValidationStarted();

        assertFalse(action.needsSubscriptionChangesProcessed());
        assertFalse(action.areEntityChangesPending());
        assertFalse(action.areReplicaLinksPending());
        assertTrue(action.hasReplicaValidationStarted());
    }

    @Test
    public void setChangeSet_mismatchedRequestId() {
        final ChangeSetMessage changeSet = ChangeSetMessage.create(1234, null, null, null, null, null);
        final RequestEntry request = new RequestEntry(5678, ValueUtil.randomString(), false, null);

        final IllegalStateException exception =
                expectThrows(IllegalStateException.class, () -> new MessageResponse(1, changeSet, request));
        assertEquals(
                exception.getMessage(),
                "Replicant-0011: Response message specified requestId '1234' but request specified requestId '5678'.");
    }
}
