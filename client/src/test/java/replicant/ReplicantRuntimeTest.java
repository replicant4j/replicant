package replicant;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

import arez.Arez;
import arez.Disposable;
import java.util.concurrent.atomic.AtomicInteger;
import org.jspecify.annotations.NonNull;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

public class ReplicantRuntimeTest extends AbstractReplicantTest {
    @AfterMethod
    @Override
    public void postTest() {
        final Disposable schedulerLock = Arez.context().pauseScheduler();
        try {
            final ReplicantContext context = Replicant.context();
            context.deactivate();
            // Need to dispose the SubscriptionReconciler so the runtime can be safely disposed
            final SubscriptionReconciler subscriptionReconciler = context.getSubscriptionReconciler();
            Disposable.dispose(subscriptionReconciler);
            // Dispose the runtime as it can have a once-off scheduled to process at a later date to reflect active
            // states
            Disposable.dispose(context.getRuntime());
        } finally {
            schedulerLock.dispose();
        }
        super.postTest();
    }

    @Test
    public void registerAndDeregisterLifecycle() {
        final ReplicantRuntime runtime1 = Replicant.context().getRuntime();
        final ReplicantRuntime runtime2 = ReplicantRuntime.create();

        final AtomicInteger callCount1 = new AtomicInteger();
        observer(() -> {
            if (Disposable.isNotDisposed(runtime1)) {
                runtime1.getConnectors();
            }
            callCount1.incrementAndGet();
        });

        final AtomicInteger callCount2 = new AtomicInteger();
        observer(() -> {
            if (Disposable.isNotDisposed(runtime2)) {
                runtime2.getConnectors();
            }
            callCount2.incrementAndGet();
        });

        safeAction(() -> assertEquals(runtime1.getConnectors().size(), 0));
        safeAction(() -> assertEquals(runtime2.getConnectors().size(), 0));
        assertEquals(callCount1.get(), 1);
        assertEquals(callCount2.get(), 1);

        // This connector will self-register to runtime1
        final Connector connector1 = createConnector();

        safeAction(() -> assertEquals(runtime1.getConnectors().size(), 1));
        safeAction(() -> assertEquals(runtime2.getConnectors().size(), 0));
        assertEquals(callCount1.get(), 2);
        assertEquals(callCount2.get(), 1);

        // Manually register to runtime2  - never happens in app but useful during testing
        runtime2.registerConnector(connector1);

        safeAction(() -> assertEquals(runtime1.getConnectors().size(), 1));
        safeAction(() -> assertEquals(runtime2.getConnectors().size(), 1));
        assertEquals(callCount1.get(), 2);
        assertEquals(callCount2.get(), 2);

        // Manually deregister from runtime2  - never happens in app but useful during testing
        safeAction(() -> runtime2.deregisterConnector(connector1));

        safeAction(() -> assertEquals(runtime1.getConnectors().size(), 1));
        safeAction(() -> assertEquals(runtime2.getConnectors().size(), 0));
        assertEquals(callCount1.get(), 2);
        assertEquals(callCount2.get(), 3);

        Disposable.dispose(connector1);

        safeAction(() -> assertEquals(runtime1.getConnectors().size(), 0));
        safeAction(() -> assertEquals(runtime2.getConnectors().size(), 0));
        assertEquals(callCount1.get(), 3);
        assertEquals(callCount2.get(), 3);
    }

    @Test
    public void duplicateRegister() {
        final ReplicantRuntime runtime1 = Replicant.context().getRuntime();

        final SystemSchema systemSchema = newSystemSchema();

        // This connector will self-register to runtime1
        final Connector connector1 = createConnector(systemSchema);

        final IllegalStateException exception =
                expectThrows(IllegalStateException.class, () -> runtime1.registerConnector(connector1));

        assertEquals(
                exception.getMessage(),
                "Replicant-0015: Invoked registerConnector for System Schema named '" + systemSchema.getName()
                        + "' but a Connector for specified System Schema exists.");
    }

