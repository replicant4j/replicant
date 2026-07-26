package replicant;

import static org.testng.Assert.*;

import arez.Disposable;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import replicant.spy.AreaOfInterestStatusUpdatedEvent;
import replicant.spy.SubscribeRequestQueuedEvent;
import replicant.spy.SubscriptionOrphanedEvent;
import replicant.spy.SubscriptionUpdateRequestQueuedEvent;
import replicant.spy.UnsubscribeRequestQueuedEvent;

public class SubscriptionReconcilerTest extends AbstractReplicantTest {
    @BeforeMethod
    @Override
    public void preTest() throws Exception {
        super.preTest();
        // Pause scheduler so the SubscriptionReconciler can be exercised manually
        pauseScheduler();
    }

    @Test
    public void construct_withUnnecessaryContext() {
        final ReplicantContext context = Replicant.context();
        final IllegalStateException exception =
                expectThrows(IllegalStateException.class, () -> SubscriptionReconciler.create(context));
        assertEquals(
                exception.getMessage(),
                "Replicant-0037: ReplicantService passed a context but Replicant.areZonesEnabled() is false");
    }

    @Test
    public void getReplicantContext() {
        final ReplicantContext context = Replicant.context();
        final SubscriptionReconciler subscriptionReconciler = context.getSubscriptionReconciler();
        assertEquals(subscriptionReconciler.getReplicantContext(), context);
        assertNull(getFieldValue(subscriptionReconciler, "_context"));
    }

    @Test
    public void getReplicantContext_zonesEnabled() {
        ReplicantTestUtil.enableZones();
        ReplicantTestUtil.resetState();

        final ReplicantContext context = Replicant.context();
        final SubscriptionReconciler subscriptionReconciler = context.getSubscriptionReconciler();
        assertEquals(subscriptionReconciler.getReplicantContext(), context);
        assertEquals(getFieldValue(subscriptionReconciler, "_context"), context);
    }

    @Test
    public void preReconciliationAction() {
        final SubscriptionReconciler c = Replicant.context().getSubscriptionReconciler();

        // should do nothing ...
        c.preReconciliation();

        final AtomicInteger callCount = new AtomicInteger();

        safeAction(() -> c.setPreReconciliationAction(callCount::incrementAndGet));

        c.preReconciliation();

        assertEquals(callCount.get(), 1);

        c.preReconciliation();

        assertEquals(callCount.get(), 2);

        safeAction(() -> c.setPreReconciliationAction(null));

        c.preReconciliation();

        assertEquals(callCount.get(), 2);
    }

    @Test
    public void reconciliationCompleteAction() {
        final SubscriptionReconciler c = Replicant.context().getSubscriptionReconciler();

        // should do nothing ...
        safeAction(c::reconciliationComplete);

        final AtomicInteger callCount = new AtomicInteger();

        safeAction(() -> c.setReconciliationCompleteAction(callCount::incrementAndGet));

        safeAction(c::reconciliationComplete);

        assertEquals(callCount.get(), 1);

        safeAction(c::reconciliationComplete);

        assertEquals(callCount.get(), 2);

        safeAction(() -> c.setReconciliationCompleteAction(null));

        safeAction(c::reconciliationComplete);

        assertEquals(callCount.get(), 2);
    }

