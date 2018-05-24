package replicant;

import arez.Disposable;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.realityforge.guiceyloops.shared.ValueUtil;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import replicant.spy.SubscribeRequestQueuedEvent;
import replicant.spy.SubscriptionOrphanedEvent;
import replicant.spy.SubscriptionUpdateRequestQueuedEvent;
import replicant.spy.UnsubscribeRequestQueuedEvent;
import static org.testng.Assert.*;

public class ConvergerTest
  extends AbstractReplicantTest
{
  @BeforeMethod
  @Override
  protected void beforeTest()
    throws Exception
  {
    super.beforeTest();
    // Pause schedule so can manually interact with converger
    pauseScheduler();
  }

  @Test
  public void construct_withUnnecessaryContext()
  {
    final ReplicantContext context = Replicant.context();
    final IllegalStateException exception =
      expectThrows( IllegalStateException.class, () -> Converger.create( context ) );
    assertEquals( exception.getMessage(),
                  "Replicant-0037: ReplicantService passed a context but Replicant.areZonesEnabled() is false" );
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

    // should do nothing ...
    safeAction( c::preConverge );

    final AtomicInteger callCount = new AtomicInteger();

    safeAction( () -> c.setPreConvergeAction( callCount::incrementAndGet ) );

    safeAction( c::preConverge );

    assertEquals( callCount.get(), 1 );

    safeAction( c::preConverge );

    assertEquals( callCount.get(), 2 );

    safeAction( () -> c.setPreConvergeAction( null ) );

    safeAction( c::preConverge );

    assertEquals( callCount.get(), 2 );
  }

  @Test
  public void convergeCompleteAction()
  {
    final Converger c = Replicant.context().getConverger();

    // should do nothing ...
    safeAction( c::convergeComplete );

    final AtomicInteger callCount = new AtomicInteger();

    safeAction( () -> c.setConvergeCompleteAction( callCount::incrementAndGet ) );

    safeAction( c::convergeComplete );

    assertEquals( callCount.get(), 1 );

    safeAction( c::convergeComplete );

    assertEquals( callCount.get(), 2 );

    safeAction( () -> c.setConvergeCompleteAction( null ) );

    safeAction( c::convergeComplete );

    assertEquals( callCount.get(), 2 );
  }

  @Test
  public void canGroup()
  {
    final Converger c = Replicant.context().getConverger();

    safeAction( () -> {
      final ChannelAddress address = new ChannelAddress( 1, 0 );
      final AreaOfInterest areaOfInterest = Replicant.context().createOrUpdateAreaOfInterest( address, null );

      assertTrue( c.canGroup( areaOfInterest,
                              AreaOfInterestRequest.Type.ADD,
                              areaOfInterest,
                              AreaOfInterestRequest.Type.ADD ) );
      assertFalse( c.canGroup( areaOfInterest,
                               AreaOfInterestRequest.Type.ADD,
                               areaOfInterest,
                               AreaOfInterestRequest.Type.UPDATE ) );
      assertFalse( c.canGroup( areaOfInterest,
                               AreaOfInterestRequest.Type.ADD,
                               areaOfInterest,
                               AreaOfInterestRequest.Type.REMOVE ) );
      assertFalse( c.canGroup( areaOfInterest,
                               AreaOfInterestRequest.Type.UPDATE,
                               areaOfInterest,
                               AreaOfInterestRequest.Type.ADD ) );
      assertTrue( c.canGroup( areaOfInterest,
                              AreaOfInterestRequest.Type.UPDATE,
                              areaOfInterest,
                              AreaOfInterestRequest.Type.UPDATE ) );
      assertFalse( c.canGroup( areaOfInterest,
                               AreaOfInterestRequest.Type.UPDATE,
                               areaOfInterest,
                               AreaOfInterestRequest.Type.REMOVE ) );
      assertFalse( c.canGroup( areaOfInterest,
                               AreaOfInterestRequest.Type.REMOVE,
                               areaOfInterest,
                               AreaOfInterestRequest.Type.ADD ) );
      assertFalse( c.canGroup( areaOfInterest,
                               AreaOfInterestRequest.Type.REMOVE,
                               areaOfInterest,
                               AreaOfInterestRequest.Type.UPDATE ) );
      assertTrue( c.canGroup( areaOfInterest,
                              AreaOfInterestRequest.Type.REMOVE,
                              areaOfInterest,
                              AreaOfInterestRequest.Type.REMOVE ) );

      final ChannelAddress channel2 = new ChannelAddress( 1, 0, 2 );
      final AreaOfInterest areaOfInterest2 = Replicant.context().createOrUpdateAreaOfInterest( channel2, null );
      assertTrue( c.canGroup( areaOfInterest,
                              AreaOfInterestRequest.Type.ADD,
                              areaOfInterest2,
                              AreaOfInterestRequest.Type.ADD ) );

      final ChannelAddress address3 = new ChannelAddress( 1, 1, 1 );
      final AreaOfInterest areaOfInterest3 = Replicant.context().createOrUpdateAreaOfInterest( address3, null );
      assertFalse( c.canGroup( areaOfInterest,
                               AreaOfInterestRequest.Type.ADD,
                               areaOfInterest3,
                               AreaOfInterestRequest.Type.ADD ) );
      assertFalse( c.canGroup( areaOfInterest,
                               AreaOfInterestRequest.Type.ADD,
                               areaOfInterest3,
                               AreaOfInterestRequest.Type.UPDATE ) );
      assertFalse( c.canGroup( areaOfInterest,
                               AreaOfInterestRequest.Type.ADD,
                               areaOfInterest3,
                               AreaOfInterestRequest.Type.REMOVE ) );

      final ChannelAddress address4 = new ChannelAddress( 1, 0, 1 );
      final AreaOfInterest areaOfInterest4 = Replicant.context().createOrUpdateAreaOfInterest( address4, "Filter" );
      assertFalse( c.canGroup( areaOfInterest,
                               AreaOfInterestRequest.Type.ADD,
                               areaOfInterest4,
                               AreaOfInterestRequest.Type.ADD ) );
      areaOfInterest.setFilter( "Filter" );
      assertTrue( c.canGroup( areaOfInterest,
                              AreaOfInterestRequest.Type.ADD,
                              areaOfInterest4,
                              AreaOfInterestRequest.Type.ADD ) );
    } );
  }

  @Test
  public void removeOrphanSubscription()
  {
    final TestConnector connector = TestConnector.create();
    connector.setConnection( new Connection( connector, ValueUtil.randomString() ) );

    pauseScheduler();
    connector.pauseMessageScheduler();

    final ChannelAddress address = new ChannelAddress( 1, 0 );

    safeAction( () -> {
      final Subscription subscription = createSubscription( address, null, true );

      connector.setState( ConnectorState.CONNECTED );

      final TestSpyEventHandler handler = registerTestSpyEventHandler();

      Replicant.context().getConverger().removeOrphanSubscriptions();

      final List<AreaOfInterestRequest> requests = connector.ensureConnection().getPendingAreaOfInterestRequests();
      assertEquals( requests.size(), 1 );
      final AreaOfInterestRequest request = requests.get( 0 );
      assertEquals( request.getType(), AreaOfInterestRequest.Type.REMOVE );
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
    final TestConnector connector = TestConnector.create();
    connector.setConnection( new Connection( connector, ValueUtil.randomString() ) );

    pauseScheduler();
    connector.pauseMessageScheduler();

    final ChannelAddress address = new ChannelAddress( 1, 0 );

    safeAction( () -> {

      Replicant.context().createOrUpdateAreaOfInterest( new ChannelAddress( 1, 1, 1 ), null );
      Replicant.context().createOrUpdateAreaOfInterest( new ChannelAddress( 1, 1, 2 ), null );
      Replicant.context().createOrUpdateAreaOfInterest( new ChannelAddress( 1, 1, 3 ), null );
      Replicant.context().createOrUpdateAreaOfInterest( new ChannelAddress( 1, 1, 4 ), null );
      Replicant.context().createOrUpdateAreaOfInterest( new ChannelAddress( 1, 1, 5 ), null );

      final Subscription subscription = createSubscription( address, null, true );

      connector.setState( ConnectorState.CONNECTED );

      final TestSpyEventHandler handler = registerTestSpyEventHandler();

      Replicant.context().getConverger().removeOrphanSubscriptions();

      final List<AreaOfInterestRequest> requests = connector.ensureConnection().getPendingAreaOfInterestRequests();
      assertEquals( requests.size(), 1 );
      final AreaOfInterestRequest request = requests.get( 0 );
      assertEquals( request.getType(), AreaOfInterestRequest.Type.REMOVE );
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
    final TestConnector connector = TestConnector.create();
    final ChannelAddress address = new ChannelAddress( 1, 0 );

    safeAction( () -> {

      createSubscription( address, null, true );

      connector.setState( ConnectorState.DISCONNECTED );

      final TestSpyEventHandler handler = registerTestSpyEventHandler();

      Replicant.context().getConverger().removeOrphanSubscriptions();

      handler.assertEventCount( 0 );
    } );
  }

  @Test
  public void removeOrphanSubscriptions_whenSubscriptionImplicit()
  {
    final TestConnector connector = TestConnector.create();
    final ChannelAddress address = new ChannelAddress( 1, 0 );

    safeAction( () -> {

      createSubscription( address, null, false );

      connector.setState( ConnectorState.CONNECTED );

      final TestSpyEventHandler handler = registerTestSpyEventHandler();

      Replicant.context().getConverger().removeOrphanSubscriptions();

      handler.assertEventCount( 0 );
    } );
  }

  @Test
  public void removeOrphanSubscriptions_whenSubscriptionExpected()
  {
    final TestConnector connector = TestConnector.create();
    final ChannelAddress address = new ChannelAddress( 1, 0 );

    safeAction( () -> {

      // Add expectation
      Replicant.context().createOrUpdateAreaOfInterest( address, null );

      createSubscription( address, null, true );

      connector.setState( ConnectorState.CONNECTED );

      final TestSpyEventHandler handler = registerTestSpyEventHandler();

      Replicant.context().getConverger().removeOrphanSubscriptions();

      handler.assertEventCount( 0 );
    } );
  }

  @Test
  public void removeOrphanSubscriptions_whenRemoveIsPending()
  {
    final TestConnector connector = TestConnector.create();
    connector.setConnection( new Connection( connector, ValueUtil.randomString() ) );
    connector.pauseMessageScheduler();

    final ChannelAddress address = new ChannelAddress( 1, 0 );

    createSubscription( address, null, true );

    // Enqueue remove request
    connector.requestUnsubscribe( address );

    final TestSpyEventHandler handler = registerTestSpyEventHandler();
    safeAction( () -> connector.setState( ConnectorState.CONNECTED ) );
    safeAction( () -> Replicant.context().getConverger().removeOrphanSubscriptions() );
    handler.assertEventCount( 0 );
  }

  @Test
  public void convergeAreaOfInterest()
  {
    final TestConnector connector = TestConnector.create();
    connector.setConnection( new Connection( connector, ValueUtil.randomString() ) );
    pauseScheduler();
    connector.pauseMessageScheduler();

    safeAction( () -> connector.setState( ConnectorState.CONNECTED ) );

    final ChannelAddress address = new ChannelAddress( 1, 0 );
    final AreaOfInterest areaOfInterest =
      safeAction( () -> Replicant.context().createOrUpdateAreaOfInterest( address, null ) );

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

    final Converger.Action result = safeAction( () -> Replicant.context().getConverger()
      .convergeAreaOfInterest( areaOfInterest, null, null ) );

    assertEquals( result, Converger.Action.SUBMITTED_ADD );

    handler.assertEventCount( 1 );
    handler.assertNextEvent( SubscribeRequestQueuedEvent.class, e -> assertEquals( e.getAddress(), address ) );
  }

  @Test
  public void convergeAreaOfInterest_alreadySubscribed()
  {
    final TestConnector connector = TestConnector.create();
    connector.setConnection( new Connection( connector, ValueUtil.randomString() ) );
    safeAction( () -> connector.setState( ConnectorState.CONNECTED ) );

    final ChannelAddress address = new ChannelAddress( 1, 0 );
    final AreaOfInterest areaOfInterest =
      safeAction( () -> Replicant.context().createOrUpdateAreaOfInterest( address, null ) );
    createSubscription( address, null, true );

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

    final Converger.Action result = safeAction( () -> Replicant.context().getConverger()
      .convergeAreaOfInterest( areaOfInterest, null, null ) );

    assertEquals( result, Converger.Action.NO_ACTION );

    handler.assertEventCount( 0 );
  }

  @Test
  public void convergeAreaOfInterest_subscribing()
  {
    final TestConnector connector = TestConnector.create();
    final Connection connection = new Connection( connector, ValueUtil.randomString() );
    connector.setConnection( connection );
    safeAction( () -> connector.setState( ConnectorState.CONNECTED ) );

    final ChannelAddress address = new ChannelAddress( 1, 0 );
    final AreaOfInterest areaOfInterest =
      safeAction( () -> Replicant.context().createOrUpdateAreaOfInterest( address, null ) );

    connection.injectCurrentAreaOfInterestRequest( new AreaOfInterestRequest( address,
                                                                              AreaOfInterestRequest.Type.ADD,
                                                                              null ) );
    final TestSpyEventHandler handler = registerTestSpyEventHandler();

    final Converger.Action result = safeAction( () -> Replicant.context().getConverger()
      .convergeAreaOfInterest( areaOfInterest, null, null ) );

    assertEquals( result, Converger.Action.IN_PROGRESS );

    handler.assertEventCount( 0 );
  }

  @Test
  public void convergeAreaOfInterest_addPending()
  {
    final TestConnector connector = TestConnector.create();
    final Connection connection = new Connection( connector, ValueUtil.randomString() );
    connector.setConnection( connection );
    connector.pauseMessageScheduler();
    safeAction( () -> connector.setState( ConnectorState.CONNECTED ) );

    final ChannelAddress address = new ChannelAddress( 1, 0 );
    final AreaOfInterest areaOfInterest =
      safeAction( () -> Replicant.context().createOrUpdateAreaOfInterest( address, null ) );

    connector.requestSubscribe( address, null );

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

    final Converger.Action result = safeAction( () -> Replicant.context().getConverger()
      .convergeAreaOfInterest( areaOfInterest, null, null ) );

    assertEquals( result, Converger.Action.IN_PROGRESS );

    handler.assertEventCount( 0 );
  }

  @Test
  public void convergeAreaOfInterest_updatePending()
  {
    final TestConnector connector = TestConnector.create();
    final Connection connection = new Connection( connector, ValueUtil.randomString() );
    connector.setConnection( connection );
    connector.pauseMessageScheduler();
    safeAction( () -> connector.setState( ConnectorState.CONNECTED ) );

    final ChannelAddress address = new ChannelAddress( 1, 0 );
    final Object filter = ValueUtil.randomString();
    final AreaOfInterest areaOfInterest =
      safeAction( () -> Replicant.context().createOrUpdateAreaOfInterest( address, filter ) );
    createSubscription( address, ValueUtil.randomString(), true );

    connector.requestSubscriptionUpdate( address, filter );

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

    final Converger.Action result = safeAction( () -> Replicant.context().getConverger()
      .convergeAreaOfInterest( areaOfInterest, null, null ) );

    assertEquals( result, Converger.Action.IN_PROGRESS );

    handler.assertEventCount( 0 );
  }

  @Test
  public void convergeAreaOfInterest_requestSubscriptionUpdate()
  {
    final TestConnector connector = TestConnector.create();
    connector.setConnection( new Connection( connector, ValueUtil.randomString() ) );
    safeAction( () -> connector.setState( ConnectorState.CONNECTED ) );
    connector.pauseMessageScheduler();

    final ChannelAddress address = new ChannelAddress( 1, 0 );
    final String filter = ValueUtil.randomString();
    final AreaOfInterest areaOfInterest =
      safeAction( () -> Replicant.context().createOrUpdateAreaOfInterest( address, filter ) );
    createSubscription( address, ValueUtil.randomString(), true );

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

    final Converger.Action result = safeAction( () -> Replicant.context().getConverger()
      .convergeAreaOfInterest( areaOfInterest, null, null ) );

    assertEquals( result, Converger.Action.SUBMITTED_UPDATE );

    handler.assertEventCount( 1 );
    handler.assertNextEvent( SubscriptionUpdateRequestQueuedEvent.class, e -> {
      assertEquals( e.getAddress(), address );
      assertEquals( e.getFilter(), filter );
    } );
  }

  @Test
  public void convergeAreaOfInterest_disposedAreaOfInterest()
  {
    final TestConnector connector = TestConnector.create();
    connector.setConnection( new Connection( connector, ValueUtil.randomString() ) );
    safeAction( () -> connector.setState( ConnectorState.CONNECTED ) );

    final ChannelAddress address = new ChannelAddress( 1, 0 );
    final String filter = ValueUtil.randomString();
    final AreaOfInterest areaOfInterest =
      safeAction( () -> Replicant.context().createOrUpdateAreaOfInterest( address, filter ) );

    Disposable.dispose( areaOfInterest );

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class,
                    () -> safeAction( () -> Replicant.context().getConverger()
                      .convergeAreaOfInterest( areaOfInterest, null, null ) ) );

    assertEquals( exception.getMessage(),
                  "Replicant-0020: Invoked convergeAreaOfInterest() with disposed AreaOfInterest." );
  }

  @Test
  public void convergeAreaOfInterest_subscribedButRemovePending()
  {
    final TestConnector connector = TestConnector.create();
    connector.setConnection( new Connection( connector, ValueUtil.randomString() ) );
    connector.pauseMessageScheduler();
    safeAction( () -> connector.setState( ConnectorState.CONNECTED ) );

    final ChannelAddress address = new ChannelAddress( 1, 0 );
    final AreaOfInterest areaOfInterest =
      safeAction( () -> Replicant.context().createOrUpdateAreaOfInterest( address, null ) );
    createSubscription( address, null, true );
    connector.requestUnsubscribe( address );

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

    final Converger.Action result = safeAction( () -> Replicant.context().getConverger()
      .convergeAreaOfInterest( areaOfInterest, null, null ) );

    assertEquals( result, Converger.Action.SUBMITTED_ADD );

    handler.assertEventCount( 1 );
    handler.assertNextEvent( SubscribeRequestQueuedEvent.class, e -> assertEquals( e.getAddress(), address ) );
  }

  @Test
  public void convergeAreaOfInterest_groupingAdd()
  {
    final TestConnector connector = TestConnector.create();
    connector.setConnection( new Connection( connector, ValueUtil.randomString() ) );
    safeAction( () -> connector.setState( ConnectorState.CONNECTED ) );
    connector.pauseMessageScheduler();

    final ChannelAddress address1 = new ChannelAddress( 1, 1, 1 );
    final ChannelAddress address2 = new ChannelAddress( 1, 1, 2 );
    final AreaOfInterest areaOfInterest1 =
      safeAction( () -> Replicant.context().createOrUpdateAreaOfInterest( address1, null ) );
    final AreaOfInterest areaOfInterest2 =
      safeAction( () -> Replicant.context().createOrUpdateAreaOfInterest( address2, null ) );

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

    final Converger.Action result = safeAction( () -> Replicant.context().getConverger()
      .convergeAreaOfInterest( areaOfInterest2, areaOfInterest1, AreaOfInterestRequest.Type.ADD ) );

    assertEquals( result, Converger.Action.SUBMITTED_ADD );

    handler.assertEventCount( 1 );
    handler.assertNextEvent( SubscribeRequestQueuedEvent.class, e -> assertEquals( e.getAddress(), address2 ) );
  }

  @Test
  public void convergeAreaOfInterest_typeDiffers()
  {
    final TestConnector connector = TestConnector.create();
    connector.setConnection( new Connection( connector, ValueUtil.randomString() ) );
    safeAction( () -> connector.setState( ConnectorState.CONNECTED ) );

    final ChannelAddress address1 = new ChannelAddress( 1, 1, 1 );
    final ChannelAddress address2 = new ChannelAddress( 1, 1, 2 );
    final AreaOfInterest areaOfInterest1 =
      safeAction( () -> Replicant.context().createOrUpdateAreaOfInterest( address1, null ) );

    // areaOfInterest2 would actually require an update as already present
    final AreaOfInterest areaOfInterest2 =
      safeAction( () -> Replicant.context().createOrUpdateAreaOfInterest( address2, null ) );
    createSubscription( address2, ValueUtil.randomString(), true );

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

    final Converger.Action result = safeAction( () -> Replicant.context().getConverger()
      .convergeAreaOfInterest( areaOfInterest2, areaOfInterest1, AreaOfInterestRequest.Type.ADD ) );

    assertEquals( result, Converger.Action.NO_ACTION );

    handler.assertEventCount( 0 );
  }

  @Test
  public void convergeAreaOfInterest_FilterDiffers()
  {
    final TestConnector connector = TestConnector.create();
    connector.setConnection( new Connection( connector, ValueUtil.randomString() ) );
    safeAction( () -> connector.setState( ConnectorState.CONNECTED ) );

    final ChannelAddress address1 = new ChannelAddress( 1, 1, 1 );
    final ChannelAddress address2 = new ChannelAddress( 1, 1, 2 );
    final AreaOfInterest areaOfInterest1 =
      safeAction( () -> Replicant.context().createOrUpdateAreaOfInterest( address1, ValueUtil.randomString() ) );
    final AreaOfInterest areaOfInterest2 =
      safeAction( () -> Replicant.context().createOrUpdateAreaOfInterest( address2, ValueUtil.randomString() ) );

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

    final Converger.Action result = safeAction( () -> Replicant.context().getConverger()
      .convergeAreaOfInterest( areaOfInterest2, areaOfInterest1, AreaOfInterestRequest.Type.ADD ) );

    assertEquals( result, Converger.Action.NO_ACTION );

    handler.assertEventCount( 0 );
  }

  @Test
  public void convergeAreaOfInterest_ChannelDiffers()
  {
    final TestConnector connector = TestConnector.create();
    connector.setConnection( new Connection( connector, ValueUtil.randomString() ) );
    safeAction( () -> connector.setState( ConnectorState.CONNECTED ) );

    final ChannelAddress address1 = new ChannelAddress( 1, 1, 1 );
    final ChannelAddress address2 = new ChannelAddress( 1, 0 );
    final AreaOfInterest areaOfInterest1 =
      safeAction( () -> Replicant.context().createOrUpdateAreaOfInterest( address1, null ) );
    final AreaOfInterest areaOfInterest2 =
      safeAction( () -> Replicant.context().createOrUpdateAreaOfInterest( address2, null ) );

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

    final Converger.Action result = safeAction( () -> Replicant.context().getConverger()
      .convergeAreaOfInterest( areaOfInterest2, areaOfInterest1, AreaOfInterestRequest.Type.ADD ) );

    assertEquals( result, Converger.Action.NO_ACTION );

    handler.assertEventCount( 0 );
  }

  @Test
  public void convergeAreaOfInterest_groupingUpdate()
  {
    final TestConnector connector = TestConnector.create();
    connector.setConnection( new Connection( connector, ValueUtil.randomString() ) );
    safeAction( () -> connector.setState( ConnectorState.CONNECTED ) );
    connector.pauseMessageScheduler();

    final ChannelAddress address1 = new ChannelAddress( 1, 1, 1 );
    final ChannelAddress address2 = new ChannelAddress( 1, 1, 2 );

    final String filterOld = ValueUtil.randomString();
    final String filterNew = ValueUtil.randomString();

    final AreaOfInterest areaOfInterest1 =
      safeAction( () -> Replicant.context().createOrUpdateAreaOfInterest( address1, filterNew ) );
    createSubscription( address1, filterOld, true );
    final AreaOfInterest areaOfInterest2 =
      safeAction( () -> Replicant.context().createOrUpdateAreaOfInterest( address2, filterNew ) );
    createSubscription( address2, filterOld, true );

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

    final Converger.Action result = safeAction( () -> Replicant.context().getConverger()
      .convergeAreaOfInterest( areaOfInterest2, areaOfInterest1, AreaOfInterestRequest.Type.UPDATE ) );

    assertEquals( result, Converger.Action.SUBMITTED_UPDATE );

    handler.assertEventCount( 1 );
    handler.assertNextEvent( SubscriptionUpdateRequestQueuedEvent.class,
                             e -> assertEquals( e.getAddress(), address2 ) );
  }

  @Test
  public void convergeAreaOfInterest_typeDiffersForUpdate()
  {
    final TestConnector connector = TestConnector.create();
    connector.setConnection( new Connection( connector, ValueUtil.randomString() ) );
    safeAction( () -> connector.setState( ConnectorState.CONNECTED ) );

    final ChannelAddress address1 = new ChannelAddress( 1, 1, 1 );
    final ChannelAddress address2 = new ChannelAddress( 1, 1, 2 );

    final String filterOld = ValueUtil.randomString();
    final String filterNew = ValueUtil.randomString();

    final AreaOfInterest areaOfInterest1 =
      safeAction( () -> Replicant.context().createOrUpdateAreaOfInterest( address1, filterNew ) );
    createSubscription( address1, filterOld, true );
    final AreaOfInterest areaOfInterest2 =
      safeAction( () -> Replicant.context().createOrUpdateAreaOfInterest( address2, filterNew ) );

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

    final Converger.Action result = safeAction( () -> Replicant.context().getConverger()
      .convergeAreaOfInterest( areaOfInterest2, areaOfInterest1, AreaOfInterestRequest.Type.UPDATE ) );

    assertEquals( result, Converger.Action.NO_ACTION );

    handler.assertEventCount( 0 );
  }

  @Test
  public void convergeAreaOfInterest_ChannelDiffersForUpdate()
  {
    final TestConnector connector = TestConnector.create();
    connector.setConnection( new Connection( connector, ValueUtil.randomString() ) );
    safeAction( () -> connector.setState( ConnectorState.CONNECTED ) );

    final ChannelAddress address1 = new ChannelAddress( 1, 1, 1 );
    final ChannelAddress address2 = new ChannelAddress( 1, 0 );

    final String filterOld = ValueUtil.randomString();
    final String filterNew = ValueUtil.randomString();

    final AreaOfInterest areaOfInterest1 =
      safeAction( () -> Replicant.context().createOrUpdateAreaOfInterest( address1, filterNew ) );
    createSubscription( address1, filterOld, true );
    final AreaOfInterest areaOfInterest2 =
      safeAction( () -> Replicant.context().createOrUpdateAreaOfInterest( address2, filterNew ) );
    createSubscription( address2, filterOld, true );

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

    final Converger.Action result = safeAction( () -> Replicant.context().getConverger()
      .convergeAreaOfInterest( areaOfInterest2, areaOfInterest1, AreaOfInterestRequest.Type.UPDATE ) );

    assertEquals( result, Converger.Action.NO_ACTION );

    handler.assertEventCount( 0 );
  }

  @Test
  public void convergeAreaOfInterest_FilterDiffersForUpdate()
  {
    final TestConnector connector = TestConnector.create();
    connector.setConnection( new Connection( connector, ValueUtil.randomString() ) );
    safeAction( () -> connector.setState( ConnectorState.CONNECTED ) );

    final ChannelAddress address1 = new ChannelAddress( 1, 1, 1 );
    final ChannelAddress address2 = new ChannelAddress( 1, 1, 2 );

    final String filterOld = ValueUtil.randomString();

    final AreaOfInterest areaOfInterest1 =
      safeAction( () -> Replicant.context().createOrUpdateAreaOfInterest( address1, ValueUtil.randomString() ) );
    createSubscription( address1, filterOld, true );
    final AreaOfInterest areaOfInterest2 =
      safeAction( () -> Replicant.context().createOrUpdateAreaOfInterest( address2, ValueUtil.randomString() ) );
    createSubscription( address2, filterOld, true );

    final TestSpyEventHandler handler = registerTestSpyEventHandler();

    final Converger.Action result = safeAction( () -> Replicant.context().getConverger()
      .convergeAreaOfInterest( areaOfInterest2, areaOfInterest1, AreaOfInterestRequest.Type.UPDATE ) );

    assertEquals( result, Converger.Action.NO_ACTION );

    handler.assertEventCount( 0 );
  }
}
