package replicant;

import static org.testng.Assert.*;

import org.testng.annotations.Test;

public class SystemSchemaServiceTest extends AbstractReplicantTest {
    @Test
    public void basicWorkflow() {
        final SystemSchemaService service = SystemSchemaService.create();

        final int systemSchemaId1 = ValueUtil.randomInt();
        final SystemSchema systemSchema1 =
                new SystemSchema(systemSchemaId1, ValueUtil.randomString(), new Dataset[0], new EntityType[0]);

        assertNull(service.findById(systemSchemaId1));
        assertEquals(service.getSystemSchemas().size(), 0);
        assertFalse(service.getSystemSchemas().contains(systemSchema1));

        service.registerSystemSchema(systemSchema1);

        assertEquals(service.findById(systemSchemaId1), systemSchema1);
        assertEquals(service.getById(systemSchemaId1), systemSchema1);
        assertEquals(service.getSystemSchemas().size(), 1);
        assertTrue(service.getSystemSchemas().contains(systemSchema1));

        service.deregisterSystemSchema(systemSchema1);

        assertNull(service.findById(systemSchemaId1));
        assertEquals(service.getSystemSchemas().size(), 0);
        assertFalse(service.getSystemSchemas().contains(systemSchema1));
    }

    @Test
    public void registerSystemSchema_duplicate() {
        final SystemSchemaService service = SystemSchemaService.create();

        final int systemSchemaId1 = 100;
        final SystemSchema systemSchema1 =
                new SystemSchema(systemSchemaId1, "MySystemSchema1", new Dataset[0], new EntityType[0]);
        final SystemSchema systemSchema2 =
                new SystemSchema(systemSchemaId1, ValueUtil.randomString(), new Dataset[0], new EntityType[0]);

        service.registerSystemSchema(systemSchema1);

        final IllegalStateException exception =
                expectThrows(IllegalStateException.class, () -> service.registerSystemSchema(systemSchema2));
        assertEquals(
                exception.getMessage(),
                "Replicant-0070: Attempted to register System Schema for System Schema ID 100 when that ID is already"
                        + " registered: MySystemSchema1");
    }

    @Test
    public void deregisterSystemSchema_missing() {
        final SystemSchemaService service = SystemSchemaService.create();

        final int systemSchemaId1 = 100;
        final SystemSchema systemSchema =
                new SystemSchema(systemSchemaId1, ValueUtil.randomString(), new Dataset[0], new EntityType[0]);

        final IllegalStateException exception =
                expectThrows(IllegalStateException.class, () -> service.deregisterSystemSchema(systemSchema));
        assertEquals(
                exception.getMessage(),
                "Replicant-0085: Attempted to deregister System Schema for System Schema ID 100 but no such System"
                        + " Schema exists.");
    }

    @Test
    public void getByIdWhenNonePresent() {
        final SystemSchemaService service = SystemSchemaService.create();

        final IllegalStateException exception = expectThrows(IllegalStateException.class, () -> service.getById(23));
        assertEquals(exception.getMessage(), "Replicant-0059: Unable to locate System Schema for System Schema ID 23");
    }
}
