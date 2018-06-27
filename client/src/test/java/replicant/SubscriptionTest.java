package replicant;

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
    final ChannelAddress address = new ChannelAddress( 1, 0 );
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
      Subscription.create( null, new ChannelAddress( 1, 0 ), filter1, ValueUtil.randomBoolean() );

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

    final ChannelAddress address = new ChannelAddress( 1, 0, 1 );
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
    safeAction( () -> subscription.delinkEntityFromSubscription( entity1, true ) );

    assertEquals( callCount.get(), 4 );
    assertEquals( findCallCount.get(), 3 );
    safeAction( () -> assertEquals( subscription.findAllEntityTypes().size(), 1 ) );
    safeAction( () -> assertEquals( subscription.findAllEntitiesByType( A.class ).size(), 1 ) );
    safeAction( () -> assertEquals( subscription.findAllEntitiesByType( String.class ).size(), 0 ) );
    safeAction( () -> assertEquals( subscription.findEntityByTypeAndId( A.class, 1 ), null ) );
    safeAction( () -> assertEquals( subscription.findEntityByTypeAndId( A.class, 2 ), entity2 ) );

    // Removing entity 2, state is reset
    safeAction( () -> subscription.delinkEntityFromSubscription( entity2, true ) );

    assertEquals( callCount.get(), 5 );
    assertEquals( findCallCount.get(), 4 );
    safeAction( () -> assertEquals( subscription.findAllEntityTypes().size(), 0 ) );
    safeAction( () -> assertEquals( subscription.findAllEntitiesByType( A.class ).size(), 0 ) );
    safeAction( () -> assertEquals( subscription.findAllEntitiesByType( String.class ).size(), 0 ) );
    safeAction( () -> assertEquals( subscription.findEntityByTypeAndId( A.class, 1 ), null ) );
    safeAction( () -> assertEquals( subscription.findEntityByTypeAndId( A.class, 2 ), null ) );
  }

  @Test
  public void delinkEntityFromChannel_noSuchType()
  {
    final EntityService entityService = Replicant.context().getEntityService();
    final Entity entity =
      safeAction( () -> entityService.findOrCreateEntity( ValueUtil.randomString(), A.class, ValueUtil.randomInt() ) );

    final Subscription subscription1 =
      Subscription.create( null, new ChannelAddress( 1, 0, 1 ), null, true );

    entity.subscriptions().put( subscription1.getAddress(), subscription1 );
    safeAction( () -> assertEquals( entity.getSubscriptions().size(), 1 ) );

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class,
                    () -> safeAction( () -> subscription1.delinkEntityFromSubscription( entity, true ) ) );
    assertEquals( exception.getMessage(), "Entity type A not present in subscription to channel 1.0.1" );
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
      Subscription.create( null, new ChannelAddress( 1, 0, 1 ), null, true );

    safeAction( () -> entity2.linkToSubscription( subscription1 ) );

    entity.subscriptions().put( subscription1.getAddress(), subscription1 );
    safeAction( () -> assertEquals( entity.getSubscriptions().size(), 1 ) );

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class,
                    () -> safeAction( () -> subscription1.delinkEntityFromSubscription( entity, true ) ) );
    assertEquals( exception.getMessage(), "Entity instance A/123 not present in subscription to channel 1.0.1" );
  }

  @SuppressWarnings( "EqualsWithItself" )
  @Test
  public void comparable()
  {
    final ChannelAddress address1 = new ChannelAddress( 1, 0 );
    final ChannelAddress address2 = new ChannelAddress( 1, 1 );

    final Subscription subscription1 = Subscription.create( null, address1, null, true );
    final Subscription subscription2 = Subscription.create( null, address2, null, true );

    assertEquals( subscription1.compareTo( subscription1 ), 0 );
    assertEquals( subscription1.compareTo( subscription2 ), -1 );
    assertEquals( subscription2.compareTo( subscription1 ), 1 );
    assertEquals( subscription2.compareTo( subscription2 ), 0 );
  }

  @Test
  public void getChannelSchema()
  {
    final ChannelSchema channelSchema =
      new ChannelSchema( 0, ValueUtil.randomString(), null, ChannelSchema.FilterType.NONE, null, false, true );
    createConnector( new SystemSchema( 1,
                                       ValueUtil.randomString(),
                                       new ChannelSchema[]{ channelSchema },
                                       new EntitySchema[ 0 ] ) );
    final ChannelAddress address1 = new ChannelAddress( 1, 0 );

    final Subscription subscription1 = Subscription.create( null, address1, null, true );

    assertEquals( subscription1.getChannelSchema(), channelSchema );
  }

  @Test
  public void getInstanceRoot()
  {
    final ChannelSchema channelSchema =
      new ChannelSchema( 0, ValueUtil.randomString(), A.class, ChannelSchema.FilterType.NONE, null, false, true );
    createConnector( new SystemSchema( 1,
                                       ValueUtil.randomString(),
                                       new ChannelSchema[]{ channelSchema },
                                       new EntitySchema[ 0 ] ) );
    final ChannelAddress address1 = new ChannelAddress( 1, 0, 33 );

    final Subscription subscription1 = Subscription.create( null, address1, null, true );

    final EntityService entityService = Replicant.context().getEntityService();
    final Entity entity1 = safeAction( () -> entityService.findOrCreateEntity( "A/33", A.class, 33 ) );
    final A entity = new A();
    safeAction( () -> entity1.setUserObject( entity ) );

    safeAction( () -> subscription1.linkSubscriptionToEntity( entity1 ) );

    safeAction( () -> assertEquals( subscription1.getInstanceRoot(), entity ) );
  }

  @Test
  public void getInstanceRoot_butEntityNotPresent()
  {
    final ChannelSchema channelSchema =
      new ChannelSchema( 0, ValueUtil.randomString(), A.class, ChannelSchema.FilterType.NONE, null, false, true );
    createConnector( new SystemSchema( 1,
                                       ValueUtil.randomString(),
                                       new ChannelSchema[]{ channelSchema },
                                       new EntitySchema[ 0 ] ) );
    final ChannelAddress address1 = new ChannelAddress( 1, 0, 33 );

    final Subscription subscription1 = Subscription.create( null, address1, null, true );

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class, () -> safeAction( subscription1::getInstanceRoot ) );
    assertEquals( exception.getMessage(),
                  "Replicant-0088: Subscription.getInstanceRoot() invoked on subscription for channel 1.0.33 but entity is not present." );
  }

  @Test
  public void getInstanceRoot_butChannelHasNoId()
  {
    final ChannelSchema channelSchema =
      new ChannelSchema( 0, ValueUtil.randomString(), A.class, ChannelSchema.FilterType.NONE, null, false, true );
    createConnector( new SystemSchema( 1,
                                       ValueUtil.randomString(),
                                       new ChannelSchema[]{ channelSchema },
                                       new EntitySchema[ 0 ] ) );
    final ChannelAddress address1 = new ChannelAddress( 1, 0 );

    final Subscription subscription1 = Subscription.create( null, address1, null, true );

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class, () -> safeAction( subscription1::getInstanceRoot ) );
    assertEquals( exception.getMessage(),
                  "Replicant-0087: Subscription.getInstanceRoot() invoked on subscription for channel 1.0 but channel has not supplied expected id." );
  }

  @Test
  public void getInstanceRoot_butChannelIsTypeBased()
  {
    final ChannelSchema channelSchema =
      new ChannelSchema( 0, ValueUtil.randomString(), null, ChannelSchema.FilterType.NONE, null, false, true );
    createConnector( new SystemSchema( 1,
                                       ValueUtil.randomString(),
                                       new ChannelSchema[]{ channelSchema },
                                       new EntitySchema[ 0 ] ) );
    final ChannelAddress address1 = new ChannelAddress( 1, 0, 44 );

    final Subscription subscription1 = Subscription.create( null, address1, null, true );

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class, () -> safeAction( subscription1::getInstanceRoot ) );
    assertEquals( exception.getMessage(),
                  "Replicant-0029: Subscription.getInstanceRoot() invoked on subscription for channel 1.0.44 but channel is not instance based." );
  }

  static class A
  {
  }
}