    @Test
    public void canGroup() {
        final SubscriptionReconciler c = Replicant.context().getSubscriptionReconciler();

        safeAction(() -> {
            final DatasetAddress datasetAddress = new DatasetAddress(1, 0);
            final AreaOfInterest areaOfInterest =
                    Replicant.context().createOrUpdateAreaOfInterest(datasetAddress, null);

            assertTrue(c.canGroup(
                    areaOfInterest, AreaOfInterestRequest.Type.ADD, areaOfInterest, AreaOfInterestRequest.Type.ADD));
            assertFalse(c.canGroup(
                    areaOfInterest, AreaOfInterestRequest.Type.ADD, areaOfInterest, AreaOfInterestRequest.Type.UPDATE));
            assertFalse(c.canGroup(
                    areaOfInterest, AreaOfInterestRequest.Type.ADD, areaOfInterest, AreaOfInterestRequest.Type.REMOVE));
            assertFalse(c.canGroup(
                    areaOfInterest, AreaOfInterestRequest.Type.UPDATE, areaOfInterest, AreaOfInterestRequest.Type.ADD));
            assertTrue(c.canGroup(
                    areaOfInterest,
                    AreaOfInterestRequest.Type.UPDATE,
                    areaOfInterest,
                    AreaOfInterestRequest.Type.UPDATE));
            assertFalse(c.canGroup(
                    areaOfInterest,
                    AreaOfInterestRequest.Type.UPDATE,
                    areaOfInterest,
                    AreaOfInterestRequest.Type.REMOVE));
            assertFalse(c.canGroup(
                    areaOfInterest, AreaOfInterestRequest.Type.REMOVE, areaOfInterest, AreaOfInterestRequest.Type.ADD));
            assertFalse(c.canGroup(
                    areaOfInterest,
                    AreaOfInterestRequest.Type.REMOVE,
                    areaOfInterest,
                    AreaOfInterestRequest.Type.UPDATE));
            assertTrue(c.canGroup(
                    areaOfInterest,
                    AreaOfInterestRequest.Type.REMOVE,
                    areaOfInterest,
                    AreaOfInterestRequest.Type.REMOVE));

            final DatasetAddress datasetAddress2 = new DatasetAddress(1, 0, 2);
            final AreaOfInterest areaOfInterest2 =
                    Replicant.context().createOrUpdateAreaOfInterest(datasetAddress2, null);
            assertTrue(c.canGroup(
                    areaOfInterest, AreaOfInterestRequest.Type.ADD, areaOfInterest2, AreaOfInterestRequest.Type.ADD));

            final DatasetAddress datasetAddress3 = new DatasetAddress(1, 1, 1);
            final AreaOfInterest areaOfInterest3 =
                    Replicant.context().createOrUpdateAreaOfInterest(datasetAddress3, null);
            assertFalse(c.canGroup(
                    areaOfInterest, AreaOfInterestRequest.Type.ADD, areaOfInterest3, AreaOfInterestRequest.Type.ADD));
            assertFalse(c.canGroup(
                    areaOfInterest,
                    AreaOfInterestRequest.Type.ADD,
                    areaOfInterest3,
                    AreaOfInterestRequest.Type.UPDATE));
            assertFalse(c.canGroup(
                    areaOfInterest,
                    AreaOfInterestRequest.Type.ADD,
                    areaOfInterest3,
                    AreaOfInterestRequest.Type.REMOVE));

            final DatasetAddress datasetAddress4 = new DatasetAddress(1, 0, 1);
            final AreaOfInterest areaOfInterest4 =
                    Replicant.context().createOrUpdateAreaOfInterest(datasetAddress4, "Filter");
            assertFalse(c.canGroup(
                    areaOfInterest, AreaOfInterestRequest.Type.ADD, areaOfInterest4, AreaOfInterestRequest.Type.ADD));
            areaOfInterest.setFilter("Filter");
            assertTrue(c.canGroup(
                    areaOfInterest, AreaOfInterestRequest.Type.ADD, areaOfInterest4, AreaOfInterestRequest.Type.ADD));
        });
    }

    @Test
    public void removeOrphanSubscription() {
        final Connector connector = createConnector();
        newConnection(connector);

        pauseScheduler();
        connector.pauseMessageScheduler();

        final DatasetAddress datasetAddress = new DatasetAddress(1, 0);

        safeAction(() -> {
            final Subscription subscription = createSubscription(datasetAddress, null, true);

            connector.setState(ConnectorState.CONNECTED);

            final TestSpyEventHandler handler = registerTestSpyEventHandler();

            Replicant.context().getSubscriptionReconciler().removeOrphanSubscriptions();

            final List<AreaOfInterestRequest> requests =
                    connector.ensureConnection().getPendingAreaOfInterestRequests();
            assertEquals(requests.size(), 1);
            final AreaOfInterestRequest request = requests.get(0);
            assertEquals(request.getType(), AreaOfInterestRequest.Type.REMOVE);
            assertEquals(request.getDatasetAddress(), datasetAddress);

            handler.assertEventCount(2);

            handler.assertNextEvent(
                    SubscriptionOrphanedEvent.class, e -> assertEquals(e.getSubscription(), subscription));
            handler.assertNextEvent(
                    UnsubscribeRequestQueuedEvent.class, e -> assertEquals(e.getDatasetAddress(), datasetAddress));
        });
    }

