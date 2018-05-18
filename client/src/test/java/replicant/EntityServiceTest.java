package replicant;

import arez.Disposable;
import java.util.concurrent.atomic.AtomicInteger;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

@SuppressWarnings( "Duplicates" )
public class EntityServiceTest
  extends AbstractReplicantTest
{
  @Test
  public void basicEntityLifecycle()
  {
    final EntityService service = Replicant.context().getEntityService();

    final AtomicInteger findAllEntityTypesCallCount = new AtomicInteger();
    autorun( () -> {
      if ( !Disposable.isDisposed( service ) )
      {
        // Access observable next line
        service.findAllEntityTypes();
      }

      findAllEntityTypesCallCount.incrementAndGet();
    } );

    final AtomicInteger findAllEntitiesByTypeACallCount = new AtomicInteger();
    autorun( () -> {
      if ( !Disposable.isDisposed( service ) )
      {
        // Access observable next line
        service.findAllEntitiesByType( A.class );
      }

      findAllEntitiesByTypeACallCount.incrementAndGet();
    } );

    final AtomicInteger findAllEntitiesByTypeBCallCount = new AtomicInteger();
    autorun( () -> {
      if ( !Disposable.isDisposed( service ) )
      {
        // Access observable next line
        service.findAllEntitiesByType( B.class );
      }

      findAllEntitiesByTypeBCallCount.incrementAndGet();
    } );

    final AtomicInteger findEntityByTypeAndId1CallCount = new AtomicInteger();
    autorun( () -> {
      if ( !Disposable.isDisposed( service ) )
      {
        // Access observable next line
        service.findEntityByTypeAndId( A.class, 1 );
      }

      findEntityByTypeAndId1CallCount.incrementAndGet();
    } );

    final AtomicInteger findEntityByTypeAndId2CallCount = new AtomicInteger();
    autorun( () -> {
      if ( !Disposable.isDisposed( service ) )
      {
        // Access observable next line
        service.findEntityByTypeAndId( A.class, 2 );
      }

      findEntityByTypeAndId2CallCount.incrementAndGet();
    } );

    assertEquals( findAllEntityTypesCallCount.get(), 1 );
    assertEquals( findAllEntitiesByTypeACallCount.get(), 1 );
    assertEquals( findAllEntitiesByTypeBCallCount.get(), 1 );
    assertEquals( findEntityByTypeAndId1CallCount.get(), 1 );
    assertEquals( findEntityByTypeAndId2CallCount.get(), 1 );

    safeAction( () -> assertEquals( service.findAllEntityTypes().size(), 0 ) );
    safeAction( () -> assertEquals( service.findAllEntitiesByType( A.class ).size(), 0 ) );
    safeAction( () -> assertEquals( service.findAllEntitiesByType( B.class ).size(), 0 ) );
    safeAction( () -> assertNull( service.findEntityByTypeAndId( A.class, 1 ) ) );
    safeAction( () -> assertNull( service.findEntityByTypeAndId( A.class, 2 ) ) );

    // add first entity
    {
      safeAction( () -> service.findOrCreateEntity( "A/1", A.class, 1 ) );

      assertEquals( findAllEntityTypesCallCount.get(), 2 );
      assertEquals( findAllEntitiesByTypeACallCount.get(), 2 );
      assertEquals( findAllEntitiesByTypeBCallCount.get(), 2 );
      assertEquals( findEntityByTypeAndId1CallCount.get(), 2 );
      assertEquals( findEntityByTypeAndId2CallCount.get(), 2 );

      safeAction( () -> assertEquals( service.findAllEntityTypes().size(), 1 ) );
      safeAction( () -> assertEquals( service.findAllEntitiesByType( A.class ).size(), 1 ) );
      safeAction( () -> assertEquals( service.findAllEntitiesByType( B.class ).size(), 0 ) );
      safeAction( () -> assertNotNull( service.findEntityByTypeAndId( A.class, 1 ) ) );
      safeAction( () -> assertNull( service.findEntityByTypeAndId( A.class, 2 ) ) );
    }

    // Attempt to add same entity
    {
      safeAction( () -> service.findOrCreateEntity( "A/1", A.class, 1 ) );

      assertEquals( findAllEntityTypesCallCount.get(), 2 );
      assertEquals( findAllEntitiesByTypeACallCount.get(), 2 );
      assertEquals( findAllEntitiesByTypeBCallCount.get(), 2 );
      assertEquals( findEntityByTypeAndId1CallCount.get(), 2 );
      assertEquals( findEntityByTypeAndId2CallCount.get(), 2 );

      safeAction( () -> assertEquals( service.findAllEntityTypes().size(), 1 ) );
      safeAction( () -> assertEquals( service.findAllEntitiesByType( A.class ).size(), 1 ) );
      safeAction( () -> assertEquals( service.findAllEntitiesByType( B.class ).size(), 0 ) );
      safeAction( () -> assertNotNull( service.findEntityByTypeAndId( A.class, 1 ) ) );
      safeAction( () -> assertNull( service.findEntityByTypeAndId( A.class, 2 ) ) );
    }

    // add an entity of the same type
    {
      safeAction( () -> service.findOrCreateEntity( "A/2", A.class, 2 ) );

      assertEquals( findAllEntityTypesCallCount.get(), 3 );
      assertEquals( findAllEntitiesByTypeACallCount.get(), 3 );
      assertEquals( findAllEntitiesByTypeBCallCount.get(), 3 );
      assertEquals( findEntityByTypeAndId1CallCount.get(), 2 );
      assertEquals( findEntityByTypeAndId2CallCount.get(), 3 );

      safeAction( () -> assertEquals( service.findAllEntityTypes().size(), 1 ) );
      safeAction( () -> assertEquals( service.findAllEntitiesByType( A.class ).size(), 2 ) );
      safeAction( () -> assertEquals( service.findAllEntitiesByType( B.class ).size(), 0 ) );
      safeAction( () -> assertNotNull( service.findEntityByTypeAndId( A.class, 1 ) ) );
      safeAction( () -> assertNotNull( service.findEntityByTypeAndId( A.class, 2 ) ) );
    }

    // Add an entity of a different type
    {
      safeAction( () -> service.findOrCreateEntity( "B/-53", B.class, -53 ) );

      assertEquals( findAllEntityTypesCallCount.get(), 4 );
      assertEquals( findAllEntitiesByTypeACallCount.get(), 4 );
      assertEquals( findAllEntitiesByTypeBCallCount.get(), 4 );
      assertEquals( findEntityByTypeAndId1CallCount.get(), 2 );
      assertEquals( findEntityByTypeAndId2CallCount.get(), 3 );

      safeAction( () -> assertEquals( service.findAllEntityTypes().size(), 2 ) );
      safeAction( () -> assertEquals( service.findAllEntitiesByType( A.class ).size(), 2 ) );
      safeAction( () -> assertEquals( service.findAllEntitiesByType( B.class ).size(), 1 ) );
      safeAction( () -> assertNotNull( service.findEntityByTypeAndId( A.class, 1 ) ) );
      safeAction( () -> assertNotNull( service.findEntityByTypeAndId( A.class, 2 ) ) );
    }

    // Dispose entity of different type
    {
      safeAction( () -> {
        final Entity entity = service.findEntityByTypeAndId( B.class, -53 );
        assertNotNull( entity );
        Disposable.dispose( entity );
      } );

      assertEquals( findAllEntityTypesCallCount.get(), 5 );
      assertEquals( findAllEntitiesByTypeACallCount.get(), 5 );
      assertEquals( findAllEntitiesByTypeBCallCount.get(), 5 );
      assertEquals( findEntityByTypeAndId1CallCount.get(), 2 );
      assertEquals( findEntityByTypeAndId2CallCount.get(), 3 );

      safeAction( () -> assertEquals( service.findAllEntityTypes().size(), 1 ) );
      safeAction( () -> assertEquals( service.findAllEntitiesByType( A.class ).size(), 2 ) );
      safeAction( () -> assertEquals( service.findAllEntitiesByType( B.class ).size(), 0 ) );
      safeAction( () -> assertNotNull( service.findEntityByTypeAndId( A.class, 1 ) ) );
      safeAction( () -> assertNotNull( service.findEntityByTypeAndId( A.class, 2 ) ) );
    }

    // Dispose entity of A type
    {
      safeAction( () -> {
        final Entity entity = service.findEntityByTypeAndId( A.class, 1 );
        assertNotNull( entity );
        Disposable.dispose( entity );
      } );

      assertEquals( findAllEntityTypesCallCount.get(), 6 );
      assertEquals( findAllEntitiesByTypeACallCount.get(), 6 );
      assertEquals( findAllEntitiesByTypeBCallCount.get(), 6 );
      assertEquals( findEntityByTypeAndId1CallCount.get(), 3 );
      assertEquals( findEntityByTypeAndId2CallCount.get(), 3 );

      safeAction( () -> assertEquals( service.findAllEntityTypes().size(), 1 ) );
      safeAction( () -> assertEquals( service.findAllEntitiesByType( A.class ).size(), 1 ) );
      safeAction( () -> assertEquals( service.findAllEntitiesByType( B.class ).size(), 0 ) );
      safeAction( () -> assertNull( service.findEntityByTypeAndId( A.class, 1 ) ) );
      safeAction( () -> assertNotNull( service.findEntityByTypeAndId( A.class, 2 ) ) );
    }
  }

  @Test
  public void unlinkEntity_missingType()
  {
    final EntityService service = Replicant.context().getEntityService();

    final Entity entity = Entity.create( null, "A", A.class, 1 );

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class,
                    () -> safeAction( () -> service.unlinkEntity( entity ) ) );

    assertEquals( exception.getMessage(), "Entity type A not present in EntityService" );
  }

  @Test
  public void unlinkEntity_missingInstance()
  {
    final EntityService service = Replicant.context().getEntityService();

    safeAction( () -> service.findOrCreateEntity( "A/1", A.class, 1 ) );
    final Entity entity = Entity.create( null, "A/2", A.class, 2 );

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class,
                    () -> safeAction( () -> service.unlinkEntity( entity ) ) );

    assertEquals( exception.getMessage(), "Entity instance A/2 not present in EntityService" );
  }

  private static class A
  {
  }

  private static class B
  {
  }
}
