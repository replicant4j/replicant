package replicant;

import arez.Disposable;
import java.util.concurrent.atomic.AtomicInteger;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class EntityServiceTest
  extends AbstractReplicantTest
{
  @Test
  public void basicEntityLifecycle()
  {
    final var service = Replicant.context().getEntityService();

    final var findAllEntityTypesCallCount = new AtomicInteger();
    observer( () -> {
      if ( Disposable.isNotDisposed( service ) )
      {
        // Access observable next line
        service.findAllEntityTypes();
      }

      findAllEntityTypesCallCount.incrementAndGet();
    } );

    final var findAllEntitiesByTypeACallCount = new AtomicInteger();
    observer( () -> {
      if ( Disposable.isNotDisposed( service ) )
      {
        // Access observable next line
        service.findAllEntitiesByType( A.class );
      }

      findAllEntitiesByTypeACallCount.incrementAndGet();
    } );

    final var findAllEntitiesByTypeBCallCount = new AtomicInteger();
    observer( () -> {
      if ( Disposable.isNotDisposed( service ) )
      {
        // Access observable next line
        service.findAllEntitiesByType( B.class );
      }

      findAllEntitiesByTypeBCallCount.incrementAndGet();
    } );

    final var findEntityByTypeAndId1CallCount = new AtomicInteger();
    observer( () -> {
      if ( Disposable.isNotDisposed( service ) )
      {
        // Access observable next line
        service.findEntityByTypeAndId( A.class, 1 );
      }

      findEntityByTypeAndId1CallCount.incrementAndGet();
    } );

    final var findEntityByTypeAndId2CallCount = new AtomicInteger();
    observer( () -> {
      if ( Disposable.isNotDisposed( service ) )
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
        final var entity = service.findEntityByTypeAndId( B.class, -53 );
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
        final var entity = service.findEntityByTypeAndId( A.class, 1 );
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
    final var service = Replicant.context().getEntityService();

    final var entity = Entity.create( null, "A", A.class, 1 );

    final var exception =
      expectThrows( IllegalStateException.class,
                    () -> safeAction( () -> service.unlinkEntity( entity ) ) );

    assertEquals( exception.getMessage(), "Entity type A not present in EntityService" );
  }

  @Test
  public void unlinkEntity_missingInstance()
  {
    final var service = Replicant.context().getEntityService();

    safeAction( () -> service.findOrCreateEntity( "A/1", A.class, 1 ) );
    final var entity = Entity.create( null, "A/2", A.class, 2 );

    final var exception =
      expectThrows( IllegalStateException.class,
                    () -> safeAction( () -> service.unlinkEntity( entity ) ) );

    assertEquals( exception.getMessage(), "Entity instance A/2 not present in EntityService" );
  }

  @Test
  public void disposedEntityNeverReturned()
  {
    final var service = Replicant.context().getEntityService();

    final var findAllEntityTypesCallCount = new AtomicInteger();
    observer( () -> {
      if ( Disposable.isNotDisposed( service ) )
      {
        // Access observable next line
        service.findAllEntityTypes();
      }

      findAllEntityTypesCallCount.incrementAndGet();
    } );

    final var findAllEntitiesByTypeACallCount = new AtomicInteger();
    observer( () -> {
      if ( Disposable.isNotDisposed( service ) )
      {
        // Access observable next line
        service.findAllEntitiesByType( A.class );
      }

      findAllEntitiesByTypeACallCount.incrementAndGet();
    } );

    final var findEntityByTypeAndId1CallCount = new AtomicInteger();
    observer( () -> {
      if ( Disposable.isNotDisposed( service ) )
      {
        // Access observable next line
        service.findEntityByTypeAndId( A.class, 1 );
      }

      findEntityByTypeAndId1CallCount.incrementAndGet();
    } );

    assertEquals( findAllEntityTypesCallCount.get(), 1 );
    assertEquals( findAllEntitiesByTypeACallCount.get(), 1 );
    assertEquals( findEntityByTypeAndId1CallCount.get(), 1 );

    safeAction( () -> assertEquals( service.findAllEntityTypes().size(), 0 ) );
    safeAction( () -> assertEquals( service.findAllEntitiesByType( A.class ).size(), 0 ) );
    safeAction( () -> assertNull( service.findEntityByTypeAndId( A.class, 1 ) ) );

    {
      safeAction( () -> service.findOrCreateEntity( "A/1", A.class, 1 ) );

      assertEquals( findAllEntityTypesCallCount.get(), 2 );
      assertEquals( findAllEntitiesByTypeACallCount.get(), 2 );
      assertEquals( findEntityByTypeAndId1CallCount.get(), 2 );

      safeAction( () -> assertEquals( service.findAllEntityTypes().size(), 1 ) );
      safeAction( () -> assertEquals( service.findAllEntitiesByType( A.class ).size(), 1 ) );
      safeAction( () -> assertNotNull( service.findEntityByTypeAndId( A.class, 1 ) ) );
      safeAction( () -> assertEquals( service.getEntities().get( A.class ).size(), 1 ) );
    }

    // Dispose entity
    {
      final var schedulerLock = pauseScheduler();
      safeAction( () -> {
        final var entity = service.findEntityByTypeAndId( A.class, 1 );
        assertNotNull( entity );
        Disposable.dispose( entity );
      } );

      assertEquals( findAllEntityTypesCallCount.get(), 2 );
      assertEquals( findAllEntitiesByTypeACallCount.get(), 2 );
      assertEquals( findEntityByTypeAndId1CallCount.get(), 2 );

      safeAction( () -> assertNull( service.getEntities().get( A.class ) ) );
      // Oddity - we have a type with 0 members. Can happen during deletion
      safeAction( () -> assertEquals( service.findAllEntityTypes().size(), 0 ) );
      safeAction( () -> assertEquals( service.findAllEntitiesByType( A.class ).size(), 0 ) );
      safeAction( () -> assertNull( service.findEntityByTypeAndId( A.class, 1 ) ) );

      schedulerLock.dispose();

      assertEquals( findAllEntityTypesCallCount.get(), 3 );
      assertEquals( findAllEntitiesByTypeACallCount.get(), 3 );
      assertEquals( findEntityByTypeAndId1CallCount.get(), 3 );

      safeAction( () -> assertEquals( service.getEntities().size(), 0 ) );
      safeAction( () -> assertEquals( service.findAllEntityTypes().size(), 0 ) );
      safeAction( () -> assertEquals( service.findAllEntitiesByType( A.class ).size(), 0 ) );
      safeAction( () -> assertNull( service.findEntityByTypeAndId( A.class, 1 ) ) );
    }
  }

  private static class A
  {
  }

  private static class B
  {
  }
}