    @Test
    public void deregisterWhenNoRegistered() {
        final ReplicantRuntime runtime2 = Replicant.context().getRuntime();

        final SystemSchema systemSchema = newSystemSchema();
        // This connector will self-register to runtime1
        final Connector connector1 = createConnector(systemSchema);

        safeAction(() -> runtime2.deregisterConnector(connector1));

        final IllegalStateException exception = expectThrows(
                IllegalStateException.class, () -> safeAction(() -> runtime2.deregisterConnector(connector1)));

        assertEquals(
                exception.getMessage(),
                "Replicant-0006: Invoked deregisterConnector for System Schema named '" + systemSchema.getName()
                        + "' but no Connector for specified System Schema exists.");
    }

    @Test
    public void getConnector() {
        final SystemSchema systemSchema1 = newSystemSchema();
        final SystemSchema systemSchema2 = newSystemSchema();
        final SystemSchema systemSchema3 = newSystemSchema();
        final ReplicantRuntime runtime = Replicant.context().getRuntime();
        final Connector connector1 = createConnector(systemSchema1);
        final Connector connector2 = createConnector(systemSchema2);

        assertEquals(runtime.getConnector(connector1.getSystemSchema().getId()), connector1);
        assertEquals(runtime.getConnector(connector2.getSystemSchema().getId()), connector2);

        assertThrows(IllegalStateException.class, () -> runtime.getConnector(systemSchema3.getId()));
    }

    @Test
    public void activate() {
        final SystemSchema systemSchema1 = newSystemSchema();

        final ReplicantRuntime runtime = Replicant.context().getRuntime();
        final Connector connector1 = createConnector(systemSchema1);

        final ConnectorEntry entry1 = runtime.getConnectorEntryBySystemSchemaId(
                connector1.getSystemSchema().getId());

        final Disposable schedulerLock1 = pauseScheduler();
        runtime.deactivate();
        reset(connector1.getTransport());
        safeAction(() -> connector1.setState(ConnectorState.DISCONNECTED));
        entry1.getRateLimiter().fillBucket();
        runtime.activate();
        schedulerLock1.dispose();
        verify(connector1.getTransport(), times(1)).requestConnect(any(TransportContext.class));

        reset(connector1.getTransport());

        // set Connector State to in transition so connect is not called

        final Disposable schedulerLock2 = pauseScheduler();
        runtime.deactivate();
        safeAction(() -> connector1.setState(ConnectorState.CONNECTING));
        entry1.getRateLimiter().fillBucket();
        runtime.activate();
        schedulerLock2.dispose();
        verify(connector1.getTransport(), never()).requestConnect(any(TransportContext.class));

        runtime.deactivate();
        safeAction(() -> connector1.setState(ConnectorState.DISCONNECTING));
        entry1.getRateLimiter().fillBucket();
        runtime.activate();
        verify(connector1.getTransport(), never()).requestConnect(any(TransportContext.class));

        // set Connector State to connected so no action required

        final Disposable schedulerLock3 = pauseScheduler();
        runtime.deactivate();
        newConnection(connector1);
        safeAction(() -> connector1.setState(ConnectorState.CONNECTED));
        entry1.getRateLimiter().fillBucket();
        runtime.activate();
        schedulerLock3.dispose();
        verify(connector1.getTransport(), never()).requestConnect(any(TransportContext.class));

        // set Connector State to disconnected but rate limit it

        final Disposable schedulerLock4 = pauseScheduler();
        runtime.deactivate();
        safeAction(() -> connector1.setState(ConnectorState.DISCONNECTED));
        entry1.getRateLimiter().setTokenCount(0);
        runtime.activate();
        schedulerLock4.dispose();
        verify(connector1.getTransport(), never()).requestConnect(any(TransportContext.class));

        // set Connector State to disconnected but rate limit it

        final Disposable schedulerLock5 = pauseScheduler();
        runtime.deactivate();
        safeAction(() -> connector1.setState(ConnectorState.DISCONNECTED));
        entry1.getRateLimiter().fillBucket();
        runtime.activate();
        schedulerLock5.dispose();
        verify(connector1.getTransport(), times(1)).requestConnect(any(TransportContext.class));
    }

