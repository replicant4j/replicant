package replicant;

import static org.testng.Assert.*;

import org.testng.annotations.Test;
import replicant.messages.SubscriptionChangeMessage;

public final class SubscriptionChangeTest extends AbstractReplicantTest {
    private SystemSchema filterSchema(final int id) {
        final Dataset[] datasets = new Dataset[24];
        datasets[23] = new Dataset(
                23,
                "Filtered",
                null,
                Dataset.FilterMode.PARAMETER_FILTERED,
                Dataset.FilterParameterMode.FIXED,
                false,
                new FilterParameterCodec<String>(value -> java.util.Map.of("value", value), value ->
                        (String) java.util.Objects.requireNonNull(((java.util.Map<?, ?>) value).get("value"))),
                null,
                false,
                Dataset.Visibility.UNIVERSAL,
                java.util.List.of());
        return new SystemSchema(id, "Test", datasets, new EntityType[0]);
    }

    @Test
    void subscribeTypeDataset() {
        final int systemSchemaId = 0;
        final SubscriptionChange change = SubscriptionChange.from(filterSchema(systemSchemaId), "+23");
        assertEquals(change.getDatasetAddress().systemSchemaId(), systemSchemaId);
        assertEquals(change.getDatasetAddress().getName(), "0.23");
        assertEquals(change.getType(), SubscriptionChange.Type.SUBSCRIBE);
        assertNull(change.getFilterParameter());
    }

    @Test
    void subscribeInstanceDataset() {
        final int systemSchemaId = 0;
        final SubscriptionChange change = SubscriptionChange.from(filterSchema(systemSchemaId), "+23.2");
        assertEquals(change.getDatasetAddress().systemSchemaId(), systemSchemaId);
        assertEquals(change.getDatasetAddress().getName(), "0.23.2");
        assertEquals(change.getType(), SubscriptionChange.Type.SUBSCRIBE);
        assertNull(change.getFilterParameter());
    }

    @Test
    void unsubscribeTypeDataset() {
        final int systemSchemaId = 0;
        final SubscriptionChange change = SubscriptionChange.from(filterSchema(systemSchemaId), "-23");
        assertEquals(change.getDatasetAddress().systemSchemaId(), systemSchemaId);
        assertEquals(change.getDatasetAddress().getName(), "0.23");
        assertEquals(change.getType(), SubscriptionChange.Type.UNSUBSCRIBE);
        assertNull(change.getFilterParameter());
    }

    @Test
    void unsubscribeInstanceDataset() {
        final int systemSchemaId = 0;
        final SubscriptionChange change = SubscriptionChange.from(filterSchema(systemSchemaId), "-23.2");
        assertEquals(change.getDatasetAddress().systemSchemaId(), systemSchemaId);
        assertEquals(change.getDatasetAddress().getName(), "0.23.2");
        assertEquals(change.getType(), SubscriptionChange.Type.UNSUBSCRIBE);
        assertNull(change.getFilterParameter());
    }

    @Test
    void subscribeParameterFilteredTypeDataset() {
        final int systemSchemaId = 0;
        final String filterParameter = ValueUtil.randomString();
        final SubscriptionChange change = SubscriptionChange.from(
                filterSchema(systemSchemaId),
                SubscriptionChangeMessage.create("+23", java.util.Map.of("value", filterParameter)));
        assertEquals(change.getDatasetAddress().systemSchemaId(), systemSchemaId);
        assertEquals(change.getDatasetAddress().getName(), "0.23");
        assertEquals(change.getType(), SubscriptionChange.Type.SUBSCRIBE);
        assertEquals(change.getFilterParameter(), filterParameter);
    }

    @Test
    void subscribeParameterFilteredInstanceDataset() {
        final int systemSchemaId = 0;
        final String filterParameter = ValueUtil.randomString();
        final SubscriptionChange change = SubscriptionChange.from(
                filterSchema(systemSchemaId),
                SubscriptionChangeMessage.create("+23.2", java.util.Map.of("value", filterParameter)));
        assertEquals(change.getDatasetAddress().systemSchemaId(), systemSchemaId);
        assertEquals(change.getDatasetAddress().getName(), "0.23.2");
        assertEquals(change.getType(), SubscriptionChange.Type.SUBSCRIBE);
        assertEquals(change.getFilterParameter(), filterParameter);
    }

