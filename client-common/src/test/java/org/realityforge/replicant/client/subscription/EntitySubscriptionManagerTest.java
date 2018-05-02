package org.realityforge.replicant.client.subscription;

import arez.Arez;
import arez.Disposable;
import javax.annotation.Nonnull;
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
    final EntitySubscriptionManager sm = EntitySubscriptionManager.create();
    assertNull( sm.findSubscription( new ChannelAddress( G.G1 ) ) );

    assertEquals( sm.getTypeSubscriptions().size(), 0 );
    assertFalse( sm.getTypeSubscriptions()
                   .stream()
                   .anyMatch( s -> s.getChannel().getAddress().getChannelType().equals( G.G1 ) ) );

    final boolean explicitSubscription = true;
    sm.recordSubscription( new ChannelAddress( G.G1 ), null, explicitSubscription );

    assertEquals( sm.getTypeSubscriptions().size(), 1 );
    assertTrue( sm.getTypeSubscriptions()
                  .stream()
                  .anyMatch( s -> s.getChannel().getAddress().getChannelType().equals( G.G1 ) ) );

    final Subscription s = sm.findSubscription( new ChannelAddress( G.G1 ) );
    assertNotNull( s );
    assertNotNull( sm.getSubscription( new ChannelAddress( G.G1 ) ) );
    assertEquals( s.getChannel().getAddress(), new ChannelAddress( G.G1 ) );
    assertEquals( s.isExplicitSubscription(), explicitSubscription );
    Arez.context().safeAction( () -> assertEquals( s.getEntities().size(), 0 ) );

    sm.removeSubscription( new ChannelAddress( G.G1 ) );
    assertNull( sm.findSubscription( new ChannelAddress( G.G1 ) ) );
  }

  @Test
  public void instanceChannelRegistrations()
  {
    final EntitySubscriptionManager sm = EntitySubscriptionManager.create();
    assertNull( sm.findSubscription( new ChannelAddress( G.G2, 1 ) ) );

    assertEquals( sm.getInstanceSubscriptions().size(), 0 );
    assertEquals( sm.getInstanceSubscriptionIds( G.G2 ).size(), 0 );

    final boolean explicitSubscription = false;
    sm.recordSubscription( new ChannelAddress( G.G2, 1 ), null, explicitSubscription );

    assertEquals( sm.getInstanceSubscriptions().size(), 1 );
    assertEquals( sm.getInstanceSubscriptionIds( G.G2 ).size(), 1 );

    assertNull( sm.findSubscription( new ChannelAddress( G.G2, 2 ) ) );
    final Subscription s = sm.findSubscription( new ChannelAddress( G.G2, 1 ) );
    assertNotNull( s );
    assertNotNull( sm.getSubscription( new ChannelAddress( G.G2, 1 ) ) );
    assertEquals( s.getChannel().getAddress(), new ChannelAddress( G.G2, 1 ) );
    assertEquals( s.isExplicitSubscription(), explicitSubscription );
    Arez.context().safeAction( () -> assertEquals( s.getEntities().size(), 0 ) );

    sm.removeSubscription( new ChannelAddress( G.G2, 1 ) );
    assertNull( sm.findSubscription( new ChannelAddress( G.G2, 1 ) ) );
  }

  @Test
  public void entitySubscriptionInTypeChannel()
  {
    final Class<A> type = A.class;
    final Object id = 1;

    final EntitySubscriptionManager sm = EntitySubscriptionManager.create();
    assertNull( sm.findSubscription( new ChannelAddress( G.G1 ) ) );
    final Subscription e1 = sm.recordSubscription( new ChannelAddress( G.G1 ), null, false );

    assertEntityNotSubscribed( sm, new ChannelAddress( G.G1, null ), type, id );

    final A entity = new A();
    updateEntity( sm,
                  type,
                  id,
                  new ChannelAddress[]{ new ChannelAddress( G.G1 ) },
                  entity );
    assertEntitySubscribed( sm, new ChannelAddress( G.G1, null ), type, id, entity );
    updateEntity( sm,
                  type,
                  id,
                  new ChannelAddress[]{ new ChannelAddress( G.G1 ) },
                  entity );

    sm.removeSubscription( new ChannelAddress( G.G1 ) );

    assertEntityNotSubscribed( sm, new ChannelAddress( G.G1, null ), type, id );

    assertTrue( Disposable.isDisposed( e1 ) );
  }

  @Test
  public void entitySubscriptionInTypeChannel_removeEntity()
  {
    final Class<A> type = A.class;
    final Object id = 1;

    final EntitySubscriptionManager sm = EntitySubscriptionManager.create();
    assertNull( sm.findSubscription( new ChannelAddress( G.G1 ) ) );
    final Subscription s1 =
      sm.recordSubscription( new ChannelAddress( G.G1 ), null, false );

    final A entity = new A();
    updateEntity( sm,
                  type,
                  id,
                  new ChannelAddress[]{ new ChannelAddress( G.G1 ) },
                  entity );

    assertEntitySubscribed( sm, new ChannelAddress( G.G1, null ), type, id, entity );

    updateEntity( sm,
                  type,
                  id,
                  new ChannelAddress[]{ new ChannelAddress( G.G1 ) },
                  entity );

    assertEntitySubscribed( sm, new ChannelAddress( G.G1, null ), type, id, entity );

    sm.removeEntity( type, id );
    assertEntityNotSubscribed( sm, new ChannelAddress( G.G1, null ), type, id );

    //assertEntityPresent( type, id, r );

    sm.removeSubscription( new ChannelAddress( G.G1 ) );

    assertTrue( Disposable.isDisposed( s1 ) );

    // Entity still here as unsubscribe did not unload as removed from subscription manager
    //assertEntityPresent( type, id, r );
  }

  @Test
  public void entitySubscriptionInInstanceChannel()
  {
    final Class<A> type = A.class;
    final Object id = 1;

    final EntitySubscriptionManager sm = EntitySubscriptionManager.create();
    assertNull( sm.findSubscription( new ChannelAddress( G.G2, 1 ) ) );
    sm.recordSubscription( new ChannelAddress( G.G2, 1 ), null, false );

    assertEntityNotSubscribed( sm, new ChannelAddress( G.G2, 1 ), type, id );

    final A entity = new A();
    updateEntity( sm,
                  type,
                  id,
                  new ChannelAddress[]{ new ChannelAddress( G.G2, 1 ) },
                  entity );
    assertEntitySubscribed( sm, new ChannelAddress( G.G2, 1 ), type, id, entity );
    updateEntity( sm,
                  type,
                  id,
                  new ChannelAddress[]{ new ChannelAddress( G.G2, 1 ) },
                  entity );

    sm.removeSubscription( new ChannelAddress( G.G2, 1 ) );

    assertEntityNotSubscribed( sm, new ChannelAddress( G.G2, 1 ), type, id );
  }

  @Test
  public void entitySubscriptionInInstanceChannel_removeEntity()
  {
    final Class<A> type = A.class;
    final Object id = 1;

    final EntitySubscriptionManager sm = EntitySubscriptionManager.create();
    assertNull( sm.findSubscription( new ChannelAddress( G.G2, 1 ) ) );
    sm.recordSubscription( new ChannelAddress( G.G2, 1 ), null, false );

    final A entity = new A();
    updateEntity( sm, type,
                  id,
                  new ChannelAddress[]{ new ChannelAddress( G.G2, 1 ) },
                  entity );

    assertEntitySubscribed( sm, new ChannelAddress( G.G2, 1 ), type, id, entity );

    updateEntity( sm, type,
                  id,
                  new ChannelAddress[]{ new ChannelAddress( G.G2, 1 ) },
                  entity );

    assertEntitySubscribed( sm, new ChannelAddress( G.G2, 1 ), type, id, entity );

    sm.removeEntity( type, id );
    assertEntityNotSubscribed( sm, new ChannelAddress( G.G2, 1 ), type, id );

    sm.removeSubscription( new ChannelAddress( G.G2, 1 ) );
  }

  @Test
  public void entityOverlappingSubscriptions()
  {
    final Class<A> type = A.class;
    final Object id = 1;

    final EntitySubscriptionManager sm = EntitySubscriptionManager.create();
    assertNull( sm.findSubscription( new ChannelAddress( G.G1 ) ) );
    assertNull( sm.findSubscription( new ChannelAddress( G.G2, 1 ) ) );

    sm.recordSubscription( new ChannelAddress( G.G1 ), null, false );
    sm.recordSubscription( new ChannelAddress( G.G2, 1 ), null, false );

    assertEntityNotSubscribed( sm, new ChannelAddress( G.G1, null ), type, id );
    assertEntityNotSubscribed( sm, new ChannelAddress( G.G2, 1 ), type, id );

    final A entity = new A();
    updateEntity( sm,
                  type,
                  id,
                  new ChannelAddress[]{ new ChannelAddress( G.G1 ) },
                  entity );
    assertEntitySubscribed( sm, new ChannelAddress( G.G1, null ), type, id, entity );
    assertEntityNotSubscribed( sm, new ChannelAddress( G.G2, 1 ), type, id );

    updateEntity( sm,
                  type,
                  id,
                  new ChannelAddress[]{ new ChannelAddress( G.G2, 1 ) },
                  entity );

    assertEntitySubscribed( sm, new ChannelAddress( G.G1, null ), type, id, entity );
    assertEntitySubscribed( sm, new ChannelAddress( G.G2, 1 ), type, id, entity );

    sm.removeSubscription( new ChannelAddress( G.G1 ) );

    assertEntityNotSubscribed( sm, new ChannelAddress( G.G1, null ), type, id );
    assertEntitySubscribed( sm, new ChannelAddress( G.G2, 1 ), type, id, entity );

    sm.removeSubscription( new ChannelAddress( G.G2, 1 ) );

    assertEntityNotSubscribed( sm, new ChannelAddress( G.G1, null ), type, id );
    assertEntityNotSubscribed( sm, new ChannelAddress( G.G2, 1 ), type, id );
  }

  private void assertEntitySubscribed( final EntitySubscriptionManager sm,
                                       final ChannelAddress descriptor,
                                       final Class<A> type,
                                       final Object id,
                                       final A entity )
  {
    final Subscription entry = sm.getSubscription( descriptor );
    assertNotNull( entry.findEntityByTypeAndId( type, id ) );
    assertTrue( sm.getEntity( type, id )
                  .getSubscriptions()
                  .stream()
                  .anyMatch( s -> s.getChannel().getAddress().equals( descriptor ) ) );
    assertEquals( sm.getEntity( type, id ).getUserObject(), entity );
  }

  private void assertEntityNotSubscribed( final EntitySubscriptionManager sm,
                                          final ChannelAddress descriptor,
                                          final Class<A> type, final Object id )
  {
    boolean found;
    try
    {
      final Subscription subscription = sm.getSubscription( descriptor );
      assertNotNull( subscription.findEntityByTypeAndId( type, id ) );
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
    final EntitySubscriptionManager sm = EntitySubscriptionManager.create();
    sm.recordSubscription( new ChannelAddress( G.G1 ), null, false );
    sm.recordSubscription( new ChannelAddress( G.G1 ), null, false );
  }

  @Test( expectedExceptions = IllegalStateException.class,
    expectedExceptionsMessageRegExp = "Channel not subscribed: .*" )
  public void getSubscription_nonExistentTypeChannel()
  {
    final EntitySubscriptionManager sm = EntitySubscriptionManager.create();
    sm.getSubscription( new ChannelAddress( G.G1 ) );
  }

  @Test( expectedExceptions = IllegalStateException.class,
    expectedExceptionsMessageRegExp = "Channel not subscribed: .*" )
  public void unsubscribe_nonExistentTypeChannel()
  {
    final EntitySubscriptionManager sm = EntitySubscriptionManager.create();
    sm.removeSubscription( new ChannelAddress( G.G1 ) );
  }

  @Test( expectedExceptions = IllegalStateException.class,
    expectedExceptionsMessageRegExp = "Channel already subscribed: .*:1" )
  public void subscribe_nonExistentInstanceChannel()
  {
    final EntitySubscriptionManager sm = EntitySubscriptionManager.create();
    sm.recordSubscription( new ChannelAddress( G.G1, "1" ), null, false );
    sm.recordSubscription( new ChannelAddress( G.G1, "1" ), null, false );
  }

  @Test( expectedExceptions = IllegalStateException.class,
    expectedExceptionsMessageRegExp = "Channel not subscribed: .*" )
  public void getSubscription_nonExistentInstanceChannel()
  {
    final EntitySubscriptionManager sm = EntitySubscriptionManager.create();
    sm.getSubscription( new ChannelAddress( G.G1, "1" ) );
  }

  @Test( expectedExceptions = IllegalStateException.class,
    expectedExceptionsMessageRegExp = "Channel not subscribed: .*" )
  public void unsubscribe_nonExistentInstanceChannel()
  {
    final EntitySubscriptionManager sm = EntitySubscriptionManager.create();
    sm.removeSubscription( new ChannelAddress( G.G1, "1" ) );
  }

  @Test( expectedExceptions = IllegalStateException.class,
    expectedExceptionsMessageRegExp = "Channel not subscribed: .*" )
  public void unsubscribe_nonExistentInstanceChannel_whenTypeCreated()
  {
    final EntitySubscriptionManager sm = EntitySubscriptionManager.create();
    sm.recordSubscription( new ChannelAddress( G.G1 ), "1", false );
    sm.removeSubscription( new ChannelAddress( G.G1, "2" ) );
  }

  private <T> void updateEntity( @Nonnull final EntitySubscriptionManager sm,
                                 @Nonnull final Class<T> type,
                                 @Nonnull final Object id,
                                 @Nonnull final ChannelAddress[] channels,
                                 @Nonnull final T userObject )
  {
    final Entity entity = sm.findOrCreateEntity( type, id );
    entity.setUserObject( userObject );
    for ( final ChannelAddress channel : channels )
    {
      final Subscription subscription = sm.findSubscription( channel );
      assert null != subscription;
      entity.linkToSubscription( subscription );
    }
  }

  enum G
  {
    G1, G2
  }

  static class A
  {
  }
}
