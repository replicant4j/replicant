package org.realityforge.replicant.client.aoi;

import arez.Arez;
import arez.ArezTestUtil;
import arez.Disposable;
import java.util.concurrent.atomic.AtomicInteger;
import org.realityforge.replicant.client.AbstractReplicantTest;
import org.realityforge.replicant.client.Channel;
import org.realityforge.replicant.client.ChannelAddress;
import org.realityforge.replicant.client.subscription.Subscription;
import org.realityforge.replicant.client.subscription.SubscriptionService;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class AreaOfInterestTest
  extends AbstractReplicantTest
{
  @Test
  public void onConstruct()
  {
    final ChannelAddress address = new ChannelAddress( G.G1 );
    final Channel channel = Channel.create( address );
    final AreaOfInterest areaOfInterest = AreaOfInterest.create( channel );

    Arez.context().safeAction( () -> {
      assertEquals( areaOfInterest.getStatus(), AreaOfInterest.Status.NOT_ASKED );
      assertEquals( areaOfInterest.getChannel(), channel );
      assertEquals( areaOfInterest.getEntry(), null );
      assertEquals( areaOfInterest.getError(), null );
    } );
  }

  @SuppressWarnings( { "ThrowableNotThrown", "ResultOfMethodCallIgnored" } )
  @Test
  public void notifications()
  {
    final ChannelAddress address = new ChannelAddress( G.G1 );
    final Channel channel = Channel.create( address );
    final AreaOfInterest areaOfInterest = AreaOfInterest.create( channel );

    final AtomicInteger getStatusCallCount = new AtomicInteger();
    Arez.context().autorun( () -> {
      if ( !Disposable.isDisposed( areaOfInterest ) )
      {
        // Observe state
        areaOfInterest.getStatus();
      }
      getStatusCallCount.incrementAndGet();
    } );

    final AtomicInteger getErrorCallCount = new AtomicInteger();
    Arez.context().autorun( () -> {
      if ( !Disposable.isDisposed( areaOfInterest ) )
      {
        // Observe state
        areaOfInterest.getError();
      }
      getErrorCallCount.incrementAndGet();
    } );

    final AtomicInteger getSubscriptionCallCount = new AtomicInteger();
    Arez.context().autorun( () -> {
      if ( !Disposable.isDisposed( areaOfInterest ) )
      {
        // Observe state
        areaOfInterest.getEntry();
      }
      getSubscriptionCallCount.incrementAndGet();
    } );

    assertEquals( getStatusCallCount.get(), 1 );
    assertEquals( getErrorCallCount.get(), 1 );
    assertEquals( getSubscriptionCallCount.get(), 1 );

    Arez.context().safeAction( () -> areaOfInterest.setStatus( AreaOfInterest.Status.LOADED ) );

    assertEquals( getStatusCallCount.get(), 2 );
    assertEquals( getErrorCallCount.get(), 1 );
    assertEquals( getSubscriptionCallCount.get(), 1 );

    Arez.context().safeAction( () -> assertEquals( areaOfInterest.getStatus(), AreaOfInterest.Status.LOADED ) );
    Arez.context().safeAction( () -> assertNull( areaOfInterest.getError() ) );
    Arez.context().safeAction( () -> assertNull( areaOfInterest.getEntry() ) );

    Arez.context().safeAction( () -> areaOfInterest.setError( new Throwable() ) );

    assertEquals( getStatusCallCount.get(), 2 );
    assertEquals( getErrorCallCount.get(), 2 );
    assertEquals( getSubscriptionCallCount.get(), 1 );

    Arez.context().safeAction( () -> assertEquals( areaOfInterest.getStatus(), AreaOfInterest.Status.LOADED ) );
    Arez.context().safeAction( () -> assertNotNull( areaOfInterest.getError() ) );
    Arez.context().safeAction( () -> assertNull( areaOfInterest.getEntry() ) );

    Arez.context()
      .safeAction( () -> {
        final SubscriptionService subscriptionService = SubscriptionService.create();
        final Subscription subscription =
          subscriptionService.createSubscription( channel.getAddress(), channel.getFilter(), true );
        areaOfInterest.setEntry( subscription );
      } );

    assertEquals( getStatusCallCount.get(), 2 );
    assertEquals( getErrorCallCount.get(), 2 );
    assertEquals( getSubscriptionCallCount.get(), 2 );

    Arez.context().safeAction( () -> assertEquals( areaOfInterest.getStatus(), AreaOfInterest.Status.LOADED ) );
    Arez.context().safeAction( () -> assertNotNull( areaOfInterest.getError() ) );
    Arez.context().safeAction( () -> assertNotNull( areaOfInterest.getEntry() ) );
  }

  @Test
  public void testToString()
  {
    final ChannelAddress address = new ChannelAddress( G.G1 );
    final Channel channel = Channel.create( address );
    final AreaOfInterest areaOfInterest = AreaOfInterest.create( channel );

    assertEquals( areaOfInterest.toString(), "AreaOfInterest[Channel[G.G1 :: Filter=null] Status: NOT_ASKED]" );
  }

  @Test
  public void testToString_namesDisabled()
  {
    ArezTestUtil.disableNames();

    // TODO: The next line should be removed when Arez is upgraded to a version greater than 0.82
    ArezTestUtil.disableNativeComponents();

    final ChannelAddress address = new ChannelAddress( G.G1 );
    final Channel channel = Channel.create( address );
    final AreaOfInterest areaOfInterest = AreaOfInterest.create( channel );

    assertEquals( areaOfInterest.toString(),
                  "org.realityforge.replicant.client.aoi.Arez_AreaOfInterest@" +
                  Integer.toHexString( System.identityHashCode( areaOfInterest ) ) );
  }

  enum G
  {
    G1
  }
}
