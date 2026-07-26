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
                new EntityType(0, ValueUtil.randomString(), Integer.class, (i, d) -> 1, null, new ChannelLinkSchema[0]);
        final EntityType entityType2 =
                new EntityType(1, ValueUtil.randomString(), String.class, (i, d) -> "", null, new ChannelLinkSchema[0]);
        final EntityType[] entityTypes = new EntityType[] {entityType1, entityType2};
        final ChannelSchema channel1 = new ChannelSchema(
                1,
                ValueUtil.randomString(),
                null,
                ChannelSchema.FilterType.NONE,
                false,
                null,
                false,
                true,
                Collections.emptyList());
        final ChannelSchema[] channels = {null, channel1};
        final SystemSchema systemSchema = new SystemSchema(id, name, channels, entityTypes);
        assertEquals(systemSchema.getId(), id);
        assertEquals(systemSchema.getName(), name);
        assertEquals(systemSchema.getEntityTypeCount(), 2);
        assertEquals(systemSchema.getEntityType(0), entityType1);
        assertEquals(systemSchema.getEntityType(1), entityType2);
        assertEquals(systemSchema.getChannelCount(), 2);
        assertEquals(systemSchema.getChannel(1), channel1);
        assertEquals(systemSchema.toString(), name);
    }

    @Test
    public void getChannel_BadIndex() {
        final SystemSchema systemSchema = new SystemSchema(
                ValueUtil.randomInt(), ValueUtil.randomString(), new ChannelSchema[] {}, new EntityType[] {});
        final IllegalStateException exception =
                expectThrows(IllegalStateException.class, () -> systemSchema.getChannel(2));
        assertEquals(
                exception.getMessage(),
                "Replicant-0058: SystemSchema.getChannel(id) passed an id that is out of range.");
    }

    @Test
    public void getEntityType_BadIndex() {
        final SystemSchema systemSchema = new SystemSchema(
                ValueUtil.randomInt(), ValueUtil.randomString(), new ChannelSchema[] {}, new EntityType[] {});
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
                () -> new SystemSchema(ValueUtil.randomInt(), "X", new ChannelSchema[] {}, new EntityType[] {null}));
        assertEquals(
                exception.getMessage(),
                "Replicant-0053: SystemSchema named 'X' passed an array of entity types that has a null element");
    }

    @Test
    public void construct_badEntityTypeIndex() {
        final EntityType entityType = new EntityType(
                23, ValueUtil.randomString(), Integer.class, (i, d) -> 1, null, new ChannelLinkSchema[0]);
        final IllegalStateException exception = expectThrows(
                IllegalStateException.class,
                () -> new SystemSchema(
                        ValueUtil.randomInt(), "X", new ChannelSchema[] {}, new EntityType[] {entityType}));
        assertEquals(
                exception.getMessage(),
                "Replicant-0054: SystemSchema named 'X' passed an array of entity types where entity type at index 0"
                        + " does not have id matching index.");
    }

    @Test
    public void construct_badChannelIndex() {
        final ChannelSchema channel1 = new ChannelSchema(
                234,
                ValueUtil.randomString(),
                null,
                ChannelSchema.FilterType.NONE,
                false,
                null,
                false,
                true,
                Collections.emptyList());
        final IllegalStateException exception = expectThrows(
                IllegalStateException.class,
                () -> new SystemSchema(
                        ValueUtil.randomInt(), "X", new ChannelSchema[] {channel1}, new EntityType[] {}));
        assertEquals(
                exception.getMessage(),
                "Replicant-0056: SystemSchema named 'X' passed an array of channels where channel at index 0 does not"
                        + " have id matching index.");
    }

    @Test
    public void getNameWhenNamesDisabled() {
        ReplicantTestUtil.disableNames();
        final SystemSchema systemSchema =
                new SystemSchema(ValueUtil.randomInt(), null, new ChannelSchema[0], new EntityType[0]);
        final IllegalStateException exception = expectThrows(IllegalStateException.class, systemSchema::getName);
        assertEquals(
                exception.getMessage(),
                "Replicant-0052: SystemSchema.getName() invoked when Replicant.areNamesEnabled() is false");
    }

    @Test
    public void toStringWhenNamesDisabled() {
        ReplicantTestUtil.disableNames();
        final SystemSchema systemSchema =
                new SystemSchema(ValueUtil.randomInt(), null, new ChannelSchema[0], new EntityType[0]);
        assertEquals(systemSchema.toString(), "replicant.SystemSchema@" + Integer.toHexString(systemSchema.hashCode()));
    }

    @Test
    public void passNameToConstructorWhenNamesDisabled() {
        ReplicantTestUtil.disableNames();
        final IllegalStateException exception = expectThrows(
                IllegalStateException.class,
                () -> new SystemSchema(ValueUtil.randomInt(), "MySystem", new ChannelSchema[0], new EntityType[0]));
        assertEquals(
                exception.getMessage(),
                "Replicant-0051: SystemSchema passed a name 'MySystem' but Replicant.areNamesEnabled() is false");
    }
}
