package replicant;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

import arez.component.Linkable;
import org.testng.annotations.Test;
import replicant.messages.ChangeSetMessage;
import replicant.messages.EntityChange;
import replicant.messages.EntityChangePayloadImpl;
import replicant.messages.SubscriptionChangeMessage;
import replicant.spy.MessageProcessingSummary;

public class MessageProcessingTest extends AbstractReplicantTest {
    @Test
    public void construct() {
        final MessageProcessing processing =
                new MessageProcessing(1, ChangeSetMessage.create(null, null, null, null, null, null), null);

        assertFalse(processing.areReplicaLinksPending());
        assertFalse(processing.areEntityChangesPending());
        assertFalse(processing.hasReplicaValidationStarted());

        assertEquals(processing.getSubscriptionSubscribeCount(), 0);
        assertEquals(processing.getSubscriptionUpdateCount(), 0);
        assertEquals(processing.getSubscriptionUnsubscribeCount(), 0);
        assertEquals(processing.getEntityUpdateCount(), 0);
        assertEquals(processing.getEntityRemoveCount(), 0);
        assertEquals(processing.getEntityLinkCount(), 0);
    }

    @Test
    public void toMessageProcessingSummary() {
        final ChangeSetMessage changeSet =
                ChangeSetMessage.create(null, null, null, new SubscriptionChangeMessage[0], new EntityChange[0], null);

        final MessageProcessing processing = new MessageProcessing(1, changeSet, null);

        processing.incSubscriptionSubscribeCount();
        processing.incSubscriptionSubscribeCount();
        processing.incSubscriptionUnsubscribeCount();
        processing.incSubscriptionUnsubscribeCount();
        processing.incSubscriptionUnsubscribeCount();
        processing.incSubscriptionUpdateCount();
        processing.incEntityUpdateCount();
        processing.incEntityRemoveCount();
        processing.incEntityRemoveCount();
        processing.incEntityLinkCount();

        final MessageProcessingSummary summary = processing.toMessageProcessingSummary();

        assertNull(summary.getRequestId());
        assertEquals(summary.getSubscriptionSubscribeCount(), 2);
        assertEquals(summary.getSubscriptionUpdateCount(), 1);
        assertEquals(summary.getSubscriptionUnsubscribeCount(), 3);
        assertEquals(summary.getEntityUpdateCount(), 1);
        assertEquals(summary.getEntityRemoveCount(), 2);
        assertEquals(summary.getEntityLinkCount(), 1);
    }

    @Test
    public void incIgnoredUnlessSpyEnabled() {
        ReplicantTestUtil.disableSpies();

        final MessageProcessing processing = new MessageProcessing(1, new ChangeSetMessage(), null);

        assertEquals(processing.getSubscriptionSubscribeCount(), 0);
        assertEquals(processing.getSubscriptionUpdateCount(), 0);
        assertEquals(processing.getSubscriptionUnsubscribeCount(), 0);
        assertEquals(processing.getEntityUpdateCount(), 0);
        assertEquals(processing.getEntityRemoveCount(), 0);
        assertEquals(processing.getEntityLinkCount(), 0);

        // We enforce this to make it easier for DCE
        processing.incSubscriptionSubscribeCount();
        processing.incSubscriptionUnsubscribeCount();
        processing.incSubscriptionUpdateCount();
        processing.incEntityUpdateCount();
        processing.incEntityRemoveCount();
        processing.incEntityLinkCount();

        assertEquals(processing.getSubscriptionSubscribeCount(), 0);
        assertEquals(processing.getSubscriptionUpdateCount(), 0);
        assertEquals(processing.getSubscriptionUnsubscribeCount(), 0);
        assertEquals(processing.getEntityUpdateCount(), 0);
        assertEquals(processing.getEntityRemoveCount(), 0);
        assertEquals(processing.getEntityLinkCount(), 0);
    }

