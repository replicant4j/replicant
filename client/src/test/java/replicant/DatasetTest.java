package replicant;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

import java.util.Collections;
import org.testng.annotations.Test;

public class DatasetTest extends AbstractReplicantTest {
    @Test
    public void findEntityTypeById() {
        final EntityType entityType =
                new EntityType(1, "MyObject", Object.class, (i, d) -> 1, (o, d) -> d.notify(), new DatasetLink[0]);
        final Dataset dataset = new Dataset(
                ValueUtil.randomInt(),
                ValueUtil.randomString(),
                null,
                Dataset.FilterType.NONE,
                false,
                null,
                false,
                false,
                Collections.singletonList(entityType));
        assertEquals(dataset.getEntityTypes().size(), 1);
        assertEquals(dataset.getEntityTypes().size(), 1);
        assertEquals(dataset.findEntityTypeById(1), entityType);
        assertNull(dataset.findEntityTypeById(0));
    }

    @Test
    public void typeDataset() {
        final EntityType entityType =
                new EntityType(1, "MyObject", Object.class, (i, d) -> 1, (o, d) -> d.notify(), new DatasetLink[0]);
        final Dataset dataset = new Dataset(
                1,
                "MetaData",
                null,
                Dataset.FilterType.NONE,
                false,
                null,
                false,
                false,
                Collections.singletonList(entityType));
        assertEquals(dataset.getId(), 1);
        assertEquals(dataset.getName(), "MetaData");
        assertEquals(dataset.toString(), "MetaData");
        assertTrue(dataset.isTypeDataset());
        assertFalse(dataset.isInstanceDataset());
        assertNull(dataset.getDatasetRootEntityType());
        assertEquals(dataset.getFilterType(), Dataset.FilterType.NONE);
        assertFalse(dataset.isCacheable());
        assertFalse(dataset.isExternal());
        assertEquals(dataset.getEntityTypes().size(), 1);
        assertTrue(dataset.getEntityTypes().contains(entityType));
    }

    @Test
    public void instanceDataset() {
        final Dataset dataset = new Dataset(
                1,
                "MetaData",
                String.class,
                Dataset.FilterType.NONE,
                false,
                null,
                false,
                true,
                Collections.emptyList());
        assertEquals(dataset.getId(), 1);
        assertEquals(dataset.getName(), "MetaData");
        assertEquals(dataset.toString(), "MetaData");
        assertFalse(dataset.isTypeDataset());
        assertTrue(dataset.isInstanceDataset());
        assertEquals(dataset.getDatasetRootEntityType(), String.class);
        assertEquals(dataset.getFilterType(), Dataset.FilterType.NONE);
        assertFalse(dataset.isCacheable());
        assertTrue(dataset.isExternal());
    }

    @Test
    public void staticFilteredDataset() {
        final int id = ValueUtil.randomInt();
        final String name = ValueUtil.randomString();
        final boolean typeDataset = false;
        final boolean cacheable = false;
        final boolean external = true;
        final Dataset dataset = new Dataset(
                id,
                name,
                String.class,
                Dataset.FilterType.STATIC,
                false,
                null,
                cacheable,
                external,
                Collections.emptyList());
        assertEquals(dataset.getId(), id);
        assertEquals(dataset.getName(), name);
        assertEquals(dataset.toString(), name);
        assertEquals(dataset.isTypeDataset(), typeDataset);
        assertTrue(dataset.isInstanceDataset());
        assertEquals(dataset.getDatasetRootEntityType(), String.class);
        assertEquals(dataset.getFilterType(), Dataset.FilterType.STATIC);
        assertEquals(dataset.isCacheable(), cacheable);
        assertEquals(dataset.isExternal(), external);
    }

    @Test
    public void staticKeyedFilteredDataset() {
        final int id = ValueUtil.randomInt();
        final String name = ValueUtil.randomString();
        final boolean cacheable = false;
        final boolean external = true;
        final Dataset dataset = new Dataset(
                id,
                name,
                String.class,
                Dataset.FilterType.STATIC,
                true,
                null,
                cacheable,
                external,
                Collections.emptyList());
        assertEquals(dataset.getId(), id);
        assertEquals(dataset.getName(), name);
        assertEquals(dataset.toString(), name);
        assertFalse(dataset.isTypeDataset());
        assertTrue(dataset.isInstanceDataset());
        assertEquals(dataset.getDatasetRootEntityType(), String.class);
        assertEquals(dataset.getFilterType(), Dataset.FilterType.STATIC);
        assertEquals(dataset.isCacheable(), cacheable);
        assertEquals(dataset.isExternal(), external);
    }