    @Test
    public void removeOrphanSubscription_whenManyPresent() {
        final Connector connector = createConnector();
        newConnection(connector);

        pauseScheduler();
        connector.pauseMessageScheduler();

        final DatasetAddress datasetAddress = new DatasetAddress(1, 0);

        safeAction(() -> {
            Replicant.context().createOrUpdateAreaOfInterest(new DatasetAddress(1, 1, 1), null);
            Replicant.context().createOrUpdateAreaOfInterest(new DatasetAddress(1, 1, 2), null);
            Replicant.context().createOrUpdateAreaOfInterest(new DatasetAddress(1, 1, 3), null);
            Replicant.context().createOrUpdateAreaOfInterest(new DatasetAddress(1, 1, 4), null);
            Replicant.context().createOrUpdateAreaOfInterest(new DatasetAddress(1, 1, 5), null);

            final Subscription subscription = createSubscription(datasetAddress, null, true);

            connector.setState(ConnectorState.CONNECTED);

            final TestSpyEventHandler handler = registerTestSpyEventHandler();

            Replicant.context().getSubscriptionReconciler().removeOrphanSubscriptions();

            final List<AreaOfInterestRequest> requests =
                    connector.ensureConnection().getPendingAreaOfInterestRequests();
            assertEquals(requests.size(), 1);
            final AreaOfInterestRequest request = requests.get(0);
            assertEquals(request.getType(), AreaOfInterestRequest.Type.REMOVE);
            assertEquals(request.getDatasetAddress(), datasetAddress);

            handler.assertEventCount(2);

            handler.assertNextEvent(
                    SubscriptionOrphanedEvent.class, e -> assertEquals(e.getSubscription(), subscription));
            handler.assertNextEvent(
                    UnsubscribeRequestQueuedEvent.class, e -> assertEquals(e.getDatasetAddress(), datasetAddress));
        });
    }

    @Test
    public void removeOrphanSubscriptions_whenConnectorDisconnected() {
        final Connector connector = createConnector();
        final DatasetAddress datasetAddress = new DatasetAddress(1, 0);

        safeAction(() -> {
            createSubscription(datasetAddress, null, true);

            connector.setState(ConnectorState.DISCONNECTED);

            final TestSpyEventHandler handler = registerTestSpyEventHandler();

            Replicant.context().getSubscriptionReconciler().removeOrphanSubscriptions();

            handler.assertEventCount(0);
        });
    }

    @Test
    public void removeOrphanSubscriptions_whenSubscriptionImplicit() {
        final Connector connector = createConnector();
        newConnection(connector);
        final DatasetAddress datasetAddress = new DatasetAddress(1, 0);

        safeAction(() -> {
            createSubscription(datasetAddress, null, false);

            connector.setState(ConnectorState.CONNECTED);

            final TestSpyEventHandler handler = registerTestSpyEventHandler();

            Replicant.context().getSubscriptionReconciler().removeOrphanSubscriptions();

            handler.assertEventCount(0);
        });
    }

    @Test
    public void removeOrphanSubscriptions_whenSubscriptionExpected() {
        final Connector connector = createConnector();
        newConnection(connector);
        final DatasetAddress datasetAddress = new DatasetAddress(1, 0);

        safeAction(() -> {

            // Add expectation
            Replicant.context().createOrUpdateAreaOfInterest(datasetAddress, null);

            createSubscription(datasetAddress, null, true);

            connector.setState(ConnectorState.CONNECTED);

            final TestSpyEventHandler handler = registerTestSpyEventHandler();

            Replicant.context().getSubscriptionReconciler().removeOrphanSubscriptions();

            handler.assertEventCount(0);
        });
    }

    @Test
    public void removeOrphanSubscriptions_whenRemoveIsPending() {
        final Connector connector = createConnector();
        newConnection(connector);
        connector.pauseMessageScheduler();

        final DatasetAddress datasetAddress = new DatasetAddress(1, 0);

        createSubscription(datasetAddress, null, true);

        // Enqueue remove request
        connector.requestUnsubscribe(datasetAddress);

        final TestSpyEventHandler handler = registerTestSpyEventHandler();
        safeAction(() -> connector.setState(ConnectorState.CONNECTED));
        Replicant.context().getSubscriptionReconciler().removeOrphanSubscriptions();
        handler.assertEventCount(0);
    }

    @Test
    public void reconcileAreaOfInterest() {
        final Connector connector = createConnector();
        newConnection(connector);
        pauseScheduler();
        connector.pauseMessageScheduler();

        safeAction(() -> connector.setState(ConnectorState.CONNECTED));

        final DatasetAddress datasetAddress = new DatasetAddress(1, 0);
        final AreaOfInterest areaOfInterest =
                safeAction(() -> Replicant.context().createOrUpdateAreaOfInterest(datasetAddress, null));

        final TestSpyEventHandler handler = registerTestSpyEventHandler();

        final SubscriptionReconciler.Action result =
                Replicant.context().getSubscriptionReconciler().reconcileAreaOfInterest(areaOfInterest, null, null);

        assertEquals(result, SubscriptionReconciler.Action.SUBMITTED_ADD);

        handler.assertEventCount(1);
        handler.assertNextEvent(
                SubscribeRequestQueuedEvent.class, e -> assertEquals(e.getDatasetAddress(), datasetAddress));
    }