    @Test
    public void testToString() {
        final ChangeSetMessage changeSet =
                ChangeSetMessage.create(null, null, null, new SubscriptionChangeMessage[0], new EntityChange[0], null);
        final MessageProcessing processing = new MessageProcessing(1, changeSet, null);
        assertEquals(
                processing.toString(),
                "MessageProcessing[Type=change-set,RequestId=null,ChangeIndex=0,ReplicasToLink.size=0]");

        // Null out Entities
        processing.nextReplicaToLink();

        assertEquals(
                processing.toString(),
                "MessageProcessing[Type=change-set,RequestId=null,ChangeIndex=0,ReplicasToLink.size=0]");

        ReplicantTestUtil.disableNames();

        assertEquals(
                processing.toString(),
                "replicant.MessageProcessing@" + Integer.toHexString(System.identityHashCode(processing)));
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

        final MessageProcessing processing = new MessageProcessing(1, changeSet, request);

        assertEquals(processing.getMessage(), changeSet);
        assertEquals(processing.getRequest(), request);

        assertFalse(processing.needsSubscriptionChangesProcessed());
        assertTrue(processing.areEntityChangesPending());
        assertFalse(processing.areReplicaLinksPending());
        assertFalse(processing.hasReplicaValidationStarted());

        // Process entity changes
        {
            assertEquals(processing.nextEntityChange(), entityChanges[0]);
            processing.replicaProcessed(entities[0]);
            processing.incEntityUpdateCount();

            assertTrue(processing.areEntityChangesPending());

            assertEquals(processing.nextEntityChange(), entityChanges[1]);
            processing.incEntityRemoveCount();

            assertTrue(processing.areEntityChangesPending());

            assertEquals(processing.nextEntityChange(), entityChanges[2]);
            processing.incEntityUpdateCount();
            processing.replicaProcessed(entities[2]);

            assertFalse(processing.areEntityChangesPending());

            assertNull(processing.nextEntityChange());

            assertFalse(processing.areEntityChangesPending());

            assertEquals(processing.getEntityUpdateCount(), 2);
            assertEquals(processing.getEntityRemoveCount(), 1);
        }

        assertFalse(processing.needsSubscriptionChangesProcessed());
        assertFalse(processing.areEntityChangesPending());
        assertTrue(processing.areReplicaLinksPending());
        assertFalse(processing.hasReplicaValidationStarted());

        // process links
        {
            assertEquals(processing.nextReplicaToLink(), entities[0]);
            processing.incEntityLinkCount();
            assertNull(processing.nextReplicaToLink());
            assertEquals(processing.getEntityLinkCount(), 1);
        }

        assertFalse(processing.areReplicaLinksPending());

        assertFalse(processing.areEntityChangesPending());
        assertFalse(processing.areReplicaLinksPending());
        assertFalse(processing.hasReplicaValidationStarted());

        processing.markReplicaValidationStarted();

        assertFalse(processing.areEntityChangesPending());
        assertFalse(processing.areReplicaLinksPending());
        assertTrue(processing.hasReplicaValidationStarted());
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

        final MessageProcessing processing = new MessageProcessing(1, changeSet, request);

        assertEquals(processing.getMessage(), changeSet);
        assertEquals(processing.getRequest(), request);

        assertTrue(processing.needsSubscriptionChangesProcessed());
        assertFalse(processing.areEntityChangesPending());
        assertFalse(processing.areReplicaLinksPending());
        assertFalse(processing.hasReplicaValidationStarted());

        // processed as single block in caller
        processing.markSubscriptionChangesProcessed();

        assertFalse(processing.needsSubscriptionChangesProcessed());
        assertFalse(processing.areEntityChangesPending());
        assertFalse(processing.areReplicaLinksPending());
        assertFalse(processing.hasReplicaValidationStarted());

        processing.markReplicaValidationStarted();

        assertFalse(processing.needsSubscriptionChangesProcessed());
        assertFalse(processing.areEntityChangesPending());
        assertFalse(processing.areReplicaLinksPending());
        assertTrue(processing.hasReplicaValidationStarted());
    }

    @Test
    public void setChangeSet_mismatchedRequestId() {
        final ChangeSetMessage changeSet = ChangeSetMessage.create(1234, null, null, null, null, null);
        final RequestEntry request = new RequestEntry(5678, ValueUtil.randomString(), false, null);

        final IllegalStateException exception =
                expectThrows(IllegalStateException.class, () -> new MessageProcessing(1, changeSet, request));
        assertEquals(
                exception.getMessage(),
                "Replicant-0011: Server-to-client message specified Request ID '1234' but the matching Request has"
                        + " Request ID '5678'.");
    }
}
