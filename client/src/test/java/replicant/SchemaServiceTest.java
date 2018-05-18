package replicant;

import arez.Arez;
import java.util.concurrent.atomic.AtomicInteger;
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

    final AtomicInteger findById1CallCount = new AtomicInteger();
    Arez.context().autorun( () -> {
      service.findById( schemaId1 );
      findById1CallCount.incrementAndGet();
    } );

    final AtomicInteger contains1CallCount = new AtomicInteger();
    Arez.context().autorun( () -> {
      service.findById( schemaId1 );
      contains1CallCount.incrementAndGet();
    } );

    final AtomicInteger getSchemasCallCount = new AtomicInteger();
    Arez.context().autorun( () -> {
      service.getSchemas();
      getSchemasCallCount.incrementAndGet();
    } );

    assertEquals( findById1CallCount.get(), 1 );
    assertEquals( contains1CallCount.get(), 1 );
    assertEquals( getSchemasCallCount.get(), 1 );

    safeAction( () -> assertNull( service.findById( schemaId1 ) ) );
    safeAction( () -> assertEquals( service.getSchemas().size(), 0 ) );
    safeAction( () -> assertEquals( service.getSchemas().contains( schema1 ), false ) );
    safeAction( () -> assertEquals( service.contains( schema1 ), false ) );

    safeAction( () -> service.registerSchema( schema1 ) );

    assertEquals( findById1CallCount.get(), 2 );
    assertEquals( contains1CallCount.get(), 2 );
    assertEquals( getSchemasCallCount.get(), 2 );

    safeAction( () -> assertEquals( service.findById( schemaId1 ), schema1 ) );
    safeAction( () -> assertEquals( service.getById( schemaId1 ), schema1 ) );
    safeAction( () -> assertEquals( service.getSchemas().size(), 1 ) );
    safeAction( () -> assertEquals( service.getSchemas().contains( schema1 ), true ) );
    safeAction( () -> assertEquals( service.contains( schema1 ), true ) );
  }

  @Test
  public void getByIdWhenNonePresent()
  {
    final SchemaService service = SchemaService.create();

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class, () -> safeAction( () -> service.getById( 23 ) ) );
    assertEquals( exception.getMessage(), "Replicant-0059: Unable to locate SystemSchema with id 23" );
  }
}
