package org.realityforge.replicant.client;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class EntitySubscriptionManagerTest
{
  @Test
  public void typeGraphRegistrations()
  {
    final EntityRepository r = new EntityRepositoryImpl();
    final EntitySubscriptionManager sm = new EntitySubscriptionManagerImpl( r );
    assertNull( sm.findSubscription( G.G1 ) );

    assertEquals( sm.getTypeSubscriptions().size(), 0 );
    assertFalse( sm.getTypeSubscriptions().contains( G.G1 ) );

    sm.subscribe( G.G1, null );

    assertEquals( sm.getTypeSubscriptions().size(), 1 );
    assertTrue( sm.getTypeSubscriptions().contains( G.G1 ) );

    final GraphSubscriptionEntry s = sm.findSubscription( G.G1 );
    assertNotNull( s );
    assertNotNull( sm.getSubscription( G.G1 ) );
    assertEquals( s.getDescriptor(), new GraphDescriptor( G.G1, null ) );
    assertEquals( s.getEntities().size(), 0 );

    sm.unsubscribe( G.G1 );
    assertNull( sm.findSubscription( G.G1 ) );
  }

  @Test
  public void instanceGraphRegistrations()
  {
    final EntityRepository r = new EntityRepositoryImpl();
    final EntitySubscriptionManager sm = new EntitySubscriptionManagerImpl( r );
    assertNull( sm.findSubscription( G.G2, 1 ) );

    assertEquals( sm.getInstanceSubscriptionKeys().size(), 0 );
    assertEquals( sm.getInstanceSubscriptions( G.G2 ).size(), 0 );

    sm.subscribe( G.G2, 1, null );

    assertEquals( sm.getInstanceSubscriptionKeys().size(), 1 );
    assertEquals( sm.getInstanceSubscriptions( G.G2 ).size(), 1 );

    assertNull( sm.findSubscription( G.G2, 2 ) );
    final GraphSubscriptionEntry s = sm.findSubscription( G.G2, 1 );
    assertNotNull( s );
    assertNotNull( sm.getSubscription( G.G2, 1 ) );
    assertEquals( s.getDescriptor(), new GraphDescriptor( G.G2, 1 ) );
    assertEquals( s.getEntities().size(), 0 );

    sm.unsubscribe( G.G2, 1 );
    assertNull( sm.findSubscription( G.G2, 1 ) );
  }

  @Test
  public void entitySubscriptionInTypeGraph()
  {
    final Class<A> type = A.class;
    final Object id = 1;

    final EntityRepository r = new EntityRepositoryImpl();
    final EntitySubscriptionManager sm = new EntitySubscriptionManagerImpl( r );
    assertNull( sm.findSubscription( G.G1 ) );
    sm.subscribe( G.G1, null );

    r.registerEntity( type, id, new A() );
    assertEntityNotSubscribed( sm, G.G1, null, type, id );

    sm.updateEntity( type,
                     id,
                     new GraphDescriptor[]{ new GraphDescriptor( G.G1, null ) } );
    assertEntitySubscribed( sm, G.G1, null, type, id );
    sm.updateEntity( type,
                     id,
                     new GraphDescriptor[]{ new GraphDescriptor( G.G1, null ) } );

    assertEntityPresent( type, id, r );

    sm.unsubscribe( G.G1 );

    assertEntityNotSubscribed( sm, G.G1, null, type, id );

    assertEntityNotPresent( type, id, r );
  }

  @Test
  public void entitySubscriptionInTypeGraph_removeEntity()
  {
    final Class<A> type = A.class;
    final Object id = 1;

    final EntityRepository r = new EntityRepositoryImpl();
    final EntitySubscriptionManager sm = new EntitySubscriptionManagerImpl( r );
    assertNull( sm.findSubscription( G.G1 ) );
    sm.subscribe( G.G1, null );

    r.registerEntity( type, id, new A() );
    sm.updateEntity( type,
                     id,
                     new GraphDescriptor[]{ new GraphDescriptor( G.G1, null ) } );

    assertEntitySubscribed( sm, G.G1, null, type, id );

    sm.updateEntity( type,
                     id,
                     new GraphDescriptor[]{ new GraphDescriptor( G.G1, null ) } );

    assertEntitySubscribed( sm, G.G1, null, type, id );

    sm.removeEntity( type, id );
    assertEntityNotSubscribed( sm, G.G1, null, type, id );

    assertEntityPresent( type, id, r );

    sm.unsubscribe( G.G1 );

    // Entity still here as unsubscribe did not unload as removed from subscription manager
    assertEntityPresent( type, id, r );
  }

  @Test
  public void entitySubscriptionInInstanceGraph()
  {
    final Class<A> type = A.class;
    final Object id = 1;

    final EntityRepository r = new EntityRepositoryImpl();
    final EntitySubscriptionManager sm = new EntitySubscriptionManagerImpl( r );
    assertNull( sm.findSubscription( G.G2, 1 ) );
    sm.subscribe( G.G2, 1, null );

    r.registerEntity( type, id, new A() );
    assertEntityNotSubscribed( sm, G.G2, 1, type, id );

    sm.updateEntity( type,
                     id,
                     new GraphDescriptor[]{ new GraphDescriptor( G.G2, 1 ) } );
    assertEntitySubscribed( sm, G.G2, 1, type, id );
    sm.updateEntity( type,
                     id,
                     new GraphDescriptor[]{ new GraphDescriptor( G.G2, 1 ) } );

    assertEntityPresent( type, id, r );

    sm.unsubscribe( G.G2, 1 );

    assertEntityNotSubscribed( sm, G.G2, 1, type, id );

    assertEntityNotPresent( type, id, r );
  }

  @Test
  public void entitySubscriptionAndUpdateInInstanceGraph()
  {
    final EntityRepository r = new EntityRepositoryImpl();
    final EntitySubscriptionManager sm = new EntitySubscriptionManagerImpl( r );
    assertNull( sm.findSubscription( G.G2, 1 ) );

    final GraphSubscriptionEntry e1 = sm.subscribe( G.G2, 1, "F1" );

    assertEquals( sm.getSubscription( G.G2, 1 ).getFilter(), "F1" );

    final GraphSubscriptionEntry e2 = sm.updateSubscription( G.G2, 1, "F2" );

    assertEquals( sm.getSubscription( G.G2, 1 ).getFilter(), "F2" );

    assertEquals( e1, e2 );
  }

  @Test
  public void entitySubscriptionAndUpdateInTypeGraph()
  {
    final EntityRepository r = new EntityRepositoryImpl();
    final EntitySubscriptionManager sm = new EntitySubscriptionManagerImpl( r );
    assertNull( sm.findSubscription( G.G1 ) );

    final GraphSubscriptionEntry e1 = sm.subscribe( G.G1, "F1" );

    assertEquals( sm.getSubscription( G.G1 ).getFilter(), "F1" );

    final GraphSubscriptionEntry e2 = sm.updateSubscription( G.G1, "F2" );

    assertEquals( sm.getSubscription( G.G1 ).getFilter(), "F2" );

    assertEquals( e1, e2 );
  }

  @Test
  public void entitySubscriptionInInstanceGraph_removeEntity()
  {
    final Class<A> type = A.class;
    final Object id = 1;

    final EntityRepository r = new EntityRepositoryImpl();
    final EntitySubscriptionManager sm = new EntitySubscriptionManagerImpl( r );
    assertNull( sm.findSubscription( G.G2, 1 ) );
    sm.subscribe( G.G2, 1, null );

    r.registerEntity( type, id, new A() );
    sm.updateEntity( type,
                     id,
                     new GraphDescriptor[]{ new GraphDescriptor( G.G2, 1 ) } );

    assertEntitySubscribed( sm, G.G2, 1, type, id );

    sm.updateEntity( type,
                     id,
                     new GraphDescriptor[]{ new GraphDescriptor( G.G2, 1 ) } );

    assertEntitySubscribed( sm, G.G2, 1, type, id );

    sm.removeEntity( type, id );
    assertEntityNotSubscribed( sm, G.G2, 1, type, id );

    assertEntityPresent( type, id, r );

    sm.unsubscribe( G.G2, 1 );

    // Entity still here as unsubscribe did not unload as removed from subscription manager
    assertEntityPresent( type, id, r );
  }

  @Test
  public void entityOverlappingSubscriptions()
  {
    final Class<A> type = A.class;
    final Object id = 1;

    final EntityRepository r = new EntityRepositoryImpl();
    final EntitySubscriptionManager sm = new EntitySubscriptionManagerImpl( r );
    assertNull( sm.findSubscription( G.G1 ) );
    assertNull( sm.findSubscription( G.G2, 1 ) );
    sm.subscribe( G.G1, null );
    sm.subscribe( G.G2, 1, null );

    r.registerEntity( type, id, new A() );

    assertEntityNotSubscribed( sm, G.G1, null, type, id );
    assertEntityNotSubscribed( sm, G.G2, 1, type, id );

    sm.updateEntity( type,
                     id,
                     new GraphDescriptor[]{ new GraphDescriptor( G.G1, null ) } );
    assertEntitySubscribed( sm, G.G1, null, type, id );
    assertEntityNotSubscribed( sm, G.G2, 1, type, id );
    sm.updateEntity( type,
                     id,
                     new GraphDescriptor[]{ new GraphDescriptor( G.G2, 1 ) } );

    assertEntitySubscribed( sm, G.G1, null, type, id );
    assertEntitySubscribed( sm, G.G2, 1, type, id );

    assertEntityPresent( type, id, r );

    sm.unsubscribe( G.G1 );

    assertEntityPresent( type, id, r );

    assertEntityNotSubscribed( sm, G.G1, null, type, id );
    assertEntitySubscribed( sm, G.G2, 1, type, id );

    sm.unsubscribe( G.G2, 1 );

    assertEntityNotSubscribed( sm, G.G1, null, type, id );
    assertEntityNotSubscribed( sm, G.G2, 1, type, id );

    assertEntityNotPresent( type, id, r );
  }

  @Test
  public void removeEntityFromGraph()
  {
    final Class<A> type = A.class;
    final Object id = 1;

    final EntityRepository r = new EntityRepositoryImpl();
    final EntitySubscriptionManager sm = new EntitySubscriptionManagerImpl( r );
    assertNull( sm.findSubscription( G.G1 ) );
    assertNull( sm.findSubscription( G.G2 ) );

    final GraphSubscriptionEntry s1 = sm.subscribe( G.G1, "X" );
    assertEquals( s1.getFilter(), "X" );
    final GraphSubscriptionEntry s2 = sm.subscribe( G.G2, null );
    assertEquals( s2.getFilter(), null );

    r.registerEntity( type, id, new A() );

    assertEntityNotSubscribed( sm, G.G1, null, type, id );
    assertEntityNotSubscribed( sm, G.G2, null, type, id );

    sm.updateEntity( type,
                     id,
                     new GraphDescriptor[]{ new GraphDescriptor( G.G1, null ) } );
    assertEntitySubscribed( sm, G.G1, null, type, id );
    assertEntityNotSubscribed( sm, G.G2, null, type, id );

    sm.updateEntity( type,
                     id,
                     new GraphDescriptor[]{ new GraphDescriptor( G.G1, null ),
                                            new GraphDescriptor( G.G2, null ) } );

    assertEntitySubscribed( sm, G.G1, null, type, id );
    assertEntitySubscribed( sm, G.G2, null, type, id );

    sm.removeEntityFromGraph( type, id, new GraphDescriptor( G.G1, null ) );

    assertEntityNotSubscribed( sm, G.G1, null, type, id );
    assertEntitySubscribed( sm, G.G2, null, type, id );

    final EntitySubscriptionEntry e =
      sm.removeEntityFromGraph( type, id, new GraphDescriptor( G.G2, null ) );

    assertEntityNotSubscribed( sm, G.G1, null, type, id );
    assertEntityNotSubscribed( sm, G.G2, null, type, id );

    assertEquals( e.getGraphSubscriptions().size(), 0 );
  }

  private void assertEntitySubscribed( final EntitySubscriptionManager sm,
                                       final G graph,
                                       final Object graphID,
                                       final Class<A> type,
                                       final Object id )
  {
    final GraphSubscriptionEntry entry =
      null == graphID ? sm.getSubscription( graph ) : sm.getSubscription( graph, graphID );
    assertNotNull( entry.getEntities().get( type ).get( id ) );
    assertNotNull(
      sm.getSubscription( type, id ).getGraphSubscriptions().get( new GraphDescriptor( graph, graphID ) ) );
  }

  private void assertEntityNotSubscribed( final EntitySubscriptionManager sm,
                                          final G graph,
                                          final Object graphID,
                                          final Class<A> type,
                                          final Object id )
  {
    boolean found = false;
    try
    {
      assertNotNull( sm.getSubscription( graph ).getEntities().get( type ).get( id ) );
      found = true;
    }
    catch ( final Throwable t )
    {
    }
    assertFalse( found, "Found subscription unexpectedly" );
    try
    {
      final EntitySubscriptionEntry subscription = sm.getSubscription( type, id );
      assertNotNull( subscription.getGraphSubscriptions().get( new GraphDescriptor( graph, graphID ) ) );
      found = true;
    }
    catch ( final Throwable t )
    {
    }
    assertFalse( found, "Found subscription unexpectedly" );
  }

  @Test( expectedExceptions = IllegalStateException.class,
         expectedExceptionsMessageRegExp = "Graph already subscribed: .*" )
  public void subscribe_nonExistentTypeGraph()
  {
    final EntityRepository r = new EntityRepositoryImpl();
    final EntitySubscriptionManager sm = new EntitySubscriptionManagerImpl( r );
    sm.subscribe( G.G1, null );
    sm.subscribe( G.G1, null );
  }

  @Test( expectedExceptions = IllegalStateException.class,
         expectedExceptionsMessageRegExp = "Graph not subscribed: .*" )
  public void getSubscription_nonExistentTypeGraph()
  {
    final EntityRepository r = new EntityRepositoryImpl();
    final EntitySubscriptionManager sm = new EntitySubscriptionManagerImpl( r );
    sm.getSubscription( G.G1 );
  }

  @Test( expectedExceptions = IllegalStateException.class,
         expectedExceptionsMessageRegExp = "Graph not subscribed: .*" )
  public void unsubscribe_nonExistentTypeGraph()
  {
    final EntityRepository r = new EntityRepositoryImpl();
    final EntitySubscriptionManager sm = new EntitySubscriptionManagerImpl( r );
    sm.unsubscribe( G.G1 );
  }

  @Test( expectedExceptions = IllegalStateException.class,
         expectedExceptionsMessageRegExp = "Graph already subscribed: .*:1" )
  public void subscribe_nonExistentInstanceGraph()
  {
    final EntityRepository r = new EntityRepositoryImpl();
    final EntitySubscriptionManager sm = new EntitySubscriptionManagerImpl( r );
    sm.subscribe( G.G1, "1", null );
    sm.subscribe( G.G1, "1", null );
  }

  @Test( expectedExceptions = IllegalStateException.class,
         expectedExceptionsMessageRegExp = "Graph not subscribed: .*" )
  public void getSubscription_nonExistentInstanceGraph()
  {
    final EntityRepository r = new EntityRepositoryImpl();
    final EntitySubscriptionManager sm = new EntitySubscriptionManagerImpl( r );
    sm.getSubscription( G.G1, "1" );
  }

  @Test( expectedExceptions = IllegalStateException.class,
         expectedExceptionsMessageRegExp = "Graph not subscribed: .*" )
  public void unsubscribe_nonExistentInstanceGraph()
  {
    final EntityRepository r = new EntityRepositoryImpl();
    final EntitySubscriptionManager sm = new EntitySubscriptionManagerImpl( r );
    sm.unsubscribe( G.G1, "1" );
  }

  @Test( expectedExceptions = IllegalStateException.class,
         expectedExceptionsMessageRegExp = "Graph not subscribed: .*" )
  public void unsubscribe_nonExistentInstanceGraph_whenTypeCreated()
  {
    final EntityRepository r = new EntityRepositoryImpl();
    final EntitySubscriptionManager sm = new EntitySubscriptionManagerImpl( r );
    sm.subscribe( G.G1, "1" );
    sm.unsubscribe( G.G1, "2" );
  }

  private void assertEntityNotPresent( final Class<A> type, final Object id, final EntityRepository r )
  {
    assertNull( r.findByID( type, id ) );
  }

  private void assertEntityPresent( final Class<A> type, final Object id, final EntityRepository r )
  {
    assertNotNull( r.findByID( type, id ) );
  }

  static enum G
  {
    G1, G2
  }

  static class A
  {
  }
}
