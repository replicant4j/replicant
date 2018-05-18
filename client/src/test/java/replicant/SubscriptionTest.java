package replicant;

import arez.Disposable;
import java.util.concurrent.atomic.AtomicInteger;
import org.realityforge.guiceyloops.shared.ValueUtil;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class SubscriptionTest
  extends AbstractReplicantTest
{
  @Test
  public void basicConstruction()
  {
    final ChannelAddress address = new ChannelAddress( G.G1 );
    final Object filter = ValueUtil.randomString();
    final Subscription subscription = Subscription.create( null, address, filter, true );

    assertEquals( subscription.getAddress(), address );

    safeAction( () -> assertEquals( subscription.isExplicitSubscription(), true ) );
    safeAction( () -> assertEquals( subscription.getEntities().size(), 0 ) );
    safeAction( () -> assertEquals( subscription.getFilter(), filter ) );
  }

  @Test
  public void filter()
  {
    final Object filter1 = ValueUtil.randomString();
    final Object filter2 = ValueUtil.randomString();

    final Subscription subscription =
      Subscription.create( null, new ChannelAddress( G.G1 ), filter1, ValueUtil.randomBoolean() );

    safeAction( () -> assertEquals( subscription.getFilter(), filter1 ) );
    safeAction( () -> subscription.setFilter( filter2 ) );
    safeAction( () -> assertEquals( subscription.getFilter(), filter2 ) );

  }

  @Test
  public void entities()
  {
    final EntityService entityService = Replicant.context().getEntityService();
    final Entity entity1 = safeAction( () -> entityService.findOrCreateEntity( "A/1", A.class, 1 ) );
    final Entity entity2 = safeAction( () -> entityService.findOrCreateEntity( "A/2", A.class, 2 ) );

    final ChannelAddress address = new ChannelAddress( G.G1, 1 );
    final Subscription subscription =
      Subscription.create( null, address, null, true );

    final AtomicInteger callCount = new AtomicInteger();
    autorun( () -> {
      // Just invoke method to get observing
      //noinspection ResultOfMethodCallIgnored
      subscription.getEntities();
      callCount.incrementAndGet();
    } );

    final AtomicInteger findCallCount = new AtomicInteger();
    autorun( () -> {
      // Just invoke method to get observing
      subscription.findEntityByTypeAndId( A.class, 1 );
      findCallCount.incrementAndGet();
    } );

    assertEquals( callCount.get(), 1 );
    assertEquals( findCallCount.get(), 1 );
    safeAction( () -> assertEquals( subscription.findAllEntityTypes().size(), 0 ) );
    safeAction( () -> assertEquals( subscription.findAllEntityTypes().contains( A.class ), false ) );
    safeAction( () -> assertEquals( subscription.findAllEntitiesByType( A.class ).size(), 0 ) );
    safeAction( () -> assertEquals( subscription.findAllEntitiesByType( String.class ).size(), 0 ) );
    safeAction( () -> assertEquals( subscription.findEntityByTypeAndId( A.class, 1 ), null ) );
    safeAction( () -> assertEquals( subscription.findEntityByTypeAndId( A.class, 2 ), null ) );

    safeAction( () -> subscription.linkSubscriptionToEntity( entity1 ) );

    assertEquals( callCount.get(), 2 );
    assertEquals( findCallCount.get(), 2 );
    safeAction( () -> assertEquals( subscription.findAllEntityTypes().size(), 1 ) );
    safeAction( () -> assertEquals( subscription.findAllEntityTypes().contains( A.class ), true ) );
    safeAction( () -> assertEquals( subscription.findAllEntitiesByType( A.class ).size(), 1 ) );
    safeAction( () -> assertEquals( subscription.findAllEntitiesByType( String.class ).size(), 0 ) );
    safeAction( () -> assertEquals( subscription.findEntityByTypeAndId( A.class, 1 ), entity1 ) );
    safeAction( () -> assertEquals( subscription.findEntityByTypeAndId( A.class, 2 ), null ) );

    // Add second entity, finder no need to re-find
    safeAction( () -> subscription.linkSubscriptionToEntity( entity2 ) );

    assertEquals( callCount.get(), 3 );
    assertEquals( findCallCount.get(), 2 );
    safeAction( () -> assertEquals( subscription.findAllEntityTypes().size(), 1 ) );
    safeAction( () -> assertEquals( subscription.findAllEntityTypes().contains( A.class ), true ) );
    safeAction( () -> assertEquals( subscription.findAllEntitiesByType( A.class ).size(), 2 ) );
    safeAction( () -> assertEquals( subscription.findAllEntitiesByType( String.class ).size(), 0 ) );
    safeAction( () -> assertEquals( subscription.findEntityByTypeAndId( A.class, 1 ), entity1 ) );
    safeAction( () -> assertEquals( subscription.findEntityByTypeAndId( A.class, 2 ), entity2 ) );

    // Duplicate link ... ignored as no change
    safeAction( () -> subscription.linkSubscriptionToEntity( entity2 ) );

    assertEquals( callCount.get(), 3 );
    assertEquals( findCallCount.get(), 2 );
    safeAction( () -> assertEquals( subscription.findAllEntityTypes().size(), 1 ) );
    safeAction( () -> assertEquals( subscription.findAllEntitiesByType( A.class ).size(), 2 ) );
    safeAction( () -> assertEquals( subscription.findAllEntitiesByType( String.class ).size(), 0 ) );
    safeAction( () -> assertEquals( subscription.findEntityByTypeAndId( A.class, 1 ), entity1 ) );
    safeAction( () -> assertEquals( subscription.findEntityByTypeAndId( A.class, 2 ), entity2 ) );

    // Removing entity 1, finder will react
    safeAction( () -> subscription.delinkEntityFromSubscription( entity1 ) );

    assertEquals( callCount.get(), 4 );
    assertEquals( findCallCount.get(), 3 );
    safeAction( () -> assertEquals( subscription.findAllEntityTypes().size(), 1 ) );
    safeAction( () -> assertEquals( subscription.findAllEntitiesByType( A.class ).size(), 1 ) );
    safeAction( () -> assertEquals( subscription.findAllEntitiesByType( String.class ).size(), 0 ) );
    safeAction( () -> assertEquals( subscription.findEntityByTypeAndId( A.class, 1 ), null ) );
    safeAction( () -> assertEquals( subscription.findEntityByTypeAndId( A.class, 2 ), entity2 ) );

    // Removing entity 2, state is reset
    safeAction( () -> subscription.delinkEntityFromSubscription( entity2 ) );

    assertEquals( callCount.get(), 5 );
    assertEquals( findCallCount.get(), 4 );
    safeAction( () -> assertEquals( subscription.findAllEntityTypes().size(), 0 ) );
    safeAction( () -> assertEquals( subscription.findAllEntitiesByType( A.class ).size(), 0 ) );
    safeAction( () -> assertEquals( subscription.findAllEntitiesByType( String.class ).size(), 0 ) );
    safeAction( () -> assertEquals( subscription.findEntityByTypeAndId( A.class, 1 ), null ) );
    safeAction( () -> assertEquals( subscription.findEntityByTypeAndId( A.class, 2 ), null ) );
  }

  @Test
  public void dispose_delinksFromEntity()
  {
    final EntityService entityService = Replicant.context().getEntityService();
    final Entity entity1 = safeAction( () -> entityService.findOrCreateEntity( "A/1", A.class, 1 ) );
    final Entity entity2 = safeAction( () -> entityService.findOrCreateEntity( "A/2", A.class, 2 ) );

    final Subscription subscription1 =
      Subscription.create( null, new ChannelAddress( G.G1, 1 ), null, true );
    final Subscription subscription2 =
      Subscription.create( null, new ChannelAddress( G.G1, 2 ), null, true );

    safeAction( () -> entity1.linkToSubscription( subscription1 ) );
    safeAction( () -> entity2.linkToSubscription( subscription1 ) );
    safeAction( () -> entity1.linkToSubscription( subscription2 ) );

    safeAction( () -> assertEquals( subscription1.findAllEntityTypes().size(), 1 ) );
    safeAction( () -> assertEquals( subscription2.findAllEntityTypes().size(), 1 ) );
    safeAction( () -> assertEquals( entity1.getSubscriptions().size(), 2 ) );

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
    final EntityService entityService = Replicant.context().getEntityService();
    final Entity entity =
      safeAction( () -> entityService.findOrCreateEntity( ValueUtil.randomString(), A.class, ValueUtil.randomInt() ) );

    final Subscription subscription1 =
      Subscription.create( null, new ChannelAddress( EntityTest.G.G1, 1 ), null, true );

    entity.subscriptions().put( subscription1.getAddress(), subscription1 );
    safeAction( () -> assertEquals( entity.getSubscriptions().size(), 1 ) );

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class,
                    () -> safeAction( () -> subscription1.delinkEntityFromSubscription( entity ) ) );
    assertEquals( exception.getMessage(), "Entity type A not present in subscription to channel G.G1:1" );
  }

  @Test
  public void delinkEntityFromChannel_noSuchInstance()
  {
    final EntityService entityService = Replicant.context().getEntityService();
    final Entity entity =
      safeAction( () -> entityService.findOrCreateEntity( "A/123", A.class, 123 ) );
    final Entity entity2 =
      safeAction( () -> entityService.findOrCreateEntity( ValueUtil.randomString(),
                                                          A.class,
                                                          ValueUtil.randomInt() ) );

    final Subscription subscription1 =
      Subscription.create( null, new ChannelAddress( EntityTest.G.G1, 1 ), null, true );

    safeAction( () -> entity2.linkToSubscription( subscription1 ) );

    entity.subscriptions().put( subscription1.getAddress(), subscription1 );
    safeAction( () -> assertEquals( entity.getSubscriptions().size(), 1 ) );

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class,
                    () -> safeAction( () -> subscription1.delinkEntityFromSubscription( entity ) ) );
    assertEquals( exception.getMessage(), "Entity instance A/123 not present in subscription to channel G.G1:1" );
  }

  @SuppressWarnings( "EqualsWithItself" )
  @Test
  public void comparable()
  {
    final ChannelAddress address1 = new ChannelAddress( G.G1 );
    final ChannelAddress address2 = new ChannelAddress( G.G2 );

    final Subscription subscription1 = Subscription.create( null, address1, null, true );
    final Subscription subscription2 = Subscription.create( null, address2, null, true );

    assertEquals( subscription1.compareTo( subscription1 ), 0 );
    assertEquals( subscription1.compareTo( subscription2 ), -1 );
    assertEquals( subscription2.compareTo( subscription1 ), 1 );
    assertEquals( subscription2.compareTo( subscription2 ), 0 );
  }

  enum G
  {
    G1, G2
  }

  static class A
  {
  }
}
