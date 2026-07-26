package replicant;

import static org.testng.Assert.*;

import org.testng.annotations.Test;

public class EntityTypeTest extends AbstractReplicantTest {
    @Test
    public void construct() {
        final EntityType.Creator<Object> creator = (i, d) -> 1;
        final EntityType.Updater<Object> updater = (o, d) -> d.notify();
        final EntityType entityType = new EntityType(1, "MyObject", Object.class, creator, updater, new DatasetLink[0]);
        assertEquals(entityType.getId(), 1);
        assertEquals(entityType.getName(), "MyObject");
        assertEquals(entityType.getType(), Object.class);
        assertEquals(entityType.getCreator(), creator);
        assertEquals(entityType.getUpdater(), updater);
        assertEquals(entityType.toString(), "MyObject");
    }

    @Test
    public void getNameWhenNamesDisabled() {
        ReplicantTestUtil.disableNames();
        final EntityType entityType =
                new EntityType(ValueUtil.randomInt(), null, Object.class, (i, d) -> 1, null, new DatasetLink[0]);
        final IllegalStateException exception = expectThrows(IllegalStateException.class, entityType::getName);
        assertEquals(
                exception.getMessage(),
                "Replicant-0050: EntityType.getName() invoked when Replicant.areNamesEnabled() is false");
    }

    @Test
    public void toStringWhenNamesDisabled() {
        ReplicantTestUtil.disableNames();
        final EntityType entityType =
                new EntityType(ValueUtil.randomInt(), null, Object.class, (i, d) -> 1, null, new DatasetLink[0]);
        assertEquals(entityType.toString(), "replicant.EntityType@" + Integer.toHexString(entityType.hashCode()));
    }

    @Test
    public void passNameToConstructorWhenNamesDisabled() {
        ReplicantTestUtil.disableNames();
        final IllegalStateException exception = expectThrows(
                IllegalStateException.class,
                () -> new EntityType(
                        ValueUtil.randomInt(), "MyEntity", Object.class, (i, d) -> 1, null, new DatasetLink[0]));
        assertEquals(
                exception.getMessage(),
                "Replicant-0049: EntityType passed a name 'MyEntity' but Replicant.areNamesEnabled() is false");
    }
}