    @Test
    public void activateMultiple() {
        final SystemSchema systemSchema1 = newSystemSchema();
        final SystemSchema systemSchema3 = newSystemSchema();

        final ReplicantRuntime runtime = Replicant.context().getRuntime();

        final Connector connector1 = createConnector(systemSchema1);
        final ConnectorEntry entry1 = runtime.getConnectorEntryBySystemSchemaId(
                connector1.getSystemSchema().getId());

        final Connector connector3 = createConnector(systemSchema3);
        final ConnectorEntry entry3 = runtime.getConnectorEntryBySystemSchemaId(
                connector3.getSystemSchema().getId());

        reset(connector1.getTransport());
        reset(connector3.getTransport());

        final Disposable schedulerLock1 = pauseScheduler();
        safeAction(() -> connector1.setState(ConnectorState.DISCONNECTED));
        safeAction(() -> connector3.setState(ConnectorState.DISCONNECTED));
        runtime.deactivate();
        entry1.getRateLimiter().fillBucket();
        entry3.getRateLimiter().fillBucket();
        runtime.activate();
        schedulerLock1.dispose();
        verify(connector1.getTransport(), times(1)).requestConnect(any(TransportContext.class));
        verify(connector3.getTransport(), times(1)).requestConnect(any(TransportContext.class));

        reset(connector1.getTransport());
        reset(connector3.getTransport());

        // set Connector State to in transition so connect is not called

        final Disposable schedulerLock2 = pauseScheduler();
        runtime.deactivate();
        safeAction(() -> connector1.setState(ConnectorState.CONNECTING));
        entry1.getRateLimiter().fillBucket();
        safeAction(() -> connector3.setState(ConnectorState.CONNECTING));
        entry3.getRateLimiter().fillBucket();
        runtime.activate();
        schedulerLock2.dispose();
        verify(connector1.getTransport(), never()).requestConnect(any(TransportContext.class));
        verify(connector3.getTransport(), never()).requestConnect(any(TransportContext.class));

        final Disposable schedulerLock3 = pauseScheduler();
        runtime.deactivate();
        safeAction(() -> connector1.setState(ConnectorState.DISCONNECTING));
        entry1.getRateLimiter().fillBucket();
        safeAction(() -> connector3.setState(ConnectorState.DISCONNECTING));
        entry3.getRateLimiter().fillBucket();
        runtime.activate();
        schedulerLock3.dispose();
        verify(connector1.getTransport(), never()).requestConnect(any(TransportContext.class));
        verify(connector3.getTransport(), never()).requestConnect(any(TransportContext.class));

        // set Connector State to connected so no action required

        final Disposable schedulerLock4 = pauseScheduler();
        runtime.deactivate();
        newConnection(connector1);
        safeAction(() -> connector1.setState(ConnectorState.CONNECTED));
        entry1.getRateLimiter().fillBucket();
        newConnection(connector3);
        safeAction(() -> connector3.setState(ConnectorState.CONNECTED));
        entry3.getRateLimiter().fillBucket();
        runtime.activate();
        schedulerLock4.dispose();
        verify(connector1.getTransport(), never()).requestConnect(any(TransportContext.class));
        verify(connector3.getTransport(), never()).requestConnect(any(TransportContext.class));

        // set Connector State to disconnected but rate limit it

        final Disposable schedulerLock5 = pauseScheduler();
        runtime.deactivate();
        safeAction(() -> connector1.setState(ConnectorState.DISCONNECTED));
        entry1.getRateLimiter().setTokenCount(0);
        safeAction(() -> connector3.setState(ConnectorState.DISCONNECTED));
        entry3.getRateLimiter().setTokenCount(0);
        runtime.activate();
        schedulerLock5.dispose();
        verify(connector1.getTransport(), never()).requestConnect(any(TransportContext.class));
        verify(connector3.getTransport(), never()).requestConnect(any(TransportContext.class));

        // set Connector State to disconnected but rate limit it

        final Disposable schedulerLock6 = pauseScheduler();
        runtime.deactivate();
        safeAction(() -> connector1.setState(ConnectorState.DISCONNECTED));
        entry1.getRateLimiter().fillBucket();
        safeAction(() -> connector3.setState(ConnectorState.DISCONNECTED));
        entry3.getRateLimiter().fillBucket();
        runtime.activate();
        schedulerLock6.dispose();
        verify(connector1.getTransport(), times(1)).requestConnect(any(TransportContext.class));
        verify(connector3.getTransport(), times(1)).requestConnect(any(TransportContext.class));
    }

