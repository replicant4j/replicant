package replicant;

import org.realityforge.guiceyloops.shared.ValueUtil;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class SchemaServiceTest
  extends AbstractReplicantTest
{
  @Test
  public void basicWorkflow()
  {
    final SchemaService service = SchemaService.create();

    final int schemaId1 = ValueUtil.randomInt();
    final SystemSchema schema1 =
      new SystemSchema( schemaId1, ValueUtil.randomString(), new ChannelSchema[ 0 ], new EntitySchema[ 0 ] );

    assertNull( service.findById( schemaId1 ) );
    assertEquals( service.getSchemas().size(), 0 );
    assertEquals( service.getSchemas().contains( schema1 ), false );

    service.registerSchema( schema1 );

    assertEquals( service.findById( schemaId1 ), schema1 );
    assertEquals( service.getById( schemaId1 ), schema1 );
    assertEquals( service.getSchemas().size(), 1 );
    assertEquals( service.getSchemas().contains( schema1 ), true );

    service.deregisterSchema( schema1 );

    assertNull( service.findById( schemaId1 ) );
    assertEquals( service.getSchemas().size(), 0 );
    assertEquals( service.getSchemas().contains( schema1 ), false );
  }

  @Test
  public void registerSchema_duplicate()
  {
    final SchemaService service = SchemaService.create();

    final int schemaId1 = 100;
    final SystemSchema schema1 =
      new SystemSchema( schemaId1, "MySchema1", new ChannelSchema[ 0 ], new EntitySchema[ 0 ] );
    final SystemSchema schema2 =
      new SystemSchema( schemaId1, ValueUtil.randomString(), new ChannelSchema[ 0 ], new EntitySchema[ 0 ] );

    service.registerSchema( schema1 );

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class, () -> service.registerSchema( schema2 ) );
    assertEquals( exception.getMessage(),
                  "Replicant-0070: Attempted to register schema with id 100 when a schema with id already exists: MySchema1" );
  }

  @Test
  public void deregisterSchema_missing()
  {
    final SchemaService service = SchemaService.create();

    final int schemaId1 = 100;
    final SystemSchema schema2 =
      new SystemSchema( schemaId1, ValueUtil.randomString(), new ChannelSchema[ 0 ], new EntitySchema[ 0 ] );

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class, () -> service.deregisterSchema( schema2 ) );
    assertEquals( exception.getMessage(),
                  "Replicant-0085: Attempted to deregister schema with id 100 but no such schema exists." );
  }

  @Test
  public void getByIdWhenNonePresent()
  {
    final SchemaService service = SchemaService.create();

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class, () -> service.getById( 23 ) );
    assertEquals( exception.getMessage(), "Replicant-0059: Unable to locate SystemSchema with id 23" );
  }
}
