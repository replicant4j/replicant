package org.realityforge.replicant.client;

import arez.component.NoSuchEntityException;
import java.util.HashMap;
import org.testng.annotations.Test;
import replicant.AbstractReplicantTest;
import static org.testng.Assert.*;

@SuppressWarnings( "SuspiciousMethodCalls" )
public class EntityLocatorTest
  extends AbstractReplicantTest
{
  @Test
  public void basicOperation()
  {
    final EntityLocator locator = new EntityLocator();

    {
      assertNull( locator.findById( A.class, 23 ) );
      final NoSuchEntityException exception =
        expectThrows( NoSuchEntityException.class, () -> locator.getById( A.class, 23 ) );
      assertEquals( exception.getId(), 23 );
    }

    final HashMap<Integer, A> entities = new HashMap<>();

    locator.registerLookup( A.class, entities::get );

    {
      assertNull( locator.findById( A.class, 23 ) );
      final NoSuchEntityException exception =
        expectThrows( NoSuchEntityException.class, () -> locator.getById( A.class, 23 ) );
      assertEquals( exception.getId(), 23 );
    }

    final A entity = new A();
    entities.put( 23, entity );

    assertEquals( locator.findById( A.class, 23 ), entity );
    assertEquals( locator.getById( A.class, 23 ), entity );
  }

  @Test
  public void registerLookup_duplicate()
  {
    final EntityLocator locator = new EntityLocator();

    locator.registerLookup( A.class, i -> new A() );

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class, () -> locator.registerLookup( A.class, i -> new A() ) );
    assertEquals( exception.getMessage(),
                  "Replicant-0086: Attempting to register findById function for type class org.realityforge.replicant.client.EntityLocatorTest$A when a function already exists." );
  }

  static class A
  {
  }

  private static class EntityLocator
    extends AbstractEntityLocator
  {
  }
}