    @Test
    public void deactivate() {
        final SystemSchema systemSchema1 = newSystemSchema();

        final ReplicantRuntime runtime = Replicant.context().getRuntime();
        final Connector connector1 = createConnector(systemSchema1);
        newConnection(connector1);
        safeAction(() -> connector1.setState(ConnectorState.CONNECTED));

        final ConnectorEntry entry1 = runtime.getConnectorEntryBySystemSchemaId(
                connector1.getSystemSchema().getId());

        runtime.activate();

        reset(connector1.getTransport());

        entry1.getRateLimiter().fillBucket();
        runtime.deactivate();
        verify(connector1.getTransport(), times(1)).requestDisconnect();

        reset(connector1.getTransport());

        // set Connector State to in transition so connect is not called

        runtime.activate();
        safeAction(() -> connector1.setState(ConnectorState.CONNECTING));
        entry1.getRateLimiter().fillBucket();
        runtime.deactivate();
        verify(connector1.getTransport(), never()).requestDisconnect();

        runtime.activate();
        safeAction(() -> connector1.setState(ConnectorState.DISCONNECTING));
        entry1.getRateLimiter().fillBucket();
        runtime.deactivate();
        verify(connector1.getTransport(), never()).requestDisconnect();

        // set Connector State to DISCONNECTED so no action required

        runtime.activate();
        safeAction(() -> connector1.setState(ConnectorState.DISCONNECTED));
        entry1.getRateLimiter().fillBucket();
        runtime.deactivate();
        verify(connector1.getTransport(), never()).requestDisconnect();

        // set Connector State to ERROR so no action required

        runtime.activate();
        safeAction(() -> connector1.setState(ConnectorState.ERROR));
        entry1.getRateLimiter().fillBucket();
        runtime.deactivate();
        verify(connector1.getTransport(), never()).requestDisconnect();

        // set Connector State to connected but rate limit it

        runtime.activate();
        safeAction(() -> connector1.setState(ConnectorState.CONNECTED));
        entry1.getRateLimiter().setTokenCount(0);
        runtime.deactivate();
        verify(connector1.getTransport(), never()).requestDisconnect();

        // set Connector State to connected

        runtime.activate();
        safeAction(() -> connector1.setState(ConnectorState.CONNECTED));
        entry1.getRateLimiter().fillBucket();
        runtime.deactivate();
        verify(connector1.getTransport(), times(1)).requestDisconnect();
    }

    @Test
    public void deactivateMultipleDataSources() {
        final SystemSchema systemSchema1 = newSystemSchema();
        final SystemSchema systemSchema2 = newSystemSchema();

        final ReplicantRuntime runtime = Replicant.context().getRuntime();
        final Connector connector1 = createConnector(systemSchema1);
        newConnection(connector1);
        safeAction(() -> connector1.setState(ConnectorState.CONNECTED));

        final Connector connector3 = createConnector(systemSchema2);
        newConnection(connector3);
        safeAction(() -> connector3.setState(ConnectorState.CONNECTED));

        final ConnectorEntry entry1 = runtime.getConnectorEntryBySystemSchemaId(
                connector1.getSystemSchema().getId());
        final ConnectorEntry entry3 = runtime.getConnectorEntryBySystemSchemaId(
                connector3.getSystemSchema().getId());
        entry3.setRequired(false);

        runtime.activate();

        reset(connector1.getTransport());
        reset(connector3.getTransport());

        entry1.getRateLimiter().fillBucket();
        entry3.getRateLimiter().fillBucket();
        runtime.deactivate();
        verify(connector1.getTransport(), times(1)).requestDisconnect();
        verify(connector3.getTransport(), times(1)).requestDisconnect();
    }

