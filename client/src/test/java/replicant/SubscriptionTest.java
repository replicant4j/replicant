package replicant;

import arez.Arez;
import arez.Disposable;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nonnull;
import org.realityforge.guiceyloops.shared.ValueUtil;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class SubscriptionTest
  extends AbstractReplicantTest
{
  @Test
  public void basicConstruction()
  {
    final Channel channel = Channel.create( new ChannelAddress( G.G1 ) );
    final Subscription subscription = Subscription.create( channel, true );

    assertEquals( subscription.getChannel(), channel );

    Arez.context().safeAction( () -> assertEquals( subscription.isExplicitSubscription(), true ) );
    Arez.context().safeAction( () -> assertEquals( subscription.getEntities().size(), 0 ) );
  }

  @Test
  public void entities()
  {
    final EntityService entityService = EntityService.create();
    final Entity entity1 = Arez.context().safeAction( () -> entityService.findOrCreateEntity( "A/1", A.class, "1" ) );
    final Entity entity2 = Arez.context().safeAction( () -> entityService.findOrCreateEntity( "A/2", A.class, "2" ) );

    final Subscription subscription = Subscription.create( Channel.create( new ChannelAddress( G.G1, 1 ) ) );

    final AtomicInteger callCount = new AtomicInteger();
    Arez.context().autorun( () -> {
      // Just invoke method to get observing
      //noinspection ResultOfMethodCallIgnored
      subscription.getEntities();
      callCount.incrementAndGet();
    } );

    final AtomicInteger findCallCount = new AtomicInteger();
    Arez.context().autorun( () -> {
      // Just invoke method to get observing
      subscription.findEntityByTypeAndId( A.class, "1" );
      findCallCount.incrementAndGet();
    } );

    assertEquals( callCount.get(), 1 );
    assertEquals( findCallCount.get(), 1 );
    Arez.context().safeAction( () -> assertEquals( subscription.findAllEntityTypes().size(), 0 ) );
    Arez.context().safeAction( () -> assertEquals( subscription.findAllEntityTypes().contains( A.class ), false ) );
    Arez.context().safeAction( () -> assertEquals( subscription.findAllEntitiesByType( A.class ).size(), 0 ) );
    Arez.context().safeAction( () -> assertEquals( subscription.findAllEntitiesByType( String.class ).size(), 0 ) );
    Arez.context().safeAction( () -> assertEquals( subscription.findEntityByTypeAndId( A.class, "1" ), null ) );
    Arez.context().safeAction( () -> assertEquals( subscription.findEntityByTypeAndId( A.class, "2" ), null ) );

    Arez.context().safeAction( () -> subscription.linkSubscriptionToEntity( entity1 ) );

    assertEquals( callCount.get(), 2 );
    assertEquals( findCallCount.get(), 2 );
    Arez.context().safeAction( () -> assertEquals( subscription.findAllEntityTypes().size(), 1 ) );
    Arez.context().safeAction( () -> assertEquals( subscription.findAllEntityTypes().contains( A.class ), true ) );
    Arez.context().safeAction( () -> assertEquals( subscription.findAllEntitiesByType( A.class ).size(), 1 ) );
    Arez.context().safeAction( () -> assertEquals( subscription.findAllEntitiesByType( String.class ).size(), 0 ) );
    Arez.context().safeAction( () -> assertEquals( subscription.findEntityByTypeAndId( A.class, "1" ), entity1 ) );
    Arez.context().safeAction( () -> assertEquals( subscription.findEntityByTypeAndId( A.class, "2" ), null ) );

    // Add second entity, finder no need to re-find
    Arez.context().safeAction( () -> subscription.linkSubscriptionToEntity( entity2 ) );

    assertEquals( callCount.get(), 3 );
    assertEquals( findCallCount.get(), 2 );
    Arez.context().safeAction( () -> assertEquals( subscription.findAllEntityTypes().size(), 1 ) );
    Arez.context().safeAction( () -> assertEquals( subscription.findAllEntityTypes().contains( A.class ), true ) );
    Arez.context().safeAction( () -> assertEquals( subscription.findAllEntitiesByType( A.class ).size(), 2 ) );
    Arez.context().safeAction( () -> assertEquals( subscription.findAllEntitiesByType( String.class ).size(), 0 ) );
    Arez.context().safeAction( () -> assertEquals( subscription.findEntityByTypeAndId( A.class, "1" ), entity1 ) );
    Arez.context().safeAction( () -> assertEquals( subscription.findEntityByTypeAndId( A.class, "2" ), entity2 ) );

    // Duplicate link ... ignored as no change
    Arez.context().safeAction( () -> subscription.linkSubscriptionToEntity( entity2 ) );

    assertEquals( callCount.get(), 3 );
    assertEquals( findCallCount.get(), 2 );
    Arez.context().safeAction( () -> assertEquals( subscription.findAllEntityTypes().size(), 1 ) );
    Arez.context().safeAction( () -> assertEquals( subscription.findAllEntitiesByType( A.class ).size(), 2 ) );
    Arez.context().safeAction( () -> assertEquals( subscription.findAllEntitiesByType( String.class ).size(), 0 ) );
    Arez.context().safeAction( () -> assertEquals( subscription.findEntityByTypeAndId( A.class, "1" ), entity1 ) );
    Arez.context().safeAction( () -> assertEquals( subscription.findEntityByTypeAndId( A.class, "2" ), entity2 ) );

    // Removing entity 1, finder will react
    Arez.context().safeAction( () -> subscription.delinkEntityFromSubscription( entity1 ) );

    assertEquals( callCount.get(), 4 );
    assertEquals( findCallCount.get(), 3 );
    Arez.context().safeAction( () -> assertEquals( subscription.findAllEntityTypes().size(), 1 ) );
    Arez.context().safeAction( () -> assertEquals( subscription.findAllEntitiesByType( A.class ).size(), 1 ) );
    Arez.context().safeAction( () -> assertEquals( subscription.findAllEntitiesByType( String.class ).size(), 0 ) );
    Arez.context().safeAction( () -> assertEquals( subscription.findEntityByTypeAndId( A.class, "1" ), null ) );
    Arez.context().safeAction( () -> assertEquals( subscription.findEntityByTypeAndId( A.class, "2" ), entity2 ) );

    // Removing entity 2, state is reset
    Arez.context().safeAction( () -> subscription.delinkEntityFromSubscription( entity2 ) );

    assertEquals( callCount.get(), 5 );
    assertEquals( findCallCount.get(), 4 );
    Arez.context().safeAction( () -> assertEquals( subscription.findAllEntityTypes().size(), 0 ) );
    Arez.context().safeAction( () -> assertEquals( subscription.findAllEntitiesByType( A.class ).size(), 0 ) );
    Arez.context().safeAction( () -> assertEquals( subscription.findAllEntitiesByType( String.class ).size(), 0 ) );
    Arez.context().safeAction( () -> assertEquals( subscription.findEntityByTypeAndId( A.class, "1" ), null ) );
    Arez.context().safeAction( () -> assertEquals( subscription.findEntityByTypeAndId( A.class, "2" ), null ) );
  }

  @Test
  public void dispose_delinksFromEntity()
  {
    final EntityService entityService = EntityService.create();
    final Entity entity1 = Arez.context().safeAction( () -> entityService.findOrCreateEntity( "A/1", A.class, "1" ) );
    final Entity entity2 = Arez.context().safeAction( () -> entityService.findOrCreateEntity( "A/2", A.class, "2" ) );

    final Subscription subscription1 = Subscription.create( Channel.create( new ChannelAddress( G.G1, 1 ) ) );
    final Subscription subscription2 = Subscription.create( Channel.create( new ChannelAddress( G.G1, 2 ) ) );

    Arez.context().safeAction( () -> entity1.linkToSubscription( subscription1 ) );
    Arez.context().safeAction( () -> entity2.linkToSubscription( subscription1 ) );
    Arez.context().safeAction( () -> entity1.linkToSubscription( subscription2 ) );

    Arez.context().safeAction( () -> assertEquals( subscription1.findAllEntityTypes().size(), 1 ) );
    Arez.context().safeAction( () -> assertEquals( subscription2.findAllEntityTypes().size(), 1 ) );
    Arez.context().safeAction( () -> assertEquals( entity1.getSubscriptions().size(), 2 ) );

    assertFalse( Disposable.isDisposed( subscription1 ) );
    assertFalse( Disposable.isDisposed( subscription2 ) );
    assertFalse( Disposable.isDisposed( entity1 ) );
    assertFalse( Disposable.isDisposed( entity2 ) );

    Disposable.dispose( subscription1 );

    assertTrue( Disposable.isDisposed( subscription1 ) );
    assertFalse( Disposable.isDisposed( subscription2 ) );
    // entity2 is associated with subscription2 so it stays
    assertFalse( Disposable.isDisposed( entity1 ) );
    // entity2 had no other subscriptions so it went away
    assertTrue( Disposable.isDisposed( entity2 ) );
  }

  @Test
  public void delinkEntityFromChannel_noSuchType()
  {
    final EntityService entityService = EntityService.create();
    final Entity entity =
      Arez.context()
        .safeAction( () -> entityService.findOrCreateEntity( ValueUtil.randomString(),
                                                             A.class,
                                                             ValueUtil.randomString() ) );

    final Subscription subscription1 = createSubscription( new ChannelAddress( EntityTest.G.G1, 1 ) );

    entity.subscriptions().put( subscription1.getChannel().getAddress(), subscription1 );
    Arez.context().safeAction( () -> assertEquals( entity.getSubscriptions().size(), 1 ) );

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class,
                    () -> Arez.context().safeAction( () -> subscription1.delinkEntityFromSubscription( entity ) ) );
    assertEquals( exception.getMessage(), "Entity type A not present in subscription to channel G.G1:1" );
  }

  @Test
  public void delinkEntityFromChannel_noSuchInstance()
  {
    final EntityService entityService = EntityService.create();
    final Entity entity =
      Arez.context().safeAction( () -> entityService.findOrCreateEntity( "A/123", A.class, "123" ) );
    final Entity entity2 =
      Arez.context().safeAction( () -> entityService.findOrCreateEntity( ValueUtil.randomString(),
                                                                         A.class,
                                                                         ValueUtil.randomString() ) );

    final Subscription subscription1 = createSubscription( new ChannelAddress( EntityTest.G.G1, 1 ) );

    Arez.context().safeAction( () -> entity2.linkToSubscription( subscription1 ) );

    entity.subscriptions().put( subscription1.getChannel().getAddress(), subscription1 );
    Arez.context().safeAction( () -> assertEquals( entity.getSubscriptions().size(), 1 ) );

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class,
                    () -> Arez.context().safeAction( () -> subscription1.delinkEntityFromSubscription( entity ) ) );
    assertEquals( exception.getMessage(), "Entity instance A/123 not present in subscription to channel G.G1:1" );
  }

  @SuppressWarnings( "EqualsWithItself" )
  @Test
  public void comparable()
  {
    final ChannelAddress address1 = new ChannelAddress( G.G1 );
    final ChannelAddress address2 = new ChannelAddress( G.G2 );

    final Subscription subscription1 = Subscription.create( Channel.create( address1 ) );
    final Subscription subscription2 = Subscription.create( Channel.create( address2 ) );

    assertEquals( subscription1.compareTo( subscription1 ), 0 );
    assertEquals( subscription1.compareTo( subscription2 ), -1 );
    assertEquals( subscription2.compareTo( subscription1 ), 1 );
    assertEquals( subscription2.compareTo( subscription2 ), 0 );
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
  {
  }
}