    @Test
    public void reconcileAreaOfInterest_alreadySubscribed() {
        final Connector connector = createConnector();
        newConnection(connector);
        safeAction(() -> connector.setState(ConnectorState.CONNECTED));

        final DatasetAddress datasetAddress = new DatasetAddress(1, 0);
        final AreaOfInterest areaOfInterest =
                safeAction(() -> Replicant.context().createOrUpdateAreaOfInterest(datasetAddress, null));
        createSubscription(datasetAddress, null, true);

        final TestSpyEventHandler handler = registerTestSpyEventHandler();

        final SubscriptionReconciler.Action result =
                Replicant.context().getSubscriptionReconciler().reconcileAreaOfInterest(areaOfInterest, null, null);

        assertEquals(result, SubscriptionReconciler.Action.NO_ACTION);

        assertEquals(areaOfInterest.getStatus(), AreaOfInterest.Status.LOADED);

        handler.assertEventCount(1);
        handler.assertNextEvent(
                AreaOfInterestStatusUpdatedEvent.class, e -> assertEquals(e.getAreaOfInterest(), areaOfInterest));
    }

    @Test
    public void reconcileAreaOfInterest_subscribing() {
        final Connector connector = createConnector();
        final Connection connection = newConnection(connector);
        safeAction(() -> connector.setState(ConnectorState.CONNECTED));

        final DatasetAddress datasetAddress = new DatasetAddress(1, 0);
        final AreaOfInterest areaOfInterest =
                safeAction(() -> Replicant.context().createOrUpdateAreaOfInterest(datasetAddress, null));

        connection.injectCurrentAreaOfInterestRequest(
                new AreaOfInterestRequest(datasetAddress, AreaOfInterestRequest.Type.ADD, null));
        final TestSpyEventHandler handler = registerTestSpyEventHandler();

        final SubscriptionReconciler.Action result =
                Replicant.context().getSubscriptionReconciler().reconcileAreaOfInterest(areaOfInterest, null, null);

        assertEquals(result, SubscriptionReconciler.Action.IN_PROGRESS);

        handler.assertEventCount(0);
    }

    @Test
    public void reconcileAreaOfInterest_addPending() {
        final Connector connector = createConnector();
        newConnection(connector);
        connector.pauseMessageScheduler();
        safeAction(() -> connector.setState(ConnectorState.CONNECTED));

        final DatasetAddress datasetAddress = new DatasetAddress(1, 0);
        final AreaOfInterest areaOfInterest =
                safeAction(() -> Replicant.context().createOrUpdateAreaOfInterest(datasetAddress, null));

        connector.requestSubscribe(datasetAddress, null);

        final TestSpyEventHandler handler = registerTestSpyEventHandler();

        final SubscriptionReconciler.Action result =
                Replicant.context().getSubscriptionReconciler().reconcileAreaOfInterest(areaOfInterest, null, null);

        assertEquals(result, SubscriptionReconciler.Action.IN_PROGRESS);

        handler.assertEventCount(0);
    }

    @Test
    public void reconcileAreaOfInterest_updatePending() {
        final Dataset dataset = new Dataset(
                0,
                ValueUtil.randomString(),
                null,
                Dataset.FilterType.DYNAMIC,
                false,
                (f, e) -> true,
                false,
                true,
                Collections.emptyList());
        final SystemSchema schema =
                new SystemSchema(1, ValueUtil.randomString(), new Dataset[] {dataset}, new EntityType[0]);
        final Connector connector = createConnector(schema);
        newConnection(connector);
        connector.pauseMessageScheduler();
        safeAction(() -> connector.setState(ConnectorState.CONNECTED));

        final DatasetAddress datasetAddress = new DatasetAddress(1, 0);
        final Object filter = ValueUtil.randomString();
        final AreaOfInterest areaOfInterest =
                safeAction(() -> Replicant.context().createOrUpdateAreaOfInterest(datasetAddress, filter));
        createSubscription(datasetAddress, ValueUtil.randomString(), true);

        connector.requestSubscriptionUpdate(datasetAddress, filter);

        final TestSpyEventHandler handler = registerTestSpyEventHandler();

        final SubscriptionReconciler.Action result =
                Replicant.context().getSubscriptionReconciler().reconcileAreaOfInterest(areaOfInterest, null, null);

        assertEquals(result, SubscriptionReconciler.Action.IN_PROGRESS);

        handler.assertEventCount(0);
    }

