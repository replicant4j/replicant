package replicant;

import arez.Arez;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.guiceyloops.shared.ValueUtil;
import org.testng.annotations.Test;
import replicant.spy.SubscriptionOrphanedEvent;
import replicant.spy.UnsubscribeRequestQueuedEvent;
import static org.testng.Assert.*;

public class ConvergerTest
  extends AbstractReplicantTest
{
  @Test
  public void construct_withUnnecessaryContext()
  {
    final ReplicantContext context = Replicant.context();
    final IllegalStateException exception =
      expectThrows( IllegalStateException.class, () -> Converger.create( context ) );
    assertEquals( exception.getMessage(),
                  "Replicant-0134: ReplicantService passed a context but Replicant.areZonesEnabled() is false" );
  }

  @Test
  public void getReplicantContext()
  {
    final ReplicantContext context = Replicant.context();
    final Converger converger = context.getConverger();
    assertEquals( converger.getReplicantContext(), context );
    assertEquals( getFieldValue( converger, "_context" ), null );
  }

  @Test
  public void getReplicantContext_zonesEnabled()
  {
    ReplicantTestUtil.enableZones();
    ReplicantTestUtil.resetState();

    final ReplicantContext context = Replicant.context();
    final Converger converger = context.getConverger();
    assertEquals( converger.getReplicantContext(), context );
    assertEquals( getFieldValue( converger, "_context" ), context );
  }

  @Test
  public void preConvergeAction()
  {
    final Converger c = Replicant.context().getConverger();

    // Pause scheduler so Autoruns don't auto-converge
    Arez.context().pauseScheduler();

    // should do nothing ...
    Arez.context().safeAction( c::preConverge );

    final AtomicInteger callCount = new AtomicInteger();

    Arez.context().safeAction( () -> c.setPreConvergeAction( callCount::incrementAndGet ) );

    Arez.context().safeAction( c::preConverge );

    assertEquals( callCount.get(), 1 );

    Arez.context().safeAction( c::preConverge );

    assertEquals( callCount.get(), 2 );

    Arez.context().safeAction( () -> c.setPreConvergeAction( null ) );

    Arez.context().safeAction( c::preConverge );

    assertEquals( callCount.get(), 2 );
  }

  @Test
  public void convergeCompleteAction()
  {
    final Converger c = Replicant.context().getConverger();

    // Pause scheduler so Autoruns don't auto-converge
    Arez.context().pauseScheduler();

    // should do nothing ...
    Arez.context().safeAction( c::convergeComplete );

    final AtomicInteger callCount = new AtomicInteger();

    Arez.context().safeAction( () -> c.setConvergeCompleteAction( callCount::incrementAndGet ) );

    Arez.context().safeAction( c::convergeComplete );

    assertEquals( callCount.get(), 1 );

    Arez.context().safeAction( c::convergeComplete );

    assertEquals( callCount.get(), 2 );

    Arez.context().safeAction( () -> c.setConvergeCompleteAction( null ) );

    Arez.context().safeAction( c::convergeComplete );

    assertEquals( callCount.get(), 2 );
  }

  @Test
  public void canGroup()
  {
    final Converger c = Replicant.context().getConverger();

    // Pause scheduler so Autoruns don't auto-converge
    Arez.context().pauseScheduler();

    Arez.context().safeAction( () -> {
      final ChannelAddress address = new ChannelAddress( G.G1 );
      final AreaOfInterest areaOfInterest = createAreaOfInterest( address, null );

      assertTrue( c.canGroup( areaOfInterest, AreaOfInterestAction.ADD, areaOfInterest, AreaOfInterestAction.ADD ) );
      assertFalse( c.canGroup( areaOfInterest,
                               AreaOfInterestAction.ADD,
                               areaOfInterest,
                               AreaOfInterestAction.UPDATE ) );
      assertFalse( c.canGroup( areaOfInterest,
                               AreaOfInterestAction.ADD,
                               areaOfInterest,
                               AreaOfInterestAction.REMOVE ) );
      assertFalse( c.canGroup( areaOfInterest,
                               AreaOfInterestAction.UPDATE,
                               areaOfInterest,
                               AreaOfInterestAction.ADD ) );
      assertTrue( c.canGroup( areaOfInterest,
                              AreaOfInterestAction.UPDATE,
                              areaOfInterest,
                              AreaOfInterestAction.UPDATE ) );
      assertFalse( c.canGroup( areaOfInterest,
                               AreaOfInterestAction.UPDATE,
                               areaOfInterest,
                               AreaOfInterestAction.REMOVE ) );
      assertFalse( c.canGroup( areaOfInterest,
                               AreaOfInterestAction.REMOVE,
                               areaOfInterest,
                               AreaOfInterestAction.ADD ) );
      assertFalse( c.canGroup( areaOfInterest,
                               AreaOfInterestAction.REMOVE,
                               areaOfInterest,
                               AreaOfInterestAction.UPDATE ) );
      assertTrue( c.canGroup( areaOfInterest,
                              AreaOfInterestAction.REMOVE,
                              areaOfInterest,
                              AreaOfInterestAction.REMOVE ) );

      final ChannelAddress channel2 = new ChannelAddress( G.G1, 2 );
      final AreaOfInterest areaOfInterest2 = createAreaOfInterest( channel2, null );
      assertTrue( c.canGroup( areaOfInterest, AreaOfInterestAction.ADD, areaOfInterest2, AreaOfInterestAction.ADD ) );

      final ChannelAddress address3 = new ChannelAddress( G.G2, 1 );
      final AreaOfInterest areaOfInterest3 = createAreaOfInterest( address3, null );
      assertFalse( c.canGroup( areaOfInterest, AreaOfInterestAction.ADD, areaOfInterest3, AreaOfInterestAction.ADD ) );
      assertFalse( c.canGroup( areaOfInterest,
                               AreaOfInterestAction.ADD,
                               areaOfInterest3,
                               AreaOfInterestAction.UPDATE ) );
      assertFalse( c.canGroup( areaOfInterest,
                               AreaOfInterestAction.ADD,
                               areaOfInterest3,
                               AreaOfInterestAction.REMOVE ) );

      final ChannelAddress address4 = new ChannelAddress( G.G1, 1 );
      final AreaOfInterest areaOfInterest4 = createAreaOfInterest( address4, "Filter" );
      assertFalse( c.canGroup( areaOfInterest, AreaOfInterestAction.ADD, areaOfInterest4, AreaOfInterestAction.ADD ) );
      areaOfInterest.setFilter( "Filter" );
      assertTrue( c.canGroup( areaOfInterest, AreaOfInterestAction.ADD, areaOfInterest4, AreaOfInterestAction.ADD ) );
    } );
  }

  @Test
  public void removeOrphanSubscription()
  {
    final ReplicantContext context = Replicant.context();

    final TestConnector connector = TestConnector.create( G.class );
    connector.setConnection( new Connection( connector, ValueUtil.randomString() ) );
    final ChannelAddress address = new ChannelAddress( G.G1 );

    // Pause scheduler so Autoruns don't auto-converge
    Arez.context().pauseScheduler();

    Arez.context().safeAction( () -> {

      final Subscription subscription = context.createSubscription( address, null, true );

      connector.setState( ConnectorState.CONNECTED );

      final TestSpyEventHandler handler = registerTestSpyEventHandler();

      context.getConverger().removeOrphanSubscriptions();

      final List<AreaOfInterestRequest> requests = connector.ensureConnection().getPendingAreaOfInterestRequests();
      assertEquals( requests.size(), 1 );
      final AreaOfInterestRequest request = requests.get( 0 );
      assertEquals( request.getAction(), AreaOfInterestAction.REMOVE );
      assertEquals( request.getAddress(), address );

      handler.assertEventCount( 2 );

      handler.assertNextEvent( SubscriptionOrphanedEvent.class,
                               e -> assertEquals( e.getSubscription(), subscription ) );
      handler.assertNextEvent( UnsubscribeRequestQueuedEvent.class,
                               e -> assertEquals( e.getAddress(), address ) );
    } );
  }

  @Test
  public void removeOrphanSubscription_whenManyPresent()
  {
    final ReplicantContext context = Replicant.context();

    final TestConnector connector = TestConnector.create( G.class );
    connector.setConnection( new Connection( connector, ValueUtil.randomString() ) );
    final ChannelAddress address = new ChannelAddress( G.G1 );

    // Pause scheduler so Autoruns don't auto-converge
    Arez.context().pauseScheduler();

    Arez.context().safeAction( () -> {

      context.createOrUpdateAreaOfInterest( new ChannelAddress( G.G2, 1 ), null );
      context.createOrUpdateAreaOfInterest( new ChannelAddress( G.G2, 2 ), null );
      context.createOrUpdateAreaOfInterest( new ChannelAddress( G.G2, 3 ), null );
      context.createOrUpdateAreaOfInterest( new ChannelAddress( G.G2, 4 ), null );
      context.createOrUpdateAreaOfInterest( new ChannelAddress( G.G2, 5 ), null );

      final Subscription subscription = context.createSubscription( address, null, true );

      connector.setState( ConnectorState.CONNECTED );

      final TestSpyEventHandler handler = registerTestSpyEventHandler();

      context.getConverger().removeOrphanSubscriptions();

      final List<AreaOfInterestRequest> requests = connector.ensureConnection().getPendingAreaOfInterestRequests();
      assertEquals( requests.size(), 1 );
      final AreaOfInterestRequest request = requests.get( 0 );
      assertEquals( request.getAction(), AreaOfInterestAction.REMOVE );
      assertEquals( request.getAddress(), address );

      handler.assertEventCount( 2 );

      handler.assertNextEvent( SubscriptionOrphanedEvent.class,
                               e -> assertEquals( e.getSubscription(), subscription ) );
      handler.assertNextEvent( UnsubscribeRequestQueuedEvent.class,
                               e -> assertEquals( e.getAddress(), address ) );
    } );
  }

  @Test
  public void removeOrphanSubscriptions_whenConnectorDisconnected()
  {
    final ReplicantContext context = Replicant.context();

    final TestConnector connector = TestConnector.create( G.class );
    final ChannelAddress address = new ChannelAddress( G.G1 );

    // Pause scheduler so Autoruns don't auto-converge
    Arez.context().pauseScheduler();

    Arez.context().safeAction( () -> {

      context.createSubscription( address, null, true );

      connector.setState( ConnectorState.DISCONNECTED );

      final TestSpyEventHandler handler = registerTestSpyEventHandler();

      context.getConverger().removeOrphanSubscriptions();

      handler.assertEventCount( 0 );
    } );
  }

  @Test
  public void removeOrphanSubscriptions_whenSubscriptionImplicit()
  {
    final ReplicantContext context = Replicant.context();

    final TestConnector connector = TestConnector.create( G.class );
    final ChannelAddress address = new ChannelAddress( G.G1 );

    // Pause scheduler so Autoruns don't auto-converge
    Arez.context().pauseScheduler();

    Arez.context().safeAction( () -> {

      context.createSubscription( address, null, false );

      connector.setState( ConnectorState.CONNECTED );

      final TestSpyEventHandler handler = registerTestSpyEventHandler();

      context.getConverger().removeOrphanSubscriptions();

      handler.assertEventCount( 0 );
    } );
  }

  @Test
  public void removeOrphanSubscriptions_whenSubscriptionExpected()
  {
    final ReplicantContext context = Replicant.context();

    final TestConnector connector = TestConnector.create( G.class );
    final ChannelAddress address = new ChannelAddress( G.G1 );

    // Pause scheduler so Autoruns don't auto-converge
    Arez.context().pauseScheduler();

    Arez.context().safeAction( () -> {

      // Add expectation
      context.createOrUpdateAreaOfInterest( address, null );

      context.createSubscription( address, null, true );

      connector.setState( ConnectorState.CONNECTED );

      final TestSpyEventHandler handler = registerTestSpyEventHandler();

      context.getConverger().removeOrphanSubscriptions();

      handler.assertEventCount( 0 );
    } );
  }

  @Test
  public void removeOrphanSubscriptions_whenRemoveIsPending()
  {
    final ReplicantContext context = Replicant.context();

    final TestConnector connector = TestConnector.create( G.class );
    connector.setConnection( new Connection( connector, ValueUtil.randomString() ) );

    final ChannelAddress address = new ChannelAddress( G.G1 );

    // Enqueue remove request
    connector.requestUnsubscribe( address );

    // Pause scheduler so Autoruns don't auto-converge
    Arez.context().pauseScheduler();

    Arez.context().safeAction( () -> {

      context.createSubscription( address, null, true );

      connector.setState( ConnectorState.CONNECTED );

      final TestSpyEventHandler handler = registerTestSpyEventHandler();

      context.getConverger().removeOrphanSubscriptions();

      handler.assertEventCount( 0 );
    } );
  }

  @Nonnull
  private AreaOfInterest createAreaOfInterest( @Nonnull final ChannelAddress address, @Nullable final Object filter )
  {
    return Replicant.context().createOrUpdateAreaOfInterest( address, filter );
  }

  private enum G
  {
    G1, G2
  }
}
