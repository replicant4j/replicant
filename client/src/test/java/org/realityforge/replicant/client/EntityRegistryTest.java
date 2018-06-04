package org.realityforge.replicant.client;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import org.testng.annotations.Test;
import replicant.AbstractReplicantTest;
import static org.testng.Assert.*;

public class EntityRegistryTest
  extends AbstractReplicantTest
{
  @Test
  public void basicOperation()
  {
    final EntityRegistry registry = new EntityRegistry();

    final A a = new A();

    final AtomicInteger callCount = new AtomicInteger();
    final BiConsumer<Object, Object> function = ( id, entity ) -> {
      assertEquals( id, 23 );
      assertEquals( entity, a );
      callCount.incrementAndGet();
    };
    registry.bind( A.class, function );

    assertEquals( callCount.get(), 0 );

    registry.registerEntity( A.class, 23, a );

    assertEquals( callCount.get(), 1 );
  }

  @Test
  public void register_noFunction()
  {
    final EntityRegistry registry = new EntityRegistry();

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class, () -> registry.registerEntity( A.class, 23, new A() ) );
    assertEquals( exception.getMessage(),
                  "Replicant-0075: Attempting to register entity of type class org.realityforge.replicant.client.EntityRegistryTest$A with id '23' but no register function exists for type." );
  }

  @Test
  public void bind_duplicate()
  {
    final EntityRegistry registry = new EntityRegistry();

    final BiConsumer<Object, Object> registerFunction = ( id, entity ) -> {
    };
    registry.bind( A.class, registerFunction );

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class, () -> registry.bind( A.class, registerFunction ) );
    assertEquals( exception.getMessage(),
                  "Replicant-0018: Attempting to bind register function for type class org.realityforge.replicant.client.EntityRegistryTest$A when a function already exists for type." );
  }

  static class A
  {
  }

  private static class EntityRegistry
    extends AbstractEntityRegistry
  {
  }
}
