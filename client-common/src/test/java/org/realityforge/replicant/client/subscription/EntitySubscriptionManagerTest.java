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
  public void entitySubscriptionInTypeChannel()
  {
    final Class<A> type = A.class;
    final Object id = 1;

    final EntitySubscriptionManager sm = EntitySubscriptionManager.create();
    final SubscriptionService ss = SubscriptionService.create();
    assertNull( ss.findSubscription( new ChannelAddress( G.G1 ) ) );
    final Subscription e1 = ss.createSubscription( new ChannelAddress( G.G1 ), null, false );

    assertEntityNotSubscribed( ss, new ChannelAddress( G.G1, null ), type, id );

    final A entity = new A();
    updateEntity( sm,
                  ss,
                  type,
                  id,
                  new ChannelAddress[]{ new ChannelAddress( G.G1 ) },
                  entity );
    assertEntitySubscribed( ss, new ChannelAddress( G.G1, null ), type, id, entity );
    updateEntity( sm,
                  ss,
                  type,
                  id,
                  new ChannelAddress[]{ new ChannelAddress( G.G1 ) },
                  entity );

    ss.removeSubscription( new ChannelAddress( G.G1 ) );

    assertEntityNotSubscribed( ss, new ChannelAddress( G.G1, null ), type, id );

    assertTrue( Disposable.isDisposed( e1 ) );
  }

  @Test
  public void entitySubscriptionInTypeChannel_removeEntity()
  {
    final Class<A> type = A.class;
    final Object id = 1;

    final EntitySubscriptionManager sm = EntitySubscriptionManager.create();
    final SubscriptionService ss = SubscriptionService.create();
    assertNull( ss.findSubscription( new ChannelAddress( G.G1 ) ) );
    final Subscription s1 =
      ss.createSubscription( new ChannelAddress( G.G1 ), null, false );

    final A entity = new A();
    updateEntity( sm,
                  ss,
                  type,
                  id,
                  new ChannelAddress[]{ new ChannelAddress( G.G1 ) },
                  entity );

    assertEntitySubscribed( ss, new ChannelAddress( G.G1, null ), type, id, entity );

    updateEntity( sm,
                  ss,
                  type,
                  id,
                  new ChannelAddress[]{ new ChannelAddress( G.G1 ) },
                  entity );

    assertEntitySubscribed( ss, new ChannelAddress( G.G1, null ), type, id, entity );

    sm.removeEntity( type, id );
    assertEntityNotSubscribed( ss, new ChannelAddress( G.G1, null ), type, id );

    //assertEntityPresent( type, id, r );

    ss.removeSubscription( new ChannelAddress( G.G1 ) );

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
    final SubscriptionService ss = SubscriptionService.create();
    assertNull( ss.findSubscription( new ChannelAddress( G.G2, 1 ) ) );
    ss.createSubscription( new ChannelAddress( G.G2, 1 ), null, false );

    assertEntityNotSubscribed( ss, new ChannelAddress( G.G2, 1 ), type, id );

    final A entity = new A();
    updateEntity( sm,
                  ss,
                  type,
                  id,
                  new ChannelAddress[]{ new ChannelAddress( G.G2, 1 ) },
                  entity );
    assertEntitySubscribed( ss, new ChannelAddress( G.G2, 1 ), type, id, entity );
    updateEntity( sm,
                  ss,
                  type,
                  id,
                  new ChannelAddress[]{ new ChannelAddress( G.G2, 1 ) },
                  entity );

    ss.removeSubscription( new ChannelAddress( G.G2, 1 ) );

    assertEntityNotSubscribed( ss, new ChannelAddress( G.G2, 1 ), type, id );
  }

  @Test
  public void entitySubscriptionInInstanceChannel_removeEntity()
  {
    final Class<A> type = A.class;
    final Object id = 1;

    final EntitySubscriptionManager sm = EntitySubscriptionManager.create();
    final SubscriptionService ss = SubscriptionService.create();
    assertNull( ss.findSubscription( new ChannelAddress( G.G2, 1 ) ) );
    ss.createSubscription( new ChannelAddress( G.G2, 1 ), null, false );

    final A entity = new A();
    updateEntity( sm,
                  ss,
                  type,
                  id,
                  new ChannelAddress[]{ new ChannelAddress( G.G2, 1 ) },
                  entity );

    assertEntitySubscribed( ss, new ChannelAddress( G.G2, 1 ), type, id, entity );

    updateEntity( sm,
                  ss,
                  type,
                  id,
                  new ChannelAddress[]{ new ChannelAddress( G.G2, 1 ) },
                  entity );

    assertEntitySubscribed( ss, new ChannelAddress( G.G2, 1 ), type, id, entity );

    sm.removeEntity( type, id );
    assertEntityNotSubscribed( ss, new ChannelAddress( G.G2, 1 ), type, id );

    ss.removeSubscription( new ChannelAddress( G.G2, 1 ) );
  }

  @Test
  public void entityOverlappingSubscriptions()
  {
    final Class<A> type = A.class;
    final Object id = 1;

    final EntitySubscriptionManager sm = EntitySubscriptionManager.create();
    final SubscriptionService ss = SubscriptionService.create();
    assertNull( ss.findSubscription( new ChannelAddress( G.G1 ) ) );
    assertNull( ss.findSubscription( new ChannelAddress( G.G2, 1 ) ) );

    ss.createSubscription( new ChannelAddress( G.G1 ), null, false );
    ss.createSubscription( new ChannelAddress( G.G2, 1 ), null, false );

    assertEntityNotSubscribed( ss, new ChannelAddress( G.G1, null ), type, id );
    assertEntityNotSubscribed( ss, new ChannelAddress( G.G2, 1 ), type, id );

    final A entity = new A();
    updateEntity( sm,
                  ss,
                  type,
                  id,
                  new ChannelAddress[]{ new ChannelAddress( G.G1 ) },
                  entity );
    assertEntitySubscribed( ss, new ChannelAddress( G.G1, null ), type, id, entity );
    assertEntityNotSubscribed( ss, new ChannelAddress( G.G2, 1 ), type, id );

    updateEntity( sm,
                  ss,
                  type,
                  id,
                  new ChannelAddress[]{ new ChannelAddress( G.G2, 1 ) },
                  entity );

    assertEntitySubscribed( ss, new ChannelAddress( G.G1, null ), type, id, entity );
    assertEntitySubscribed( ss, new ChannelAddress( G.G2, 1 ), type, id, entity );

    ss.removeSubscription( new ChannelAddress( G.G1 ) );

    assertEntityNotSubscribed( ss, new ChannelAddress( G.G1, null ), type, id );
    assertEntitySubscribed( ss, new ChannelAddress( G.G2, 1 ), type, id, entity );

    ss.removeSubscription( new ChannelAddress( G.G2, 1 ) );

    assertEntityNotSubscribed( ss, new ChannelAddress( G.G1, null ), type, id );
    assertEntityNotSubscribed( ss, new ChannelAddress( G.G2, 1 ), type, id );
  }

  private void assertEntitySubscribed( final SubscriptionService subscriptionService,
                                       final ChannelAddress address,
                                       final Class<A> type,
                                       final Object id,
                                       final A entity )
  {
    final Subscription subscription = subscriptionService.findSubscription( address );
    assertNotNull( subscription );
    final Entity slot = subscription.findEntityByTypeAndId( type, id );
    assertNotNull( slot );
    assertTrue( slot.getSubscriptions().stream().anyMatch( s -> s.getChannel().getAddress().equals( address ) ) );
    assertEquals( slot.getUserObject(), entity );
  }

  private void assertEntityNotSubscribed( final SubscriptionService subscriptionService,
                                          final ChannelAddress address,
                                          final Class<A> type,
                                          final Object id )
  {
    boolean found;
    try
    {
      final Subscription subscription = subscriptionService.findSubscription( address );
      assertNotNull( subscription );
      assertNotNull( subscription.findEntityByTypeAndId( type, id ) );
      found = true;
    }
    catch ( final Throwable t )
    {
      found = false;
    }
    assertFalse( found, "Found subscription unexpectedly" );
  }

  private <T> void updateEntity( @Nonnull final EntitySubscriptionManager sm,
                                 @Nonnull final SubscriptionService ss,
                                 @Nonnull final Class<T> type,
                                 @Nonnull final Object id,
                                 @Nonnull final ChannelAddress[] channels,
                                 @Nonnull final T userObject )
  {
    final Entity entity = sm.findOrCreateEntity( type, id );
    entity.setUserObject( userObject );
    for ( final ChannelAddress channel : channels )
    {
      final Subscription subscription = ss.findSubscription( channel );
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
