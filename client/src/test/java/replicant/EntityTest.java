package replicant;

import arez.Arez;
import arez.Disposable;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nonnull;
import org.realityforge.guiceyloops.shared.ValueUtil;
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
    final int id = ValueUtil.randomInt();
    final String name = "A/" + id;
    final Entity entity = Arez.context().safeAction( () -> entityService.findOrCreateEntity( name, type, id ) );

    assertEquals( entity.getName(), name );
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
    final int id = 123;
    final String name = "A/123";
    final Entity entity = Arez.context().safeAction( () -> entityService.findOrCreateEntity( name, type, id ) );

    assertEquals( entity.toString(), name );
    ReplicantTestUtil.disableNames();

    assertEquals( entity.toString(), entity.getClass().getName() + "@" + Integer.toHexString( entity.hashCode() ) );
  }

  @Test
  public void namePassedToConstructorWhenNamesDisabled()
  {
    final EntityService entityService = EntityService.create();

    ReplicantTestUtil.disableNames();

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class,
                    () -> Arez.context().safeAction( () -> entityService.findOrCreateEntity( "A/123",
                                                                                             A.class,
                                                                                             123 ) ) );

    assertEquals( exception.getMessage(),
                  "Replicant-0032: Entity passed a name 'A/123' but Replicant.areNamesEnabled() is false" );
  }

  @Test
  public void getNameInvokedWhenNamesDisabled()
  {
    final EntityService entityService = EntityService.create();

    ReplicantTestUtil.disableNames();

    final Entity entity = Arez.context().safeAction( () -> entityService.findOrCreateEntity( null, A.class, 123 ) );

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class, () -> Arez.context().safeAction( entity::getName ) );

    assertEquals( exception.getMessage(),
                  "Replicant-0009: Entity.getName() invoked when Replicant.areNamesEnabled() is false" );
  }

  @Test
  public void userObject()
  {
    final EntityService entityService = EntityService.create();

    final Entity entity =
      Arez.context().safeAction( () -> entityService.findOrCreateEntity( ValueUtil.randomString(),
                                                                         A.class,
                                                                         ValueUtil.randomInt() ) );

    Arez.context().safeAction( () -> assertEquals( entity.getUserObject(), null ) );

    final A userObject = new A();
    Arez.context().safeAction( () -> entity.setUserObject( userObject ) );
    Arez.context().safeAction( () -> assertEquals( entity.getUserObject(), userObject ) );
  }

  @Test
  public void typeSubscriptions()
  {
    final EntityService entityService = EntityService.create();

    final Entity entity =
      Arez.context().safeAction( () -> entityService.findOrCreateEntity( ValueUtil.randomString(),
                                                                         String.class,
                                                                         ValueUtil.randomInt() ) );

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

    final Entity entity =
      Arez.context().safeAction( () -> entityService.findOrCreateEntity( ValueUtil.randomString(),
                                                                         String.class,
                                                                         ValueUtil.randomInt() ) );

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
    final int id = ValueUtil.randomInt();
    final String name = "A/" + id;
    final Entity entity = Arez.context().safeAction( () -> entityService.findOrCreateEntity( name, type, id ) );

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

    final Entity entity =
      Arez.context().safeAction( () -> entityService.findOrCreateEntity( ValueUtil.randomString(),
                                                                         A.class,
                                                                         ValueUtil.randomInt() ) );
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
      Arez.context().safeAction( () -> entityService.findOrCreateEntity( ValueUtil.randomString(),
                                                                         A.class,
                                                                         ValueUtil.randomInt() ) );

    final Subscription subscription1 = createSubscription();

    expectThrows( UnsupportedOperationException.class,
                  () -> Arez.context().safeAction( () -> entity.getSubscriptions().add( subscription1 ) ) );
  }

  @Test
  public void delinkSubscriptionFromEntity_whenSubscriptionMissing()
  {
    final EntityService entityService = EntityService.create();

    final Entity entity =
      Arez.context().safeAction( () -> entityService.findOrCreateEntity( "A/123", A.class, 123 ) );

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

  @Test
  public void postDispose_whenSubscriptionsExist()
  {
    final EntityService entityService = EntityService.create();

    final Entity entity =
      Arez.context().safeAction( () -> entityService.findOrCreateEntity( "A/123", A.class, 123 ) );

    final Subscription subscription = createSubscription( new ChannelAddress( G.G1, 1 ) );

    Arez.context().safeAction( () -> entity.linkToSubscription( subscription ) );
    Arez.context().safeAction( () -> assertEquals( entity.getSubscriptions().size(), 1 ) );

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class, () -> Arez.context().safeAction( entity::postDispose ) );
    assertEquals( exception.getMessage(),
                  "Entity A/123 was disposed in non-standard way that left 1 subscriptions linked." );

    Arez.context().safeAction( () -> assertEquals( entity.getSubscriptions().size(), 1 ) );
  }

  @Nonnull
  private Subscription createSubscription()
  {
    return createSubscription( new ChannelAddress( G.G1 ) );
  }

  @Nonnull
  private Subscription createSubscription( @Nonnull final ChannelAddress address )
  {
    return Subscription.create( SubscriptionService.create(), Channel.create( address ) );
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
