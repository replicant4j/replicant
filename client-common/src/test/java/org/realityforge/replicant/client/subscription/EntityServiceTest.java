package org.realityforge.replicant.client.subscription;

import arez.Arez;
import arez.Disposable;
import java.util.concurrent.atomic.AtomicInteger;
import org.realityforge.replicant.client.AbstractReplicantTest;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class EntityServiceTest
  extends AbstractReplicantTest
{
  @Test
  public void basicEntityLifecycle()
  {
    final EntityService service = EntityService.create();

    final AtomicInteger findAllEntityTypesCallCount = new AtomicInteger();
    Arez.context().autorun( () -> {
      if ( !Disposable.isDisposed( service ) )
      {
        // Access observable next line
        service.findAllEntityTypes();
      }

      findAllEntityTypesCallCount.incrementAndGet();
    } );

    final AtomicInteger findAllEntitiesByTypeACallCount = new AtomicInteger();
    Arez.context().autorun( () -> {
      if ( !Disposable.isDisposed( service ) )
      {
        // Access observable next line
        service.findAllEntitiesByType( A.class );
      }

      findAllEntitiesByTypeACallCount.incrementAndGet();
    } );

    final AtomicInteger findAllEntitiesByTypeBCallCount = new AtomicInteger();
    Arez.context().autorun( () -> {
      if ( !Disposable.isDisposed( service ) )
      {
        // Access observable next line
        service.findAllEntitiesByType( B.class );
      }

      findAllEntitiesByTypeBCallCount.incrementAndGet();
    } );

    final AtomicInteger findEntityByTypeAndId1CallCount = new AtomicInteger();
    Arez.context().autorun( () -> {
      if ( !Disposable.isDisposed( service ) )
      {
        // Access observable next line
        service.findEntityByTypeAndId( A.class, 1 );
      }

      findEntityByTypeAndId1CallCount.incrementAndGet();
    } );

    final AtomicInteger findEntityByTypeAndId2CallCount = new AtomicInteger();
    Arez.context().autorun( () -> {
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

    Arez.context().safeAction( () -> assertEquals( service.findAllEntityTypes().size(), 0 ) );
    Arez.context().safeAction( () -> assertEquals( service.findAllEntitiesByType( A.class ).size(), 0 ) );
    Arez.context().safeAction( () -> assertEquals( service.findAllEntitiesByType( B.class ).size(), 0 ) );
    Arez.context().safeAction( () -> assertNull( service.findEntityByTypeAndId( A.class, 1 ) ) );
    Arez.context().safeAction( () -> assertNull( service.findEntityByTypeAndId( A.class, 2 ) ) );

    // add first entity
    {
      Arez.context().safeAction( () -> service.findOrCreateEntity( A.class, 1 ) );

      assertEquals( findAllEntityTypesCallCount.get(), 2 );
      assertEquals( findAllEntitiesByTypeACallCount.get(), 2 );
      assertEquals( findAllEntitiesByTypeBCallCount.get(), 2 );
      assertEquals( findEntityByTypeAndId1CallCount.get(), 2 );
      assertEquals( findEntityByTypeAndId2CallCount.get(), 2 );

      Arez.context().safeAction( () -> assertEquals( service.findAllEntityTypes().size(), 1 ) );
      Arez.context().safeAction( () -> assertEquals( service.findAllEntitiesByType( A.class ).size(), 1 ) );
      Arez.context().safeAction( () -> assertEquals( service.findAllEntitiesByType( B.class ).size(), 0 ) );
      Arez.context().safeAction( () -> assertNotNull( service.findEntityByTypeAndId( A.class, 1 ) ) );
      Arez.context().safeAction( () -> assertNull( service.findEntityByTypeAndId( A.class, 2 ) ) );
    }

    // Attempt to add same entity
    {
      Arez.context().safeAction( () -> service.findOrCreateEntity( A.class, 1 ) );

      assertEquals( findAllEntityTypesCallCount.get(), 2 );
      assertEquals( findAllEntitiesByTypeACallCount.get(), 2 );
      assertEquals( findAllEntitiesByTypeBCallCount.get(), 2 );
      assertEquals( findEntityByTypeAndId1CallCount.get(), 2 );
      assertEquals( findEntityByTypeAndId2CallCount.get(), 2 );

      Arez.context().safeAction( () -> assertEquals( service.findAllEntityTypes().size(), 1 ) );
      Arez.context().safeAction( () -> assertEquals( service.findAllEntitiesByType( A.class ).size(), 1 ) );
      Arez.context().safeAction( () -> assertEquals( service.findAllEntitiesByType( B.class ).size(), 0 ) );
      Arez.context().safeAction( () -> assertNotNull( service.findEntityByTypeAndId( A.class, 1 ) ) );
      Arez.context().safeAction( () -> assertNull( service.findEntityByTypeAndId( A.class, 2 ) ) );
    }

    // add an entity of the same type
    {
      Arez.context().safeAction( () -> service.findOrCreateEntity( A.class, 2 ) );

      assertEquals( findAllEntityTypesCallCount.get(), 3 );
      assertEquals( findAllEntitiesByTypeACallCount.get(), 3 );
      assertEquals( findAllEntitiesByTypeBCallCount.get(), 3 );
      assertEquals( findEntityByTypeAndId1CallCount.get(), 2 );
      assertEquals( findEntityByTypeAndId2CallCount.get(), 3 );

      Arez.context().safeAction( () -> assertEquals( service.findAllEntityTypes().size(), 1 ) );
      Arez.context().safeAction( () -> assertEquals( service.findAllEntitiesByType( A.class ).size(), 2 ) );
      Arez.context().safeAction( () -> assertEquals( service.findAllEntitiesByType( B.class ).size(), 0 ) );
      Arez.context().safeAction( () -> assertNotNull( service.findEntityByTypeAndId( A.class, 1 ) ) );
      Arez.context().safeAction( () -> assertNotNull( service.findEntityByTypeAndId( A.class, 2 ) ) );
    }

    // Add an entity of a different type
    {
      Arez.context().safeAction( () -> service.findOrCreateEntity( B.class, "X" ) );

      assertEquals( findAllEntityTypesCallCount.get(), 4 );
      assertEquals( findAllEntitiesByTypeACallCount.get(), 4 );
      assertEquals( findAllEntitiesByTypeBCallCount.get(), 4 );
      assertEquals( findEntityByTypeAndId1CallCount.get(), 2 );
      assertEquals( findEntityByTypeAndId2CallCount.get(), 3 );

      Arez.context().safeAction( () -> assertEquals( service.findAllEntityTypes().size(), 2 ) );
      Arez.context().safeAction( () -> assertEquals( service.findAllEntitiesByType( A.class ).size(), 2 ) );
      Arez.context().safeAction( () -> assertEquals( service.findAllEntitiesByType( B.class ).size(), 1 ) );
      Arez.context().safeAction( () -> assertNotNull( service.findEntityByTypeAndId( A.class, 1 ) ) );
      Arez.context().safeAction( () -> assertNotNull( service.findEntityByTypeAndId( A.class, 2 ) ) );
    }

    // Dispose entity of different type
    {
      Arez.context().safeAction( () -> {
        final Entity entity = service.findEntityByTypeAndId( B.class, "X" );
        assertNotNull( entity );
        Disposable.dispose( entity );
      } );

      assertEquals( findAllEntityTypesCallCount.get(), 5 );
      assertEquals( findAllEntitiesByTypeACallCount.get(), 5 );
      assertEquals( findAllEntitiesByTypeBCallCount.get(), 5 );
      assertEquals( findEntityByTypeAndId1CallCount.get(), 2 );
      assertEquals( findEntityByTypeAndId2CallCount.get(), 3 );

      Arez.context().safeAction( () -> assertEquals( service.findAllEntityTypes().size(), 1 ) );
      Arez.context().safeAction( () -> assertEquals( service.findAllEntitiesByType( A.class ).size(), 2 ) );
      Arez.context().safeAction( () -> assertEquals( service.findAllEntitiesByType( B.class ).size(), 0 ) );
      Arez.context().safeAction( () -> assertNotNull( service.findEntityByTypeAndId( A.class, 1 ) ) );
      Arez.context().safeAction( () -> assertNotNull( service.findEntityByTypeAndId( A.class, 2 ) ) );
    }

    // Dispose entity of A type
    {
      Arez.context().safeAction( () -> {
        final Entity entity = service.findEntityByTypeAndId( A.class, 1 );
        assertNotNull( entity );
        Disposable.dispose( entity );
      } );

      assertEquals( findAllEntityTypesCallCount.get(), 6 );
      assertEquals( findAllEntitiesByTypeACallCount.get(), 6 );
      assertEquals( findAllEntitiesByTypeBCallCount.get(), 6 );
      assertEquals( findEntityByTypeAndId1CallCount.get(), 3 );
      assertEquals( findEntityByTypeAndId2CallCount.get(), 3 );

      Arez.context().safeAction( () -> assertEquals( service.findAllEntityTypes().size(), 1 ) );
      Arez.context().safeAction( () -> assertEquals( service.findAllEntitiesByType( A.class ).size(), 1 ) );
      Arez.context().safeAction( () -> assertEquals( service.findAllEntitiesByType( B.class ).size(), 0 ) );
      Arez.context().safeAction( () -> assertNull( service.findEntityByTypeAndId( A.class, 1 ) ) );
      Arez.context().safeAction( () -> assertNotNull( service.findEntityByTypeAndId( A.class, 2 ) ) );
    }
  }

  static class A
  {
  }

  static class B
  {
  }
}