    @Test
    void unsubscribeParameterFilteredTypeDataset() {
        final int systemSchemaId = 0;
        final String filterParameter = ValueUtil.randomString();
        final SubscriptionChange change = SubscriptionChange.from(
                filterSchema(systemSchemaId),
                SubscriptionChangeMessage.create("-23", java.util.Map.of("value", filterParameter)));
        assertEquals(change.getDatasetAddress().systemSchemaId(), systemSchemaId);
        assertEquals(change.getDatasetAddress().getName(), "0.23");
        assertEquals(change.getType(), SubscriptionChange.Type.UNSUBSCRIBE);
        assertEquals(change.getFilterParameter(), filterParameter);
    }

    @Test
    void unsubscribeParameterFilteredInstanceDataset() {
        final int systemSchemaId = 0;
        final String filterParameter = ValueUtil.randomString();
        final SubscriptionChange change = SubscriptionChange.from(
                filterSchema(systemSchemaId),
                SubscriptionChangeMessage.create("-23.2", java.util.Map.of("value", filterParameter)));
        assertEquals(change.getDatasetAddress().systemSchemaId(), systemSchemaId);
        assertEquals(change.getDatasetAddress().getName(), "0.23.2");
        assertEquals(change.getType(), SubscriptionChange.Type.UNSUBSCRIBE);
        assertEquals(change.getFilterParameter(), filterParameter);
    }

    @Test
    void updateParameterFilteredTypeDataset() {
        final int systemSchemaId = 0;
        final String filterParameter = ValueUtil.randomString();
        final SubscriptionChange change = SubscriptionChange.from(
                filterSchema(systemSchemaId),
                SubscriptionChangeMessage.create("=23", java.util.Map.of("value", filterParameter)));
        assertEquals(change.getDatasetAddress().systemSchemaId(), systemSchemaId);
        assertEquals(change.getDatasetAddress().getName(), "0.23");
        assertEquals(change.getType(), SubscriptionChange.Type.UPDATE);
        assertEquals(change.getFilterParameter(), filterParameter);
    }

    @Test
    void updateParameterFilteredInstanceDataset() {
        final int systemSchemaId = 0;
        final String filterParameter = ValueUtil.randomString();
        final SubscriptionChange change = SubscriptionChange.from(
                filterSchema(systemSchemaId),
                SubscriptionChangeMessage.create("=23.2", java.util.Map.of("value", filterParameter)));
        assertEquals(change.getDatasetAddress().systemSchemaId(), systemSchemaId);
        assertEquals(change.getDatasetAddress().getName(), "0.23.2");
        assertEquals(change.getType(), SubscriptionChange.Type.UPDATE);
        assertEquals(change.getFilterParameter(), filterParameter);
    }

    @Test
    void invalidateDatasetAddress() {
        final int systemSchemaId = 0;
        final SubscriptionChange change = SubscriptionChange.from(filterSchema(systemSchemaId), "!23.2");
        assertEquals(change.getDatasetAddress().systemSchemaId(), systemSchemaId);
        assertEquals(change.getDatasetAddress().getName(), "0.23.2");
        assertEquals(change.getType(), SubscriptionChange.Type.INVALIDATE_DATASET_ADDRESS);
        assertNull(change.getFilterParameter());
    }

    @Test
    void badChange() {
        final IllegalStateException exception =
                expectThrows(IllegalStateException.class, () -> SubscriptionChange.from(filterSchema(0), "*1"));
        assertEquals(exception.getMessage(), "Failed to parse Subscription Change '*1'");
    }

    @Test
    void invalidDatasetAddress() {
        final IllegalStateException exception =
                expectThrows(IllegalStateException.class, () -> SubscriptionChange.from(filterSchema(0), "+X"));
        assertEquals(exception.getMessage(), "Failed to parse Subscription Change '+X'");
    }
}
