package org.realityforge.replicant.client.subscription;

import arez.Arez;
import arez.Disposable;
import org.realityforge.replicant.client.AbstractReplicantTest;
import org.realityforge.replicant.client.ChannelAddress;
import org.testng.IHookCallBack;
import org.testng.IHookable;
import org.testng.ITestResult;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class EntitySubscriptionManagerTest
  extends AbstractReplicantTest
  implements IHookable
{
  @Override
  public void run( final IHookCallBack callBack, final ITestResult testResult )
  {
    Arez.context().safeAction( () -> callBack.runTestMethod( testResult ) );
  }

  @Test
  public void typeChannelRegistrations()
  {
    final EntitySubscriptionManager sm = new Arez_EntitySubscriptionManager();
    assertNull( sm.findChannelSubscription( new ChannelAddress( G.G1 ) ) );

    assertEquals( sm.getTypeChannelSubscriptions().size(), 0 );
    assertFalse( sm.getTypeChannelSubscriptions().contains( G.G1 ) );

    final boolean explicitSubscription = true;
    sm.recordChannelSubscription( new ChannelAddress( G.G1 ), null, explicitSubscription );

    assertEquals( sm.getTypeChannelSubscriptions().size(), 1 );
    assertTrue( sm.getTypeChannelSubscriptions().contains( G.G1 ) );

    final ChannelSubscriptionEntry s = sm.findChannelSubscription( new ChannelAddress( G.G1 ) );
    assertNotNull( s );
    assertNotNull( sm.getChannelSubscription( new ChannelAddress( G.G1 ) ) );
    assertEquals( s.getChannel().getAddress(), new ChannelAddress( G.G1 ) );
    assertEquals( s.isExplicitSubscription(), explicitSubscription );
    assertEquals( s.getEntities().size(), 0 );

    sm.removeChannelSubscription( new ChannelAddress( G.G1 ) );
    assertNull( sm.findChannelSubscription( new ChannelAddress( G.G1 ) ) );
  }

  @Test
  public void instanceChannelRegistrations()
  {
    final EntitySubscriptionManager sm = new Arez_EntitySubscriptionManager();
    assertNull( sm.findChannelSubscription( new ChannelAddress( G.G2, 1 ) ) );

    assertEquals( sm.getInstanceChannelSubscriptionKeys().size(), 0 );
    assertEquals( sm.getInstanceChannelSubscriptions( G.G2 ).size(), 0 );

    final boolean explicitSubscription = false;
    sm.recordChannelSubscription( new ChannelAddress( G.G2, 1 ), null, explicitSubscription );

    assertEquals( sm.getInstanceChannelSubscriptionKeys().size(), 1 );
    assertEquals( sm.getInstanceChannelSubscriptions( G.G2 ).size(), 1 );

    assertNull( sm.findChannelSubscription( new ChannelAddress( G.G2, 2 ) ) );
    final ChannelSubscriptionEntry s = sm.findChannelSubscription( new ChannelAddress( G.G2, 1 ) );
    assertNotNull( s );
    assertNotNull( sm.getChannelSubscription( new ChannelAddress( G.G2, 1 ) ) );
    assertEquals( s.getChannel().getAddress(), new ChannelAddress( G.G2, 1 ) );
    assertEquals( s.isExplicitSubscription(), explicitSubscription );
    assertEquals( s.getEntities().size(), 0 );

    sm.removeChannelSubscription( new ChannelAddress( G.G2, 1 ) );
    assertNull( sm.findChannelSubscription( new ChannelAddress( G.G2, 1 ) ) );
  }

  @Test
  public void entitySubscriptionInTypeChannel()
  {
    final Class<A> type = A.class;
    final Object id = 1;

    final EntitySubscriptionManager sm = new Arez_EntitySubscriptionManager();
    assertNull( sm.findChannelSubscription( new ChannelAddress( G.G1 ) ) );
    final ChannelSubscriptionEntry e1 = sm.recordChannelSubscription( new ChannelAddress( G.G1 ), null, false );

    assertEntityNotSubscribed( sm, new ChannelAddress( G.G1, null ), type, id );

    final A entity = new A();
    sm.updateEntity( type,
                     id,
                     new ChannelAddress[]{ new ChannelAddress( G.G1 ) },
                     entity );
    assertEntitySubscribed( sm, new ChannelAddress( G.G1, null ), type, id, entity );
    sm.updateEntity( type,
                     id,
                     new ChannelAddress[]{ new ChannelAddress( G.G1 ) },
                     entity );

    sm.removeChannelSubscription( new ChannelAddress( G.G1 ) );

    assertEntityNotSubscribed( sm, new ChannelAddress( G.G1, null ), type, id );

    assertTrue( Disposable.isDisposed( e1 ) );
  }

  @Test
  public void entitySubscriptionInTypeChannel_removeEntity()
  {
    final Class<A> type = A.class;
    final Object id = 1;

    final EntitySubscriptionManager sm = new Arez_EntitySubscriptionManager();
    assertNull( sm.findChannelSubscription( new ChannelAddress( G.G1 ) ) );
    final ChannelSubscriptionEntry s1 =
      sm.recordChannelSubscription( new ChannelAddress( G.G1 ), null, false );

    final A entity = new A();
    sm.updateEntity( type,
                     id,
                     new ChannelAddress[]{ new ChannelAddress( G.G1 ) },
                     entity );

    assertEntitySubscribed( sm, new ChannelAddress( G.G1, null ), type, id, entity );

    sm.updateEntity( type,
                     id,
                     new ChannelAddress[]{ new ChannelAddress( G.G1 ) },
                     entity );

    assertEntitySubscribed( sm, new ChannelAddress( G.G1, null ), type, id, entity );

    sm.removeEntity( type, id );
    assertEntityNotSubscribed( sm, new ChannelAddress( G.G1, null ), type, id );

    //assertEntityPresent( type, id, r );

    sm.removeChannelSubscription( new ChannelAddress( G.G1 ) );

    assertTrue( Disposable.isDisposed( s1 ) );

    // Entity still here as unsubscribe did not unload as removed from subscription manager
    //assertEntityPresent( type, id, r );
  }

  @Test
  public void entitySubscriptionInInstanceChannel()
  {
    final Class<A> type = A.class;
    final Object id = 1;

    final EntitySubscriptionManager sm = new Arez_EntitySubscriptionManager();
    assertNull( sm.findChannelSubscription( new ChannelAddress( G.G2, 1 ) ) );
    sm.recordChannelSubscription( new ChannelAddress( G.G2, 1 ), null, false );

    assertEntityNotSubscribed( sm, new ChannelAddress( G.G2, 1 ), type, id );

    final A entity = new A();
    sm.updateEntity( type,
                     id,
                     new ChannelAddress[]{ new ChannelAddress( G.G2, 1 ) },
                     entity );
    assertEntitySubscribed( sm, new ChannelAddress( G.G2, 1 ), type, id, entity );
    sm.updateEntity( type,
                     id,
                     new ChannelAddress[]{ new ChannelAddress( G.G2, 1 ) },
                     entity );

    sm.removeChannelSubscription( new ChannelAddress( G.G2, 1 ) );

    assertEntityNotSubscribed( sm, new ChannelAddress( G.G2, 1 ), type, id );
  }

  @Test
  public void entitySubscriptionAndUpdateInInstanceChannel()
  {
    final EntitySubscriptionManager sm = new Arez_EntitySubscriptionManager();
    assertNull( sm.findChannelSubscription( new ChannelAddress( G.G2, 1 ) ) );

    final ChannelSubscriptionEntry e1 = sm.recordChannelSubscription( new ChannelAddress( G.G2, 1 ), "F1", false );

    assertEquals( sm.getChannelSubscription( new ChannelAddress( G.G2, 1 ) ).getChannel().getFilter(), "F1" );

    final ChannelSubscriptionEntry e2 = sm.updateChannelSubscription( new ChannelAddress( G.G2, 1 ), "F2" );

    assertEquals( sm.getChannelSubscription( new ChannelAddress( G.G2, 1 ) ).getChannel().getFilter(), "F2" );

    assertEquals( e1, e2 );
  }

  @Test
  public void entitySubscriptionAndUpdateInTypeChannel()
  {
    final EntitySubscriptionManager sm = new Arez_EntitySubscriptionManager();
    assertNull( sm.findChannelSubscription( new ChannelAddress( G.G1 ) ) );

    final ChannelSubscriptionEntry e1 = sm.recordChannelSubscription( new ChannelAddress( G.G1 ), "F1", false );

    assertEquals( sm.getChannelSubscription( new ChannelAddress( G.G1 ) ).getChannel().getFilter(), "F1" );

    final ChannelSubscriptionEntry e2 = sm.updateChannelSubscription( new ChannelAddress( G.G1 ), "F2" );

    assertEquals( sm.getChannelSubscription( new ChannelAddress( G.G1 ) ).getChannel().getFilter(), "F2" );

    assertEquals( e1, e2 );
  }

  @Test
  public void entitySubscriptionInInstanceChannel_removeEntity()
  {
    final Class<A> type = A.class;
    final Object id = 1;

    final EntitySubscriptionManager sm = new Arez_EntitySubscriptionManager();
    assertNull( sm.findChannelSubscription( new ChannelAddress( G.G2, 1 ) ) );
    sm.recordChannelSubscription( new ChannelAddress( G.G2, 1 ), null, false );

    final A entity = new A();
    sm.updateEntity( type,
                     id,
                     new ChannelAddress[]{ new ChannelAddress( G.G2, 1 ) },
                     entity );

    assertEntitySubscribed( sm, new ChannelAddress( G.G2, 1 ), type, id, entity );

    sm.updateEntity( type,
                     id,
                     new ChannelAddress[]{ new ChannelAddress( G.G2, 1 ) },
                     entity );

    assertEntitySubscribed( sm, new ChannelAddress( G.G2, 1 ), type, id, entity );

    sm.removeEntity( type, id );
    assertEntityNotSubscribed( sm, new ChannelAddress( G.G2, 1 ), type, id );

    sm.removeChannelSubscription( new ChannelAddress( G.G2, 1 ) );
  }

  @Test
  public void entityOverlappingSubscriptions()
  {
    final Class<A> type = A.class;
    final Object id = 1;

    final EntitySubscriptionManager sm = new Arez_EntitySubscriptionManager();
    assertNull( sm.findChannelSubscription( new ChannelAddress( G.G1 ) ) );
    assertNull( sm.findChannelSubscription( new ChannelAddress( G.G2, 1 ) ) );
    sm.recordChannelSubscription( new ChannelAddress( G.G1 ), null, false );
    sm.recordChannelSubscription( new ChannelAddress( G.G2, 1 ), null, false );

    assertEntityNotSubscribed( sm, new ChannelAddress( G.G1, null ), type, id );
    assertEntityNotSubscribed( sm, new ChannelAddress( G.G2, 1 ), type, id );

    final A entity = new A();
    sm.updateEntity( type,
                     id,
                     new ChannelAddress[]{ new ChannelAddress( G.G1 ) },
                     entity );
    assertEntitySubscribed( sm, new ChannelAddress( G.G1, null ), type, id, entity );
    assertEntityNotSubscribed( sm, new ChannelAddress( G.G2, 1 ), type, id );

    sm.updateEntity( type,
                     id,
                     new ChannelAddress[]{ new ChannelAddress( G.G2, 1 ) },
                     entity );

    assertEntitySubscribed( sm, new ChannelAddress( G.G1, null ), type, id, entity );
    assertEntitySubscribed( sm, new ChannelAddress( G.G2, 1 ), type, id, entity );

    sm.removeChannelSubscription( new ChannelAddress( G.G1 ) );

    assertEntityNotSubscribed( sm, new ChannelAddress( G.G1, null ), type, id );
    assertEntitySubscribed( sm, new ChannelAddress( G.G2, 1 ), type, id, entity );

    sm.removeChannelSubscription( new ChannelAddress( G.G2, 1 ) );

    assertEntityNotSubscribed( sm, new ChannelAddress( G.G1, null ), type, id );
    assertEntityNotSubscribed( sm, new ChannelAddress( G.G2, 1 ), type, id );
  }

  @Test
  public void removeEntityFromChannel()
  {
    final Class<A> type = A.class;
    final Object id = 1;

    final EntitySubscriptionManager sm = new Arez_EntitySubscriptionManager();
    assertNull( sm.findChannelSubscription( new ChannelAddress( G.G1 ) ) );
    assertNull( sm.findChannelSubscription( new ChannelAddress( G.G2 ) ) );

    final ChannelSubscriptionEntry s1 = sm.recordChannelSubscription( new ChannelAddress( G.G1 ), "X", false );
    assertEquals( s1.getChannel().getFilter(), "X" );
    final ChannelSubscriptionEntry s2 = sm.recordChannelSubscription( new ChannelAddress( G.G2 ), null, false );
    assertEquals( s2.getChannel().getFilter(), null );

    assertEntityNotSubscribed( sm, new ChannelAddress( G.G1, null ), type, id );
    assertEntityNotSubscribed( sm, new ChannelAddress( G.G2, null ), type, id );

    final A entity1 = new A();
    sm.updateEntity( type,
                     id,
                     new ChannelAddress[]{ new ChannelAddress( G.G1 ) },
                     entity1 );
    assertEntitySubscribed( sm, new ChannelAddress( G.G1, null ), type, id, entity1 );
    assertEntityNotSubscribed( sm, new ChannelAddress( G.G2, null ), type, id );

    sm.updateEntity( type,
                     id,
                     new ChannelAddress[]{ new ChannelAddress( G.G1 ),
                                           new ChannelAddress( G.G2 ) },
                     entity1 );

    assertEntitySubscribed( sm, new ChannelAddress( G.G1, null ), type, id, entity1 );
    assertEntitySubscribed( sm, new ChannelAddress( G.G2, null ), type, id, entity1 );

    sm.removeEntityFromChannel( type, id, new ChannelAddress( G.G1 ) );

    assertEntityNotSubscribed( sm, new ChannelAddress( G.G1, null ), type, id );
    assertEntitySubscribed( sm, new ChannelAddress( G.G2, null ), type, id, entity1 );

    final EntitySubscriptionEntry e =
      sm.removeEntityFromChannel( type, id, new ChannelAddress( G.G2 ) );

    assertEntityNotSubscribed( sm, new ChannelAddress( G.G1, null ), type, id );
    assertEntityNotSubscribed( sm, new ChannelAddress( G.G2, null ), type, id );

    assertEquals( e.getChannelSubscriptions().size(), 0 );
  }

  private void assertEntitySubscribed( final EntitySubscriptionManager sm,
                                       final ChannelAddress descriptor,
                                       final Class<A> type,
                                       final Object id,
                                       final A entity )
  {
    final ChannelSubscriptionEntry entry = sm.getChannelSubscription( descriptor );
    assertNotNull( entry.getEntities().get( type ).get( id ) );
    assertNotNull( sm.getEntitySubscription( type, id ).getChannelSubscriptions().get( descriptor ) );
    assertEquals( sm.getEntitySubscription( type, id ).getEntity(), entity );
  }

  private void assertEntityNotSubscribed( final EntitySubscriptionManager sm,
                                          final ChannelAddress descriptor,
                                          final Class<A> type, final Object id )
  {
    boolean found;
    try
    {
      final ChannelSubscriptionEntry subscription = sm.getChannelSubscription( descriptor );
      assertNotNull( subscription.getEntities().get( type ).get( id ) );
      found = true;
    }
    catch ( final Throwable t )
    {
      found = false;
    }
    assertFalse( found, "Found subscription unexpectedly" );
  }

  @Test( expectedExceptions = IllegalStateException.class,
    expectedExceptionsMessageRegExp = "Channel already subscribed: .*" )
  public void subscribe_nonExistentTypeChannel()
  {
    final EntitySubscriptionManager sm = new Arez_EntitySubscriptionManager();
    sm.recordChannelSubscription( new ChannelAddress( G.G1 ), null, false );
    sm.recordChannelSubscription( new ChannelAddress( G.G1 ), null, false );
  }

  @Test( expectedExceptions = IllegalStateException.class,
    expectedExceptionsMessageRegExp = "Channel not subscribed: .*" )
  public void getSubscription_nonExistentTypeChannel()
  {
    final EntitySubscriptionManager sm = new Arez_EntitySubscriptionManager();
    sm.getChannelSubscription( new ChannelAddress( G.G1 ) );
  }

  @Test( expectedExceptions = IllegalStateException.class,
    expectedExceptionsMessageRegExp = "Channel not subscribed: .*" )
  public void unsubscribe_nonExistentTypeChannel()
  {
    final EntitySubscriptionManager sm = new Arez_EntitySubscriptionManager();
    sm.removeChannelSubscription( new ChannelAddress( G.G1 ) );
  }

  @Test( expectedExceptions = IllegalStateException.class,
    expectedExceptionsMessageRegExp = "Channel already subscribed: .*:1" )
  public void subscribe_nonExistentInstanceChannel()
  {
    final EntitySubscriptionManager sm = new Arez_EntitySubscriptionManager();
    sm.recordChannelSubscription( new ChannelAddress( G.G1, "1" ), null, false );
    sm.recordChannelSubscription( new ChannelAddress( G.G1, "1" ), null, false );
  }

  @Test( expectedExceptions = IllegalStateException.class,
    expectedExceptionsMessageRegExp = "Channel not subscribed: .*" )
  public void getSubscription_nonExistentInstanceChannel()
  {
    final EntitySubscriptionManager sm = new Arez_EntitySubscriptionManager();
    sm.getChannelSubscription( new ChannelAddress( G.G1, "1" ) );
  }

  @Test( expectedExceptions = IllegalStateException.class,
    expectedExceptionsMessageRegExp = "Channel not subscribed: .*" )
  public void unsubscribe_nonExistentInstanceChannel()
  {
    final EntitySubscriptionManager sm = new Arez_EntitySubscriptionManager();
    sm.removeChannelSubscription( new ChannelAddress( G.G1, "1" ) );
  }

  @Test( expectedExceptions = IllegalStateException.class,
    expectedExceptionsMessageRegExp = "Channel not subscribed: .*" )
  public void unsubscribe_nonExistentInstanceChannel_whenTypeCreated()
  {
    final EntitySubscriptionManager sm = new Arez_EntitySubscriptionManager();
    sm.recordChannelSubscription( new ChannelAddress( G.G1 ), "1", false );
    sm.removeChannelSubscription( new ChannelAddress( G.G1, "2" ) );
  }

  enum G
  {
    G1, G2
  }

  static class A
  {
  }
}
