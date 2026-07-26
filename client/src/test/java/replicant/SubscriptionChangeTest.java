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
        assertNull(change.getFilterParameter());
    }

    @Test
    void subscribeInstanceDataset() {
        final int schemaId = 0;
        final SubscriptionChange change = SubscriptionChange.from(schemaId, "+23.2");
        assertEquals(change.getDatasetAddress().schemaId(), schemaId);
        assertEquals(change.getDatasetAddress().getName(), "0.23.2");
        assertEquals(change.getType(), SubscriptionChange.Type.SUBSCRIBE);
        assertNull(change.getFilterParameter());
    }

    @Test
    void unsubscribeTypeDataset() {
        final int schemaId = 0;
        final SubscriptionChange change = SubscriptionChange.from(schemaId, "-23");
        assertEquals(change.getDatasetAddress().schemaId(), schemaId);
        assertEquals(change.getDatasetAddress().getName(), "0.23");
        assertEquals(change.getType(), SubscriptionChange.Type.UNSUBSCRIBE);
        assertNull(change.getFilterParameter());
    }

    @Test
    void unsubscribeInstanceDataset() {
        final int schemaId = 0;
        final SubscriptionChange change = SubscriptionChange.from(schemaId, "-23.2");
        assertEquals(change.getDatasetAddress().schemaId(), schemaId);
        assertEquals(change.getDatasetAddress().getName(), "0.23.2");
        assertEquals(change.getType(), SubscriptionChange.Type.UNSUBSCRIBE);
        assertNull(change.getFilterParameter());
    }

    @Test
    void subscribeFilteredTypeDataset() {
        final int schemaId = 0;
        final String filterParameter = ValueUtil.randomString();
        final SubscriptionChange change =
                SubscriptionChange.from(schemaId, SubscriptionChangeMessage.create("+23", filterParameter));
        assertEquals(change.getDatasetAddress().schemaId(), schemaId);
        assertEquals(change.getDatasetAddress().getName(), "0.23");
        assertEquals(change.getType(), SubscriptionChange.Type.SUBSCRIBE);
        assertEquals(change.getFilterParameter(), filterParameter);
    }

    @Test
    void subscribeFilteredInstanceDataset() {
        final int schemaId = 0;
        final String filterParameter = ValueUtil.randomString();
        final SubscriptionChange change =
                SubscriptionChange.from(schemaId, SubscriptionChangeMessage.create("+23.2", filterParameter));
        assertEquals(change.getDatasetAddress().schemaId(), schemaId);
        assertEquals(change.getDatasetAddress().getName(), "0.23.2");
        assertEquals(change.getType(), SubscriptionChange.Type.SUBSCRIBE);
        assertEquals(change.getFilterParameter(), filterParameter);
    }

    @Test
    void unsubscribeFilteredTypeDataset() {
        final int schemaId = 0;
        final String filterParameter = ValueUtil.randomString();
        final SubscriptionChange change =
                SubscriptionChange.from(schemaId, SubscriptionChangeMessage.create("-23", filterParameter));
        assertEquals(change.getDatasetAddress().schemaId(), schemaId);
        assertEquals(change.getDatasetAddress().getName(), "0.23");
        assertEquals(change.getType(), SubscriptionChange.Type.UNSUBSCRIBE);
        assertEquals(change.getFilterParameter(), filterParameter);
    }

    @Test
    void unsubscribeFilteredInstanceDataset() {
        final int schemaId = 0;
        final String filterParameter = ValueUtil.randomString();
        final SubscriptionChange change =
                SubscriptionChange.from(schemaId, SubscriptionChangeMessage.create("-23.2", filterParameter));
        assertEquals(change.getDatasetAddress().schemaId(), schemaId);
        assertEquals(change.getDatasetAddress().getName(), "0.23.2");
        assertEquals(change.getType(), SubscriptionChange.Type.UNSUBSCRIBE);
        assertEquals(change.getFilterParameter(), filterParameter);
    }

    @Test
    void updateFilteredTypeDataset() {
        final int schemaId = 0;
        final String filterParameter = ValueUtil.randomString();
        final SubscriptionChange change =
                SubscriptionChange.from(schemaId, SubscriptionChangeMessage.create("=23", filterParameter));
        assertEquals(change.getDatasetAddress().schemaId(), schemaId);
        assertEquals(change.getDatasetAddress().getName(), "0.23");
        assertEquals(change.getType(), SubscriptionChange.Type.UPDATE);
        assertEquals(change.getFilterParameter(), filterParameter);
    }

    @Test
    void updateFilteredInstanceDataset() {
        final int schemaId = 0;
        final String filterParameter = ValueUtil.randomString();
        final SubscriptionChange change =
                SubscriptionChange.from(schemaId, SubscriptionChangeMessage.create("=23.2", filterParameter));
        assertEquals(change.getDatasetAddress().schemaId(), schemaId);
        assertEquals(change.getDatasetAddress().getName(), "0.23.2");
        assertEquals(change.getType(), SubscriptionChange.Type.UPDATE);
        assertEquals(change.getFilterParameter(), filterParameter);
    }

    @Test
    void deleteInstanceDataset() {
        final int schemaId = 0;
        final SubscriptionChange change = SubscriptionChange.from(schemaId, "!23.2");
        assertEquals(change.getDatasetAddress().schemaId(), schemaId);
        assertEquals(change.getDatasetAddress().getName(), "0.23.2");
        assertEquals(change.getType(), SubscriptionChange.Type.DELETE);
        assertNull(change.getFilterParameter());
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