    @Test
    public void updateStatus() {
        // No connectors just active/inactive state

        assertUpdateState(ReplicantContextState.CONNECTED, true);
        assertUpdateState(ReplicantContextState.DISCONNECTED, false);

        // Single data source, required

        assertUpdateState(ReplicantContextState.DISCONNECTED, ConnectorState.DISCONNECTED);
        assertUpdateState(ReplicantContextState.CONNECTED, ConnectorState.CONNECTED);
        assertUpdateState(ReplicantContextState.CONNECTING, ConnectorState.CONNECTING);
        assertUpdateState(ReplicantContextState.DISCONNECTING, ConnectorState.DISCONNECTING);
        assertUpdateState(ReplicantContextState.ERROR, ConnectorState.ERROR);

        // 2 Data sources, both required

        assertUpdateState(ReplicantContextState.ERROR, ConnectorState.CONNECTED, ConnectorState.ERROR);
        assertUpdateState(ReplicantContextState.ERROR, ConnectorState.CONNECTING, ConnectorState.ERROR);
        assertUpdateState(ReplicantContextState.ERROR, ConnectorState.DISCONNECTED, ConnectorState.ERROR);
        assertUpdateState(ReplicantContextState.ERROR, ConnectorState.DISCONNECTING, ConnectorState.ERROR);
        assertUpdateState(ReplicantContextState.ERROR, ConnectorState.ERROR, ConnectorState.ERROR);

        assertUpdateState(ReplicantContextState.CONNECTED, ConnectorState.CONNECTED, ConnectorState.CONNECTED);
        assertUpdateState(ReplicantContextState.CONNECTING, ConnectorState.CONNECTING, ConnectorState.CONNECTED);
        assertUpdateState(ReplicantContextState.DISCONNECTED, ConnectorState.DISCONNECTED, ConnectorState.CONNECTED);
        assertUpdateState(ReplicantContextState.DISCONNECTING, ConnectorState.DISCONNECTING, ConnectorState.CONNECTED);

        assertUpdateState(ReplicantContextState.CONNECTING, ConnectorState.CONNECTING, ConnectorState.CONNECTING);
        assertUpdateState(ReplicantContextState.DISCONNECTED, ConnectorState.DISCONNECTED, ConnectorState.CONNECTING);
        assertUpdateState(ReplicantContextState.DISCONNECTING, ConnectorState.DISCONNECTING, ConnectorState.CONNECTING);

        assertUpdateState(
                ReplicantContextState.DISCONNECTED, ConnectorState.DISCONNECTED, ConnectorState.DISCONNECTING);
        assertUpdateState(
                ReplicantContextState.DISCONNECTING, ConnectorState.DISCONNECTING, ConnectorState.DISCONNECTING);

        assertUpdateState(ReplicantContextState.DISCONNECTED, ConnectorState.DISCONNECTED, ConnectorState.DISCONNECTED);

        // 3 Data sources, first two required, third optional

        assertUpdateState(
                ReplicantContextState.CONNECTED,
                ConnectorState.CONNECTED,
                ConnectorState.CONNECTED,
                ConnectorState.CONNECTED);
        assertUpdateState(
                ReplicantContextState.CONNECTED,
                ConnectorState.CONNECTED,
                ConnectorState.CONNECTED,
                ConnectorState.ERROR);
        assertUpdateState(
                ReplicantContextState.CONNECTED,
                ConnectorState.CONNECTED,
                ConnectorState.CONNECTED,
                ConnectorState.CONNECTING);
        assertUpdateState(
                ReplicantContextState.CONNECTED,
                ConnectorState.CONNECTED,
                ConnectorState.CONNECTED,
                ConnectorState.DISCONNECTING);
        assertUpdateState(
                ReplicantContextState.CONNECTED,
                ConnectorState.CONNECTED,
                ConnectorState.CONNECTED,
                ConnectorState.DISCONNECTED);

        assertUpdateState(
                ReplicantContextState.CONNECTING,
                ConnectorState.CONNECTING,
                ConnectorState.CONNECTED,
                ConnectorState.ERROR);
        assertUpdateState(
                ReplicantContextState.CONNECTING,
                ConnectorState.CONNECTING,
                ConnectorState.CONNECTED,
                ConnectorState.CONNECTED);
        assertUpdateState(
                ReplicantContextState.CONNECTING,
                ConnectorState.CONNECTING,
                ConnectorState.CONNECTED,
                ConnectorState.CONNECTING);
        assertUpdateState(
                ReplicantContextState.CONNECTING,
                ConnectorState.CONNECTING,
                ConnectorState.CONNECTED,
                ConnectorState.DISCONNECTED);
        assertUpdateState(
                ReplicantContextState.CONNECTING,
                ConnectorState.CONNECTING,
                ConnectorState.CONNECTED,
                ConnectorState.DISCONNECTING);
    }

