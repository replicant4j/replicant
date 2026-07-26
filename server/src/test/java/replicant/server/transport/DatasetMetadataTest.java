package replicant.server.transport;

import static org.testng.Assert.*;

import java.util.Set;
import org.testng.annotations.Test;

public class DatasetMetadataTest {
    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    public void typeDataset() {
        final var metadata = new DatasetMetadata(
                1,
                "Metadata",
                null,
                DatasetMetadata.FilterMode.UNFILTERED,
                null,
                false,
                DatasetMetadata.CacheType.NONE,
                false);

        assertEquals(metadata.getDatasetId(), 1);
        assertEquals(metadata.getName(), "Metadata");
        assertTrue(metadata.isTypeDataset());
        assertFalse(metadata.isInstanceDataset());
        assertTrue(metadata.isUnfiltered());
        assertFalse(metadata.isImplicitlyFiltered());
        assertFalse(metadata.isParameterFiltered());
        assertNull(metadata.getFilterParameterMode());
        assertFalse(metadata.hasFixedFilterParameter());
        assertFalse(metadata.hasUpdatableFilterParameter());
        assertFalse(metadata.isKeyed());
        assertFalse(metadata.isCacheable());
        assertFalse(metadata.isExternal());
        assertThrows(metadata::getDatasetRootEntityTypeId);
    }

    @Test
    public void instanceDataset() {
        final var metadata = new DatasetMetadata(
                1,
                "Metadata",
                23,
                DatasetMetadata.FilterMode.IMPLICIT,
                null,
                false,
                DatasetMetadata.CacheType.NONE,
                true);

        assertFalse(metadata.isTypeDataset());
        assertTrue(metadata.isInstanceDataset());
        assertEquals(metadata.getDatasetRootEntityTypeId(), (Integer) 23);
        assertFalse(metadata.isUnfiltered());
        assertTrue(metadata.isImplicitlyFiltered());
        assertFalse(metadata.isParameterFiltered());
        assertNull(metadata.getFilterParameterMode());
        assertFalse(metadata.isKeyed());
        assertTrue(metadata.isExternal());
    }

    @Test
    public void fixedParameterFilteredDataset() {
        final var metadata = new DatasetMetadata(
                1,
                "Metadata",
                22,
                DatasetMetadata.FilterMode.PARAMETER_FILTERED,
                DatasetMetadata.FilterParameterMode.FIXED,
                false,
                DatasetMetadata.CacheType.NONE,
                true);

        assertTrue(metadata.isParameterFiltered());
        assertEquals(metadata.getFilterParameterMode(), DatasetMetadata.FilterParameterMode.FIXED);
        assertTrue(metadata.hasFixedFilterParameter());
        assertFalse(metadata.hasUpdatableFilterParameter());
        assertFalse(metadata.isKeyed());
    }

    @Test
    public void updatableParameterFilteredKeyedDataset() {
        final var metadata = new DatasetMetadata(
                2,
                "Metadata",
                22,
                DatasetMetadata.FilterMode.PARAMETER_FILTERED,
                DatasetMetadata.FilterParameterMode.UPDATABLE,
                true,
                DatasetMetadata.CacheType.NONE,
                true);

        assertTrue(metadata.isParameterFiltered());
        assertEquals(metadata.getFilterParameterMode(), DatasetMetadata.FilterParameterMode.UPDATABLE);
        assertFalse(metadata.hasFixedFilterParameter());
        assertTrue(metadata.hasUpdatableFilterParameter());
        assertTrue(metadata.isKeyed());
    }

    @Test
    public void parameterFilteredDatasetAllowsEveryParameterModeAndKeyingCombination() {
        for (final var filterParameterMode : DatasetMetadata.FilterParameterMode.values()) {
            for (final var keyed : new boolean[] {false, true}) {
                final var metadata = new DatasetMetadata(
                        1,
                        "Metadata",
                        null,
                        DatasetMetadata.FilterMode.PARAMETER_FILTERED,
                        filterParameterMode,
                        keyed,
                        DatasetMetadata.CacheType.NONE,
                        true);

                assertEquals(metadata.getFilterParameterMode(), filterParameterMode);
                assertEquals(metadata.isKeyed(), keyed);
            }
        }
    }

