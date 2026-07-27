package replicant.server.transport;

import static org.testng.Assert.*;

import java.util.Set;
import org.testng.annotations.Test;

public class DatasetTest {
    @Test
    public void visibilityOrigins() {
        assertTrue(Dataset.Visibility.EXTERNAL.permitsAreaOfInterestOrigin());
        assertFalse(Dataset.Visibility.EXTERNAL.permitsDatasetLinkOrRequiredTypeDatasetOrigin());
        assertFalse(Dataset.Visibility.INTERNAL.permitsAreaOfInterestOrigin());
        assertTrue(Dataset.Visibility.INTERNAL.permitsDatasetLinkOrRequiredTypeDatasetOrigin());
        assertTrue(Dataset.Visibility.UNIVERSAL.permitsAreaOfInterestOrigin());
        assertTrue(Dataset.Visibility.UNIVERSAL.permitsDatasetLinkOrRequiredTypeDatasetOrigin());
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Test
    public void typeDataset() {
        final var dataset = new Dataset(
                1,
                "ReferenceData",
                null,
                Dataset.FilterMode.UNFILTERED,
                null,
                false,
                false,
                Dataset.Visibility.INTERNAL);

        assertEquals(dataset.getId(), 1);
        assertEquals(dataset.getName(), "ReferenceData");
        assertTrue(dataset.isTypeDataset());
        assertFalse(dataset.isInstanceDataset());
        assertTrue(dataset.isUnfiltered());
        assertFalse(dataset.isImplicitlyFiltered());
        assertFalse(dataset.isParameterFiltered());
        assertNull(dataset.getFilterParameterMode());
        assertFalse(dataset.hasFixedFilterParameter());
        assertFalse(dataset.hasUpdatableFilterParameter());
        assertFalse(dataset.isKeyed());
        assertFalse(dataset.isCacheable());
        assertEquals(dataset.getVisibility(), Dataset.Visibility.INTERNAL);
        assertThrows(dataset::getDatasetRootEntityTypeId);
    }

    @Test
    public void instanceDataset() {
        final var dataset = new Dataset(
                1, "ReferenceData", 23, Dataset.FilterMode.IMPLICIT, null, false, false, Dataset.Visibility.UNIVERSAL);

        assertFalse(dataset.isTypeDataset());
        assertTrue(dataset.isInstanceDataset());
        assertEquals(dataset.getDatasetRootEntityTypeId(), (Integer) 23);
        assertFalse(dataset.isUnfiltered());
        assertTrue(dataset.isImplicitlyFiltered());
        assertFalse(dataset.isParameterFiltered());
        assertNull(dataset.getFilterParameterMode());
        assertFalse(dataset.isKeyed());
        assertEquals(dataset.getVisibility(), Dataset.Visibility.UNIVERSAL);
    }

    @Test
    public void fixedParameterFilteredDataset() {
        final var dataset = new Dataset(
                1,
                "ReferenceData",
                22,
                Dataset.FilterMode.PARAMETER_FILTERED,
                Dataset.FilterParameterMode.FIXED,
                false,
                false,
                Dataset.Visibility.UNIVERSAL);

        assertTrue(dataset.isParameterFiltered());
        assertEquals(dataset.getFilterParameterMode(), Dataset.FilterParameterMode.FIXED);
        assertTrue(dataset.hasFixedFilterParameter());
        assertFalse(dataset.hasUpdatableFilterParameter());
        assertFalse(dataset.isKeyed());
    }

    @Test
    public void updatableParameterFilteredKeyedDataset() {
        final var dataset = new Dataset(
                2,
                "ReferenceData",
                22,
                Dataset.FilterMode.PARAMETER_FILTERED,
                Dataset.FilterParameterMode.UPDATABLE,
                true,
                false,
                Dataset.Visibility.UNIVERSAL);

        assertTrue(dataset.isParameterFiltered());
        assertEquals(dataset.getFilterParameterMode(), Dataset.FilterParameterMode.UPDATABLE);
        assertFalse(dataset.hasFixedFilterParameter());
        assertTrue(dataset.hasUpdatableFilterParameter());
        assertTrue(dataset.isKeyed());
    }

    @Test
    public void parameterFilteredDatasetAllowsEveryParameterModeAndKeyingCombination() {
        for (final var filterParameterMode : Dataset.FilterParameterMode.values()) {
            for (final var keyed : new boolean[] {false, true}) {
                final var dataset = new Dataset(
                        1,
                        "ReferenceData",
                        null,
                        Dataset.FilterMode.PARAMETER_FILTERED,
                        filterParameterMode,
                        keyed,
                        false,
                        Dataset.Visibility.UNIVERSAL);

                assertEquals(dataset.getFilterParameterMode(), filterParameterMode);
                assertEquals(dataset.isKeyed(), keyed);
            }
        }
    }

    @Test
    public void parameterFilteredDatasetRequiresFilterParameterMode() {
        final var error = expectThrows(
                IllegalArgumentException.class,
                () -> new Dataset(
                        1,
                        "ReferenceData",
                        null,
                        Dataset.FilterMode.PARAMETER_FILTERED,
                        null,
                        false,
                        false,
                        Dataset.Visibility.UNIVERSAL));

        assertEquals(error.getMessage(), "Parameter-Filtered Dataset requires a Filter Parameter Mode");
    }

    @Test
    public void unfilteredDatasetRejectsFilterParameterMode() {
        assertFilterParameterModeRejected(Dataset.FilterMode.UNFILTERED);
    }

    @Test
    public void implicitlyFilteredDatasetRejectsFilterParameterMode() {
        assertFilterParameterModeRejected(Dataset.FilterMode.IMPLICIT);
    }

    @Test
    public void unfilteredDatasetCannotBeKeyed() {
        assertKeyedRejected(Dataset.FilterMode.UNFILTERED);
    }

    @Test
    public void implicitlyFilteredDatasetCannotBeKeyed() {
        assertKeyedRejected(Dataset.FilterMode.IMPLICIT);
    }

    @Test
    public void requiredTypeDatasetsTrackDependencyDirection() {
        final var requiredTypeDataset = new Dataset(
                1,
                "ReferenceData",
                null,
                Dataset.FilterMode.UNFILTERED,
                null,
                false,
                false,
                Dataset.Visibility.INTERNAL);
        final var requiringDataset = new Dataset(
                2,
                "Event",
                22,
                Dataset.FilterMode.UNFILTERED,
                null,
                false,
                false,
                Dataset.Visibility.UNIVERSAL,
                requiredTypeDataset);

        assertEquals(requiringDataset.getRequiredTypeDatasets(), new Dataset[] {requiredTypeDataset});
        assertEquals(requiredTypeDataset.getDependentDatasets(), Set.of(requiringDataset));
        assertTrue(requiringDataset.getDependentDatasets().isEmpty());
        assertEquals(requiredTypeDataset.getRequiredTypeDatasets().length, 0);
    }

    @Test
    public void requiredTypeDatasetMustBeTypeDataset() {
        final var instanceDataset = new Dataset(
                1, "Event", 22, Dataset.FilterMode.UNFILTERED, null, false, false, Dataset.Visibility.UNIVERSAL);

        final var error = expectThrows(
                IllegalArgumentException.class,
                () -> new Dataset(
                        2,
                        "Requiring",
                        null,
                        Dataset.FilterMode.UNFILTERED,
                        null,
                        false,
                        false,
                        Dataset.Visibility.UNIVERSAL,
                        instanceDataset));
        assertEquals(error.getMessage(), "Specified Required Type Dataset Event is not a Type Dataset");
    }

    @Test
    public void requiredTypeDatasetMustPermitRequiredTypeDatasetOrigin() {
        final var externalDataset = new Dataset(
                1,
                "ReferenceData",
                null,
                Dataset.FilterMode.UNFILTERED,
                null,
                false,
                false,
                Dataset.Visibility.EXTERNAL);

        final var error = expectThrows(
                IllegalArgumentException.class,
                () -> new Dataset(
                        2,
                        "Requiring",
                        null,
                        Dataset.FilterMode.UNFILTERED,
                        null,
                        false,
                        false,
                        Dataset.Visibility.UNIVERSAL,
                        externalDataset));
        assertEquals(
                error.getMessage(),
                "Specified Required Type Dataset ReferenceData has EXTERNAL Dataset Visibility, which does not permit"
                        + " a Required Type Dataset origin");
    }

    private void assertFilterParameterModeRejected(final Dataset.FilterMode filterMode) {
        for (final var filterParameterMode : Dataset.FilterParameterMode.values()) {
            final var error = expectThrows(
                    IllegalArgumentException.class,
                    () -> new Dataset(
                            1,
                            "ReferenceData",
                            null,
                            filterMode,
                            filterParameterMode,
                            false,
                            false,
                            Dataset.Visibility.UNIVERSAL));

            assertEquals(error.getMessage(), "Filter Parameter Mode is only valid for a Parameter-Filtered Dataset");
        }
    }

    private void assertKeyedRejected(final Dataset.FilterMode filterMode) {
        final var error = expectThrows(
                IllegalArgumentException.class,
                () -> new Dataset(
                        1, "ReferenceData", null, filterMode, null, true, false, Dataset.Visibility.UNIVERSAL));

        assertEquals(error.getMessage(), "Only a Parameter-Filtered Dataset can be keyed");
    }
}
