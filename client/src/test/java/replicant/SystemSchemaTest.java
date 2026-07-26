package replicant;

import static org.testng.Assert.*;

import java.util.Collections;
import org.testng.annotations.Test;

public class SystemSchemaTest extends AbstractReplicantTest {
    @Test
    public void basicOperation() {
        final int id = ValueUtil.randomInt();
        final String name = ValueUtil.randomString();
        final EntityType entityType1 =
                new EntityType(0, ValueUtil.randomString(), Integer.class, (i, d) -> 1, null, new DatasetLink[0]);
        final EntityType entityType2 =
                new EntityType(1, ValueUtil.randomString(), String.class, (i, d) -> "", null, new DatasetLink[0]);
        final EntityType[] entityTypes = new EntityType[] {entityType1, entityType2};
        final Dataset dataset1 = new Dataset(
                1,
                ValueUtil.randomString(),
                null,
                Dataset.FilterType.NONE,
                false,
                null,
                false,
                true,
                Collections.emptyList());
        final Dataset[] datasets = {null, dataset1};
        final SystemSchema systemSchema = new SystemSchema(id, name, datasets, entityTypes);
        assertEquals(systemSchema.getId(), id);
        assertEquals(systemSchema.getName(), name);
        assertEquals(systemSchema.getEntityTypeCount(), 2);
        assertEquals(systemSchema.getEntityType(0), entityType1);
        assertEquals(systemSchema.getEntityType(1), entityType2);
        assertEquals(systemSchema.getDatasetCount(), 2);
        assertEquals(systemSchema.getDataset(1), dataset1);
        assertEquals(systemSchema.toString(), name);
    }

    @Test
    public void getDataset_BadIndex() {
        final SystemSchema systemSchema = new SystemSchema(
                ValueUtil.randomInt(), ValueUtil.randomString(), new Dataset[] {}, new EntityType[] {});
        final IllegalStateException exception =
                expectThrows(IllegalStateException.class, () -> systemSchema.getDataset(2));
        assertEquals(
                exception.getMessage(),
                "Replicant-0058: SystemSchema.getDataset(id) passed an id that is out of range.");
    }

    @Test
    public void getEntityType_BadIndex() {
        final SystemSchema systemSchema = new SystemSchema(
                ValueUtil.randomInt(), ValueUtil.randomString(), new Dataset[] {}, new EntityType[] {});
        final IllegalStateException exception =
                expectThrows(IllegalStateException.class, () -> systemSchema.getEntityType(2));
        assertEquals(
                exception.getMessage(),
                "Replicant-0057: SystemSchema.getEntityType(id) passed an id that is out of range.");
    }

    @Test
    public void construct_nullEntityType() {

        final IllegalStateException exception = expectThrows(
                IllegalStateException.class,
                () -> new SystemSchema(ValueUtil.randomInt(), "X", new Dataset[] {}, new EntityType[] {null}));
        assertEquals(
                exception.getMessage(),
                "Replicant-0053: SystemSchema named 'X' passed an array of entity types that has a null element");
    }

    @Test
    public void construct_badEntityTypeIndex() {
        final EntityType entityType =
                new EntityType(23, ValueUtil.randomString(), Integer.class, (i, d) -> 1, null, new DatasetLink[0]);
        final IllegalStateException exception = expectThrows(
                IllegalStateException.class,
                () -> new SystemSchema(ValueUtil.randomInt(), "X", new Dataset[] {}, new EntityType[] {entityType}));
        assertEquals(
                exception.getMessage(),
                "Replicant-0054: SystemSchema named 'X' passed an array of entity types where entity type at index 0"
                        + " does not have id matching index.");
    }

    @Test
    public void construct_badDatasetIndex() {
        final Dataset dataset1 = new Dataset(
                234,
                ValueUtil.randomString(),
                null,
                Dataset.FilterType.NONE,
                false,
                null,
                false,
                true,
                Collections.emptyList());
        final IllegalStateException exception = expectThrows(
                IllegalStateException.class,
                () -> new SystemSchema(ValueUtil.randomInt(), "X", new Dataset[] {dataset1}, new EntityType[] {}));
        assertEquals(
                exception.getMessage(),
                "Replicant-0056: SystemSchema named 'X' passed an array of Datasets where Dataset at index 0 does not "
                        + "have id matching index.");
    }

    @Test
    public void getNameWhenNamesDisabled() {
        ReplicantTestUtil.disableNames();
        final SystemSchema systemSchema =
                new SystemSchema(ValueUtil.randomInt(), null, new Dataset[0], new EntityType[0]);
        final IllegalStateException exception = expectThrows(IllegalStateException.class, systemSchema::getName);
        assertEquals(
                exception.getMessage(),
                "Replicant-0052: SystemSchema.getName() invoked when Replicant.areNamesEnabled() is false");
    }

    @Test
    public void toStringWhenNamesDisabled() {
        ReplicantTestUtil.disableNames();
        final SystemSchema systemSchema =
                new SystemSchema(ValueUtil.randomInt(), null, new Dataset[0], new EntityType[0]);
        assertEquals(systemSchema.toString(), "replicant.SystemSchema@" + Integer.toHexString(systemSchema.hashCode()));
    }

    @Test
    public void passNameToConstructorWhenNamesDisabled() {
        ReplicantTestUtil.disableNames();
        final IllegalStateException exception = expectThrows(
                IllegalStateException.class,
                () -> new SystemSchema(ValueUtil.randomInt(), "MySystem", new Dataset[0], new EntityType[0]));
        assertEquals(
                exception.getMessage(),
                "Replicant-0051: SystemSchema passed a name 'MySystem' but Replicant.areNamesEnabled() is false");
    }
}