    @Test
    public void parameterFilteredDatasetRequiresFilterParameterMode() {
        final var error = expectThrows(
                IllegalArgumentException.class,
                () -> new DatasetMetadata(
                        1,
                        "Metadata",
                        null,
                        DatasetMetadata.FilterMode.PARAMETER_FILTERED,
                        null,
                        false,
                        DatasetMetadata.CacheType.NONE,
                        true));

        assertEquals(error.getMessage(), "Parameter-Filtered Dataset requires a Filter Parameter Mode");
    }

    @Test
    public void unfilteredDatasetRejectsFilterParameterMode() {
        assertFilterParameterModeRejected(DatasetMetadata.FilterMode.UNFILTERED);
    }

    @Test
    public void implicitlyFilteredDatasetRejectsFilterParameterMode() {
        assertFilterParameterModeRejected(DatasetMetadata.FilterMode.IMPLICIT);
    }

    @Test
    public void unfilteredDatasetCannotBeKeyed() {
        assertKeyedRejected(DatasetMetadata.FilterMode.UNFILTERED);
    }

    @Test
    public void implicitlyFilteredDatasetCannotBeKeyed() {
        assertKeyedRejected(DatasetMetadata.FilterMode.IMPLICIT);
    }

    @Test
    public void requiredTypeDatasetsTrackDependencyDirection() {
        final var requiredTypeDataset = new DatasetMetadata(
                1,
                "Metadata",
                null,
                DatasetMetadata.FilterMode.UNFILTERED,
                null,
                false,
                DatasetMetadata.CacheType.NONE,
                false);
        final var requiringDataset = new DatasetMetadata(
                2,
                "Event",
                22,
                DatasetMetadata.FilterMode.UNFILTERED,
                null,
                false,
                DatasetMetadata.CacheType.NONE,
                true,
                requiredTypeDataset);

        assertEquals(requiringDataset.getRequiredTypeDatasets(), new DatasetMetadata[] {requiredTypeDataset});
        assertEquals(requiredTypeDataset.getDependentDatasets(), Set.of(requiringDataset));
        assertTrue(requiringDataset.getDependentDatasets().isEmpty());
        assertEquals(requiredTypeDataset.getRequiredTypeDatasets().length, 0);
    }

    @Test
    public void requiredTypeDatasetMustBeTypeDataset() {
        final var instanceDataset = new DatasetMetadata(
                1,
                "Event",
                22,
                DatasetMetadata.FilterMode.UNFILTERED,
                null,
                false,
                DatasetMetadata.CacheType.NONE,
                true);

        final var error = expectThrows(
                IllegalArgumentException.class,
                () -> new DatasetMetadata(
                        2,
                        "Requiring",
                        null,
                        DatasetMetadata.FilterMode.UNFILTERED,
                        null,
                        false,
                        DatasetMetadata.CacheType.NONE,
                        true,
                        instanceDataset));
        assertEquals(error.getMessage(), "Specified Required Type Dataset Event is not a Type Dataset");
    }

    private void assertFilterParameterModeRejected(final DatasetMetadata.FilterMode filterMode) {
        for (final var filterParameterMode : DatasetMetadata.FilterParameterMode.values()) {
            final var error = expectThrows(
                    IllegalArgumentException.class,
                    () -> new DatasetMetadata(
                            1,
                            "Metadata",
                            null,
                            filterMode,
                            filterParameterMode,
                            false,
                            DatasetMetadata.CacheType.NONE,
                            true));

            assertEquals(error.getMessage(), "Filter Parameter Mode is only valid for a Parameter-Filtered Dataset");
        }
    }

    private void assertKeyedRejected(final DatasetMetadata.FilterMode filterMode) {
        final var error = expectThrows(
                IllegalArgumentException.class,
                () -> new DatasetMetadata(
                        1, "Metadata", null, filterMode, null, true, DatasetMetadata.CacheType.NONE, true));

        assertEquals(error.getMessage(), "Only a Parameter-Filtered Dataset can be keyed");
    }
}
