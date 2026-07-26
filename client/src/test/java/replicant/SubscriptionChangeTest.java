package replicant;

import static org.testng.Assert.*;

import org.testng.annotations.Test;
import replicant.messages.SubscriptionChangeMessage;

public final class SubscriptionChangeTest extends AbstractReplicantTest {
    @Test
    void subscribeTypeDataset() {
        final int schemaId = 0;
        final SubscriptionChange change = SubscriptionChange.from(schemaId, "+23");
        assertEquals(change.getDatasetAddress().schemaId(), schemaId);
        assertEquals(change.getDatasetAddress().getName(), "0.23");
        assertEquals(change.getType(), SubscriptionChange.Type.SUBSCRIBE);
        assertNull(change.getFilter());
    }

    @Test
    void subscribeInstanceDataset() {
        final int schemaId = 0;
        final SubscriptionChange change = SubscriptionChange.from(schemaId, "+23.2");
        assertEquals(change.getDatasetAddress().schemaId(), schemaId);
        assertEquals(change.getDatasetAddress().getName(), "0.23.2");
        assertEquals(change.getType(), SubscriptionChange.Type.SUBSCRIBE);
        assertNull(change.getFilter());
    }

    @Test
    void unsubscribeTypeDataset() {
        final int schemaId = 0;
        final SubscriptionChange change = SubscriptionChange.from(schemaId, "-23");
        assertEquals(change.getDatasetAddress().schemaId(), schemaId);
        assertEquals(change.getDatasetAddress().getName(), "0.23");
        assertEquals(change.getType(), SubscriptionChange.Type.UNSUBSCRIBE);
        assertNull(change.getFilter());
    }

    @Test
    void unsubscribeInstanceDataset() {
        final int schemaId = 0;
        final SubscriptionChange change = SubscriptionChange.from(schemaId, "-23.2");
        assertEquals(change.getDatasetAddress().schemaId(), schemaId);
        assertEquals(change.getDatasetAddress().getName(), "0.23.2");
        assertEquals(change.getType(), SubscriptionChange.Type.UNSUBSCRIBE);
        assertNull(change.getFilter());
    }

    @Test
    void subscribeFilteredTypeDataset() {
        final int schemaId = 0;
        final String filter = ValueUtil.randomString();
        final SubscriptionChange change =
                SubscriptionChange.from(schemaId, SubscriptionChangeMessage.create("+23", filter));
        assertEquals(change.getDatasetAddress().schemaId(), schemaId);
        assertEquals(change.getDatasetAddress().getName(), "0.23");
        assertEquals(change.getType(), SubscriptionChange.Type.SUBSCRIBE);
        assertEquals(change.getFilter(), filter);
    }

    @Test
    void subscribeFilteredInstanceDataset() {
        final int schemaId = 0;
        final String filter = ValueUtil.randomString();
        final SubscriptionChange change =
                SubscriptionChange.from(schemaId, SubscriptionChangeMessage.create("+23.2", filter));
        assertEquals(change.getDatasetAddress().schemaId(), schemaId);
        assertEquals(change.getDatasetAddress().getName(), "0.23.2");
        assertEquals(change.getType(), SubscriptionChange.Type.SUBSCRIBE);
        assertEquals(change.getFilter(), filter);
    }

    @Test
    void unsubscribeFilteredTypeDataset() {
        final int schemaId = 0;
        final String filter = ValueUtil.randomString();
        final SubscriptionChange change =
                SubscriptionChange.from(schemaId, SubscriptionChangeMessage.create("-23", filter));
        assertEquals(change.getDatasetAddress().schemaId(), schemaId);
        assertEquals(change.getDatasetAddress().getName(), "0.23");
        assertEquals(change.getType(), SubscriptionChange.Type.UNSUBSCRIBE);
        assertEquals(change.getFilter(), filter);
    }

    @Test
    void unsubscribeFilteredInstanceDataset() {
        final int schemaId = 0;
        final String filter = ValueUtil.randomString();
        final SubscriptionChange change =
                SubscriptionChange.from(schemaId, SubscriptionChangeMessage.create("-23.2", filter));
        assertEquals(change.getDatasetAddress().schemaId(), schemaId);
        assertEquals(change.getDatasetAddress().getName(), "0.23.2");
        assertEquals(change.getType(), SubscriptionChange.Type.UNSUBSCRIBE);
        assertEquals(change.getFilter(), filter);
    }

    @Test
    void updateFilteredTypeDataset() {
        final int schemaId = 0;
        final String filter = ValueUtil.randomString();
        final SubscriptionChange change =
                SubscriptionChange.from(schemaId, SubscriptionChangeMessage.create("=23", filter));
        assertEquals(change.getDatasetAddress().schemaId(), schemaId);
        assertEquals(change.getDatasetAddress().getName(), "0.23");
        assertEquals(change.getType(), SubscriptionChange.Type.UPDATE);
        assertEquals(change.getFilter(), filter);
    }

    @Test
    void updateFilteredInstanceDataset() {
        final int schemaId = 0;
        final String filter = ValueUtil.randomString();
        final SubscriptionChange change =
                SubscriptionChange.from(schemaId, SubscriptionChangeMessage.create("=23.2", filter));
        assertEquals(change.getDatasetAddress().schemaId(), schemaId);
        assertEquals(change.getDatasetAddress().getName(), "0.23.2");
        assertEquals(change.getType(), SubscriptionChange.Type.UPDATE);
        assertEquals(change.getFilter(), filter);
    }

    @Test
    void deleteInstanceDataset() {
        final int schemaId = 0;
        final SubscriptionChange change = SubscriptionChange.from(schemaId, "!23.2");
        assertEquals(change.getDatasetAddress().schemaId(), schemaId);
        assertEquals(change.getDatasetAddress().getName(), "0.23.2");
        assertEquals(change.getType(), SubscriptionChange.Type.DELETE);
        assertNull(change.getFilter());
    }

    @Test
    void badAction() {
        final IllegalStateException exception =
                expectThrows(IllegalStateException.class, () -> SubscriptionChange.from(0, "*1"));
        assertEquals(exception.getMessage(), "Failed to parse Subscription action '*1'");
    }

    @Test
    void invalidDatasetAddress() {
        final IllegalStateException exception =
                expectThrows(IllegalStateException.class, () -> SubscriptionChange.from(0, "+X"));
        assertEquals(exception.getMessage(), "Failed to parse Subscription action '+X'");
    }
}