    private void assertUpdateState(@NonNull final ReplicantContextState expectedContextState, final boolean isActive) {
        ReplicantTestUtil.resetState();
        final Disposable schedulerLock = pauseScheduler();
        final ReplicantRuntime runtime = Replicant.context().getRuntime();
        if (isActive) {
            runtime.activate();
        } else {
            runtime.deactivate();
        }
        assertEquals(runtime.getState(), expectedContextState);

        schedulerLock.dispose();
    }

    private void assertUpdateState(
            @NonNull final ReplicantContextState expectedContextState, @NonNull final ConnectorState state) {
        ReplicantTestUtil.resetState();

        final Disposable schedulerLock = pauseScheduler();
        final ReplicantRuntime runtime = Replicant.context().getRuntime();
        createConnectorInState(state);

        assertEquals(runtime.getState(), expectedContextState);

        schedulerLock.dispose();
    }

    @NonNull
    private Connector createConnectorInState(@NonNull final ConnectorState state) {
        final Connector connector = createConnector(newSystemSchema());
        if (ConnectorState.CONNECTED == state) {
            newConnection(connector);
        }
        safeAction(() -> connector.setState(state));
        return connector;
    }

    private void assertUpdateState(
            @NonNull final ReplicantContextState expectedContextState,
            @NonNull final ConnectorState connector1State,
            @NonNull final ConnectorState connector2State) {
        ReplicantTestUtil.resetState();

        final Disposable schedulerLock = pauseScheduler();
        final ReplicantRuntime runtime = Replicant.context().getRuntime();
        createConnectorInState(connector1State);
        createConnectorInState(connector2State);

        assertEquals(runtime.getState(), expectedContextState);

        schedulerLock.dispose();
    }

    private void assertUpdateState(
            @NonNull final ReplicantContextState expectedContextState,
            @NonNull final ConnectorState connector1State,
            @NonNull final ConnectorState connector2State,
            @NonNull final ConnectorState connector3State) {
        ReplicantTestUtil.resetState();

        final Disposable schedulerLock = pauseScheduler();

        final ReplicantRuntime runtime = Replicant.context().getRuntime();
        createConnectorInState(connector1State);
        createConnectorInState(connector2State);

        final Connector connector3 = createConnectorInState(connector3State);

        runtime.setConnectorRequired(connector3.getSystemSchema().getId(), false);

        assertEquals(runtime.getState(), expectedContextState);

        schedulerLock.dispose();
    }
}
