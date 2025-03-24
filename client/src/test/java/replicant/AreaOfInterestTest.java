package replicant;

import arez.Disposable;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nonnull;
import org.realityforge.guiceyloops.shared.ValueUtil;
import org.testng.annotations.Test;
import replicant.spy.AreaOfInterestDisposedEvent;
import replicant.spy.AreaOfInterestStatusUpdatedEvent;
import static org.testng.Assert.*;

public class AreaOfInterestTest
  extends AbstractReplicantTest
{
  @Test
  public void shouldDataBePresent()
  {
    assertFalse( AreaOfInterest.Status.NOT_ASKED.shouldDataBePresent() );
    assertFalse( AreaOfInterest.Status.LOADING.shouldDataBePresent() );
    assertFalse( AreaOfInterest.Status.LOAD_FAILED.shouldDataBePresent() );
    assertTrue( AreaOfInterest.Status.LOADED.shouldDataBePresent() );
    assertTrue( AreaOfInterest.Status.UPDATING.shouldDataBePresent() );
    assertFalse( AreaOfInterest.Status.UPDATE_FAILED.shouldDataBePresent() );
    assertTrue( AreaOfInterest.Status.UPDATED.shouldDataBePresent() );
    assertTrue( AreaOfInterest.Status.UNLOADING.shouldDataBePresent() );
    assertFalse( AreaOfInterest.Status.UNLOADED.shouldDataBePresent() );
    assertFalse( AreaOfInterest.Status.DELETED.shouldDataBePresent() );
  }

  @Test
  public void isErrorState()
  {
    assertFalse( AreaOfInterest.Status.NOT_ASKED.isErrorState() );
    assertFalse( AreaOfInterest.Status.LOADING.isErrorState() );
    assertTrue( AreaOfInterest.Status.LOAD_FAILED.isErrorState() );
    assertFalse( AreaOfInterest.Status.LOADED.isErrorState() );
    assertFalse( AreaOfInterest.Status.UPDATING.isErrorState() );
    assertTrue( AreaOfInterest.Status.UPDATE_FAILED.isErrorState() );
    assertFalse( AreaOfInterest.Status.UPDATED.isErrorState() );
    assertFalse( AreaOfInterest.Status.UNLOADING.isErrorState() );
    assertFalse( AreaOfInterest.Status.UNLOADED.isErrorState() );
    assertFalse( AreaOfInterest.Status.DELETED.isErrorState() );
  }

  @Test
  public void isDeleted()
  {
    assertFalse( AreaOfInterest.Status.NOT_ASKED.isDeleted() );
    assertFalse( AreaOfInterest.Status.LOADING.isDeleted() );
    assertFalse( AreaOfInterest.Status.LOAD_FAILED.isDeleted() );
    assertFalse( AreaOfInterest.Status.LOADED.isDeleted() );
    assertFalse( AreaOfInterest.Status.UPDATING.isDeleted() );
    assertFalse( AreaOfInterest.Status.UPDATE_FAILED.isDeleted() );
    assertFalse( AreaOfInterest.Status.UPDATED.isDeleted() );
    assertFalse( AreaOfInterest.Status.UNLOADING.isDeleted() );
    assertFalse( AreaOfInterest.Status.UNLOADED.isDeleted() );
    assertTrue( AreaOfInterest.Status.DELETED.isDeleted() );
  }

  @Test
  public void onConstruct()
  {
    final AreaOfInterest areaOfInterest = createAreaOfInterest( new ChannelAddress( 1, 0 ) );

    safeAction( () -> {
      assertEquals( areaOfInterest.getStatus(), AreaOfInterest.Status.NOT_ASKED );
      assertEquals( areaOfInterest.getAddress(), new ChannelAddress( 1, 0 ) );
      assertNull( areaOfInterest.getFilter() );
      assertNull( areaOfInterest.getSubscription() );
      assertNull( areaOfInterest.getError() );
    } );
  }

  @Test
  public void disposeAreaOfInterestGeneratesSpyEvent()
  {
    final AreaOfInterest areaOfInterest = createAreaOfInterest( new ChannelAddress( 1, 0 ) );

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

    Disposable.dispose( areaOfInterest );
    handler.assertEventCount( 1 );

    final AreaOfInterestDisposedEvent event = handler.assertNextEvent( AreaOfInterestDisposedEvent.class );
    assertEquals( event.getAreaOfInterest(), areaOfInterest );
  }

  @SuppressWarnings( { "ThrowableNotThrown", "ResultOfMethodCallIgnored" } )
  @Test
  public void notifications()
  {
    createConnector();
    final AreaOfInterest areaOfInterest = createAreaOfInterest( new ChannelAddress( 1, 0 ) );

    final AtomicInteger getStatusCallCount = new AtomicInteger();
    observer( () -> {
      if ( Disposable.isNotDisposed( areaOfInterest ) )
      {
        // Observe state
        areaOfInterest.getStatus();
      }
      getStatusCallCount.incrementAndGet();
    } );

    final AtomicInteger getErrorCallCount = new AtomicInteger();
    observer( () -> {
      if ( Disposable.isNotDisposed( areaOfInterest ) )
      {
        // Observe state
        areaOfInterest.getError();
      }
      getErrorCallCount.incrementAndGet();
    } );

    final AtomicInteger getSubscriptionCallCount = new AtomicInteger();
    observer( () -> {
      if ( Disposable.isNotDisposed( areaOfInterest ) )
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

    assertEquals( areaOfInterest.getStatus(), AreaOfInterest.Status.LOADED );
    safeAction( () -> assertNull( areaOfInterest.getError() ) );
    safeAction( () -> assertNull( areaOfInterest.getSubscription() ) );

    safeAction( () -> areaOfInterest.setError( new Throwable() ) );

    assertEquals( getStatusCallCount.get(), 2 );
    assertEquals( getErrorCallCount.get(), 2 );
    assertEquals( getSubscriptionCallCount.get(), 1 );

    assertEquals( areaOfInterest.getStatus(), AreaOfInterest.Status.LOADED );
    safeAction( () -> assertNotNull( areaOfInterest.getError() ) );
    safeAction( () -> assertNull( areaOfInterest.getSubscription() ) );

    safeAction( () -> Replicant.context().getSubscriptionService()
      .createSubscription( areaOfInterest.getAddress(), areaOfInterest.getFilter(), true ) );

    assertEquals( getStatusCallCount.get(), 2 );
    assertEquals( getErrorCallCount.get(), 2 );
    assertEquals( getSubscriptionCallCount.get(), 2 );

    assertEquals( areaOfInterest.getStatus(), AreaOfInterest.Status.LOADED );
    safeAction( () -> assertNotNull( areaOfInterest.getError() ) );
    safeAction( () -> assertNotNull( areaOfInterest.getSubscription() ) );
  }

  @Test
  public void testToString()
  {
    final AreaOfInterest areaOfInterest = createAreaOfInterest( new ChannelAddress( 1, 0 ) );
    assertEquals( areaOfInterest.toString(), "AreaOfInterest[1.0 Status: NOT_ASKED]" );
  }

  @Test
  public void testToStringWithFilter()
  {
    final AreaOfInterest areaOfInterest = AreaOfInterest.create( null, new ChannelAddress( 1, 0 ), "MyFilter" );
    assertEquals( areaOfInterest.toString(), "AreaOfInterest[1.0 Filter: MyFilter Status: NOT_ASKED]" );
  }

  @Test
  public void testToString_namesDisabled()
  {
    ReplicantTestUtil.disableNames();

    final AreaOfInterest areaOfInterest = createAreaOfInterest( new ChannelAddress( 1, 0 ) );

    assertEquals( areaOfInterest.toString(),
                  "replicant.Arez_AreaOfInterest@" + Integer.toHexString( areaOfInterest.hashCode() ) );
  }

  @Test
  public void updateAreaOfInterest()
  {
    pauseScheduler();
    final AreaOfInterest aoi = createAreaOfInterest( new ChannelAddress( 1, 0 ) );
    final Throwable error = new Throwable();

    aoi.updateAreaOfInterest( AreaOfInterest.Status.NOT_ASKED, null );
    assertEquals( aoi.getStatus(), AreaOfInterest.Status.NOT_ASKED );
    safeAction( () -> assertNull( aoi.getSubscription() ) );
    safeAction( () -> assertNull( aoi.getError() ) );

    aoi.updateAreaOfInterest( AreaOfInterest.Status.LOADING, null );
    assertEquals( aoi.getStatus(), AreaOfInterest.Status.LOADING );
    safeAction( () -> assertNull( aoi.getSubscription() ) );
    safeAction( () -> assertNull( aoi.getError() ) );

    aoi.updateAreaOfInterest( AreaOfInterest.Status.LOAD_FAILED, error );
    assertEquals( aoi.getStatus(), AreaOfInterest.Status.LOAD_FAILED );
    safeAction( () -> assertNull( aoi.getSubscription() ) );
    safeAction( () -> assertEquals( aoi.getError(), error ) );

    final Subscription subscription = createSubscription( aoi.getAddress(), null, true );

    aoi.updateAreaOfInterest( AreaOfInterest.Status.LOADED, null );
    assertEquals( aoi.getStatus(), AreaOfInterest.Status.LOADED );
    safeAction( () -> assertEquals( aoi.getSubscription(), subscription ) );
    safeAction( () -> assertNull( aoi.getError() ) );

    aoi.updateAreaOfInterest( AreaOfInterest.Status.UPDATING, null );
    assertEquals( aoi.getStatus(), AreaOfInterest.Status.UPDATING );
    safeAction( () -> assertEquals( aoi.getSubscription(), subscription ) );
    safeAction( () -> assertNull( aoi.getError() ) );

    aoi.updateAreaOfInterest( AreaOfInterest.Status.UPDATED, null );
    assertEquals( aoi.getStatus(), AreaOfInterest.Status.UPDATED );
    safeAction( () -> assertEquals( aoi.getSubscription(), subscription ) );
    safeAction( () -> assertNull( aoi.getError() ) );

    aoi.updateAreaOfInterest( AreaOfInterest.Status.UPDATE_FAILED, error );
    assertEquals( aoi.getStatus(), AreaOfInterest.Status.UPDATE_FAILED );
    safeAction( () -> assertEquals( aoi.getSubscription(), subscription ) );
    safeAction( () -> assertEquals( aoi.getError(), error ) );

    aoi.updateAreaOfInterest( AreaOfInterest.Status.UNLOADING, null );
    assertEquals( aoi.getStatus(), AreaOfInterest.Status.UNLOADING );
    safeAction( () -> assertEquals( aoi.getSubscription(), subscription ) );
    safeAction( () -> assertNull( aoi.getError() ) );

    Disposable.dispose( subscription );

    aoi.updateAreaOfInterest( AreaOfInterest.Status.UNLOADED, null );
    assertEquals( aoi.getStatus(), AreaOfInterest.Status.UNLOADED );
    safeAction( () -> assertNull( aoi.getSubscription() ) );
    safeAction( () -> assertNull( aoi.getError() ) );
  }

  @Test
  public void updateAreaOfInterest_generatesSpyEvent()
  {
    pauseScheduler();
    final AreaOfInterest aoi = createAreaOfInterest( new ChannelAddress( 1, 0 ) );

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

    aoi.updateAreaOfInterest( AreaOfInterest.Status.LOADING, null );

    handler.assertEventCount( 1 );
    final AreaOfInterestStatusUpdatedEvent event = handler.assertNextEvent( AreaOfInterestStatusUpdatedEvent.class );
    assertEquals( event.getAreaOfInterest(), aoi );
  }

  @Test
  public void updateAreaOfInterest_missingErrorWhenExpected()
  {
    pauseScheduler();
    final AreaOfInterest aoi = createAreaOfInterest( new ChannelAddress( 1, 0 ) );

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class,
                    () -> aoi.updateAreaOfInterest( AreaOfInterest.Status.LOAD_FAILED, null ) );
    assertEquals( exception.getMessage(),
                  "Replicant-0016: Invoked updateAreaOfInterest for channel at address 1.0 with status LOAD_FAILED but failed to supply the expected error." );
  }

  @Test
  public void updateAreaOfInterest_errorWhenUnexpected()
  {
    pauseScheduler();
    final AreaOfInterest aoi = createAreaOfInterest( new ChannelAddress( 1, 0 ) );
    final Throwable error = new Throwable();

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class,
                    () -> aoi.updateAreaOfInterest( AreaOfInterest.Status.UNLOADED, error ) );
    assertEquals( exception.getMessage(),
                  "Replicant-0017: Invoked updateAreaOfInterest for channel at address 1.0 with status UNLOADED and supplied an unexpected error." );
  }

  @Test
  public void updateAreaOfInterest_subscription_when_NOT_ASKED()
  {
    pauseScheduler();
    final AreaOfInterest aoi = createAreaOfInterest( new ChannelAddress( 1, 0 ) );

    createSubscription( aoi.getAddress(), null, true );

    aoi.updateAreaOfInterest( AreaOfInterest.Status.NOT_ASKED, null );
    assertEquals( aoi.getStatus(), AreaOfInterest.Status.NOT_ASKED );
    safeAction( () -> assertNull( aoi.getSubscription() ) );
    safeAction( () -> assertNull( aoi.getError() ) );
  }

  @Test
  public void updateAreaOfInterest_subscriptionWhenUnexpected()
  {
    pauseScheduler();
    final AreaOfInterest aoi = createAreaOfInterest( new ChannelAddress( 1, 0 ) );

    createSubscription( aoi.getAddress(), null, true );

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class,
                    () -> aoi.updateAreaOfInterest( AreaOfInterest.Status.UNLOADED, null ) );
    assertEquals( exception.getMessage(),
                  "Replicant-0019: Invoked updateAreaOfInterest for channel at address 1.0 with status UNLOADED and found unexpected subscription in the context." );
  }

  // Disable test as it does not work on M1 from commandline ... for some reason
  @Test( timeOut = 4000L, enabled = false )
  public void refCounting()
    throws Exception
  {
    final AreaOfInterest areaOfInterest =
      createAreaOfInterest( new ChannelAddress( ValueUtil.randomInt(), ValueUtil.randomInt() ) );

    assertEquals( areaOfInterest.getRefCount(), 0 );

    areaOfInterest.incRefCount();
    areaOfInterest.incRefCount();

    assertEquals( areaOfInterest.getRefCount(), 2 );

    areaOfInterest.decRefCount();

    assertEquals( areaOfInterest.getRefCount(), 1 );

    // Synchronize to ensure test sequencing occurs as expected
    //noinspection SynchronizationOnLocalVariableOrMethodParameter
    synchronized ( areaOfInterest )
    {
      areaOfInterest.decRefCount();
      assertEquals( areaOfInterest.getRefCount(), 0 );
      areaOfInterest.incRefCount();
      assertEquals( areaOfInterest.getRefCount(), 1 );
    }

    Thread.sleep( 10 );

    assertEquals( areaOfInterest.getRefCount(), 1 );
    assertTrue( Disposable.isNotDisposed( areaOfInterest ) );

    areaOfInterest.decRefCount();
    assertEquals( areaOfInterest.getRefCount(), 0 );

    Thread.sleep( 10 );

    assertTrue( Disposable.isDisposed( areaOfInterest ) );
  }

  @Nonnull
  private AreaOfInterest createAreaOfInterest( @Nonnull final ChannelAddress address )
  {
    return AreaOfInterest.create( Replicant.areZonesEnabled() ? Replicant.context() : null, address, null );
  }
}
