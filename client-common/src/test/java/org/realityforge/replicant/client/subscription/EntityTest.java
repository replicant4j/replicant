package org.realityforge.replicant.client.subscription;

import arez.Arez;
import arez.ArezTestUtil;
import arez.Disposable;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nonnull;
import org.realityforge.guiceyloops.shared.ValueUtil;
import org.realityforge.replicant.client.AbstractReplicantTest;
import org.realityforge.replicant.client.Channel;
import org.realityforge.replicant.client.ChannelAddress;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class EntityTest
  extends AbstractReplicantTest
{
  @Test
  public void basicConstruction()
  {
    final EntityService entityService = EntityService.create();

    final Class<A> type = A.class;
    final String id = ValueUtil.randomString();
    final Entity entity = Arez.context().safeAction( () -> entityService.findOrCreateEntity( type, id ) );

    assertEquals( entity.getType(), type );
    assertEquals( entity.getId(), id );

    Arez.context().safeAction( () -> assertEquals( entity.getUserObject(), null ) );
    Arez.context().safeAction( () -> assertEquals( entity.getSubscriptions().size(), 0 ) );
  }

  @Test
  public void toStringTest()
  {
    final EntityService entityService = EntityService.create();

    final Class<A> type = A.class;
    final String id = "123";
    final Entity entity = Arez.context().safeAction( () -> entityService.findOrCreateEntity( type, id ) );

    assertEquals( entity.toString(), "A/123" );
    ArezTestUtil.disableNames();

    assertEquals( entity.toString(), entity.getClass().getName() + "@" + Integer.toHexString( entity.hashCode() ) );
  }

  @Test
  public void userObject()
  {
    final EntityService entityService = EntityService.create();

    final Class<A> type = A.class;
    final String id = ValueUtil.randomString();
    final Entity entity = Arez.context().safeAction( () -> entityService.findOrCreateEntity( type, id ) );

    Arez.context().safeAction( () -> assertEquals( entity.getUserObject(), null ) );

    final A userObject = new A();
    Arez.context().safeAction( () -> entity.setUserObject( userObject ) );
    Arez.context().safeAction( () -> assertEquals( entity.getUserObject(), userObject ) );
  }

  @Test
  public void userObjectBadType()
  {
    final EntityService entityService = EntityService.create();

    final Class<A> type = A.class;
    final String id = "1234";
    final Entity entity = Arez.context().safeAction( () -> entityService.findOrCreateEntity( type, id ) );

    Arez.context().safeAction( () -> assertEquals( entity.getUserObject(), null ) );

    final Object userObject = new Object();
    final IllegalStateException exception =
      expectThrows( IllegalStateException.class,
                    () -> Arez.context().safeAction( () -> entity.setUserObject( userObject ) ) );
    assertEquals( exception.getMessage(),
                  "Entity A/1234 specified non-null userObject of type java.lang.Object but the entity expected type org.realityforge.replicant.client.subscription.EntityTest$A" );
  }

  @Test
  public void typeSubscriptions()
  {
    final EntityService entityService = EntityService.create();

    final Class<String> type = String.class;
    final String id = ValueUtil.randomString();
    final Entity entity = Arez.context().safeAction( () -> entityService.findOrCreateEntity( type, id ) );

    final Subscription subscription1 = createSubscription( new ChannelAddress( G.G1 ) );
    final Subscription subscription2 = createSubscription( new ChannelAddress( G.G2 ) );

    final AtomicInteger callCount = new AtomicInteger();
    Arez.context().autorun( () -> {
      if ( !Disposable.isDisposed( entity ) )
      {
        // Access observable next line
        entity.getSubscriptions();
      }
      callCount.incrementAndGet();
    } );

    assertEquals( callCount.get(), 1 );
    Arez.context().safeAction( () -> assertEquals( entity.getSubscriptions().size(), 0 ) );

    // Add initial subscription
    {
      Arez.context().safeAction( () -> entity.linkToSubscription( subscription1 ) );

      assertEquals( callCount.get(), 2 );
      Arez.context().safeAction( () -> assertEquals( entity.getSubscriptions().size(), 1 ) );
    }

    // Add same subscription so no notification or change
    {
      Arez.context().safeAction( () -> entity.linkToSubscription( subscription1 ) );

      assertEquals( callCount.get(), 2 );
      Arez.context().safeAction( () -> assertEquals( entity.getSubscriptions().size(), 1 ) );
    }

    // Add second subscription and thus get notified
    {
      Arez.context().safeAction( () -> entity.linkToSubscription( subscription2 ) );

      assertEquals( callCount.get(), 3 );
      Arez.context().safeAction( () -> assertEquals( entity.getSubscriptions().size(), 2 ) );
    }

    // Remove subscription and thus get notified
    {
      Arez.context().safeAction( () -> entity.delinkFromSubscription( subscription2 ) );

      assertEquals( callCount.get(), 4 );
      Arez.context().safeAction( () -> assertEquals( entity.getSubscriptions().size(), 1 ) );
    }

    // Remove last subscription and thus get notified, also entity should now be disposed
    {
      Arez.context().safeAction( () -> entity.delinkFromSubscription( subscription1 ) );

      assertEquals( callCount.get(), 5 );
      assertEquals( Disposable.isDisposed( entity ), true );
      // Have to access test-only method as entity is disposed
      assertEquals( entity.subscriptions().size(), 0 );
    }
  }

  @Test
  public void instanceSubscriptions()
  {
    final EntityService entityService = EntityService.create();

    final Class<String> type = String.class;
    final String id = ValueUtil.randomString();
    final Entity entity = Arez.context().safeAction( () -> entityService.findOrCreateEntity( type, id ) );

    final Subscription subscription1 = createSubscription( new ChannelAddress( G.G1, 1 ) );
    final Subscription subscription2 = createSubscription( new ChannelAddress( G.G1, 2 ) );

    final AtomicInteger callCount = new AtomicInteger();
    Arez.context().autorun( () -> {
      if ( !Disposable.isDisposed( entity ) )
      {
        // Access observable next line
        entity.getSubscriptions();
      }
      callCount.incrementAndGet();
    } );

    assertEquals( callCount.get(), 1 );
    Arez.context().safeAction( () -> assertEquals( entity.getSubscriptions().size(), 0 ) );

    // Add initial subscription
    {
      Arez.context().safeAction( () -> entity.linkToSubscription( subscription1 ) );

      assertEquals( callCount.get(), 2 );
      Arez.context().safeAction( () -> assertEquals( entity.getSubscriptions().size(), 1 ) );
    }

    // Add second subscription and thus get notified
    // second subscription is of the same channelType, so should go through second path
    {
      Arez.context().safeAction( () -> entity.linkToSubscription( subscription2 ) );

      assertEquals( callCount.get(), 3 );
      Arez.context().safeAction( () -> assertEquals( entity.getSubscriptions().size(), 2 ) );
    }

    // Add same subscription again and thus not notified
    {
      Arez.context().safeAction( () -> entity.linkToSubscription( subscription2 ) );

      assertEquals( callCount.get(), 3 );
      Arez.context().safeAction( () -> assertEquals( entity.getSubscriptions().size(), 2 ) );
    }

    // Remove subscription and thus get notified
    {
      Arez.context().safeAction( () -> entity.delinkFromSubscription( subscription2 ) );

      assertEquals( callCount.get(), 4 );
      Arez.context().safeAction( () -> assertEquals( entity.getSubscriptions().size(), 1 ) );
    }

    // Remove last subscription and thus get notified, also entity should now be disposed
    {
      Arez.context().safeAction( () -> entity.delinkFromSubscription( subscription1 ) );

      assertEquals( callCount.get(), 5 );
      assertEquals( Disposable.isDisposed( entity ), true );
      // Have to access test-only method as entity is disposed
      assertEquals( entity.subscriptions().size(), 0 );
    }
  }

  @Test
  public void disposeRemovesEntityFromSubscriptions()
  {
    final EntityService entityService = EntityService.create();

    final Class<A> type = A.class;
    final String id = ValueUtil.randomString();
    final Entity entity = Arez.context().safeAction( () -> entityService.findOrCreateEntity( type, id ) );

    final Subscription subscription1 = createSubscription( new ChannelAddress( G.G1, 1 ) );
    final Subscription subscription2 = createSubscription( new ChannelAddress( G.G1, 2 ) );

    Arez.context().safeAction( () -> assertEquals( entity.getSubscriptions().size(), 0 ) );
    Arez.context().safeAction( () -> entity.linkToSubscription( subscription1 ) );
    Arez.context().safeAction( () -> entity.linkToSubscription( subscription2 ) );
    Arez.context().safeAction( () -> assertEquals( entity.getSubscriptions().size(), 2 ) );

    Arez.context().safeAction( () -> assertEquals( subscription1.findEntityByTypeAndId( type, id ), entity ) );
    Arez.context().safeAction( () -> assertEquals( subscription2.findEntityByTypeAndId( type, id ), entity ) );

    Disposable.dispose( entity );

    Arez.context().safeAction( () -> assertEquals( subscription1.findAllEntitiesByType( type ).size(), 0 ) );
    Arez.context().safeAction( () -> assertEquals( subscription2.findAllEntitiesByType( type ).size(), 0 ) );
  }

  @Test
  public void disposeWillDisposeUserObject()
  {
    final EntityService entityService = EntityService.create();

    final Class<A> type = A.class;
    final String id = ValueUtil.randomString();
    final Entity entity = Arez.context().safeAction( () -> entityService.findOrCreateEntity( type, id ) );
    final A userObject = new A();
    Arez.context().safeAction( () -> entity.setUserObject( userObject ) );

    assertFalse( Disposable.isDisposed( entity ) );
    assertFalse( userObject.isDisposed() );

    Disposable.dispose( entity );

    assertTrue( Disposable.isDisposed( entity ) );
    assertTrue( userObject.isDisposed() );
  }

  @Test
  public void getSubscriptions_mutability()
  {
    final EntityService entityService = EntityService.create();

    final Entity entity =
      Arez.context().safeAction( () -> entityService.findOrCreateEntity( A.class, ValueUtil.randomString() ) );

    final Subscription subscription1 = createSubscription();

    expectThrows( UnsupportedOperationException.class,
                  () -> Arez.context().safeAction( () -> entity.getSubscriptions().add( subscription1 ) ) );
  }

  @Test
  public void delinkSubscriptionFromEntity_whenSubscriptionMissing()
  {
    final EntityService entityService = EntityService.create();

    final Entity entity = Arez.context().safeAction( () -> entityService.findOrCreateEntity( A.class, "123" ) );

    final Subscription subscription1 = createSubscription( new ChannelAddress( G.G1, 1 ) );

    Arez.context().safeAction( () -> assertEquals( entity.getSubscriptions().size(), 0 ) );
    Arez.context().safeAction( () -> entity.linkToSubscription( subscription1 ) );
    Arez.context().safeAction( () -> assertEquals( entity.getSubscriptions().size(), 1 ) );

    entity.subscriptions().remove( subscription1.getChannel().getAddress() );

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class,
                    () -> Arez.context().safeAction( () -> entity.delinkSubscriptionFromEntity( subscription1 ) ) );
    assertEquals( exception.getMessage(), "Unable to locate subscription for channel G.G1:1 on entity A/123" );
  }

  @Nonnull
  private Subscription createSubscription()
  {
    return createSubscription( new ChannelAddress( G.G1 ) );
  }

  @Nonnull
  private Subscription createSubscription( @Nonnull final ChannelAddress address )
  {
    return Subscription.create( Channel.create( address ) );
  }

  enum G
  {
    G1, G2
  }

  static class A
    implements Disposable
  {
    private boolean _disposed;

    @Override
    public void dispose()
    {
      _disposed = true;
    }

    @Override
    public boolean isDisposed()
    {
      return _disposed;
    }
  }
}
