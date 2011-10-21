package org.realityforge.imit.client;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class EntityRepositoryTest
{
  @Test
  public void registerAndDeregisterHasExpectedEffects()
  {
    final EntityRepository r = new EntityRepositoryImpl();
    final Class<B> parentType = B.class;
    final Class<A> type = A.class;
    final String id = "A";
    final A entity = new A();

    // If I register an ID it should be accessible in all weird and wonderful ways
    r.registerEntity( type, id, entity );

    assertPresent( r, type, id, entity );
    assertPresent( r, parentType, id, entity );
    assertNotPresent( r, type, "X" );
    assertNotPresent( r, parentType, "X" );

    r.deregisterEntity( type, id );

    assertNotPresent( r, type, id );
    assertNotPresent( r, parentType, id );
    assertNotPresentInSet( r, type, entity );
  }

  @Test
  public void duplicateRegisterRaisesException()
  {
    final EntityRepository r = new EntityRepositoryImpl();
    final Class<A> type = A.class;
    final String id = "A";
    final A entity = new A();

    r.registerEntity( type, id, entity );

    try
    {
      r.registerEntity( type, id, entity );
    }
    catch ( Exception e )
    {
      return;
    }
    fail( "Expected to raise an exception when re-registering an entity" );
  }

  @Test
  public void deregisterNonExistentRaisesException()
  {
    final EntityRepository r = new EntityRepositoryImpl();
    final Class<A> type = A.class;
    final String id = "A";

    try
    {
      r.deregisterEntity( type, id );
    }
    catch ( Exception e )
    {
      return;
    }
    fail( "Expected to raise an exception when de-registering an entity not present" );
  }

  private void assertPresent( final EntityRepository r, final Class<?> type, final Object id, final Object entity )
  {
    assertCanFind( r, type, id, entity );
    assertCanGet( r, type, id, entity );
    assertTrue( r.findAll( type ).contains( entity ) );
  }

  private void assertNotPresent( final EntityRepository r, final Class<?> type, final Object id )
  {
    assertCanNotFind( r, type, id );
    assertCanNotGet( r, type, id );
  }

  private void assertNotPresentInSet( final EntityRepository r, final Class<?> type, final Object entity )
  {
    assertFalse( r.findAll( type ).contains( entity ) );
  }

  private void assertCanFind( final EntityRepository r, final Class<?> type, final Object id, final Object entity )
  {
    assertEquals( entity, r.findByID( type, id ) );
  }

  private void assertCanGet( final EntityRepository r, final Class<?> type, final Object id, final Object entity )
  {
    assertEquals( entity, r.getByID( type, id ) );
  }

  private void assertCanNotFind( final EntityRepository r, final Class<?> type, final Object id )
  {
    assertNull( r.findByID( type, id ) );
  }

  private void assertCanNotGet( final EntityRepository r, final Class<?> type, final Object id )
  {
    boolean failed;
    try
    {
      r.getByID( type, id );
      failed = false;
    }
    catch ( final Exception e )
    {
      failed = true;
    }
    if ( !failed )
    {
      fail( "Expected getByID to fail with exception for bad ID" );
    }
  }


  static class B
  {
  }

  static class A
    extends B
  {
  }
}