    @Test
    public void reconcileAreaOfInterest_requestSubscriptionUpdate() {
        final Dataset dataset = new Dataset(
                0,
                ValueUtil.randomString(),
                null,
                Dataset.FilterType.DYNAMIC,
                false,
                (f, e) -> true,
                false,
                true,
                Collections.emptyList());
        final SystemSchema schema =
                new SystemSchema(1, ValueUtil.randomString(), new Dataset[] {dataset}, new EntityType[0]);
        final Connector connector = createConnector(schema);
        newConnection(connector);
        safeAction(() -> connector.setState(ConnectorState.CONNECTED));
        connector.pauseMessageScheduler();

        final DatasetAddress datasetAddress = new DatasetAddress(1, 0);
        final String filter = ValueUtil.randomString();
        final AreaOfInterest areaOfInterest =
                safeAction(() -> Replicant.context().createOrUpdateAreaOfInterest(datasetAddress, filter));
        createSubscription(datasetAddress, ValueUtil.randomString(), true);

        final TestSpyEventHandler handler = registerTestSpyEventHandler();

        final SubscriptionReconciler.Action result =
                Replicant.context().getSubscriptionReconciler().reconcileAreaOfInterest(areaOfInterest, null, null);

        assertEquals(result, SubscriptionReconciler.Action.SUBMITTED_UPDATE);

        handler.assertEventCount(1);
        handler.assertNextEvent(SubscriptionUpdateRequestQueuedEvent.class, e -> {
            assertEquals(e.getDatasetAddress(), datasetAddress);
            assertEquals(e.getFilter(), filter);
        });
    }

    @Test
    public void reconcileAreaOfInterest_disposedAreaOfInterest() {
        final Connector connector = createConnector();
        newConnection(connector);
        safeAction(() -> connector.setState(ConnectorState.CONNECTED));

        final DatasetAddress datasetAddress = new DatasetAddress(1, 0);
        final String filter = ValueUtil.randomString();
        final AreaOfInterest areaOfInterest =
                safeAction(() -> Replicant.context().createOrUpdateAreaOfInterest(datasetAddress, filter));

        Disposable.dispose(areaOfInterest);

        final IllegalStateException exception = expectThrows(
                IllegalStateException.class,
                () -> safeAction(() -> Replicant.context()
                        .getSubscriptionReconciler()
                        .reconcileAreaOfInterest(areaOfInterest, null, null)));

        assertEquals(
                exception.getMessage(),
                "Replicant-0020: Invoked reconcileAreaOfInterest() with disposed AreaOfInterest.");
    }

    @Test
    public void reconcileAreaOfInterest_subscribedButRemovePending() {
        final Connector connector = createConnector();
        newConnection(connector);
        connector.pauseMessageScheduler();
        safeAction(() -> connector.setState(ConnectorState.CONNECTED));

        final DatasetAddress datasetAddress = new DatasetAddress(1, 0);
        final AreaOfInterest areaOfInterest =
                safeAction(() -> Replicant.context().createOrUpdateAreaOfInterest(datasetAddress, null));
        createSubscription(datasetAddress, null, true);
        connector.requestUnsubscribe(datasetAddress);

        final TestSpyEventHandler handler = registerTestSpyEventHandler();

        final SubscriptionReconciler.Action result =
                Replicant.context().getSubscriptionReconciler().reconcileAreaOfInterest(areaOfInterest, null, null);

        assertEquals(result, SubscriptionReconciler.Action.SUBMITTED_ADD);

        handler.assertEventCount(1);
        handler.assertNextEvent(
                SubscribeRequestQueuedEvent.class, e -> assertEquals(e.getDatasetAddress(), datasetAddress));
    }

    @Test
    public void reconcileAreaOfInterest_subscribedAndFilterDiffersButFilterTypeStatic() {
        final Dataset dataset0 = new Dataset(
                0,
                ValueUtil.randomString(),
                null,
                Dataset.FilterType.STATIC,
                false,
                null,
                true,
                true,
                Collections.emptyList());
        final SystemSchema schema =
                new SystemSchema(1, ValueUtil.randomString(), new Dataset[] {dataset0}, new EntityType[0]);
        final Connector connector = createConnector(schema);
        newConnection(connector);
        connector.pauseMessageScheduler();
        safeAction(() -> connector.setState(ConnectorState.CONNECTED));

        final DatasetAddress datasetAddress = new DatasetAddress(1, 0);
        final AreaOfInterest areaOfInterest = safeAction(
                () -> Replicant.context().createOrUpdateAreaOfInterest(datasetAddress, ValueUtil.randomString()));
        createSubscription(datasetAddress, ValueUtil.randomString(), true);

        final TestSpyEventHandler handler = registerTestSpyEventHandler();

        final SubscriptionReconciler.Action result =
                Replicant.context().getSubscriptionReconciler().reconcileAreaOfInterest(areaOfInterest, null, null);

        assertEquals(result, SubscriptionReconciler.Action.SUBMITTED_REMOVE);

        handler.assertEventCount(1);
        handler.assertNextEvent(
                UnsubscribeRequestQueuedEvent.class, e -> assertEquals(e.getDatasetAddress(), datasetAddress));
    }

