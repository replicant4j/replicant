package replicant;

import static org.testng.Assert.*;

import org.testng.annotations.Test;
import replicant.messages.SubscriptionChangeMessage;

public final class SubscriptionChangeTest extends AbstractReplicantTest {
    @Test
    void subscribeTypeDataset() {
        final int systemSchemaId = 0;
        final SubscriptionChange change = SubscriptionChange.from(systemSchemaId, "+23");
        assertEquals(change.getDatasetAddress().systemSchemaId(), systemSchemaId);
        assertEquals(change.getDatasetAddress().getName(), "0.23");
        assertEquals(change.getType(), SubscriptionChange.Type.SUBSCRIBE);
        assertNull(change.getFilterParameter());
    }

    @Test
    void subscribeInstanceDataset() {
        final int systemSchemaId = 0;
        final SubscriptionChange change = SubscriptionChange.from(systemSchemaId, "+23.2");
        assertEquals(change.getDatasetAddress().systemSchemaId(), systemSchemaId);
        assertEquals(change.getDatasetAddress().getName(), "0.23.2");
        assertEquals(change.getType(), SubscriptionChange.Type.SUBSCRIBE);
        assertNull(change.getFilterParameter());
    }

    @Test
    void unsubscribeTypeDataset() {
        final int systemSchemaId = 0;
        final SubscriptionChange change = SubscriptionChange.from(systemSchemaId, "-23");
        assertEquals(change.getDatasetAddress().systemSchemaId(), systemSchemaId);
        assertEquals(change.getDatasetAddress().getName(), "0.23");
        assertEquals(change.getType(), SubscriptionChange.Type.UNSUBSCRIBE);
        assertNull(change.getFilterParameter());
    }

    @Test
    void unsubscribeInstanceDataset() {
        final int systemSchemaId = 0;
        final SubscriptionChange change = SubscriptionChange.from(systemSchemaId, "-23.2");
        assertEquals(change.getDatasetAddress().systemSchemaId(), systemSchemaId);
        assertEquals(change.getDatasetAddress().getName(), "0.23.2");
        assertEquals(change.getType(), SubscriptionChange.Type.UNSUBSCRIBE);
        assertNull(change.getFilterParameter());
    }

    @Test
    void subscribeParameterFilteredTypeDataset() {
        final int systemSchemaId = 0;
        final String filterParameter = ValueUtil.randomString();
        final SubscriptionChange change =
                SubscriptionChange.from(systemSchemaId, SubscriptionChangeMessage.create("+23", filterParameter));
        assertEquals(change.getDatasetAddress().systemSchemaId(), systemSchemaId);
        assertEquals(change.getDatasetAddress().getName(), "0.23");
        assertEquals(change.getType(), SubscriptionChange.Type.SUBSCRIBE);
        assertEquals(change.getFilterParameter(), filterParameter);
    }

    @Test
    void subscribeParameterFilteredInstanceDataset() {
        final int systemSchemaId = 0;
        final String filterParameter = ValueUtil.randomString();
        final SubscriptionChange change =
                SubscriptionChange.from(systemSchemaId, SubscriptionChangeMessage.create("+23.2", filterParameter));
        assertEquals(change.getDatasetAddress().systemSchemaId(), systemSchemaId);
        assertEquals(change.getDatasetAddress().getName(), "0.23.2");
        assertEquals(change.getType(), SubscriptionChange.Type.SUBSCRIBE);
        assertEquals(change.getFilterParameter(), filterParameter);
    }

    @Test
    void unsubscribeParameterFilteredTypeDataset() {
        final int systemSchemaId = 0;
        final String filterParameter = ValueUtil.randomString();
        final SubscriptionChange change =
                SubscriptionChange.from(systemSchemaId, SubscriptionChangeMessage.create("-23", filterParameter));
        assertEquals(change.getDatasetAddress().systemSchemaId(), systemSchemaId);
        assertEquals(change.getDatasetAddress().getName(), "0.23");
        assertEquals(change.getType(), SubscriptionChange.Type.UNSUBSCRIBE);
        assertEquals(change.getFilterParameter(), filterParameter);
    }

    @Test
    void unsubscribeParameterFilteredInstanceDataset() {
        final int systemSchemaId = 0;
        final String filterParameter = ValueUtil.randomString();
        final SubscriptionChange change =
                SubscriptionChange.from(systemSchemaId, SubscriptionChangeMessage.create("-23.2", filterParameter));
        assertEquals(change.getDatasetAddress().systemSchemaId(), systemSchemaId);
        assertEquals(change.getDatasetAddress().getName(), "0.23.2");
        assertEquals(change.getType(), SubscriptionChange.Type.UNSUBSCRIBE);
        assertEquals(change.getFilterParameter(), filterParameter);
    }

    @Test
    void updateParameterFilteredTypeDataset() {
        final int systemSchemaId = 0;
        final String filterParameter = ValueUtil.randomString();
        final SubscriptionChange change =
                SubscriptionChange.from(systemSchemaId, SubscriptionChangeMessage.create("=23", filterParameter));
        assertEquals(change.getDatasetAddress().systemSchemaId(), systemSchemaId);
        assertEquals(change.getDatasetAddress().getName(), "0.23");
        assertEquals(change.getType(), SubscriptionChange.Type.UPDATE);
        assertEquals(change.getFilterParameter(), filterParameter);
    }

    @Test
    void updateParameterFilteredInstanceDataset() {
        final int systemSchemaId = 0;
        final String filterParameter = ValueUtil.randomString();
        final SubscriptionChange change =
                SubscriptionChange.from(systemSchemaId, SubscriptionChangeMessage.create("=23.2", filterParameter));
        assertEquals(change.getDatasetAddress().systemSchemaId(), systemSchemaId);
        assertEquals(change.getDatasetAddress().getName(), "0.23.2");
        assertEquals(change.getType(), SubscriptionChange.Type.UPDATE);
        assertEquals(change.getFilterParameter(), filterParameter);
    }

    @Test
    void invalidateDatasetAddress() {
        final int systemSchemaId = 0;
        final SubscriptionChange change = SubscriptionChange.from(systemSchemaId, "!23.2");
        assertEquals(change.getDatasetAddress().systemSchemaId(), systemSchemaId);
        assertEquals(change.getDatasetAddress().getName(), "0.23.2");
        assertEquals(change.getType(), SubscriptionChange.Type.INVALIDATE_DATASET_ADDRESS);
        assertNull(change.getFilterParameter());
    }

    @Test
    void badChange() {
        final IllegalStateException exception =
                expectThrows(IllegalStateException.class, () -> SubscriptionChange.from(0, "*1"));
        assertEquals(exception.getMessage(), "Failed to parse Subscription Change '*1'");
    }

    @Test
    void invalidDatasetAddress() {
        final IllegalStateException exception =
                expectThrows(IllegalStateException.class, () -> SubscriptionChange.from(0, "+X"));
        assertEquals(exception.getMessage(), "Failed to parse Subscription Change '+X'");
    }
}