    @Test
    public void dynamicFilteredDataset() {
        final int id = ValueUtil.randomInt();
        final String name = ValueUtil.randomString();
        final boolean cacheable = false;
        final boolean external = true;
        final SubscriptionUpdateReplicaFilter<?> filter = mock(SubscriptionUpdateReplicaFilter.class);
        final Dataset dataset = new Dataset(
                id,
                name,
                null,
                Dataset.FilterType.DYNAMIC,
                false,
                filter,
                cacheable,
                external,
                Collections.emptyList());
        assertEquals(dataset.getId(), id);
        assertEquals(dataset.getName(), name);
        assertEquals(dataset.toString(), name);
        assertTrue(dataset.isTypeDataset());
        assertFalse(dataset.isInstanceDataset());
        assertNull(dataset.getDatasetRootEntityType());
        assertEquals(dataset.getFilterType(), Dataset.FilterType.DYNAMIC);
        assertEquals(dataset.getFilter(), filter);
        assertEquals(dataset.isCacheable(), cacheable);
        assertEquals(dataset.isExternal(), external);
    }

    @Test
    public void noNameSuppliedWhenNamesDisabled() {
        ReplicantTestUtil.disableNames();
        final Dataset dataset = new Dataset(
                ValueUtil.randomInt(),
                null,
                null,
                Dataset.FilterType.NONE,
                false,
                null,
                ValueUtil.randomBoolean(),
                ValueUtil.randomBoolean(),
                Collections.emptyList());
        final IllegalStateException exception = expectThrows(IllegalStateException.class, dataset::getName);
        assertEquals(
                exception.getMessage(),
                "Replicant-0044: Dataset.getName() invoked when Replicant.areNamesEnabled() is false");
        assertEquals(dataset.toString(), "replicant.Dataset@" + Integer.toHexString(dataset.hashCode()));
    }

    @Test
    public void passNameToConstructorWhenNamesDisabled() {
        ReplicantTestUtil.disableNames();
        final IllegalStateException exception = expectThrows(
                IllegalStateException.class,
                () -> new Dataset(
                        ValueUtil.randomInt(),
                        "MyDataset",
                        null,
                        Dataset.FilterType.NONE,
                        false,
                        null,
                        ValueUtil.randomBoolean(),
                        ValueUtil.randomBoolean(),
                        Collections.emptyList()));
        assertEquals(
                exception.getMessage(),
                "Replicant-0045: Dataset passed a name 'MyDataset' but Replicant.areNamesEnabled() is false");
    }

    @Test
    public void constructorPassedNoFilterWhenExpected() {
        final IllegalStateException exception = expectThrows(
                IllegalStateException.class,
                () -> new Dataset(
                        222,
                        "MyDataset",
                        null,
                        Dataset.FilterType.DYNAMIC,
                        false,
                        null,
                        ValueUtil.randomBoolean(),
                        ValueUtil.randomBoolean(),
                        Collections.emptyList()));
        assertEquals(
                exception.getMessage(),
                "Replicant-0076: Dataset 222 has a DYNAMIC filterType but has supplied no filter.");
    }

    @Test
    public void constructorPassedFilterWhenNotExpected() {
        final IllegalStateException exception = expectThrows(
                IllegalStateException.class,
                () -> new Dataset(
                        222,
                        "MyDataset",
                        null,
                        Dataset.FilterType.STATIC,
                        false,
                        mock(SubscriptionUpdateReplicaFilter.class),
                        ValueUtil.randomBoolean(),
                        ValueUtil.randomBoolean(),
                        Collections.emptyList()));
        assertEquals(
                exception.getMessage(),
                "Replicant-0077: Dataset 222 does not have a DYNAMIC filterType but has supplied a filter.");
    }

    @Test
    public void constructorPassedFilterWhenNotExpected_staticKeyed() {
        final IllegalStateException exception = expectThrows(
                IllegalStateException.class,
                () -> new Dataset(
                        222,
                        "MyDataset",
                        null,
                        Dataset.FilterType.STATIC,
                        true,
                        mock(SubscriptionUpdateReplicaFilter.class),
                        ValueUtil.randomBoolean(),
                        ValueUtil.randomBoolean(),
                        Collections.emptyList()));
        assertEquals(
                exception.getMessage(),
                "Replicant-0077: Dataset 222 does not have a DYNAMIC filterType but has supplied a filter.");
    }
}
