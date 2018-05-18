package replicant;

import arez.Arez;
import arez.Disposable;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nonnull;
import org.testng.annotations.Test;
import replicant.spy.AreaOfInterestDisposedEvent;
import replicant.spy.AreaOfInterestStatusUpdatedEvent;
import static org.testng.Assert.*;

public class AreaOfInterestTest
  extends AbstractReplicantTest
{
  @Test
  public void onConstruct()
  {
    final AreaOfInterest areaOfInterest = createAreaOfInterest( new ChannelAddress( G.G1 ) );

    safeAction( () -> {
      assertEquals( areaOfInterest.getStatus(), AreaOfInterest.Status.NOT_ASKED );
      assertEquals( areaOfInterest.getAddress(), new ChannelAddress( G.G1 ) );
      assertEquals( areaOfInterest.getFilter(), null );
      assertEquals( areaOfInterest.getSubscription(), null );
      assertEquals( areaOfInterest.getError(), null );
    } );
  }

  @Test
  public void disposeAreaOfInterestGeneratesSpyEvent()
  {
    final AreaOfInterest areaOfInterest = createAreaOfInterest( new ChannelAddress( G.G1 ) );

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

    safeAction( () -> Disposable.dispose( areaOfInterest ) );
    handler.assertEventCount( 1 );

    final AreaOfInterestDisposedEvent event = handler.assertNextEvent( AreaOfInterestDisposedEvent.class );
    assertEquals( event.getAreaOfInterest(), areaOfInterest );
  }

  @SuppressWarnings( { "ThrowableNotThrown", "ResultOfMethodCallIgnored" } )
  @Test
  public void notifications()
  {
    final AreaOfInterest areaOfInterest = createAreaOfInterest( new ChannelAddress( G.G1 ) );

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
        areaOfInterest.getSubscription();
      }
      getSubscriptionCallCount.incrementAndGet();
    } );

    assertEquals( getStatusCallCount.get(), 1 );
    assertEquals( getErrorCallCount.get(), 1 );
    assertEquals( getSubscriptionCallCount.get(), 1 );

    safeAction( () -> areaOfInterest.setStatus( AreaOfInterest.Status.LOADED ) );

    assertEquals( getStatusCallCount.get(), 2 );
    assertEquals( getErrorCallCount.get(), 1 );
    assertEquals( getSubscriptionCallCount.get(), 1 );

    safeAction( () -> assertEquals( areaOfInterest.getStatus(), AreaOfInterest.Status.LOADED ) );
    safeAction( () -> assertNull( areaOfInterest.getError() ) );
    safeAction( () -> assertNull( areaOfInterest.getSubscription() ) );

    safeAction( () -> areaOfInterest.setError( new Throwable() ) );

    assertEquals( getStatusCallCount.get(), 2 );
    assertEquals( getErrorCallCount.get(), 2 );
    assertEquals( getSubscriptionCallCount.get(), 1 );

    safeAction( () -> assertEquals( areaOfInterest.getStatus(), AreaOfInterest.Status.LOADED ) );
    safeAction( () -> assertNotNull( areaOfInterest.getError() ) );
    safeAction( () -> assertNull( areaOfInterest.getSubscription() ) );

    Arez.context()
      .safeAction( () -> {
        final SubscriptionService subscriptionService = SubscriptionService.create( null );
        final Subscription subscription =
          subscriptionService.createSubscription( areaOfInterest.getAddress(), areaOfInterest.getFilter(), true );
        areaOfInterest.setSubscription( subscription );
      } );

    assertEquals( getStatusCallCount.get(), 2 );
    assertEquals( getErrorCallCount.get(), 2 );
    assertEquals( getSubscriptionCallCount.get(), 2 );

    safeAction( () -> assertEquals( areaOfInterest.getStatus(), AreaOfInterest.Status.LOADED ) );
    safeAction( () -> assertNotNull( areaOfInterest.getError() ) );
    safeAction( () -> assertNotNull( areaOfInterest.getSubscription() ) );
  }

  @Test
  public void testToString()
  {
    final AreaOfInterest areaOfInterest = createAreaOfInterest( new ChannelAddress( G.G1 ) );
    assertEquals( areaOfInterest.toString(), "AreaOfInterest[G.G1 Status: NOT_ASKED]" );
  }

  @Test
  public void testToStringWithFilter()
  {
    final AreaOfInterest areaOfInterest = AreaOfInterest.create( null, new ChannelAddress( G.G1 ), "MyFilter" );
    assertEquals( areaOfInterest.toString(), "AreaOfInterest[G.G1 Filter: MyFilter Status: NOT_ASKED]" );
  }

  @Test
  public void testToString_namesDisabled()
  {
    ReplicantTestUtil.disableNames();

    final AreaOfInterest areaOfInterest = createAreaOfInterest( new ChannelAddress( G.G1 ) );

    assertEquals( areaOfInterest.toString(),
                  "replicant.Arez_AreaOfInterest@" + Integer.toHexString( areaOfInterest.hashCode() ) );
  }

  @SuppressWarnings( "ThrowableNotThrown" )
  @Test
  public void updateAreaOfInterest()
  {
    Arez.context().pauseScheduler();
    final AreaOfInterest aoi = createAreaOfInterest( new ChannelAddress( G.G1 ) );
    final Throwable error = new Throwable();

    safeAction( () -> aoi.updateAreaOfInterest( AreaOfInterest.Status.NOT_ASKED, null ) );
    safeAction( () -> assertEquals( aoi.getStatus(), AreaOfInterest.Status.NOT_ASKED ) );
    safeAction( () -> assertEquals( aoi.getSubscription(), null ) );
    safeAction( () -> assertEquals( aoi.getError(), null ) );

    safeAction( () -> aoi.updateAreaOfInterest( AreaOfInterest.Status.LOADING, null ) );
    safeAction( () -> assertEquals( aoi.getStatus(), AreaOfInterest.Status.LOADING ) );
    safeAction( () -> assertEquals( aoi.getSubscription(), null ) );
    safeAction( () -> assertEquals( aoi.getError(), null ) );

    safeAction( () -> aoi.updateAreaOfInterest( AreaOfInterest.Status.LOAD_FAILED, error ) );
    safeAction( () -> assertEquals( aoi.getStatus(), AreaOfInterest.Status.LOAD_FAILED ) );
    safeAction( () -> assertEquals( aoi.getSubscription(), null ) );
    safeAction( () -> assertEquals( aoi.getError(), error ) );

    final Subscription subscription =
      safeAction( () -> aoi.getReplicantContext().createSubscription( aoi.getAddress(), null, true ) );

    safeAction( () -> aoi.updateAreaOfInterest( AreaOfInterest.Status.LOADED, null ) );
    safeAction( () -> assertEquals( aoi.getStatus(), AreaOfInterest.Status.LOADED ) );
    safeAction( () -> assertEquals( aoi.getSubscription(), subscription ) );
    safeAction( () -> assertEquals( aoi.getError(), null ) );

    safeAction( () -> aoi.updateAreaOfInterest( AreaOfInterest.Status.UPDATING, null ) );
    safeAction( () -> assertEquals( aoi.getStatus(), AreaOfInterest.Status.UPDATING ) );
    safeAction( () -> assertEquals( aoi.getSubscription(), subscription ) );
    safeAction( () -> assertEquals( aoi.getError(), null ) );

    safeAction( () -> aoi.updateAreaOfInterest( AreaOfInterest.Status.UPDATED, null ) );
    safeAction( () -> assertEquals( aoi.getStatus(), AreaOfInterest.Status.UPDATED ) );
    safeAction( () -> assertEquals( aoi.getSubscription(), subscription ) );
    safeAction( () -> assertEquals( aoi.getError(), null ) );

    safeAction( () -> aoi.updateAreaOfInterest( AreaOfInterest.Status.UPDATE_FAILED, error ) );
    safeAction( () -> assertEquals( aoi.getStatus(), AreaOfInterest.Status.UPDATE_FAILED ) );
    safeAction( () -> assertEquals( aoi.getSubscription(), subscription ) );
    safeAction( () -> assertEquals( aoi.getError(), error ) );

    safeAction( () -> aoi.updateAreaOfInterest( AreaOfInterest.Status.UNLOADING, null ) );
    safeAction( () -> assertEquals( aoi.getStatus(), AreaOfInterest.Status.UNLOADING ) );
    safeAction( () -> assertEquals( aoi.getSubscription(), subscription ) );
    safeAction( () -> assertEquals( aoi.getError(), null ) );

    Disposable.dispose( subscription );

    safeAction( () -> aoi.updateAreaOfInterest( AreaOfInterest.Status.UNLOADED, null ) );
    safeAction( () -> assertEquals( aoi.getStatus(), AreaOfInterest.Status.UNLOADED ) );
    safeAction( () -> assertEquals( aoi.getSubscription(), null ) );
    safeAction( () -> assertEquals( aoi.getError(), null ) );
  }

  @Test
  public void updateAreaOfInterest_generatesSpyEvent()
  {
    Arez.context().pauseScheduler();
    final AreaOfInterest aoi = createAreaOfInterest( new ChannelAddress( G.G1 ) );

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

    safeAction( () -> aoi.updateAreaOfInterest( AreaOfInterest.Status.LOADING, null ) );

    handler.assertEventCount( 1 );
    final AreaOfInterestStatusUpdatedEvent event = handler.assertNextEvent( AreaOfInterestStatusUpdatedEvent.class );
    assertEquals( event.getAreaOfInterest(), aoi );
  }

  @Test
  public void updateAreaOfInterest_missingErrorWhenExpected()
  {
    Arez.context().pauseScheduler();
    final AreaOfInterest aoi = createAreaOfInterest( new ChannelAddress( G.G1 ) );

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class,
                    () -> Arez.context()
                      .safeAction( () -> aoi.updateAreaOfInterest( AreaOfInterest.Status.LOAD_FAILED, null ) ) );
    assertEquals( exception.getMessage(),
                  "Replicant-0016: Invoked updateAreaOfInterest for channel at address G.G1 with status LOAD_FAILED but failed to supply the expected error." );
  }

  @SuppressWarnings( "ThrowableNotThrown" )
  @Test
  public void updateAreaOfInterest_errorWhenUnexpected()
  {
    Arez.context().pauseScheduler();
    final AreaOfInterest aoi = createAreaOfInterest( new ChannelAddress( G.G1 ) );
    final Throwable error = new Throwable();

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class,
                    () -> Arez.context()
                      .safeAction( () -> aoi.updateAreaOfInterest( AreaOfInterest.Status.UNLOADED, error ) ) );
    assertEquals( exception.getMessage(),
                  "Replicant-0017: Invoked updateAreaOfInterest for channel at address G.G1 with status UNLOADED and supplied an unexpected error." );
  }

  @Test
  public void updateAreaOfInterest_missingSubscriptionWhenExpected()
  {
    Arez.context().pauseScheduler();
    final AreaOfInterest aoi = createAreaOfInterest( new ChannelAddress( G.G1 ) );

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class,
                    () -> Arez.context()
                      .safeAction( () -> aoi.updateAreaOfInterest( AreaOfInterest.Status.LOADED, null ) ) );
    assertEquals( exception.getMessage(),
                  "Replicant-0018: Invoked updateAreaOfInterest for channel at address G.G1 with status LOADED and the context is missing expected subscription." );
  }

  @Test
  public void updateAreaOfInterest_subscriptionWhenUnexpected()
  {
    Arez.context().pauseScheduler();
    final AreaOfInterest aoi = createAreaOfInterest( new ChannelAddress( G.G1 ) );

    safeAction( () -> aoi.getReplicantContext().createSubscription( aoi.getAddress(), null, true ) );

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class,
                    () -> Arez.context()
                      .safeAction( () -> aoi.updateAreaOfInterest( AreaOfInterest.Status.UNLOADED, null ) ) );
    assertEquals( exception.getMessage(),
                  "Replicant-0019: Invoked updateAreaOfInterest for channel at address G.G1 with status UNLOADED and found unexpected subscription in the context." );
  }

  @Nonnull
  private AreaOfInterest createAreaOfInterest( @Nonnull final ChannelAddress address )
  {
    return AreaOfInterest.create( Replicant.areZonesEnabled() ? Replicant.context() : null, address, null );
  }

  enum G
  {
    G1
  }
}