    @Test
    public void reconcileAreaOfInterest_subscribedAndFilterDiffersButFilterTypeStaticAndNotExplicitlySubscribed() {
        final Dataset dataset0 = new Dataset(
                0,
                ValueUtil.randomString(),
                null,
                Dataset.FilterType.STATIC,
                false,
                null,
                true,
                true,
                Collections.emptyList());
        final SystemSchema schema =
                new SystemSchema(1, ValueUtil.randomString(), new Dataset[] {dataset0}, new EntityType[0]);
        final Connector connector = createConnector(schema);
        newConnection(connector);
        connector.pauseMessageScheduler();
        safeAction(() -> connector.setState(ConnectorState.CONNECTED));

        final DatasetAddress datasetAddress = new DatasetAddress(1, 0);
        final AreaOfInterest areaOfInterest = safeAction(
                () -> Replicant.context().createOrUpdateAreaOfInterest(datasetAddress, ValueUtil.randomString()));
        createSubscription(datasetAddress, ValueUtil.randomString(), false);

        final IllegalStateException exception = expectThrows(
                IllegalStateException.class,
                () -> safeAction(() -> Replicant.context()
                        .getSubscriptionReconciler()
                        .reconcileAreaOfInterest(areaOfInterest, null, null)));

        assertEquals(
                exception.getMessage(),
                "Replicant-0083: Attempting to update Dataset Address 1.0 but the Dataset does not allow dynamic filter"
                        + " updates and has not been explicitly subscribed.");
    }

    @Test
    public void reconcileAreaOfInterest_groupingAdd() {
        final Connector connector = createConnector();
        newConnection(connector);
        safeAction(() -> connector.setState(ConnectorState.CONNECTED));
        connector.pauseMessageScheduler();

        final DatasetAddress datasetAddress1 = new DatasetAddress(1, 1, 1);
        final DatasetAddress datasetAddress2 = new DatasetAddress(1, 1, 2);
        final AreaOfInterest areaOfInterest1 =
                safeAction(() -> Replicant.context().createOrUpdateAreaOfInterest(datasetAddress1, null));
        final AreaOfInterest areaOfInterest2 =
                safeAction(() -> Replicant.context().createOrUpdateAreaOfInterest(datasetAddress2, null));

        final TestSpyEventHandler handler = registerTestSpyEventHandler();

        final SubscriptionReconciler.Action result = Replicant.context()
                .getSubscriptionReconciler()
                .reconcileAreaOfInterest(areaOfInterest2, areaOfInterest1, AreaOfInterestRequest.Type.ADD);

        assertEquals(result, SubscriptionReconciler.Action.SUBMITTED_ADD);

        handler.assertEventCount(1);
        handler.assertNextEvent(
                SubscribeRequestQueuedEvent.class, e -> assertEquals(e.getDatasetAddress(), datasetAddress2));
    }

    @Test
    public void reconcileAreaOfInterest_typeDiffers() {
        final Dataset dataset0 = new Dataset(
                0,
                ValueUtil.randomString(),
                null,
                Dataset.FilterType.DYNAMIC,
                false,
                (f, e) -> true,
                true,
                true,
                Collections.emptyList());
        final SystemSchema schema =
                new SystemSchema(1, ValueUtil.randomString(), new Dataset[] {dataset0}, new EntityType[0]);
        final Connector connector = createConnector(schema);
        newConnection(connector);
        safeAction(() -> connector.setState(ConnectorState.CONNECTED));

        final DatasetAddress datasetAddress1 = new DatasetAddress(1, 0, 1);
        final DatasetAddress datasetAddress2 = new DatasetAddress(1, 0, 2);
        final AreaOfInterest areaOfInterest1 =
                safeAction(() -> Replicant.context().createOrUpdateAreaOfInterest(datasetAddress1, null));

        // areaOfInterest2 would actually require an update as already present
        final AreaOfInterest areaOfInterest2 =
                safeAction(() -> Replicant.context().createOrUpdateAreaOfInterest(datasetAddress2, null));
        createSubscription(datasetAddress2, ValueUtil.randomString(), true);

        final TestSpyEventHandler handler = registerTestSpyEventHandler();

        final SubscriptionReconciler.Action result = Replicant.context()
                .getSubscriptionReconciler()
                .reconcileAreaOfInterest(areaOfInterest2, areaOfInterest1, AreaOfInterestRequest.Type.ADD);

        assertEquals(result, SubscriptionReconciler.Action.NO_ACTION);

        handler.assertEventCount(0);
    }

