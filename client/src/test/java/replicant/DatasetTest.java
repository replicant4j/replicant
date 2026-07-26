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
                Dataset.FilterMode.UNFILTERED,
                null,
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
                Dataset.FilterMode.UNFILTERED,
                null,
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
        assertEquals(dataset.getFilterMode(), Dataset.FilterMode.UNFILTERED);
        assertTrue(dataset.isUnfiltered());
        assertFalse(dataset.isImplicitlyFiltered());
        assertFalse(dataset.isParameterFiltered());
        assertNull(dataset.getFilterParameterMode());
        assertFalse(dataset.hasFixedFilterParameter());
        assertFalse(dataset.hasUpdatableFilterParameter());
        assertFalse(dataset.isKeyed());
        assertNull(dataset.getFilterParameterUpdateReplicaMatcher());
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
                Dataset.FilterMode.UNFILTERED,
                null,
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
        assertEquals(dataset.getFilterMode(), Dataset.FilterMode.UNFILTERED);
        assertTrue(dataset.isUnfiltered());
        assertFalse(dataset.isImplicitlyFiltered());
        assertFalse(dataset.isParameterFiltered());
        assertNull(dataset.getFilterParameterMode());
        assertFalse(dataset.hasFixedFilterParameter());
        assertFalse(dataset.hasUpdatableFilterParameter());
        assertFalse(dataset.isKeyed());
        assertNull(dataset.getFilterParameterUpdateReplicaMatcher());
        assertFalse(dataset.isCacheable());
        assertTrue(dataset.isExternal());
    }

    @Test
    public void fixedParameterFilteredDataset() {
        final int id = ValueUtil.randomInt();
        final String name = ValueUtil.randomString();
        final boolean typeDataset = false;
        final boolean cacheable = false;
        final boolean external = true;
        final Dataset dataset = new Dataset(
                id,
                name,
                String.class,
                Dataset.FilterMode.PARAMETER_FILTERED,
                Dataset.FilterParameterMode.FIXED,
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
        assertEquals(dataset.getFilterMode(), Dataset.FilterMode.PARAMETER_FILTERED);
        assertEquals(dataset.getFilterParameterMode(), Dataset.FilterParameterMode.FIXED);
        assertFalse(dataset.isUnfiltered());
        assertFalse(dataset.isImplicitlyFiltered());
        assertTrue(dataset.isParameterFiltered());
        assertTrue(dataset.hasFixedFilterParameter());
        assertFalse(dataset.hasUpdatableFilterParameter());
        assertFalse(dataset.isKeyed());
        assertNull(dataset.getFilterParameterUpdateReplicaMatcher());
        assertEquals(dataset.isCacheable(), cacheable);
        assertEquals(dataset.isExternal(), external);
    }

    @Test
    public void fixedParameterFilteredKeyedDataset() {
        final int id = ValueUtil.randomInt();
        final String name = ValueUtil.randomString();
        final boolean cacheable = false;
        final boolean external = true;
        final Dataset dataset = new Dataset(
                id,
                name,
                String.class,
                Dataset.FilterMode.PARAMETER_FILTERED,
                Dataset.FilterParameterMode.FIXED,
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
        assertEquals(dataset.getFilterMode(), Dataset.FilterMode.PARAMETER_FILTERED);
        assertEquals(dataset.getFilterParameterMode(), Dataset.FilterParameterMode.FIXED);
        assertTrue(dataset.isParameterFiltered());
        assertTrue(dataset.hasFixedFilterParameter());
        assertFalse(dataset.hasUpdatableFilterParameter());
        assertTrue(dataset.isKeyed());
        assertNull(dataset.getFilterParameterUpdateReplicaMatcher());
        assertEquals(dataset.isCacheable(), cacheable);
        assertEquals(dataset.isExternal(), external);
    }

    @Test
    public void updatableParameterFilteredDataset() {
        final int id = ValueUtil.randomInt();
        final String name = ValueUtil.randomString();
        final boolean cacheable = false;
        final boolean external = true;
        final FilterParameterUpdateReplicaMatcher<?> matcher = mock(FilterParameterUpdateReplicaMatcher.class);
        final Dataset dataset = new Dataset(
                id,
                name,
                null,
                Dataset.FilterMode.PARAMETER_FILTERED,
                Dataset.FilterParameterMode.UPDATABLE,
                false,
                matcher,
                cacheable,
                external,
                Collections.emptyList());
        assertEquals(dataset.getId(), id);
        assertEquals(dataset.getName(), name);
        assertEquals(dataset.toString(), name);
        assertTrue(dataset.isTypeDataset());
        assertFalse(dataset.isInstanceDataset());
        assertNull(dataset.getDatasetRootEntityType());
        assertEquals(dataset.getFilterMode(), Dataset.FilterMode.PARAMETER_FILTERED);
        assertEquals(dataset.getFilterParameterMode(), Dataset.FilterParameterMode.UPDATABLE);
        assertFalse(dataset.isUnfiltered());
        assertFalse(dataset.isImplicitlyFiltered());
        assertTrue(dataset.isParameterFiltered());
        assertFalse(dataset.hasFixedFilterParameter());
        assertTrue(dataset.hasUpdatableFilterParameter());
        assertFalse(dataset.isKeyed());
        assertEquals(dataset.getFilterParameterUpdateReplicaMatcher(), matcher);
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
                Dataset.FilterMode.UNFILTERED,
                null,
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
                        Dataset.FilterMode.UNFILTERED,
                        null,
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
    public void constructorPassedNoMatcherForUpdatableFilterParameter() {
        final IllegalStateException exception = expectThrows(
                IllegalStateException.class,
                () -> new Dataset(
                        222,
                        "MyDataset",
                        null,
                        Dataset.FilterMode.PARAMETER_FILTERED,
                        Dataset.FilterParameterMode.UPDATABLE,
                        false,
                        null,
                        ValueUtil.randomBoolean(),
                        ValueUtil.randomBoolean(),
                        Collections.emptyList()));
        assertEquals(
                exception.getMessage(),
                "Replicant-0076: Dataset 222 has an updatable Filter Parameter but has supplied no Filter Parameter"
                        + " update Replica matcher.");
    }

    @Test
    public void constructorPassedMatcherForFixedFilterParameter() {
        final IllegalStateException exception = expectThrows(
                IllegalStateException.class,
                () -> new Dataset(
                        222,
                        "MyDataset",
                        null,
                        Dataset.FilterMode.PARAMETER_FILTERED,
                        Dataset.FilterParameterMode.FIXED,
                        false,
                        mock(FilterParameterUpdateReplicaMatcher.class),
                        ValueUtil.randomBoolean(),
                        ValueUtil.randomBoolean(),
                        Collections.emptyList()));
        assertEquals(
                exception.getMessage(),
                "Replicant-0077: Dataset 222 does not have an updatable Filter Parameter but has supplied a Filter"
                        + " Parameter update Replica matcher.");
    }

    @Test
    public void constructorPassedMatcherForFixedFilterParameterOnKeyedDataset() {
        final IllegalStateException exception = expectThrows(
                IllegalStateException.class,
                () -> new Dataset(
                        222,
                        "MyDataset",
                        null,
                        Dataset.FilterMode.PARAMETER_FILTERED,
                        Dataset.FilterParameterMode.FIXED,
                        true,
                        mock(FilterParameterUpdateReplicaMatcher.class),
                        ValueUtil.randomBoolean(),
                        ValueUtil.randomBoolean(),
                        Collections.emptyList()));
        assertEquals(
                exception.getMessage(),
                "Replicant-0077: Dataset 222 does not have an updatable Filter Parameter but has supplied a Filter"
                        + " Parameter update Replica matcher.");
    }

    @Test
    public void implicitlyFilteredDataset() {
        final Dataset dataset = new Dataset(
                1,
                "MetaData",
                null,
                Dataset.FilterMode.IMPLICIT,
                null,
                false,
                null,
                false,
                true,
                Collections.emptyList());
        assertEquals(dataset.getFilterMode(), Dataset.FilterMode.IMPLICIT);
        assertFalse(dataset.isUnfiltered());
        assertTrue(dataset.isImplicitlyFiltered());
        assertFalse(dataset.isParameterFiltered());
        assertNull(dataset.getFilterParameterMode());
        assertFalse(dataset.hasFixedFilterParameter());
        assertFalse(dataset.hasUpdatableFilterParameter());
        assertFalse(dataset.isKeyed());
        assertNull(dataset.getFilterParameterUpdateReplicaMatcher());
    }

    @Test
    public void updatableParameterFilteredKeyedDataset() {
        final FilterParameterUpdateReplicaMatcher<?> matcher = mock(FilterParameterUpdateReplicaMatcher.class);
        final Dataset dataset = new Dataset(
                1,
                "MetaData",
                null,
                Dataset.FilterMode.PARAMETER_FILTERED,
                Dataset.FilterParameterMode.UPDATABLE,
                true,
                matcher,
                false,
                true,
                Collections.emptyList());
        assertTrue(dataset.isParameterFiltered());
        assertTrue(dataset.hasUpdatableFilterParameter());
        assertTrue(dataset.isKeyed());
        assertEquals(dataset.getFilterParameterUpdateReplicaMatcher(), matcher);
    }

    @Test
    public void constructorPassedFilterParameterModeForUnfilteredDataset() {
        final IllegalStateException exception = expectThrows(
                IllegalStateException.class,
                () -> new Dataset(
                        222,
                        "MyDataset",
                        null,
                        Dataset.FilterMode.UNFILTERED,
                        Dataset.FilterParameterMode.FIXED,
                        false,
                        null,
                        false,
                        true,
                        Collections.emptyList()));
        assertEquals(
                exception.getMessage(),
                "Replicant-0100: Dataset 222 is not parameter-filtered but has supplied a Filter Parameter mode.");
    }

    @Test
    public void constructorPassedFilterParameterModeForImplicitlyFilteredDataset() {
        final IllegalStateException exception = expectThrows(
                IllegalStateException.class,
                () -> new Dataset(
                        222,
                        "MyDataset",
                        null,
                        Dataset.FilterMode.IMPLICIT,
                        Dataset.FilterParameterMode.UPDATABLE,
                        false,
                        mock(FilterParameterUpdateReplicaMatcher.class),
                        false,
                        true,
                        Collections.emptyList()));
        assertEquals(
                exception.getMessage(),
                "Replicant-0100: Dataset 222 is not parameter-filtered but has supplied a Filter Parameter mode.");
    }

    @Test
    public void constructorPassedNoFilterParameterModeForParameterFilteredDataset() {
        final IllegalStateException exception = expectThrows(
                IllegalStateException.class,
                () -> new Dataset(
                        222,
                        "MyDataset",
                        null,
                        Dataset.FilterMode.PARAMETER_FILTERED,
                        null,
                        false,
                        null,
                        false,
                        true,
                        Collections.emptyList()));
        assertEquals(
                exception.getMessage(),
                "Replicant-0101: Dataset 222 is parameter-filtered but has supplied no Filter Parameter mode.");
    }

    @Test
    public void constructorPassedKeyForUnfilteredDataset() {
        final IllegalStateException exception = expectThrows(
                IllegalStateException.class,
                () -> new Dataset(
                        222,
                        "MyDataset",
                        null,
                        Dataset.FilterMode.UNFILTERED,
                        null,
                        true,
                        null,
                        false,
                        true,
                        Collections.emptyList()));
        assertEquals(exception.getMessage(), "Replicant-0102: Dataset 222 is keyed but is not parameter-filtered.");
    }

    @Test
    public void constructorPassedKeyForImplicitlyFilteredDataset() {
        final IllegalStateException exception = expectThrows(
                IllegalStateException.class,
                () -> new Dataset(
                        222,
                        "MyDataset",
                        null,
                        Dataset.FilterMode.IMPLICIT,
                        null,
                        true,
                        null,
                        false,
                        true,
                        Collections.emptyList()));
        assertEquals(exception.getMessage(), "Replicant-0102: Dataset 222 is keyed but is not parameter-filtered.");
    }
}
