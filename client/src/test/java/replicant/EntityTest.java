package replicant;

import arez.Disposable;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nonnull;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class EntityTest
  extends AbstractReplicantTest
{
  @Test
  public void basicConstruction()
  {
    final var entityService = Replicant.context().getEntityService();

    final var type = A.class;
    final var id = ValueUtil.randomInt();
    final var name = "A/" + id;
    final var entity = safeAction( () -> entityService.findOrCreateEntity( name, type, id ) );

    assertEquals( entity.getName(), name );
    assertEquals( entity.getType(), type );
    assertEquals( entity.getId(), id );

    safeAction( () -> assertNull( entity.maybeUserObject() ) );
    safeAction( () -> assertEquals( entity.getSubscriptions().size(), 0 ) );
  }

  @Test
  public void toStringTest()
  {
    final var entityService = Replicant.context().getEntityService();

    final var type = A.class;
    final var id = 123;
    final var name = "A/123";
    final var entity = safeAction( () -> entityService.findOrCreateEntity( name, type, id ) );

    assertEquals( entity.toString(), name );
    ReplicantTestUtil.disableNames();

    assertEquals( entity.toString(), entity.getClass().getName() + "@" + Integer.toHexString( entity.hashCode() ) );
  }

  @Test
  public void namePassedToConstructorWhenNamesDisabled()
  {
    final var entityService = Replicant.context().getEntityService();

    ReplicantTestUtil.disableNames();

    final var exception =
      expectThrows( IllegalStateException.class,
                    () -> safeAction( () -> entityService.findOrCreateEntity( "A/123",
                                                                              A.class,
                                                                              123 ) ) );

    assertEquals( exception.getMessage(),
                  "Replicant-0032: Entity passed a name 'A/123' but Replicant.areNamesEnabled() is false" );
  }

  @Test
  public void getNameInvokedWhenNamesDisabled()
  {
    final var entityService = Replicant.context().getEntityService();

    ReplicantTestUtil.disableNames();

    final var entity = safeAction( () -> entityService.findOrCreateEntity( null, A.class, 123 ) );

    final var exception =
      expectThrows( IllegalStateException.class, () -> safeAction( entity::getName ) );

    assertEquals( exception.getMessage(),
                  "Replicant-0009: Entity.getName() invoked when Replicant.areNamesEnabled() is false" );
  }

  @Test
  public void userObject()
  {
    final var entityService = Replicant.context().getEntityService();

    final var entity =
      safeAction( () -> entityService.findOrCreateEntity( ValueUtil.randomString(),
                                                          A.class,
                                                          ValueUtil.randomInt() ) );

    safeAction( () -> assertNull( entity.maybeUserObject() ) );

    final var userObject = new A();
    safeAction( () -> entity.setUserObject( userObject ) );
    safeAction( () -> assertEquals( entity.maybeUserObject(), userObject ) );
    safeAction( () -> assertEquals( entity.getUserObject(), userObject ) );
  }

  @Test
  public void getUserObject_whenNull()
  {
    final var entityService = Replicant.context().getEntityService();

    final var entity =
      safeAction( () -> entityService.findOrCreateEntity( ValueUtil.randomString(),
                                                          A.class,
                                                          ValueUtil.randomInt() ) );

    final var exception =
      expectThrows( IllegalStateException.class,
                    () -> safeAction( () -> assertNull( entity.getUserObject() ) ) );

    assertEquals( exception.getMessage(), "Replicant-0071: Entity.getUserObject() invoked when no userObject present" );
  }

  @Test
  public void typeSubscriptions()
  {
    final var entityService = Replicant.context().getEntityService();

    final var entity =
      safeAction( () -> entityService.findOrCreateEntity( ValueUtil.randomString(),
                                                          String.class,
                                                          ValueUtil.randomInt() ) );

    final var subscription1 = createSubscription( new ChannelAddress( 1, 0 ) );
    final var subscription2 = createSubscription( new ChannelAddress( 1, 1 ) );

    final var callCount = new AtomicInteger();
    observer( () -> {
      if ( Disposable.isNotDisposed( entity ) )
      {
        // Access observable next line
        entity.getSubscriptions();
      }
      callCount.incrementAndGet();
    } );

    assertEquals( callCount.get(), 1 );
    safeAction( () -> assertEquals( entity.getSubscriptions().size(), 0 ) );

    // Add initial subscription
    {
      safeAction( () -> entity.linkToSubscription( subscription1 ) );

      assertEquals( callCount.get(), 2 );
      safeAction( () -> assertEquals( entity.getSubscriptions().size(), 1 ) );
    }

    // Add second subscription and thus get notified
    {
      safeAction( () -> entity.linkToSubscription( subscription2 ) );

      assertEquals( callCount.get(), 3 );
      safeAction( () -> assertEquals( entity.getSubscriptions().size(), 2 ) );
    }

    // Remove subscription and thus get notified
    {
      safeAction( () -> entity.delinkFromSubscription( subscription2 ) );

      assertEquals( callCount.get(), 4 );
      safeAction( () -> assertEquals( entity.getSubscriptions().size(), 1 ) );
    }

    // Remove last subscription and thus get notified, also entity should now be disposed
    {
      safeAction( () -> entity.delinkFromSubscription( subscription1 ) );

      assertEquals( callCount.get(), 5 );
      assertTrue( Disposable.isDisposed( entity ) );
      // Have to access test-only method as entity is disposed
      assertEquals( entity.subscriptions().size(), 0 );
    }
  }

  @Test
  public void instanceSubscriptions()
  {
    final var entityService = Replicant.context().getEntityService();

    final var entity =
      safeAction( () -> entityService.findOrCreateEntity( ValueUtil.randomString(),
                                                          String.class,
                                                          ValueUtil.randomInt() ) );

    final var subscription1 = createSubscription( new ChannelAddress( 1, 0, 1 ) );
    final var subscription2 = createSubscription( new ChannelAddress( 1, 0, 2 ) );

    final var callCount = new AtomicInteger();
    observer( () -> {
      if ( Disposable.isNotDisposed( entity ) )
      {
        // Access observable next line
        entity.getSubscriptions();
      }
      callCount.incrementAndGet();
    } );

    assertEquals( callCount.get(), 1 );
    safeAction( () -> assertEquals( entity.getSubscriptions().size(), 0 ) );

    // Add initial subscription
    {
      safeAction( () -> entity.linkToSubscription( subscription1 ) );

      assertEquals( callCount.get(), 2 );
      safeAction( () -> assertEquals( entity.getSubscriptions().size(), 1 ) );
    }

    // Add second subscription and thus get notified
    // second subscription is of the same channelType, so should go through second path
    {
      safeAction( () -> entity.linkToSubscription( subscription2 ) );

      assertEquals( callCount.get(), 3 );
      safeAction( () -> assertEquals( entity.getSubscriptions().size(), 2 ) );
    }

    // Remove subscription and thus get notified
    {
      safeAction( () -> entity.delinkFromSubscription( subscription2 ) );

      assertEquals( callCount.get(), 4 );
      safeAction( () -> assertEquals( entity.getSubscriptions().size(), 1 ) );
    }

    // Remove last subscription and thus get notified, also entity should now be disposed
    {
      safeAction( () -> entity.delinkFromSubscription( subscription1 ) );

      assertEquals( callCount.get(), 5 );
      assertTrue( Disposable.isDisposed( entity ) );
      // Have to access test-only method as entity is disposed
      assertEquals( entity.subscriptions().size(), 0 );
    }
  }

  @Test
  public void delinkFromSubscription_whenNotLinked()
  {
    final var entityService = Replicant.context().getEntityService();

    final var entity =
      safeAction( () -> entityService.findOrCreateEntity( "MyEntity",
                                                          String.class,
                                                          ValueUtil.randomInt() ) );

    final var subscription1 = createSubscription( new ChannelAddress( 1, 0, 1 ) );

    final var exception =
      expectThrows( IllegalStateException.class,
                    () -> safeAction( () -> entity.delinkFromSubscription( subscription1 ) ) );
    assertEquals( exception.getMessage(),
                  "Replicant-0081: Entity.delinkFromSubscription invoked on Entity MyEntity passing subscription 1.0.1 but entity is not linked to subscription." );
  }

  @Test
  public void linkToSubscription_whenNotLinked()
  {
    final var entityService = Replicant.context().getEntityService();

    final var entity =
      safeAction( () -> entityService.findOrCreateEntity( "MyEntity", String.class, ValueUtil.randomInt() ) );

    final var subscription1 = createSubscription( new ChannelAddress( 1, 0, 1 ) );

    safeAction( () -> entity.linkToSubscription( subscription1 ) );

    final var exception =
      expectThrows( IllegalStateException.class,
                    () -> safeAction( () -> entity.linkToSubscription( subscription1 ) ) );
    assertEquals( exception.getMessage(),
                  "Replicant-0080: Entity.linkToSubscription invoked on Entity MyEntity passing subscription 1.0.1 but entity is already linked to subscription." );
  }

  @Test
  public void tryLinkToSubscription()
  {
    final var entityService = Replicant.context().getEntityService();

    final var entity =
      safeAction( () -> entityService.findOrCreateEntity( "MyEntity", String.class, ValueUtil.randomInt() ) );

    final var subscription1 = createSubscription( new ChannelAddress( 1, 0, 1 ) );

    safeAction( () -> entity.tryLinkToSubscription( subscription1 ) );

    safeAction( () -> assertEquals( entity.getSubscriptions().size(), 1 ) );

    // Should perform no action
    entity.tryLinkToSubscription( subscription1 );

    safeAction( () -> assertEquals( entity.getSubscriptions().size(), 1 ) );
  }

  @Test
  public void disposeRemovesEntityFromSubscriptions()
  {
    final var entityService = Replicant.context().getEntityService();

    final var type = A.class;
    final var id = ValueUtil.randomInt();
    final var name = "A/" + id;
    final var entity = safeAction( () -> entityService.findOrCreateEntity( name, type, id ) );

    final var subscription1 = createSubscription( new ChannelAddress( 1, 0, 1 ) );
    final var subscription2 = createSubscription( new ChannelAddress( 1, 0, 2 ) );

    safeAction( () -> assertEquals( entity.getSubscriptions().size(), 0 ) );
    safeAction( () -> entity.linkToSubscription( subscription1 ) );
    safeAction( () -> entity.linkToSubscription( subscription2 ) );
    safeAction( () -> assertEquals( entity.getSubscriptions().size(), 2 ) );

    safeAction( () -> assertEquals( subscription1.findEntityByTypeAndId( type, id ), entity ) );
    safeAction( () -> assertEquals( subscription2.findEntityByTypeAndId( type, id ), entity ) );

    Disposable.dispose( entity );

    safeAction( () -> assertEquals( subscription1.findAllEntitiesByType( type ).size(), 0 ) );
    safeAction( () -> assertEquals( subscription2.findAllEntitiesByType( type ).size(), 0 ) );
  }

  @Test
  public void disposeWillDisposeUserObject()
  {
    final var entityService = Replicant.context().getEntityService();

    final var entity =
      safeAction( () -> entityService.findOrCreateEntity( ValueUtil.randomString(),
                                                          A.class,
                                                          ValueUtil.randomInt() ) );
    final var userObject = new A();
    safeAction( () -> entity.setUserObject( userObject ) );

    assertFalse( Disposable.isDisposed( entity ) );
    assertFalse( userObject.isDisposed() );

    Disposable.dispose( entity );

    assertTrue( Disposable.isDisposed( entity ) );
    assertTrue( userObject.isDisposed() );
  }

  @Test
  public void getSubscriptions_mutability()
  {
    final var entityService = Replicant.context().getEntityService();

    final var entity =
      safeAction( () -> entityService.findOrCreateEntity( ValueUtil.randomString(),
                                                          A.class,
                                                          ValueUtil.randomInt() ) );

    final var subscription1 = createSubscription();

    expectThrows( UnsupportedOperationException.class,
                  () -> safeAction( () -> entity.getSubscriptions().add( subscription1 ) ) );
  }

  @Test
  public void delinkSubscriptionFromEntity_whenSubscriptionMissing()
  {
    final var entityService = Replicant.context().getEntityService();

    final var entity =
      safeAction( () -> entityService.findOrCreateEntity( "A/123", A.class, 123 ) );

    final var subscription1 = createSubscription( new ChannelAddress( 1, 0, 1 ) );

    safeAction( () -> assertEquals( entity.getSubscriptions().size(), 0 ) );
    safeAction( () -> entity.linkToSubscription( subscription1 ) );
    safeAction( () -> assertEquals( entity.getSubscriptions().size(), 1 ) );

    entity.subscriptions().remove( subscription1.address() );

    final var exception =
      expectThrows( IllegalStateException.class,
                    () -> safeAction( () -> entity.delinkSubscriptionFromEntity( subscription1 ) ) );
    assertEquals( exception.getMessage(), "Unable to locate subscription for channel 1.0.1 on entity A/123" );
  }

  @Nonnull
  private Subscription createSubscription()
  {
    return createSubscription( new ChannelAddress( 1, 0 ) );
  }

  @Nonnull
  private Subscription createSubscription( @Nonnull final ChannelAddress address )
  {
    return Subscription.create( null, address, null, true );
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