    @Test
    public void reconcileAreaOfInterest_FilterDiffers() {
        final Connector connector = createConnector();
        newConnection(connector);
        safeAction(() -> connector.setState(ConnectorState.CONNECTED));

        final DatasetAddress datasetAddress1 = new DatasetAddress(1, 1, 1);
        final DatasetAddress datasetAddress2 = new DatasetAddress(1, 1, 2);
        final AreaOfInterest areaOfInterest1 = safeAction(
                () -> Replicant.context().createOrUpdateAreaOfInterest(datasetAddress1, ValueUtil.randomString()));
        final AreaOfInterest areaOfInterest2 = safeAction(
                () -> Replicant.context().createOrUpdateAreaOfInterest(datasetAddress2, ValueUtil.randomString()));

        final TestSpyEventHandler handler = registerTestSpyEventHandler();

        final SubscriptionReconciler.Action result = Replicant.context()
                .getSubscriptionReconciler()
                .reconcileAreaOfInterest(areaOfInterest2, areaOfInterest1, AreaOfInterestRequest.Type.ADD);

        assertEquals(result, SubscriptionReconciler.Action.NO_ACTION);

        handler.assertEventCount(0);
    }

    @Test
    public void reconcileAreaOfInterest_DatasetDiffers() {
        final Connector connector = createConnector();
        newConnection(connector);
        safeAction(() -> connector.setState(ConnectorState.CONNECTED));

        final DatasetAddress datasetAddress1 = new DatasetAddress(1, 1, 1);
        final DatasetAddress datasetAddress2 = new DatasetAddress(1, 0);
        final AreaOfInterest areaOfInterest1 =
                safeAction(() -> Replicant.context().createOrUpdateAreaOfInterest(datasetAddress1, null));
        final AreaOfInterest areaOfInterest2 =
                safeAction(() -> Replicant.context().createOrUpdateAreaOfInterest(datasetAddress2, null));

        final TestSpyEventHandler handler = registerTestSpyEventHandler();

        final SubscriptionReconciler.Action result = Replicant.context()
                .getSubscriptionReconciler()
                .reconcileAreaOfInterest(areaOfInterest2, areaOfInterest1, AreaOfInterestRequest.Type.ADD);

        assertEquals(result, SubscriptionReconciler.Action.NO_ACTION);

        handler.assertEventCount(0);
    }

    @Test
    public void reconcileAreaOfInterest_groupingUpdate() {
        final Dataset dataset = new Dataset(
                0,
                ValueUtil.randomString(),
                null,
                Dataset.FilterType.DYNAMIC,
                false,
                (f, e) -> true,
                false,
                true,
                Collections.emptyList());
        final SystemSchema schema =
                new SystemSchema(1, ValueUtil.randomString(), new Dataset[] {dataset}, new EntityType[0]);
        final Connector connector = createConnector(schema);
        newConnection(connector);
        safeAction(() -> connector.setState(ConnectorState.CONNECTED));
        connector.pauseMessageScheduler();

        final DatasetAddress datasetAddress1 = new DatasetAddress(1, 0, 1);
        final DatasetAddress datasetAddress2 = new DatasetAddress(1, 0, 2);

        final String filterOld = ValueUtil.randomString();
        final String filterNew = ValueUtil.randomString();

        final AreaOfInterest areaOfInterest1 =
                safeAction(() -> Replicant.context().createOrUpdateAreaOfInterest(datasetAddress1, filterNew));
        createSubscription(datasetAddress1, filterOld, true);
        final AreaOfInterest areaOfInterest2 =
                safeAction(() -> Replicant.context().createOrUpdateAreaOfInterest(datasetAddress2, filterNew));
        createSubscription(datasetAddress2, filterOld, true);

        final TestSpyEventHandler handler = registerTestSpyEventHandler();

        final SubscriptionReconciler.Action result = Replicant.context()
                .getSubscriptionReconciler()
                .reconcileAreaOfInterest(areaOfInterest2, areaOfInterest1, AreaOfInterestRequest.Type.UPDATE);

        assertEquals(result, SubscriptionReconciler.Action.SUBMITTED_UPDATE);

        handler.assertEventCount(1);
        handler.assertNextEvent(
                SubscriptionUpdateRequestQueuedEvent.class, e -> assertEquals(e.getDatasetAddress(), datasetAddress2));
    }

    @Test
    public void reconcileAreaOfInterest_typeDiffersForUpdate() {
        final Connector connector = createConnector();
        newConnection(connector);
        safeAction(() -> connector.setState(ConnectorState.CONNECTED));

        final DatasetAddress datasetAddress1 = new DatasetAddress(1, 1, 1);
        final DatasetAddress datasetAddress2 = new DatasetAddress(1, 1, 2);

        final String filterOld = ValueUtil.randomString();
        final String filterNew = ValueUtil.randomString();

        final AreaOfInterest areaOfInterest1 =
                safeAction(() -> Replicant.context().createOrUpdateAreaOfInterest(datasetAddress1, filterNew));
        createSubscription(datasetAddress1, filterOld, true);
        final AreaOfInterest areaOfInterest2 =
                safeAction(() -> Replicant.context().createOrUpdateAreaOfInterest(datasetAddress2, filterNew));

        final TestSpyEventHandler handler = registerTestSpyEventHandler();

        final SubscriptionReconciler.Action result = Replicant.context()
                .getSubscriptionReconciler()
                .reconcileAreaOfInterest(areaOfInterest2, areaOfInterest1, AreaOfInterestRequest.Type.UPDATE);

        assertEquals(result, SubscriptionReconciler.Action.NO_ACTION);

        handler.assertEventCount(0);
    }

