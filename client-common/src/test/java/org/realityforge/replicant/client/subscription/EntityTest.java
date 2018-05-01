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
    final Class<A> type = A.class;
    final String id = ValueUtil.randomString();
    final Entity entity = Entity.create( type, id );

    assertEquals( entity.getType(), type );
    assertEquals( entity.getId(), id );

    Arez.context().safeAction( () -> assertEquals( entity.getUserObject(), null ) );
    Arez.context().safeAction( () -> assertEquals( entity.getChannelSubscriptions().size(), 0 ) );
  }

  @Test
  public void toStringTest()
  {
    final Class<A> type = A.class;
    final String id = "123";
    final Entity entity = Entity.create( type, id );

    assertEquals( entity.toString(), "A/123" );
    ArezTestUtil.disableNames();

    assertEquals( entity.toString(), entity.getClass().getName() + "@" + Integer.toHexString( entity.hashCode() ) );
  }

  @Test
  public void userObject()
  {
    final Class<A> type = A.class;
    final String id = ValueUtil.randomString();
    final Entity entity = Entity.create( type, id );

    Arez.context().safeAction( () -> assertEquals( entity.getUserObject(), null ) );

    final A userObject = new A();
    Arez.context().safeAction( () -> entity.setUserObject( userObject ) );
    Arez.context().safeAction( () -> assertEquals( entity.getUserObject(), userObject ) );
  }

  @Test
  public void userObjectBadType()
  {
    final Class<A> type = A.class;
    final String id = "1234";
    final Entity entity = Entity.create( type, id );

    Arez.context().safeAction( () -> assertEquals( entity.getUserObject(), null ) );

    final Object userObject = new Object();
    final IllegalStateException exception =
      expectThrows( IllegalStateException.class,
                    () -> Arez.context().safeAction( () -> entity.setUserObject( userObject ) ) );
    assertEquals( exception.getMessage(),
                  "Entity A/1234 specified non-null userObject of type java.lang.Object but the entity expected type org.realityforge.replicant.client.subscription.EntityTest$A" );
  }

  @Test
  public void typeChannelSubscriptions()
  {
    final Class<String> type = String.class;
    final String id = ValueUtil.randomString();
    final Entity entity = Entity.create( type, id );

    final Subscription subscription1 = createSubscription( new ChannelAddress( G.G1 ) );
    final Subscription subscription2 = createSubscription( new ChannelAddress( G.G2 ) );

    final AtomicInteger callCount = new AtomicInteger();
    Arez.context().autorun( () -> {
      // Access observable next line
      entity.getChannelSubscriptions();
      callCount.incrementAndGet();
    } );

    assertEquals( callCount.get(), 1 );
    Arez.context().safeAction( () -> assertEquals( entity.getChannelSubscriptions().size(), 0 ) );

    // Add initial subscription
    {
      Arez.context().safeAction( () -> entity.addChannelSubscription( subscription1 ) );

      assertEquals( callCount.get(), 2 );
      Arez.context().safeAction( () -> assertEquals( entity.getChannelSubscriptions().size(), 1 ) );
    }

    // Add same subscription so no notification or change
    {
      Arez.context().safeAction( () -> entity.addChannelSubscription( subscription1 ) );

      assertEquals( callCount.get(), 2 );
      Arez.context().safeAction( () -> assertEquals( entity.getChannelSubscriptions().size(), 1 ) );
    }

    // Add second subscription and thus get notified
    {
      Arez.context().safeAction( () -> entity.addChannelSubscription( subscription2 ) );

      assertEquals( callCount.get(), 3 );
      Arez.context().safeAction( () -> assertEquals( entity.getChannelSubscriptions().size(), 2 ) );
    }

    // Remove subscription and thus get notified
    {
      Arez.context().safeAction( () -> entity.removeChannelSubscription( subscription2 ) );

      assertEquals( callCount.get(), 4 );
      Arez.context().safeAction( () -> assertEquals( entity.getChannelSubscriptions().size(), 1 ) );
    }
  }

  @Test
  public void instanceChannelSubscriptions()
  {
    final Class<String> type = String.class;
    final String id = ValueUtil.randomString();
    final Entity entity = Entity.create( type, id );

    final Subscription subscription1 = createSubscription( new ChannelAddress( G.G1, 1 ) );
    final Subscription subscription2 = createSubscription( new ChannelAddress( G.G1, 2 ) );

    final AtomicInteger callCount = new AtomicInteger();
    Arez.context().autorun( () -> {
      // Access observable next line
      entity.getChannelSubscriptions();
      callCount.incrementAndGet();
    } );

    assertEquals( callCount.get(), 1 );
    Arez.context().safeAction( () -> assertEquals( entity.getChannelSubscriptions().size(), 0 ) );

    // Add initial subscription
    {
      Arez.context().safeAction( () -> entity.addChannelSubscription( subscription1 ) );

      assertEquals( callCount.get(), 2 );
      Arez.context().safeAction( () -> assertEquals( entity.getChannelSubscriptions().size(), 1 ) );
    }

    // Add second subscription and thus get notified
    // second subscription is of the same channelType, so should go through second path
    {
      Arez.context().safeAction( () -> entity.addChannelSubscription( subscription2 ) );

      assertEquals( callCount.get(), 3 );
      Arez.context().safeAction( () -> assertEquals( entity.getChannelSubscriptions().size(), 2 ) );
    }

    // Add same subscription again and thus not notified
    {
      Arez.context().safeAction( () -> entity.addChannelSubscription( subscription2 ) );

      assertEquals( callCount.get(), 3 );
      Arez.context().safeAction( () -> assertEquals( entity.getChannelSubscriptions().size(), 2 ) );
    }

    // Remove subscription and thus get notified
    {
      Arez.context().safeAction( () -> entity.removeChannelSubscription( subscription2 ) );

      assertEquals( callCount.get(), 4 );
      Arez.context().safeAction( () -> assertEquals( entity.getChannelSubscriptions().size(), 1 ) );
    }
  }

  @Test
  public void disposeRemovesEntityFromChannels()
  {
    final Class<A> type = A.class;
    final String id = ValueUtil.randomString();
    final Entity entity = Entity.create( type, id );

    final Subscription subscription1 = createSubscription( new ChannelAddress( G.G1, 1 ) );
    final Subscription subscription2 = createSubscription( new ChannelAddress( G.G1, 2 ) );

    Arez.context().safeAction( () -> assertEquals( entity.getChannelSubscriptions().size(), 0 ) );
    Arez.context().safeAction( () -> entity.addChannelSubscription( subscription1 ) );
    Arez.context().safeAction( () -> entity.addChannelSubscription( subscription2 ) );
    Arez.context().safeAction( () -> assertEquals( entity.getChannelSubscriptions().size(), 2 ) );

    Arez.context().safeAction( () -> assertEquals( subscription1.getEntities().get( type ).get( id ), entity ) );
    Arez.context().safeAction( () -> assertEquals( subscription2.getEntities().get( type ).get( id ), entity ) );

    Disposable.dispose( entity );

    Arez.context().safeAction( () -> assertNull( subscription1.getEntities().get( type ) ) );
    Arez.context().safeAction( () -> assertNull( subscription2.getEntities().get( type ) ) );
  }

  @Test
  public void getChannelSubscriptions_mutability()
  {
    final Entity entity = Entity.create( A.class, ValueUtil.randomString() );

    final Subscription subscription1 = createSubscription();

    expectThrows( UnsupportedOperationException.class,
                  () -> Arez.context().safeAction( () -> entity.getChannelSubscriptions().add( subscription1 ) ) );
  }

  @Test
  public void delinkChannelFromEntity_whenSubscriptionMissing()
  {
    final Entity entity = Entity.create( A.class, "123" );

    final Subscription subscription1 = createSubscription( new ChannelAddress( G.G1, 1 ) );

    Arez.context().safeAction( () -> assertEquals( entity.getChannelSubscriptions().size(), 0 ) );
    Arez.context().safeAction( () -> entity.addChannelSubscription( subscription1 ) );
    Arez.context().safeAction( () -> assertEquals( entity.getChannelSubscriptions().size(), 1 ) );

    entity.channelSubscriptions().remove( subscription1.getChannel().getAddress() );

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class,
                    () -> Arez.context().safeAction( () -> entity.delinkChannelFromEntity( subscription1 ) ) );
    assertEquals( exception.getMessage(), "Unable to locate subscription for channel G.G1:1 on entity A/123" );
  }

  @Test
  public void delinkEntityFromChannel_noSuchType()
  {
    final Entity entity = Entity.create( A.class, "123" );

    final Subscription subscription1 = createSubscription( new ChannelAddress( G.G1, 1 ) );

    entity.channelSubscriptions().put( subscription1.getChannel().getAddress(), subscription1 );
    Arez.context().safeAction( () -> assertEquals( entity.getChannelSubscriptions().size(), 1 ) );

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class,
                    () -> Arez.context().safeAction( () -> entity.delinkEntityFromChannel( subscription1 ) ) );
    assertEquals( exception.getMessage(), "Entity type A not present in channel G.G1:1" );
  }

  @Test
  public void delinkEntityFromChannel_noSuchInstance()
  {
    final Entity entity = Entity.create( A.class, "123" );
    final Entity entity2 = Entity.create( A.class, ValueUtil.randomString() );

    final Subscription subscription1 = createSubscription( new ChannelAddress( G.G1, 1 ) );

    Arez.context().safeAction( () -> entity2.addChannelSubscription( subscription1 ) );

    entity.channelSubscriptions().put( subscription1.getChannel().getAddress(), subscription1 );
    Arez.context().safeAction( () -> assertEquals( entity.getChannelSubscriptions().size(), 1 ) );

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class,
                    () -> Arez.context().safeAction( () -> entity.delinkEntityFromChannel( subscription1 ) ) );
    assertEquals( exception.getMessage(), "Entity instance A/123 not present in channel G.G1:1" );
  }

  @Nonnull
  private Subscription createSubscription()
  {
    return createSubscription( new ChannelAddress( G.G1 ) );
  }

  @Nonnull
  private Subscription createSubscription( @Nonnull final ChannelAddress address )
  {
    return Subscription.create( Channel.create( address ), true );
  }

  enum G
  {
    G1, G2
  }

  static class A
  {
  }
}