    @Test
    public void reconcileAreaOfInterest_DatasetDiffersForUpdate() {
        final Dataset dataset0 = new Dataset(
                0,
                ValueUtil.randomString(),
                null,
                Dataset.FilterType.DYNAMIC,
                false,
                (f, e) -> true,
                true,
                true,
                Collections.emptyList());
        final Dataset dataset1 = new Dataset(
                1,
                ValueUtil.randomString(),
                null,
                Dataset.FilterType.DYNAMIC,
                false,
                (f, e) -> true,
                true,
                true,
                Collections.emptyList());
        final SystemSchema schema =
                new SystemSchema(1, ValueUtil.randomString(), new Dataset[] {dataset0, dataset1}, new EntityType[0]);
        final Connector connector = createConnector(schema);
        newConnection(connector);
        safeAction(() -> connector.setState(ConnectorState.CONNECTED));

        final DatasetAddress datasetAddress1 = new DatasetAddress(1, 1, 1);
        final DatasetAddress datasetAddress2 = new DatasetAddress(1, 0);

        final String filterOld = ValueUtil.randomString();
        final String filterNew = ValueUtil.randomString();

        final AreaOfInterest areaOfInterest1 =
                safeAction(() -> Replicant.context().createOrUpdateAreaOfInterest(datasetAddress1, filterNew));
        createSubscription(datasetAddress1, filterOld, true);
        final AreaOfInterest areaOfInterest2 =
                safeAction(() -> Replicant.context().createOrUpdateAreaOfInterest(datasetAddress2, filterNew));
        createSubscription(datasetAddress2, filterOld, true);

        final TestSpyEventHandler handler = registerTestSpyEventHandler();

        final SubscriptionReconciler.Action result = Replicant.context()
                .getSubscriptionReconciler()
                .reconcileAreaOfInterest(areaOfInterest2, areaOfInterest1, AreaOfInterestRequest.Type.UPDATE);

        assertEquals(result, SubscriptionReconciler.Action.NO_ACTION);

        handler.assertEventCount(0);
    }

    @Test
    public void reconcileAreaOfInterest_FilterDiffersForUpdate() {
        final Dataset dataset0 = new Dataset(
                0,
                ValueUtil.randomString(),
                null,
                Dataset.FilterType.DYNAMIC,
                false,
                (f, e) -> true,
                true,
                true,
                Collections.emptyList());
        final Dataset dataset1 = new Dataset(
                1,
                ValueUtil.randomString(),
                null,
                Dataset.FilterType.DYNAMIC,
                false,
                (f, e) -> true,
                true,
                true,
                Collections.emptyList());
        final SystemSchema schema =
                new SystemSchema(1, ValueUtil.randomString(), new Dataset[] {dataset0, dataset1}, new EntityType[0]);
        final Connector connector = createConnector(schema);
        newConnection(connector);
        safeAction(() -> connector.setState(ConnectorState.CONNECTED));

        final DatasetAddress datasetAddress1 = new DatasetAddress(1, 1, 1);
        final DatasetAddress datasetAddress2 = new DatasetAddress(1, 1, 2);

        final String filterOld = ValueUtil.randomString();

        final AreaOfInterest areaOfInterest1 = safeAction(
                () -> Replicant.context().createOrUpdateAreaOfInterest(datasetAddress1, ValueUtil.randomString()));
        createSubscription(datasetAddress1, filterOld, true);
        final AreaOfInterest areaOfInterest2 = safeAction(
                () -> Replicant.context().createOrUpdateAreaOfInterest(datasetAddress2, ValueUtil.randomString()));
        createSubscription(datasetAddress2, filterOld, true);

        final TestSpyEventHandler handler = registerTestSpyEventHandler();

        final SubscriptionReconciler.Action result = Replicant.context()
                .getSubscriptionReconciler()
                .reconcileAreaOfInterest(areaOfInterest2, areaOfInterest1, AreaOfInterestRequest.Type.UPDATE);

        assertEquals(result, SubscriptionReconciler.Action.NO_ACTION);

        handler.assertEventCount(0);
    }
}
