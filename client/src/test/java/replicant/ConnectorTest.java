package replicant;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

import arez.Disposable;
import arez.component.Linkable;
import arez.component.Verifiable;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.testng.annotations.Test;
import replicant.messages.ChangeSetMessage;
import replicant.messages.EntityChange;
import replicant.messages.EntityChangeData;
import replicant.messages.EntityChangeDataImpl;
import replicant.messages.OkMessage;
import replicant.messages.ServerToClientMessage;
import replicant.messages.SubscriptionChangeMessage;
import replicant.messages.UseDatasetCacheEntryMessage;
import replicant.spy.ConnectFailureEvent;
import replicant.spy.ConnectedEvent;
import replicant.spy.DisconnectFailureEvent;
import replicant.spy.DisconnectedEvent;
import replicant.spy.ExecCompletedEvent;
import replicant.spy.ExecRequestQueuedEvent;
import replicant.spy.ExecStartedEvent;
import replicant.spy.InSyncEvent;
import replicant.spy.MessageProcessFailureEvent;
import replicant.spy.MessageProcessedEvent;
import replicant.spy.MessageReadFailureEvent;
import replicant.spy.OutOfSyncEvent;
import replicant.spy.RestartEvent;
import replicant.spy.SubscribeCompletedEvent;
import replicant.spy.SubscribeRequestQueuedEvent;
import replicant.spy.SubscribeStartedEvent;
import replicant.spy.SubscriptionCreatedEvent;
import replicant.spy.SubscriptionDisposedEvent;
import replicant.spy.SubscriptionUpdateCompletedEvent;
import replicant.spy.SubscriptionUpdateRequestQueuedEvent;
import replicant.spy.SubscriptionUpdateStartedEvent;
import replicant.spy.SyncRequestEvent;
import replicant.spy.UnsubscribeCompletedEvent;
import replicant.spy.UnsubscribeRequestQueuedEvent;
import replicant.spy.UnsubscribeStartedEvent;

@SuppressWarnings({"NonJREEmulationClassesInClientCode"})
public final class ConnectorTest extends AbstractReplicantTest {
    @Test
    public void construct() {
        final Disposable schedulerLock = pauseScheduler();
        final ReplicantRuntime runtime = Replicant.context().getRuntime();

        safeAction(() -> assertEquals(runtime.getConnectors().size(), 0));

        final SystemSchema systemSchema =
                new SystemSchema(ValueUtil.randomInt(), ValueUtil.randomString(), new Dataset[0], new EntityType[0]);
        final Connector connector = createConnector(systemSchema);

        assertEquals(connector.getSystemSchema(), systemSchema);

        safeAction(() -> assertEquals(runtime.getConnectors().size(), 1));

        assertEquals(connector.getReplicantRuntime(), runtime);
        assertTrue(connector
                .getReplicantContext()
                .getSystemSchemaService()
                .getSystemSchemas()
                .contains(systemSchema));

        assertEquals(connector.getState(), ConnectorState.DISCONNECTED);

        schedulerLock.dispose();

        assertEquals(connector.getState(), ConnectorState.CONNECTING);
    }

    @Test
    public void dispose() {
        final ReplicantRuntime runtime = Replicant.context().getRuntime();

        safeAction(() -> assertEquals(runtime.getConnectors().size(), 0));

        final SystemSchema systemSchema = newSystemSchema();
        final Connector connector = createConnector(systemSchema);

        safeAction(() -> assertEquals(runtime.getConnectors().size(), 1));
        assertTrue(connector
                .getReplicantContext()
                .getSystemSchemaService()
                .getSystemSchemas()
                .contains(systemSchema));

        Disposable.dispose(connector);

        safeAction(() -> assertEquals(runtime.getConnectors().size(), 0));
        assertFalse(connector
                .getReplicantContext()
                .getSystemSchemaService()
                .getSystemSchemas()
                .contains(systemSchema));
    }

    @Test
    public void testToString() {
        final SystemSchema systemSchema = newSystemSchema();
        final Connector connector = createConnector(systemSchema);
        assertEquals(connector.toString(), "Connector[" + systemSchema.getName() + "]");
        ReplicantTestUtil.disableNames();
        assertEquals(connector.toString(), "replicant.Arez_Connector@" + Integer.toHexString(connector.hashCode()));
    }

    @Test
    public void setConnection_whenConnectorProcessingMessage() {
        final Connector connector = createConnector();

        final Connection connection = newConnection(connector);

        pauseScheduler();
        connector.pauseMessageScheduler();

        setCurrentMessageResponse(connection, ChangeSetMessage.create(null, null, null, null, null, null));

        final DatasetAddress datasetAddress =
                new DatasetAddress(connector.getSystemSchema().getId(), 0);
        final Subscription subscription = createSubscription(datasetAddress, null, SubscriptionMode.EXPLICIT);

        connector.onConnection(ValueUtil.randomString());

        // Connection not swapped yet but will do one MessageProcess completes
        assertFalse(Disposable.isDisposed(subscription));
        assertEquals(connector.getConnection(), connection);
        assertNotNull(connector.getPostMessageResponseAction());
    }

    @Test
    public void setConnection_whenExistingConnection() {
        final Connector connector = createConnector();

        final Connection connection = newConnection(connector);

        pauseScheduler();
        connector.pauseMessageScheduler();

        assertEquals(connector.getConnection(), connection);

        final String newConnectionId = ValueUtil.randomString();
        connector.onConnection(newConnectionId);

        assertEquals(connector.ensureConnection().getConnectionId(), newConnectionId);

        assertTrue(connector.ensureConnection().getPendingResponses().isEmpty());
    }

    @Test
    public void connect() {
        pauseScheduler();

        final Connector connector = createConnector();
        assertEquals(connector.getState(), ConnectorState.DISCONNECTED);

        safeAction(connector::connect);

        verify(connector.getTransport()).requestConnect(any(TransportContext.class));

        assertEquals(connector.getState(), ConnectorState.CONNECTING);
    }

    @Test
    public void connect_causesError() {
        pauseScheduler();

        final Connector connector = createConnector();
        assertEquals(connector.getState(), ConnectorState.DISCONNECTED);

        reset(connector.getTransport());

        final IllegalStateException exception = new IllegalStateException();
        doAnswer(i -> {
                    throw exception;
                })
                .when(connector.getTransport())
                .requestConnect(any(TransportContext.class));

        final IllegalStateException actual =
                expectThrows(IllegalStateException.class, () -> safeAction(connector::connect));

        assertEquals(actual, exception);
        assertEquals(connector.getState(), ConnectorState.ERROR);

        verify(connector.getTransport()).unbind();
    }

    @Test
    public void disconnect() {
        pauseScheduler();

        final Connector connector = createConnector();
        newConnection(connector);

        safeAction(() -> connector.setState(ConnectorState.CONNECTED));

        safeAction(connector::disconnect);

        verify(connector.getTransport()).requestDisconnect();

        assertEquals(connector.getState(), ConnectorState.DISCONNECTING);

        verify(connector.getTransport(), never()).unbind();
    }

    @Test
    public void disconnect_causesError() {
        pauseScheduler();

        final Connector connector = createConnector();
        newConnection(connector);
        safeAction(() -> connector.setState(ConnectorState.CONNECTED));

        reset(connector.getTransport());

        final IllegalStateException exception = new IllegalStateException();
        doAnswer(i -> {
                    throw exception;
                })
                .when(connector.getTransport())
                .requestDisconnect();

        final IllegalStateException actual =
                expectThrows(IllegalStateException.class, () -> safeAction(connector::disconnect));

        assertEquals(actual, exception);

        assertEquals(connector.getState(), ConnectorState.ERROR);
        verify(connector.getTransport()).unbind();
    }

    @Test
    public void onDisconnected() {
        final Connector connector = createConnector();

        newConnection(connector);

        safeAction(() -> connector.setState(ConnectorState.CONNECTING));

        assertEquals(connector.getReplicantRuntime().getState(), RuntimeState.CONNECTING);

        // Pause scheduler so runtime does not try to update state
        pauseScheduler();

        reset(connector.getTransport());

        connector.onDisconnected();

        assertEquals(connector.getState(), ConnectorState.DISCONNECTED);
        assertEquals(connector.getReplicantRuntime().getState(), RuntimeState.DISCONNECTED);

        verify(connector.getTransport()).unbind();
    }

    @Test
    public void onDisconnected_generatesSpyMessage() {
        final Connector connector = createConnector();

        safeAction(() -> connector.setState(ConnectorState.CONNECTING));

        final TestSpyEventHandler handler = registerTestSpyEventHandler();

        // Pause scheduler so runtime does not try to update state
        pauseScheduler();

        connector.onDisconnected();

        handler.assertEventCount(1);
        handler.assertNextEvent(
                DisconnectedEvent.class,
                e -> assertEquals(
                        e.getSystemSchemaId(), connector.getSystemSchema().getId()));
    }

    @Test
    public void onDisconnectFailure() {
        final Connector connector = createConnector();

        safeAction(() -> connector.setState(ConnectorState.CONNECTING));

        assertEquals(connector.getReplicantRuntime().getState(), RuntimeState.CONNECTING);

        // Pause scheduler so runtime does not try to update state
        pauseScheduler();

        connector.onDisconnectFailure();

        assertEquals(connector.getState(), ConnectorState.ERROR);
        assertEquals(connector.getReplicantRuntime().getState(), RuntimeState.ERROR);
    }

    @Test
    public void onDisconnectFailure_generatesSpyMessage() {
        final Connector connector = createConnector();

        safeAction(() -> connector.setState(ConnectorState.CONNECTING));

        final TestSpyEventHandler handler = registerTestSpyEventHandler();

        // Pause scheduler so runtime does not try to update state
        pauseScheduler();

        connector.onDisconnectFailure();

        handler.assertEventCount(1);
        handler.assertNextEvent(
                DisconnectFailureEvent.class,
                e -> assertEquals(
                        e.getSystemSchemaId(), connector.getSystemSchema().getId()));
    }

    @Test
    public void onConnected() throws Exception {
        final Connection connection = createConnection();
        final Connector connector = connection.getConnector();

        safeAction(() -> connector.setState(ConnectorState.CONNECTING));

        assertEquals(connector.getReplicantRuntime().getState(), RuntimeState.CONNECTING);

        // Pause scheduler so runtime does not try to update state
        pauseScheduler();

        final Field field = Connector.class.getDeclaredField("_connection");
        field.setAccessible(true);
        field.set(connector, connection);

        connector.onConnected();

        assertEquals(connector.getState(), ConnectorState.CONNECTED);
        assertEquals(connector.getReplicantRuntime().getState(), RuntimeState.CONNECTED);
    }

    @Test
    public void onConnected_generatesSpyMessage() {
        final Connector connector = createConnector();
        newConnection(connector);

        safeAction(() -> connector.setState(ConnectorState.CONNECTING));

        final TestSpyEventHandler handler = registerTestSpyEventHandler();

        connector.onConnected();

        handler.assertEventCount(1);
        handler.assertNextEvent(
                ConnectedEvent.class,
                e -> assertEquals(
                        e.getSystemSchemaId(), connector.getSystemSchema().getId()));
    }

    @Test
    public void onConnectFailure() {
        final Connector connector = createConnector();

        safeAction(() -> connector.setState(ConnectorState.CONNECTING));

        assertEquals(connector.getReplicantRuntime().getState(), RuntimeState.CONNECTING);

        // Pause scheduler so runtime does not try to update state
        pauseScheduler();

        connector.onConnectFailure();

        assertEquals(connector.getState(), ConnectorState.ERROR);
        assertEquals(connector.getReplicantRuntime().getState(), RuntimeState.ERROR);
    }

    @Test
    public void onConnectFailure_generatesSpyMessage() {
        final Connector connector = createConnector();

        safeAction(() -> connector.setState(ConnectorState.CONNECTING));

        final TestSpyEventHandler handler = registerTestSpyEventHandler();

        // Pause scheduler so runtime does not try to update state
        pauseScheduler();

        connector.onConnectFailure();

        handler.assertEventCount(1);
        handler.assertNextEvent(
                ConnectFailureEvent.class,
                e -> assertEquals(
                        e.getSystemSchemaId(), connector.getSystemSchema().getId()));
    }

    @Test
    public void onMessageReceived() {
        final Connector connector = createConnector();
        final Connection connection = newConnection(connector);

        pauseScheduler();
        connector.pauseMessageScheduler();

        assertEquals(connection.getPendingResponses().size(), 0);
        assertFalse(connector.isSchedulerActive());

        final ChangeSetMessage message = ChangeSetMessage.create(null, null, null, null, null, null);
        connector.onMessageReceived(message);

        assertEquals(connection.getPendingResponses().size(), 1);
        assertEquals(connection.getPendingResponses().get(0).getMessage(), message);
    }

    @Test
    public void onMessageReceived_missingDatasetCacheEntryIsRecoverable() {
        final Connector connector = createConnector();
        newConnection(connector);
        safeAction(() -> connector.setState(ConnectorState.CONNECTED));
        pauseScheduler();

        final DatasetAddress datasetAddress = new DatasetAddress(1, 0);
        final DatasetCacheService datasetCacheService = mock(DatasetCacheService.class);
        Replicant.context().setDatasetCacheService(datasetCacheService);

        connector.onMessageReceived(UseDatasetCacheEntryMessage.create(
                null, datasetAddress.asDatasetAddressDescriptor(), ValueUtil.randomString()));

        verify(datasetCacheService).invalidateDatasetCacheEntry(datasetAddress);
        assertEquals(connector.getState(), ConnectorState.DISCONNECTING);
    }

    @Test
    public void onMessageReceived_datasetCacheEntryLookupFailureIsRecoverable() {
        final Connector connector = createConnector();
        newConnection(connector);
        safeAction(() -> connector.setState(ConnectorState.CONNECTED));
        pauseScheduler();

        final DatasetAddress datasetAddress = new DatasetAddress(1, 0);
        final DatasetCacheService datasetCacheService = mock(DatasetCacheService.class);
        Replicant.context().setDatasetCacheService(datasetCacheService);
        when(datasetCacheService.lookupDatasetCacheEntry(datasetAddress))
                .thenThrow(new IllegalStateException("Unavailable"));
        when(datasetCacheService.invalidateDatasetCacheEntry(datasetAddress))
                .thenThrow(new IllegalStateException("Unavailable"));

        connector.onMessageReceived(UseDatasetCacheEntryMessage.create(
                null, datasetAddress.asDatasetAddressDescriptor(), ValueUtil.randomString()));

        verify(datasetCacheService).invalidateDatasetCacheEntry(datasetAddress);
        assertEquals(connector.getState(), ConnectorState.DISCONNECTING);
    }

    @Test
    public void onMessageReceived_cacheVersionMismatchIsRecoverable() {
        final Connector connector = createConnector();
        newConnection(connector);
        safeAction(() -> connector.setState(ConnectorState.CONNECTED));
        pauseScheduler();

        final DatasetAddress datasetAddress = new DatasetAddress(1, 0);
        final DatasetCacheService datasetCacheService = mock(DatasetCacheService.class);
        Replicant.context().setDatasetCacheService(datasetCacheService);
        final DatasetCacheEntry entry =
                new DatasetCacheEntry(datasetAddress, ValueUtil.randomString(), ValueUtil.randomString());
        when(datasetCacheService.lookupDatasetCacheEntry(datasetAddress)).thenReturn(entry);

        connector.onMessageReceived(UseDatasetCacheEntryMessage.create(
                null, datasetAddress.asDatasetAddressDescriptor(), ValueUtil.randomString()));

        verify(datasetCacheService).invalidateDatasetCacheEntry(datasetAddress);
        assertEquals(connector.getState(), ConnectorState.DISCONNECTING);
    }

    @Test
    public void onMessageReceived_corruptDatasetCacheEntryIsRecoverable() {
        final Connector connector = createConnector();
        newConnection(connector);
        safeAction(() -> connector.setState(ConnectorState.CONNECTED));
        pauseScheduler();

        final DatasetAddress datasetAddress = new DatasetAddress(1, 0);
        final String datasetCacheVersion = ValueUtil.randomString();
        final DatasetCacheService datasetCacheService = mock(DatasetCacheService.class);
        final DatasetCacheEntry entry = new DatasetCacheEntry(datasetAddress, datasetCacheVersion, "{");
        Replicant.context().setDatasetCacheService(datasetCacheService);
        when(datasetCacheService.lookupDatasetCacheEntry(datasetAddress)).thenReturn(entry);

        connector.onMessageReceived(UseDatasetCacheEntryMessage.create(
                null, datasetAddress.asDatasetAddressDescriptor(), datasetCacheVersion));

        verify(datasetCacheService).invalidateDatasetCacheEntry(datasetAddress);
        assertEquals(connector.getState(), ConnectorState.DISCONNECTING);
    }

    @Test
    public void onConnection_cacheEnumerationFailureIsIgnored() {
        final Connector connector = createConnector();
        safeAction(() -> connector.setState(ConnectorState.CONNECTING));
        pauseScheduler();

        final DatasetCacheService datasetCacheService = mock(DatasetCacheService.class);
        Replicant.context().setDatasetCacheService(datasetCacheService);
        when(datasetCacheService.getDatasetAddresses(connector.getSystemSchema().getId()))
                .thenThrow(new IllegalStateException("Unavailable"));

        connector.onConnection(ValueUtil.randomString());

        assertEquals(connector.getState(), ConnectorState.CONNECTED);
        verify(connector.getTransport(), never()).updateDatasetCacheVersionsSync(anyMap());
    }

    @Test
    public void onConnection_unreadableCacheVersionIsInvalidatedAndIgnored() {
        final Connector connector = createConnector();
        safeAction(() -> connector.setState(ConnectorState.CONNECTING));
        pauseScheduler();

        final DatasetAddress datasetAddress =
                new DatasetAddress(connector.getSystemSchema().getId(), 0);
        final DatasetCacheService datasetCacheService = mock(DatasetCacheService.class);
        Replicant.context().setDatasetCacheService(datasetCacheService);
        when(datasetCacheService.getDatasetAddresses(datasetAddress.systemSchemaId()))
                .thenReturn(Collections.singleton(datasetAddress));
        when(datasetCacheService.lookupDatasetCacheVersion(datasetAddress))
                .thenThrow(new IllegalStateException("Unavailable"));

        connector.onConnection(ValueUtil.randomString());

        assertEquals(connector.getState(), ConnectorState.CONNECTED);
        verify(datasetCacheService).invalidateDatasetCacheEntry(datasetAddress);
        verify(connector.getTransport(), never()).updateDatasetCacheVersionsSync(anyMap());
    }

    @Test
    public void onMessageReceived_rejectedDatasetCacheEntryIsNotAdvertisedOnReconnect() {
        final Connector connector = createConnector();
        newConnection(connector);
        safeAction(() -> connector.setState(ConnectorState.CONNECTED));
        pauseScheduler();

        final DatasetAddress datasetAddress = new DatasetAddress(1, 0);
        final DatasetCacheService datasetCacheService = mock(DatasetCacheService.class);
        Replicant.context().setDatasetCacheService(datasetCacheService);
        when(datasetCacheService.getDatasetAddresses(datasetAddress.systemSchemaId()))
                .thenReturn(Collections.singleton(datasetAddress));
        when(datasetCacheService.lookupDatasetCacheVersion(datasetAddress)).thenReturn(ValueUtil.randomString());

        connector.onMessageReceived(UseDatasetCacheEntryMessage.create(
                null, datasetAddress.asDatasetAddressDescriptor(), ValueUtil.randomString()));

        assertEquals(connector.getState(), ConnectorState.DISCONNECTING);
        reset(connector.getTransport());
        safeAction(() -> connector.setState(ConnectorState.CONNECTING));
        connector.onConnection(ValueUtil.randomString());

        verify(connector.getTransport(), never()).updateDatasetCacheVersionsSync(anyMap());
    }

    @Test
    public void onMessageProcessed() {
        final Connector connector = createConnector();

        safeAction(() -> connector.setState(ConnectorState.CONNECTING));

        final TestSpyEventHandler handler = registerTestSpyEventHandler();

        final MessageResponse response =
                new MessageResponse(1, ChangeSetMessage.create(null, null, null, null, null, null), null);
        connector.onMessageProcessed(response);

        handler.assertEventCount(1);

        handler.assertNextEvent(
                MessageProcessedEvent.class,
                e -> assertEquals(
                        e.getSystemSchemaId(), connector.getSystemSchema().getId()));
    }

    @Test
    public void onMessageProcessFailure() {
        final Connector connector = createConnector();
        newConnection(connector);

        safeAction(() -> connector.setState(ConnectorState.CONNECTED));

        final Throwable error = new Throwable();

        // Pause scheduler so runtime does not try to update state
        pauseScheduler();

        connector.onMessageProcessFailure(error);

        assertEquals(connector.getState(), ConnectorState.DISCONNECTING);
    }

    @Test
    public void onMessageProcessFailure_generatesSpyMessage() {
        final Connector connector = createConnector();

        safeAction(() -> connector.setState(ConnectorState.CONNECTING));

        final TestSpyEventHandler handler = registerTestSpyEventHandler();

        final Throwable error = new Throwable();

        connector.onMessageProcessFailure(error);

        handler.assertEventCount(1);
        handler.assertNextEvent(MessageProcessFailureEvent.class, e -> {
            assertEquals(e.getSystemSchemaId(), connector.getSystemSchema().getId());
            assertEquals(e.getError(), error);
        });
    }

    @Test
    public void disconnectIfPossible() {
        final Connector connector = createConnector();
        newConnection(connector);

        safeAction(() -> connector.setState(ConnectorState.CONNECTED));

        safeAction(connector::disconnectIfPossible);

        assertEquals(connector.getState(), ConnectorState.DISCONNECTING);
    }

    @Test
    public void disconnectIfPossible_noActionAsConnecting() {
        final Connector connector = createConnector();

        safeAction(() -> connector.setState(ConnectorState.CONNECTING));

        final TestSpyEventHandler handler = registerTestSpyEventHandler();

        connector.disconnectIfPossible();

        handler.assertEventCount(0);

        assertEquals(connector.getState(), ConnectorState.CONNECTING);
    }

    @Test
    public void disconnectIfPossible_generatesSpyEvent() {
        final Connector connector = createConnector();
        newConnection(connector);

        safeAction(() -> connector.setState(ConnectorState.CONNECTED));

        final TestSpyEventHandler handler = registerTestSpyEventHandler();

        safeAction(connector::disconnectIfPossible);

        handler.assertEventCount(1);
        handler.assertNextEvent(
                RestartEvent.class,
                e -> assertEquals(
                        e.getSystemSchemaId(), connector.getSystemSchema().getId()));
    }

    @Test
    public void onMessageReadFailure() {
        final Connector connector = createConnector();
        newConnection(connector);

        safeAction(() -> connector.setState(ConnectorState.CONNECTED));

        // Pause scheduler so runtime does not try to update state
        pauseScheduler();

        connector.onMessageReadFailure();

        assertEquals(connector.getState(), ConnectorState.DISCONNECTING);
    }

    @Test
    public void onMessageReadFailure_generatesSpyMessage() {
        final Connector connector = createConnector();

        safeAction(() -> connector.setState(ConnectorState.CONNECTING));

        final TestSpyEventHandler handler = registerTestSpyEventHandler();

        connector.onMessageReadFailure();

        handler.assertEventCount(1);
        handler.assertNextEvent(
                MessageReadFailureEvent.class,
                e -> assertEquals(
                        e.getSystemSchemaId(), connector.getSystemSchema().getId()));
    }

    @Test
    public void onSubscribeStarted() {
        final Connector connector = createConnector();

        final DatasetAddress datasetAddress = new DatasetAddress(1, 0);
        final AreaOfInterest areaOfInterest =
                safeAction(() -> Replicant.context().createOrUpdateAreaOfInterest(datasetAddress, null));

        assertEquals(areaOfInterest.getStatus(), AreaOfInterest.Status.PENDING);
        assertFalse(areaOfInterest.isDataAvailable());

        final TestSpyEventHandler handler = registerTestSpyEventHandler();

        connector.onSubscribeStarted(datasetAddress);

        assertEquals(areaOfInterest.getStatus(), AreaOfInterest.Status.PENDING);
        assertFalse(areaOfInterest.isDataAvailable());

        handler.assertEventCount(1);
        handler.assertNextEvent(SubscribeStartedEvent.class, e -> {
            assertEquals(e.getSystemSchemaId(), connector.getSystemSchema().getId());
            assertEquals(e.getDatasetAddress(), datasetAddress);
        });
    }

    @Test
    public void onSubscribeCompleted_transitionsImplicitToExplicitModeWithoutReplacingSubscription() {
        final Connector connector = createConnector();

        final DatasetAddress datasetAddress = new DatasetAddress(1, 0);
        final String filterParameter = ValueUtil.randomString();
        final AreaOfInterest areaOfInterest =
                safeAction(() -> Replicant.context().createOrUpdateAreaOfInterest(datasetAddress, filterParameter));

        assertEquals(areaOfInterest.getStatus(), AreaOfInterest.Status.PENDING);
        assertFalse(areaOfInterest.isDataAvailable());

        final Subscription subscription =
                createSubscription(datasetAddress, filterParameter, SubscriptionMode.IMPLICIT);
        final ReplicaEntry replicaEntry = findOrCreateReplicaEntry(String.class, ValueUtil.randomInt());
        safeAction(() -> replicaEntry.linkToSubscription(subscription));

        final TestSpyEventHandler handler = registerTestSpyEventHandler();

        connector.onSubscribeCompleted(datasetAddress);

        assertEquals(areaOfInterest.getStatus(), AreaOfInterest.Status.SATISFIED);
        assertTrue(areaOfInterest.isDataAvailable());
        safeAction(() -> {
            assertSame(areaOfInterest.getSubscription(), subscription);
            assertSame(Replicant.context().findSubscription(datasetAddress), subscription);
            assertEquals(subscription.getMode(), SubscriptionMode.EXPLICIT);
            assertEquals(subscription.getFilterParameter(), filterParameter);
            assertSame(subscription.findReplicaEntryByTypeAndId(String.class, replicaEntry.getId()), replicaEntry);
        });

        handler.assertEventCount(1);
        handler.assertNextEvent(SubscribeCompletedEvent.class, e -> {
            assertEquals(e.getSystemSchemaId(), connector.getSystemSchema().getId());
            assertEquals(e.getDatasetAddress(), datasetAddress);
        });
    }

    @Test
    public void onSubscribeCompleted_DatasetAddressInvalidated() {
        final Connector connector = createConnector();

        final DatasetAddress datasetAddress = new DatasetAddress(1, 0);
        final AreaOfInterest areaOfInterest =
                safeAction(() -> Replicant.context().createOrUpdateAreaOfInterest(datasetAddress, null));

        safeAction(() -> Replicant.context().getAreaOfInterestService().invalidateDatasetAddress(datasetAddress));

        createSubscription(datasetAddress, null, SubscriptionMode.EXPLICIT);

        final TestSpyEventHandler handler = registerTestSpyEventHandler();

        connector.onSubscribeCompleted(datasetAddress);

        assertEquals(areaOfInterest.getStatus(), AreaOfInterest.Status.INVALIDATED);

        handler.assertEventCount(1);
        handler.assertNextEvent(SubscribeCompletedEvent.class, e -> {
            assertEquals(e.getSystemSchemaId(), connector.getSystemSchema().getId());
            assertEquals(e.getDatasetAddress(), datasetAddress);
        });
    }

    @Test
    public void onUnsubscribeStarted() {
        final Connector connector = createConnector();

        final DatasetAddress datasetAddress = new DatasetAddress(1, 0);
        final AreaOfInterest areaOfInterest =
                safeAction(() -> Replicant.context().createOrUpdateAreaOfInterest(datasetAddress, null));

        assertEquals(areaOfInterest.getStatus(), AreaOfInterest.Status.PENDING);
        assertFalse(areaOfInterest.isDataAvailable());

        final Subscription subscription = createSubscription(datasetAddress, null, SubscriptionMode.EXPLICIT);

        final TestSpyEventHandler handler = registerTestSpyEventHandler();

        connector.onUnsubscribeStarted(datasetAddress);

        assertEquals(areaOfInterest.getStatus(), AreaOfInterest.Status.SATISFIED);
        assertTrue(areaOfInterest.isDataAvailable());
        safeAction(() -> assertEquals(areaOfInterest.getSubscription(), subscription));

        handler.assertEventCount(1);
        handler.assertNextEvent(UnsubscribeStartedEvent.class, e -> {
            assertEquals(e.getSystemSchemaId(), connector.getSystemSchema().getId());
            assertEquals(e.getDatasetAddress(), datasetAddress);
        });
    }

    @Test
    public void onUnsubscribeCompleted() {
        final Connector connector = createConnector();

        final DatasetAddress datasetAddress = new DatasetAddress(1, 0);
        final AreaOfInterest areaOfInterest =
                safeAction(() -> Replicant.context().createOrUpdateAreaOfInterest(datasetAddress, null));

        assertEquals(areaOfInterest.getStatus(), AreaOfInterest.Status.PENDING);
        assertFalse(areaOfInterest.isDataAvailable());

        final TestSpyEventHandler handler = registerTestSpyEventHandler();

        connector.onUnsubscribeCompleted(datasetAddress);

        assertEquals(areaOfInterest.getStatus(), AreaOfInterest.Status.PENDING);
        assertFalse(areaOfInterest.isDataAvailable());

        handler.assertEventCount(1);
        handler.assertNextEvent(UnsubscribeCompletedEvent.class, e -> {
            assertEquals(e.getSystemSchemaId(), connector.getSystemSchema().getId());
            assertEquals(e.getDatasetAddress(), datasetAddress);
        });
    }

    @Test
    public void onSubscriptionUpdateStarted() {
        final Connector connector = createConnector();

        final DatasetAddress datasetAddress = new DatasetAddress(1, 0);
        final AreaOfInterest areaOfInterest =
                safeAction(() -> Replicant.context().createOrUpdateAreaOfInterest(datasetAddress, null));

        assertEquals(areaOfInterest.getStatus(), AreaOfInterest.Status.PENDING);
        assertFalse(areaOfInterest.isDataAvailable());

        final Subscription subscription = createSubscription(datasetAddress, null, SubscriptionMode.EXPLICIT);

        final TestSpyEventHandler handler = registerTestSpyEventHandler();

        connector.onSubscriptionUpdateStarted(datasetAddress);

        assertEquals(areaOfInterest.getStatus(), AreaOfInterest.Status.SATISFIED);
        assertTrue(areaOfInterest.isDataAvailable());
        safeAction(() -> assertEquals(areaOfInterest.getSubscription(), subscription));

        handler.assertEventCount(1);
        handler.assertNextEvent(SubscriptionUpdateStartedEvent.class, e -> {
            assertEquals(e.getSystemSchemaId(), connector.getSystemSchema().getId());
            assertEquals(e.getDatasetAddress(), datasetAddress);
        });
    }

    @Test
    public void onSubscriptionUpdateCompleted() {
        final Connector connector = createConnector();

        final DatasetAddress datasetAddress = new DatasetAddress(1, 0);
        final AreaOfInterest areaOfInterest =
                safeAction(() -> Replicant.context().createOrUpdateAreaOfInterest(datasetAddress, null));

        assertEquals(areaOfInterest.getStatus(), AreaOfInterest.Status.PENDING);
        assertFalse(areaOfInterest.isDataAvailable());

        final Subscription subscription = createSubscription(datasetAddress, null, SubscriptionMode.EXPLICIT);

        final TestSpyEventHandler handler = registerTestSpyEventHandler();

        connector.onSubscriptionUpdateCompleted(datasetAddress);

        assertEquals(areaOfInterest.getStatus(), AreaOfInterest.Status.SATISFIED);
        assertTrue(areaOfInterest.isDataAvailable());
        safeAction(() -> assertEquals(areaOfInterest.getSubscription(), subscription));

        handler.assertEventCount(1);
        handler.assertNextEvent(SubscriptionUpdateCompletedEvent.class, e -> {
            assertEquals(e.getSystemSchemaId(), connector.getSystemSchema().getId());
            assertEquals(e.getDatasetAddress(), datasetAddress);
        });
    }

    @Test
    public void subscriptionOperationPendingQueries() {
        final Connector connector = createConnector();

        final DatasetAddress datasetAddress = new DatasetAddress(1, 0);
        final String filterParameter = ValueUtil.randomString();

        assertFalse(connector.isSubscriptionOperationPending(
                SubscriptionOperation.Type.SUBSCRIBE, datasetAddress, filterParameter));
        assertEquals(
                connector.lastIndexOfPendingSubscriptionOperation(
                        SubscriptionOperation.Type.SUBSCRIBE, datasetAddress, filterParameter),
                -1);

        final Connection connection = newConnection(connector);

        assertFalse(connector.isSubscriptionOperationPending(
                SubscriptionOperation.Type.SUBSCRIBE, datasetAddress, filterParameter));
        assertEquals(
                connector.lastIndexOfPendingSubscriptionOperation(
                        SubscriptionOperation.Type.SUBSCRIBE, datasetAddress, filterParameter),
                -1);

        connection.requestSubscribe(datasetAddress, filterParameter);

        assertTrue(connector.isSubscriptionOperationPending(
                SubscriptionOperation.Type.SUBSCRIBE, datasetAddress, filterParameter));
        assertEquals(
                connector.lastIndexOfPendingSubscriptionOperation(
                        SubscriptionOperation.Type.SUBSCRIBE, datasetAddress, filterParameter),
                1);
    }

    @Test
    public void connection() {
        final Connector connector = createConnector();

        assertNull(connector.getConnection());

        final Connection connection = newConnection(connector);

        final Subscription subscription1 =
                createSubscription(new DatasetAddress(1, 0), null, SubscriptionMode.EXPLICIT);

        assertEquals(connector.getConnection(), connection);
        assertEquals(connector.ensureConnection(), connection);
        assertFalse(Disposable.isDisposed(subscription1));

        connector.onDisconnection();

        assertNull(connector.getConnection());
        assertTrue(Disposable.isDisposed(subscription1));
    }

    @Test
    public void ensureConnection_WhenNoConnection() {
        final Connector connector = createConnector();

        final IllegalStateException exception = expectThrows(IllegalStateException.class, connector::ensureConnection);

        assertEquals(
                exception.getMessage(), "Replicant-0031: Connector.ensureConnection() when no connection is present.");
    }

    @Test
    public void purgeSubscriptions() {
        final Connector connector1 = createConnector(newSystemSchema(1));
        createConnector(newSystemSchema(2));
        final DatasetAddress invalidatedDatasetAddress = new DatasetAddress(1, 0);
        final AreaOfInterest areaOfInterest =
                safeAction(() -> Replicant.context().createOrUpdateAreaOfInterest(invalidatedDatasetAddress, null));
        safeAction(() ->
                Replicant.context().getAreaOfInterestService().invalidateDatasetAddress(invalidatedDatasetAddress));

        final Subscription subscription1 =
                createSubscription(invalidatedDatasetAddress, null, SubscriptionMode.EXPLICIT);
        final Subscription subscription2 =
                createSubscription(new DatasetAddress(1, 1, 2), null, SubscriptionMode.EXPLICIT);
        // The next two are from a different Connector
        final Subscription subscription3 =
                createSubscription(new DatasetAddress(2, 0, 1), null, SubscriptionMode.EXPLICIT);
        final Subscription subscription4 =
                createSubscription(new DatasetAddress(2, 0, 2), null, SubscriptionMode.EXPLICIT);

        assertFalse(Disposable.isDisposed(subscription1));
        assertFalse(Disposable.isDisposed(subscription2));
        assertFalse(Disposable.isDisposed(subscription3));
        assertFalse(Disposable.isDisposed(subscription4));

        connector1.purgeSubscriptions();

        assertTrue(Disposable.isDisposed(subscription1));
        assertTrue(Disposable.isDisposed(subscription2));
        assertFalse(Disposable.isDisposed(subscription3));
        assertFalse(Disposable.isDisposed(subscription4));
        assertEquals(areaOfInterest.getStatus(), AreaOfInterest.Status.INVALIDATED);
        assertFalse(areaOfInterest.isDataAvailable());
    }

    @Test
    public void progressMessages() {
        final Connector connector = createConnector();
        final Connection connection = newConnection(connector);

        final String[] subscriptionChanges = {"+0"};
        final MessageResponse response = setCurrentMessageResponse(
                connection, ChangeSetMessage.create(null, null, subscriptionChanges, null, null, null));
        response.setParsedSubscriptionChanges(
                Collections.singletonList(SubscriptionChange.from(1, subscriptionChanges[0])));

        assertNull(connector.getSchedulerLock());

        connector.resumeMessageScheduler();

        // response needs processing of Subscription Changes

        final boolean result0 = connector.progressMessages();

        assertTrue(result0);
        final Disposable schedulerLock0 = connector.getSchedulerLock();
        assertNotNull(schedulerLock0);

        // response needs worldValidated

        final boolean result1 = connector.progressMessages();

        assertTrue(result1);
        assertNull(connector.getSchedulerLock());
        assertTrue(Disposable.isDisposed(schedulerLock0));

        final boolean result2 = connector.progressMessages();

        assertTrue(result2);
        // Current message should be nulled and completed processing now
        assertNull(connection.getCurrentMessageResponse());

        final boolean result3 = connector.progressMessages();

        assertFalse(result3);
        assertNull(connector.getSchedulerLock());
    }

    @Test
    public void progressMessages_subscribeResponseProcessesNextAreaOfInterest() {
        assertSubscribeResponseProcessesNextAreaOfInterest();
    }

    @Test
    public void progressMessages_subscribeResponseProcessesNextAreaOfInterest_whenValidationDisabled() {
        ReplicantTestUtil.noValidateReplicasOnLoad();
        assertSubscribeResponseProcessesNextAreaOfInterest();
    }

    private void assertSubscribeResponseProcessesNextAreaOfInterest() {
        final Disposable schedulerLock = pauseScheduler();
        final Connector connector = createConnector();
        connector.pauseMessageScheduler();
        final Connection connection = newConnection(connector);
        safeAction(() -> connector.setState(ConnectorState.CONNECTED));

        final DatasetAddress datasetAddress1 = new DatasetAddress(1, 0);
        final DatasetAddress datasetAddress2 = new DatasetAddress(1, 1);
        safeAction(() -> {
            Replicant.context().createOrUpdateAreaOfInterest(datasetAddress1, null);
            Replicant.context().createOrUpdateAreaOfInterest(datasetAddress2, null);
        });
        final List<AreaOfInterest> areasOfInterest =
                safeAction(() -> Replicant.context().getAreasOfInterest());
        final DatasetAddress firstDatasetAddress = areasOfInterest.get(0).getDatasetAddress();
        final DatasetAddress secondDatasetAddress = areasOfInterest.get(1).getDatasetAddress();

        final RequestEntry request = newRequest(connection);
        connector.requestSubscribe(firstDatasetAddress, null);
        final SubscriptionOperation operation =
                connection.getCurrentSubscriptionOperations().get(0);
        operation.markAsInProgress(request.getRequestId());

        final String subscriptionChange = "+" + firstDatasetAddress.datasetId();
        final MessageResponse response = setCurrentMessageResponse(
                connection,
                ChangeSetMessage.create(
                        request.getRequestId(), null, new String[] {subscriptionChange}, null, null, null),
                request);
        response.setParsedSubscriptionChanges(
                Collections.singletonList(SubscriptionChange.from(1, subscriptionChange)));
        doAnswer(i -> {
                    newRequest(connection);
                    return null;
                })
                .when(connector.getTransport())
                .requestSubscribe(secondDatasetAddress, null);

        schedulerLock.dispose();
        connector.resumeMessageScheduler();

        assertNotNull(Replicant.context().findSubscription(firstDatasetAddress));
        assertTrue(connector.isSubscriptionOperationPending(
                SubscriptionOperation.Type.SUBSCRIBE, secondDatasetAddress, null));
        verify(connector.getTransport()).requestSubscribe(secondDatasetAddress, null);
    }

    @Test
    public void progressMessages_whenConnectionHasBeenDisconnectedInMeantime() {
        final Connector connector = createConnector();
        final Connection connection = newConnection(connector);

        final String[] subscriptionChanges = {"+0"};
        final MessageResponse response = setCurrentMessageResponse(
                connection, ChangeSetMessage.create(null, null, subscriptionChanges, null, null, null));
        response.setParsedSubscriptionChanges(
                Collections.singletonList(SubscriptionChange.from(1, subscriptionChanges[0])));

        final AtomicInteger callCount = new AtomicInteger();
        connector.setPostMessageResponseAction(callCount::incrementAndGet);

        assertNull(connector.getSchedulerLock());
        assertEquals(callCount.get(), 0);

        connector.resumeMessageScheduler();

        assertNull(connector.getSchedulerLock());
        assertEquals(callCount.get(), 0);

        assertTrue(connector.progressMessages());

        assertEquals(callCount.get(), 0);
        assertNotNull(connector.getSchedulerLock());

        safeAction(() -> {
            connector.setState(ConnectorState.ERROR);
            connector.setConnection(null);
        });

        // The rest of the message has been skipped as no connection left
        assertFalse(connector.progressMessages());

        assertNull(connector.getSchedulerLock());
        assertEquals(callCount.get(), 1);
    }

    @Test
    public void progressMessages_withError() {
        final Connector connector = createConnector();
        final Connection connection = newConnection(connector);

        connection.injectCurrentSubscriptionOperation(
                new SubscriptionOperation(new DatasetAddress(0, 0), SubscriptionOperation.Type.UNSUBSCRIBE, null));
        final TestSpyEventHandler handler = registerTestSpyEventHandler();

        connector.resumeMessageScheduler();

        final boolean result2 = connector.progressMessages();

        assertFalse(result2);

        assertNull(connector.getSchedulerLock());

        handler.assertEventCountAtLeast(1);
        handler.assertNextEvent(MessageProcessFailureEvent.class, e -> {
            assertEquals(e.getSystemSchemaId(), connector.getSystemSchema().getId());
            assertEquals(
                    e.getError().getMessage(),
                    "Replicant-0046: Request to unsubscribe at Dataset Address 0.0 but no Subscription exists.");
        });
    }

    @Test
    public void requestSubscribe() {
        final Connector connector = createConnector();
        newConnection(connector);
        connector.pauseMessageScheduler();

        final DatasetAddress datasetAddress = new DatasetAddress(1, 0);

        assertFalse(
                connector.isSubscriptionOperationPending(SubscriptionOperation.Type.SUBSCRIBE, datasetAddress, null));

        final TestSpyEventHandler handler = registerTestSpyEventHandler();

        connector.requestSubscribe(datasetAddress, null);

        assertTrue(
                connector.isSubscriptionOperationPending(SubscriptionOperation.Type.SUBSCRIBE, datasetAddress, null));

        handler.assertEventCount(1);
        handler.assertNextEvent(
                SubscribeRequestQueuedEvent.class, e -> assertEquals(e.getDatasetAddress(), datasetAddress));
    }

    @Test
    public void requestSubscribe_isKeyed_forUpdatableParameterKeyedDataset() {
        final Dataset dataset = new Dataset(
                0,
                ValueUtil.randomString(),
                null,
                Dataset.FilterMode.PARAMETER_FILTERED,
                Dataset.FilterParameterMode.UPDATABLE,
                true,
                (f, e) -> true,
                false,
                true,
                Collections.emptyList());
        final SystemSchema systemSchema =
                new SystemSchema(1, ValueUtil.randomString(), new Dataset[] {dataset}, new EntityType[0]);
        final Connector connector = createConnector(systemSchema);
        newConnection(connector);

        final DatasetAddress datasetAddress = new DatasetAddress(1, 0);

        final IllegalStateException exception =
                expectThrows(IllegalStateException.class, () -> connector.requestSubscribe(datasetAddress, null));

        assertEquals(
                exception.getMessage(),
                "Replicant-0098: Dataset Address 1.0 requires a Dataset Key but none was supplied.");
    }

    @Test
    public void requestSubscribe_isKeyed_forFixedParameterKeyedDataset() {
        final Dataset dataset = new Dataset(
                0,
                ValueUtil.randomString(),
                null,
                Dataset.FilterMode.PARAMETER_FILTERED,
                Dataset.FilterParameterMode.FIXED,
                true,
                null,
                false,
                true,
                Collections.emptyList());
        final SystemSchema systemSchema =
                new SystemSchema(1, ValueUtil.randomString(), new Dataset[] {dataset}, new EntityType[0]);
        final Connector connector = createConnector(systemSchema);
        newConnection(connector);

        final DatasetAddress datasetAddress = new DatasetAddress(1, 0);

        final IllegalStateException exception =
                expectThrows(IllegalStateException.class, () -> connector.requestSubscribe(datasetAddress, null));

        assertEquals(
                exception.getMessage(),
                "Replicant-0098: Dataset Address 1.0 requires a Dataset Key but none was supplied.");
    }

    @Test
    public void requestSubscribe_rejectsDatasetKey_forNonKeyedDataset() {
        final Dataset dataset = new Dataset(
                0,
                ValueUtil.randomString(),
                null,
                Dataset.FilterMode.PARAMETER_FILTERED,
                Dataset.FilterParameterMode.UPDATABLE,
                false,
                (f, e) -> true,
                false,
                true,
                Collections.emptyList());
        final SystemSchema systemSchema =
                new SystemSchema(1, ValueUtil.randomString(), new Dataset[] {dataset}, new EntityType[0]);
        final Connector connector = createConnector(systemSchema);
        newConnection(connector);

        final DatasetAddress datasetAddress = new DatasetAddress(1, 0, null, "inst");

        final IllegalStateException exception =
                expectThrows(IllegalStateException.class, () -> connector.requestSubscribe(datasetAddress, null));

        assertEquals(
                exception.getMessage(),
                "Replicant-0099: Dataset Address 1.0#inst does not support Dataset Keys but one was supplied.");
    }

    @Test
    public void requestSubscribe_updatableParameterKeyed_withDatasetKey() {
        final Dataset dataset = new Dataset(
                0,
                ValueUtil.randomString(),
                null,
                Dataset.FilterMode.PARAMETER_FILTERED,
                Dataset.FilterParameterMode.UPDATABLE,
                true,
                (f, e) -> true,
                false,
                true,
                Collections.emptyList());
        final SystemSchema systemSchema =
                new SystemSchema(1, ValueUtil.randomString(), new Dataset[] {dataset}, new EntityType[0]);
        final Connector connector = createConnector(systemSchema);
        newConnection(connector);
        connector.pauseMessageScheduler();

        final DatasetAddress datasetAddress = new DatasetAddress(1, 0, null, "inst");

        assertFalse(
                connector.isSubscriptionOperationPending(SubscriptionOperation.Type.SUBSCRIBE, datasetAddress, null));

        connector.requestSubscribe(datasetAddress, null);

        assertTrue(
                connector.isSubscriptionOperationPending(SubscriptionOperation.Type.SUBSCRIBE, datasetAddress, null));
    }

    @Test
    public void requestSubscribe_fixedParameterKeyed_withDatasetKey() {
        final Dataset dataset = new Dataset(
                0,
                ValueUtil.randomString(),
                null,
                Dataset.FilterMode.PARAMETER_FILTERED,
                Dataset.FilterParameterMode.FIXED,
                true,
                null,
                false,
                true,
                Collections.emptyList());
        final SystemSchema systemSchema =
                new SystemSchema(1, ValueUtil.randomString(), new Dataset[] {dataset}, new EntityType[0]);
        final Connector connector = createConnector(systemSchema);
        newConnection(connector);
        connector.pauseMessageScheduler();

        final DatasetAddress datasetAddress = new DatasetAddress(1, 0, null, "inst");

        assertFalse(
                connector.isSubscriptionOperationPending(SubscriptionOperation.Type.SUBSCRIBE, datasetAddress, null));

        connector.requestSubscribe(datasetAddress, null);

        assertTrue(
                connector.isSubscriptionOperationPending(SubscriptionOperation.Type.SUBSCRIBE, datasetAddress, null));
    }

    @Test
    public void requestSubscriptionUpdate() {
        final Dataset dataset = new Dataset(
                0,
                ValueUtil.randomString(),
                null,
                Dataset.FilterMode.PARAMETER_FILTERED,
                Dataset.FilterParameterMode.UPDATABLE,
                false,
                (f, e) -> true,
                false,
                true,
                Collections.emptyList());
        final SystemSchema systemSchema =
                new SystemSchema(1, ValueUtil.randomString(), new Dataset[] {dataset}, new EntityType[0]);
        final Connector connector = createConnector(systemSchema);
        newConnection(connector);
        connector.pauseMessageScheduler();

        final DatasetAddress datasetAddress = new DatasetAddress(1, 0);

        assertFalse(connector.isSubscriptionOperationPending(SubscriptionOperation.Type.UPDATE, datasetAddress, null));

        final TestSpyEventHandler handler = registerTestSpyEventHandler();

        connector.requestSubscriptionUpdate(datasetAddress, null);

        assertTrue(connector.isSubscriptionOperationPending(SubscriptionOperation.Type.UPDATE, datasetAddress, null));

        handler.assertEventCount(1);
        handler.assertNextEvent(
                SubscriptionUpdateRequestQueuedEvent.class, e -> assertEquals(e.getDatasetAddress(), datasetAddress));
    }

    @Test
    public void requestSubscriptionUpdate_isKeyed_forUpdatableParameterKeyedDataset() {
        final Dataset dataset = new Dataset(
                0,
                ValueUtil.randomString(),
                null,
                Dataset.FilterMode.PARAMETER_FILTERED,
                Dataset.FilterParameterMode.UPDATABLE,
                true,
                (f, e) -> true,
                false,
                true,
                Collections.emptyList());
        final SystemSchema systemSchema =
                new SystemSchema(1, ValueUtil.randomString(), new Dataset[] {dataset}, new EntityType[0]);
        final Connector connector = createConnector(systemSchema);
        newConnection(connector);
        connector.pauseMessageScheduler();

        final DatasetAddress datasetAddress = new DatasetAddress(1, 0);

        final IllegalStateException exception = expectThrows(
                IllegalStateException.class, () -> connector.requestSubscriptionUpdate(datasetAddress, null));

        assertEquals(
                exception.getMessage(),
                "Replicant-0098: Dataset Address 1.0 requires a Dataset Key but none was supplied.");
    }

    @Test
    public void requestSubscriptionUpdate_rejectsDatasetWithoutUpdatableFilterParameter() {
        final Dataset dataset = new Dataset(
                0,
                ValueUtil.randomString(),
                null,
                Dataset.FilterMode.PARAMETER_FILTERED,
                Dataset.FilterParameterMode.FIXED,
                false,
                null,
                false,
                true,
                Collections.emptyList());
        final SystemSchema systemSchema =
                new SystemSchema(1, ValueUtil.randomString(), new Dataset[] {dataset}, new EntityType[0]);
        final Connector connector = createConnector(systemSchema);
        newConnection(connector);
        connector.pauseMessageScheduler();

        final DatasetAddress datasetAddress = new DatasetAddress(1, 0);

        assertFalse(connector.isSubscriptionOperationPending(SubscriptionOperation.Type.UPDATE, datasetAddress, null));

        final IllegalStateException exception = expectThrows(
                IllegalStateException.class, () -> connector.requestSubscriptionUpdate(datasetAddress, null));

        assertEquals(
                exception.getMessage(),
                "Replicant-0082: Connector.requestSubscriptionUpdate invoked for Dataset Address 1.0 but the Dataset"
                        + " does not have an updatable Filter Parameter.");
    }

    @Test
    public void requestUnsubscribe_isKeyed_forUpdatableParameterKeyedDataset() {
        final Dataset dataset = new Dataset(
                0,
                ValueUtil.randomString(),
                null,
                Dataset.FilterMode.PARAMETER_FILTERED,
                Dataset.FilterParameterMode.UPDATABLE,
                true,
                (f, e) -> true,
                false,
                true,
                Collections.emptyList());
        final SystemSchema systemSchema =
                new SystemSchema(1, ValueUtil.randomString(), new Dataset[] {dataset}, new EntityType[0]);
        final Connector connector = createConnector(systemSchema);
        newConnection(connector);

        final DatasetAddress datasetAddress = new DatasetAddress(1, 0);

        final IllegalStateException exception =
                expectThrows(IllegalStateException.class, () -> connector.requestUnsubscribe(datasetAddress));

        assertEquals(
                exception.getMessage(),
                "Replicant-0098: Dataset Address 1.0 requires a Dataset Key but none was supplied.");
    }

    @Test
    public void requestUnsubscribe_isKeyed_forFixedParameterKeyedDataset() {
        final Dataset dataset = new Dataset(
                0,
                ValueUtil.randomString(),
                null,
                Dataset.FilterMode.PARAMETER_FILTERED,
                Dataset.FilterParameterMode.FIXED,
                true,
                null,
                false,
                true,
                Collections.emptyList());
        final SystemSchema systemSchema =
                new SystemSchema(1, ValueUtil.randomString(), new Dataset[] {dataset}, new EntityType[0]);
        final Connector connector = createConnector(systemSchema);
        newConnection(connector);

        final DatasetAddress datasetAddress = new DatasetAddress(1, 0);

        final IllegalStateException exception =
                expectThrows(IllegalStateException.class, () -> connector.requestUnsubscribe(datasetAddress));

        assertEquals(
                exception.getMessage(),
                "Replicant-0098: Dataset Address 1.0 requires a Dataset Key but none was supplied.");
    }

    @Test
    public void requestUnsubscribe() throws InterruptedException {
        final Connector connector = createConnector();
        pauseScheduler();
        connector.pauseMessageScheduler();
        newConnection(connector);

        final DatasetAddress datasetAddress = new DatasetAddress(1, 0);

        createSubscription(datasetAddress, null, SubscriptionMode.EXPLICIT);

        assertFalse(
                connector.isSubscriptionOperationPending(SubscriptionOperation.Type.UNSUBSCRIBE, datasetAddress, null));

        final TestSpyEventHandler handler = registerTestSpyEventHandler();

        connector.requestUnsubscribe(datasetAddress);

        Thread.sleep(100);

        assertTrue(
                connector.isSubscriptionOperationPending(SubscriptionOperation.Type.UNSUBSCRIBE, datasetAddress, null));

        handler.assertEventCount(1);
        handler.assertNextEvent(
                UnsubscribeRequestQueuedEvent.class, e -> assertEquals(e.getDatasetAddress(), datasetAddress));
    }

    @Test
    public void reevaluateReplicaMembershipAfterFilterParameterUpdate() {
        final FilterParameterUpdateReplicaMatcher<?> filterParameter = (f, replicaEntry) -> replicaEntry.getId() > 0;
        final Dataset dataset = new Dataset(
                0,
                ValueUtil.randomString(),
                null,
                Dataset.FilterMode.PARAMETER_FILTERED,
                Dataset.FilterParameterMode.UPDATABLE,
                false,
                filterParameter,
                true,
                true,
                Collections.emptyList());
        final SystemSchema systemSchema =
                new SystemSchema(1, ValueUtil.randomString(), new Dataset[] {dataset}, new EntityType[0]);

        final Connector connector = createConnector(systemSchema);
        newConnection(connector);

        final DatasetAddress datasetAddress1 = new DatasetAddress(1, 0, 1);
        final DatasetAddress datasetAddress2 = new DatasetAddress(1, 0, 2);

        final Subscription subscription1 =
                createSubscription(datasetAddress1, ValueUtil.randomString(), SubscriptionMode.EXPLICIT);
        final Subscription subscription2 =
                createSubscription(datasetAddress2, ValueUtil.randomString(), SubscriptionMode.EXPLICIT);

        // Use Integer and String as arbitrary types for our Replica Entries...
        // Anything with id below 0 will be removed during update ...
        final ReplicaEntry replicaEntry1 = findOrCreateReplicaEntry(Integer.class, -1);
        final ReplicaEntry replicaEntry2 = findOrCreateReplicaEntry(Integer.class, -2);
        final ReplicaEntry replicaEntry3 = findOrCreateReplicaEntry(Integer.class, -3);
        final ReplicaEntry replicaEntry4 = findOrCreateReplicaEntry(Integer.class, -4);
        final ReplicaEntry replicaEntry5 = findOrCreateReplicaEntry(String.class, 5);
        final ReplicaEntry replicaEntry6 = findOrCreateReplicaEntry(String.class, 6);

        safeAction(() -> {
            replicaEntry1.linkToSubscription(subscription1);
            replicaEntry2.linkToSubscription(subscription1);
            replicaEntry3.linkToSubscription(subscription1);
            replicaEntry4.linkToSubscription(subscription1);
            replicaEntry5.linkToSubscription(subscription1);
            replicaEntry6.linkToSubscription(subscription1);

            replicaEntry3.linkToSubscription(subscription2);
            replicaEntry4.linkToSubscription(subscription2);

            assertEquals(subscription1.getReplicaEntries().size(), 2);
            assertEquals(
                    subscription1.findAllReplicaEntriesByType(Integer.class).size(), 4);
            assertEquals(subscription1.findAllReplicaEntriesByType(String.class).size(), 2);
            assertEquals(subscription2.getReplicaEntries().size(), 1);
            assertEquals(
                    subscription2.findAllReplicaEntriesByType(Integer.class).size(), 2);
        });

        safeAction(() -> connector.reevaluateReplicaMembershipAfterFilterParameterUpdate(subscription1));

        safeAction(() -> {
            assertTrue(Disposable.isDisposed(replicaEntry1));
            assertTrue(Disposable.isDisposed(replicaEntry2));
            assertFalse(Disposable.isDisposed(replicaEntry3));
            assertFalse(Disposable.isDisposed(replicaEntry4));
            assertFalse(Disposable.isDisposed(replicaEntry5));
            assertFalse(Disposable.isDisposed(replicaEntry6));

            assertEquals(subscription1.getReplicaEntries().size(), 1);
            assertEquals(
                    subscription1.findAllReplicaEntriesByType(Integer.class).size(), 0);
            assertEquals(subscription1.findAllReplicaEntriesByType(String.class).size(), 2);
            assertEquals(subscription2.getReplicaEntries().size(), 1);
            assertEquals(
                    subscription2.findAllReplicaEntriesByType(Integer.class).size(), 2);
        });
    }

    @Test
    public void reevaluateReplicaMembershipAfterFilterParameterUpdate_rejectsFixedFilterParameter() {
        final Dataset dataset = new Dataset(
                0,
                ValueUtil.randomString(),
                null,
                Dataset.FilterMode.PARAMETER_FILTERED,
                Dataset.FilterParameterMode.FIXED,
                false,
                null,
                true,
                true,
                Collections.emptyList());
        final SystemSchema systemSchema =
                new SystemSchema(1, ValueUtil.randomString(), new Dataset[] {dataset}, new EntityType[0]);

        final Connector connector = createConnector(systemSchema);
        newConnection(connector);

        final DatasetAddress datasetAddress1 = new DatasetAddress(1, 0, 1);

        final Subscription subscription1 =
                createSubscription(datasetAddress1, ValueUtil.randomString(), SubscriptionMode.EXPLICIT);

        final IllegalStateException exception = expectThrows(
                IllegalStateException.class,
                () -> safeAction(() -> connector.reevaluateReplicaMembershipAfterFilterParameterUpdate(subscription1)));

        assertEquals(
                exception.getMessage(),
                "Replicant-0079: Connector.reevaluateReplicaMembershipAfterFilterParameterUpdate invoked for Dataset"
                        + " Address 1.0.1 but the Dataset does not have an updatable Filter Parameter.");
    }

    @SuppressWarnings("unchecked")
    @Test
    public void processEntityChanges() {
        final int systemSchemaId = 1;
        final Dataset dataset = new Dataset(
                0,
                ValueUtil.randomString(),
                null,
                Dataset.FilterMode.UNFILTERED,
                null,
                false,
                null,
                false,
                true,
                Collections.emptyList());
        final EntityType.Creator<Linkable> creator = mock(EntityType.Creator.class);
        final EntityType.Updater<Linkable> updater = mock(EntityType.Updater.class);
        final EntityType entityType =
                new EntityType(0, ValueUtil.randomString(), Linkable.class, creator, updater, new DatasetLink[0]);
        final SystemSchema systemSchema = new SystemSchema(
                systemSchemaId, ValueUtil.randomString(), new Dataset[] {dataset}, new EntityType[] {entityType});
        final Connector connector = createConnector(systemSchema);
        connector.setLinksToProcessPerTick(1);

        final Connection connection = newConnection(connector);

        final Linkable replica1 = mock(Linkable.class);
        final Linkable replica2 = mock(Linkable.class);

        // Pause scheduler to prevent subscription reconciliation
        pauseScheduler();

        final DatasetAddress datasetAddress =
                new DatasetAddress(connector.getSystemSchema().getId(), 1);
        final Subscription subscription = createSubscription(datasetAddress, null, SubscriptionMode.EXPLICIT);

        // This Replica Entry is to be updated
        final ReplicaEntry replicaEntry2 = findOrCreateReplicaEntry(Linkable.class, 2);
        safeAction(() -> replicaEntry2.setReplica(replica2));
        // The Replica already belongs to the Subscription and that should be fine
        safeAction(() -> replicaEntry2.linkToSubscription(subscription));
        // This Replica Entry is to be removed
        final ReplicaEntry replicaEntry3 = findOrCreateReplicaEntry(Linkable.class, 3);

        final EntityChangeData data1 = mock(EntityChangeData.class);
        final EntityChangeData data2 = mock(EntityChangeData.class);
        final EntityChange[] entityChanges = {
            // Update changes
            EntityChange.create(0, 1, new String[] {"1"}, data1),
            EntityChange.create(0, 2, new String[] {"1"}, data2),
            // Remove change
            EntityChange.create(0, 3, new String[] {"1"})
        };
        final MessageResponse response = setCurrentMessageResponse(
                connection, ChangeSetMessage.create(null, null, null, null, entityChanges, null));

        when(creator.createReplica(1, data1)).thenReturn(replica1);

        assertEquals(response.getEntityUpdateCount(), 0);
        assertEquals(response.getEntityRemoveCount(), 0);

        connector.setChangesToProcessPerTick(1);

        connector.processEntityChanges();

        verify(creator, times(1)).createReplica(1, data1);
        verify(updater, never()).updateReplica(replica1, data1);
        verify(creator, never()).createReplica(2, data2);
        verify(updater, never()).updateReplica(replica2, data2);

        assertEquals(response.getEntityUpdateCount(), 1);
        assertEquals(response.getEntityRemoveCount(), 0);

        connector.setChangesToProcessPerTick(2);

        connector.processEntityChanges();

        verify(creator, times(1)).createReplica(1, data1);
        verify(updater, never()).updateReplica(replica1, data1);
        verify(creator, never()).createReplica(2, data2);
        verify(updater, times(1)).updateReplica(replica2, data2);

        assertEquals(response.getEntityUpdateCount(), 2);
        assertEquals(response.getEntityRemoveCount(), 1);
        assertFalse(Disposable.isDisposed(replicaEntry2));
        assertTrue(Disposable.isDisposed(replicaEntry3));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void processEntityChanges_withDatasetKey() {
        final int systemSchemaId = 1;
        final Dataset dataset = new Dataset(
                0,
                ValueUtil.randomString(),
                Linkable.class,
                Dataset.FilterMode.PARAMETER_FILTERED,
                Dataset.FilterParameterMode.UPDATABLE,
                true,
                (f, e) -> true,
                false,
                true,
                Collections.emptyList());
        final EntityType.Creator<Linkable> creator = mock(EntityType.Creator.class);
        final EntityType.Updater<Linkable> updater = mock(EntityType.Updater.class);
        final EntityType entityType =
                new EntityType(0, ValueUtil.randomString(), Linkable.class, creator, updater, new DatasetLink[0]);
        final SystemSchema systemSchema = new SystemSchema(
                systemSchemaId, ValueUtil.randomString(), new Dataset[] {dataset}, new EntityType[] {entityType});
        final Connector connector = createConnector(systemSchema);
        connector.setLinksToProcessPerTick(1);

        final Connection connection = newConnection(connector);

        // Pause scheduler to prevent subscription reconciliation
        pauseScheduler();

        final int datasetRootId = ValueUtil.randomInt();
        final DatasetAddress datasetAddress =
                new DatasetAddress(connector.getSystemSchema().getId(), 0, datasetRootId, "fi");
        final Subscription subscription = createSubscription(datasetAddress, null, SubscriptionMode.EXPLICIT);

        final EntityChangeData data = mock(EntityChangeData.class);
        final EntityChange[] entityChanges = {
            EntityChange.create(0, 1, new String[] {"0." + datasetRootId + "#fi"}, data)
        };
        setCurrentMessageResponse(connection, ChangeSetMessage.create(null, null, null, null, entityChanges, null));

        when(creator.createReplica(1, data)).thenReturn(mock(Linkable.class));

        connector.processEntityChanges();

        safeAction(() -> assertNotNull(subscription.findReplicaEntryByTypeAndId(Linkable.class, 1)));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void processEntityChanges_referenceNonExistentSubscription() {
        final int systemSchemaId = 1;
        final Dataset dataset = new Dataset(
                0,
                ValueUtil.randomString(),
                null,
                Dataset.FilterMode.UNFILTERED,
                null,
                false,
                null,
                false,
                true,
                Collections.emptyList());
        final EntityType.Creator<Linkable> creator = mock(EntityType.Creator.class);
        final EntityType.Updater<Linkable> updater = mock(EntityType.Updater.class);
        final EntityType entityType =
                new EntityType(0, ValueUtil.randomString(), Linkable.class, creator, updater, new DatasetLink[0]);
        final SystemSchema systemSchema = new SystemSchema(
                systemSchemaId, ValueUtil.randomString(), new Dataset[] {dataset}, new EntityType[] {entityType});
        final Connector connector = createConnector(systemSchema);
        connector.setLinksToProcessPerTick(1);

        final Connection connection = newConnection(connector);

        final Linkable replica1 = mock(Linkable.class);

        // Pause scheduler to prevent subscription reconciliation
        pauseScheduler();

        final EntityChangeData data1 = mock(EntityChangeData.class);
        final EntityChange[] entityChanges = {EntityChange.create(0, 1, new String[] {"1"}, data1)};
        setCurrentMessageResponse(connection, ChangeSetMessage.create(null, null, null, null, entityChanges, null));

        when(creator.createReplica(1, data1)).thenReturn(replica1);

        final IllegalStateException exception =
                expectThrows(IllegalStateException.class, connector::processEntityChanges);
        assertEquals(
                exception.getMessage(),
                "Replicant-0069: ChangeSetMessage contained an Entity Change referencing Dataset Address 1.1 but"
                        + " no such"
                        + " subscription exists locally.");
    }

    @Test
    public void processEntityChanges_deleteNonExistingEntity() {
        final int systemSchemaId = 1;
        final Dataset dataset = new Dataset(
                0,
                ValueUtil.randomString(),
                null,
                Dataset.FilterMode.UNFILTERED,
                null,
                false,
                null,
                false,
                true,
                Collections.emptyList());
        final EntityType entityType = new EntityType(
                0, ValueUtil.randomString(), MyEntity.class, (i, d) -> new MyEntity(), null, new DatasetLink[0]);
        final SystemSchema systemSchema = new SystemSchema(
                systemSchemaId, ValueUtil.randomString(), new Dataset[] {dataset}, new EntityType[] {entityType});
        final Connector connector = createConnector(systemSchema);
        connector.setLinksToProcessPerTick(1);

        final Connection connection = newConnection(connector);

        // Pause scheduler to prevent subscription reconciliation
        pauseScheduler();

        final EntityChange[] entityChanges = {
            // Remove change
            EntityChange.create(0, 3, new String[] {"1"})
        };
        final MessageResponse response = setCurrentMessageResponse(
                connection, ChangeSetMessage.create(null, null, null, null, entityChanges, null));

        connector.setChangesToProcessPerTick(1);

        connector.processEntityChanges();

        assertEquals(response.getEntityRemoveCount(), 0);
    }

    @Test
    public void processReplicaLinks() {
        final Connector connector = createConnector();
        connector.setLinksToProcessPerTick(1);

        final Connection connection = newConnection(connector);
        final MessageResponse response = setCurrentMessageResponse(
                connection, ChangeSetMessage.create(null, null, new String[0], null, new EntityChange[0], null));

        final Linkable replica1 = mock(Linkable.class);
        final Linkable replica2 = mock(Linkable.class);
        final Linkable replica3 = mock(Linkable.class);
        final Linkable replica4 = mock(Linkable.class);

        response.replicaProcessed(replica1);
        response.replicaProcessed(replica2);
        response.replicaProcessed(replica3);
        response.replicaProcessed(replica4);

        verify(replica1, never()).link();
        verify(replica2, never()).link();
        verify(replica3, never()).link();
        verify(replica4, never()).link();

        assertEquals(response.getEntityLinkCount(), 0);

        connector.setLinksToProcessPerTick(1);

        connector.processReplicaLinks();

        assertEquals(response.getEntityLinkCount(), 1);
        verify(replica1, times(1)).link();
        verify(replica2, never()).link();
        verify(replica3, never()).link();
        verify(replica4, never()).link();

        connector.setLinksToProcessPerTick(2);

        connector.processReplicaLinks();

        assertEquals(response.getEntityLinkCount(), 3);
        verify(replica1, times(1)).link();
        verify(replica2, times(1)).link();
        verify(replica3, times(1)).link();
        verify(replica4, never()).link();
    }

    @Test
    public void completeSubscriptionOperation() {
        final Connector connector = createConnector();
        final Connection connection = newConnection(connector);

        connection.injectCurrentSubscriptionOperation(
                new SubscriptionOperation(new DatasetAddress(1, 0), SubscriptionOperation.Type.SUBSCRIBE, null));

        assertFalse(connection.getCurrentSubscriptionOperations().isEmpty());

        connector.completeSubscriptionOperation();

        assertTrue(connection.getCurrentSubscriptionOperations().isEmpty());
    }

    @Test
    public void processSubscriptionChanges_subscribe() {
        final Connector connector = createConnector();
        final Connection connection = newConnection(connector);

        final int datasetId = 0;
        final int datasetRootId = ValueUtil.randomInt();
        final String filterParameter = null;
        final String[] subscriptionChanges = {"+0." + datasetRootId};

        final MessageResponse response = setCurrentMessageResponse(
                connection, ChangeSetMessage.create(null, null, subscriptionChanges, null, null, null));
        response.setParsedSubscriptionChanges(
                Collections.singletonList(SubscriptionChange.from(1, subscriptionChanges[0])));

        assertTrue(response.needsSubscriptionChangesProcessed());

        final TestSpyEventHandler handler = registerTestSpyEventHandler();

        connector.processSubscriptionChanges();

        assertFalse(response.needsSubscriptionChangesProcessed());

        final DatasetAddress datasetAddress = new DatasetAddress(1, datasetId, datasetRootId);
        final Subscription subscription =
                Objects.requireNonNull(Replicant.context().findSubscription(datasetAddress));
        assertEquals(subscription.datasetAddress(), datasetAddress);
        safeAction(() -> assertEquals(subscription.getFilterParameter(), filterParameter));
        safeAction(() -> assertEquals(subscription.getMode(), SubscriptionMode.IMPLICIT));

        handler.assertEventCount(1);
        handler.assertNextEvent(SubscriptionCreatedEvent.class, e -> {
            assertEquals(e.getSubscription().datasetAddress(), datasetAddress);
            safeAction(() -> assertEquals(e.getSubscription().getFilterParameter(), filterParameter));
        });
    }

    @Test
    public void processSubscriptionChanges_subscribe_withFilter() {
        final Connector connector = createConnector();
        final Connection connection = newConnection(connector);

        final int datasetId = 0;
        final int datasetRootId = ValueUtil.randomInt();
        final String filterParameter = ValueUtil.randomString();
        final SubscriptionChangeMessage[] filterParameterSubscriptionChanges = {
            SubscriptionChangeMessage.create("+0." + datasetRootId, filterParameter)
        };
        final MessageResponse response = setCurrentMessageResponse(
                connection, ChangeSetMessage.create(null, null, null, filterParameterSubscriptionChanges, null, null));
        response.setParsedSubscriptionChanges(
                Collections.singletonList(SubscriptionChange.from(1, filterParameterSubscriptionChanges[0])));

        assertTrue(response.needsSubscriptionChangesProcessed());

        final TestSpyEventHandler handler = registerTestSpyEventHandler();

        connector.processSubscriptionChanges();

        assertFalse(response.needsSubscriptionChangesProcessed());

        final DatasetAddress datasetAddress = new DatasetAddress(1, datasetId, datasetRootId);
        final Subscription subscription =
                Objects.requireNonNull(Replicant.context().findSubscription(datasetAddress));
        assertEquals(subscription.datasetAddress(), datasetAddress);
        safeAction(() -> assertEquals(subscription.getFilterParameter(), filterParameter));
        safeAction(() -> assertEquals(subscription.getMode(), SubscriptionMode.IMPLICIT));

        handler.assertEventCount(1);
        handler.assertNextEvent(SubscriptionCreatedEvent.class, e -> {
            assertEquals(e.getSubscription().datasetAddress(), datasetAddress);
            safeAction(() -> assertEquals(e.getSubscription().getFilterParameter(), filterParameter));
        });
    }

    @Test
    public void processSubscriptionChanges_subscribe_replacesFixedFilterParameterSubscription() {
        final Dataset dataset = new Dataset(
                0,
                ValueUtil.randomString(),
                null,
                Dataset.FilterMode.PARAMETER_FILTERED,
                Dataset.FilterParameterMode.FIXED,
                false,
                null,
                false,
                true,
                Collections.emptyList());
        final SystemSchema systemSchema =
                new SystemSchema(1, ValueUtil.randomString(), new Dataset[] {dataset}, new EntityType[0]);
        final Connector connector = createConnector(systemSchema);
        connector.pauseMessageScheduler();
        final Connection connection = newConnection(connector);

        final DatasetAddress datasetAddress = new DatasetAddress(1, 0);
        final String oldFilterParameter = ValueUtil.randomString();
        final String newFilterParameter = ValueUtil.randomString();
        safeAction(() -> Replicant.context().createOrUpdateAreaOfInterest(datasetAddress, newFilterParameter));
        final Subscription initialSubscription =
                createSubscription(datasetAddress, oldFilterParameter, SubscriptionMode.EXPLICIT);
        final ReplicaEntry replicaEntry = findOrCreateReplicaEntry(String.class, ValueUtil.randomInt());
        safeAction(() -> replicaEntry.linkToSubscription(initialSubscription));

        final SubscriptionChangeMessage subscriptionChange = SubscriptionChangeMessage.create("+0", newFilterParameter);
        final MessageResponse response = setCurrentMessageResponse(
                connection,
                ChangeSetMessage.create(
                        null, null, null, new SubscriptionChangeMessage[] {subscriptionChange}, null, null));
        response.setParsedSubscriptionChanges(
                Collections.singletonList(SubscriptionChange.from(1, subscriptionChange)));

        connector.processSubscriptionChanges();

        final Subscription replacementSubscription =
                Objects.requireNonNull(Replicant.context().findSubscription(datasetAddress));
        assertNotSame(replacementSubscription, initialSubscription);
        assertTrue(Disposable.isDisposed(initialSubscription));
        assertFalse(Disposable.isDisposed(replacementSubscription));
        safeAction(() -> assertEquals(replacementSubscription.getFilterParameter(), newFilterParameter));
        safeAction(() -> assertEquals(replacementSubscription.getMode(), SubscriptionMode.EXPLICIT));
        assertTrue(Disposable.isDisposed(replicaEntry));
        assertTrue(replicaEntry.subscriptions().isEmpty());
        assertEquals(response.getSubscriptionSubscribeCount(), 1);
    }

    @Test
    public void processSubscriptionChanges_subscribe_rejectsSameFixedFilterParameter() {
        final Dataset dataset = new Dataset(
                0,
                ValueUtil.randomString(),
                null,
                Dataset.FilterMode.PARAMETER_FILTERED,
                Dataset.FilterParameterMode.FIXED,
                false,
                null,
                false,
                true,
                Collections.emptyList());
        final SystemSchema systemSchema =
                new SystemSchema(1, ValueUtil.randomString(), new Dataset[] {dataset}, new EntityType[0]);
        final Connector connector = createConnector(systemSchema);
        final Connection connection = newConnection(connector);

        final DatasetAddress datasetAddress = new DatasetAddress(1, 0);
        final String filterParameter = ValueUtil.randomString();
        createSubscription(datasetAddress, filterParameter, SubscriptionMode.EXPLICIT);
        final SubscriptionChangeMessage subscriptionChange = SubscriptionChangeMessage.create("+0", filterParameter);
        final MessageResponse response = setCurrentMessageResponse(
                connection,
                ChangeSetMessage.create(
                        null, null, null, new SubscriptionChangeMessage[] {subscriptionChange}, null, null));
        response.setParsedSubscriptionChanges(
                Collections.singletonList(SubscriptionChange.from(1, subscriptionChange)));

        final IllegalStateException exception =
                expectThrows(IllegalStateException.class, connector::processSubscriptionChanges);

        assertEquals(
                exception.getMessage(),
                "Replicant-0064: createSubscription invoked with Dataset Address 1.0 but a subscription with that"
                        + " Dataset Address already exists.");
    }

    @Test
    public void processSubscriptionChanges_subscribe_rejectsExistingUpdatableFilterParameterSubscription() {
        final Dataset dataset = new Dataset(
                0,
                ValueUtil.randomString(),
                null,
                Dataset.FilterMode.PARAMETER_FILTERED,
                Dataset.FilterParameterMode.UPDATABLE,
                false,
                (filterParameter, replicaEntry) -> true,
                false,
                true,
                Collections.emptyList());
        final SystemSchema systemSchema =
                new SystemSchema(1, ValueUtil.randomString(), new Dataset[] {dataset}, new EntityType[0]);
        final Connector connector = createConnector(systemSchema);
        final Connection connection = newConnection(connector);

        final DatasetAddress datasetAddress = new DatasetAddress(1, 0);
        createSubscription(datasetAddress, ValueUtil.randomString(), SubscriptionMode.EXPLICIT);
        final SubscriptionChangeMessage subscriptionChange =
                SubscriptionChangeMessage.create("+0", ValueUtil.randomString());
        final MessageResponse response = setCurrentMessageResponse(
                connection,
                ChangeSetMessage.create(
                        null, null, null, new SubscriptionChangeMessage[] {subscriptionChange}, null, null));
        response.setParsedSubscriptionChanges(
                Collections.singletonList(SubscriptionChange.from(1, subscriptionChange)));

        final IllegalStateException exception =
                expectThrows(IllegalStateException.class, connector::processSubscriptionChanges);

        assertEquals(
                exception.getMessage(),
                "Replicant-0064: createSubscription invoked with Dataset Address 1.0 but a subscription with that"
                        + " Dataset Address already exists.");
    }

    @Test
    public void processSubscriptionChanges_subscribe_withCorrespondingAreaOfInterest() {
        final Connector connector = createConnector();
        final Connection connection = newConnection(connector);

        final int datasetId = 0;
        final int datasetRootId = ValueUtil.randomInt();

        final DatasetAddress datasetAddress = new DatasetAddress(1, datasetId, datasetRootId);

        safeAction(() -> Replicant.context().createOrUpdateAreaOfInterest(datasetAddress, null));

        final String[] subscriptionChanges = {"+0." + datasetRootId};
        final MessageResponse response = setCurrentMessageResponse(
                connection, ChangeSetMessage.create(null, null, subscriptionChanges, null, null, null));
        response.setParsedSubscriptionChanges(
                Collections.singletonList(SubscriptionChange.from(1, subscriptionChanges[0])));

        assertTrue(response.needsSubscriptionChangesProcessed());

        final TestSpyEventHandler handler = registerTestSpyEventHandler();

        connector.processSubscriptionChanges();

        assertFalse(response.needsSubscriptionChangesProcessed());

        final Subscription subscription =
                Objects.requireNonNull(Replicant.context().findSubscription(datasetAddress));
        assertEquals(subscription.datasetAddress(), datasetAddress);
        safeAction(() -> assertNull(subscription.getFilterParameter()));
        safeAction(() -> assertEquals(subscription.getMode(), SubscriptionMode.EXPLICIT));

        handler.assertEventCount(1);
        handler.assertNextEvent(SubscriptionCreatedEvent.class, e -> {
            assertEquals(e.getSubscription().datasetAddress(), datasetAddress);
            safeAction(() -> assertNull(e.getSubscription().getFilterParameter()));
        });
    }

    @Test
    public void processSubscriptionChanges_subscribeWithAreaOfInterestUsesExplicitMode() {
        final Connector connector = createConnector();
        final Connection connection = newConnection(connector);

        connector.pauseMessageScheduler();

        final DatasetAddress datasetAddress = new DatasetAddress(1, 0, ValueUtil.randomInt());

        safeAction(() -> Replicant.context().createOrUpdateAreaOfInterest(datasetAddress, null));

        final String[] subscriptionChanges = {"+0." + datasetAddress.datasetRootId()};
        final MessageResponse response = setCurrentMessageResponse(
                connection, ChangeSetMessage.create(null, null, subscriptionChanges, null, null, null));
        response.setParsedSubscriptionChanges(
                Collections.singletonList(SubscriptionChange.from(1, subscriptionChanges[0])));

        final SubscriptionOperation request =
                new SubscriptionOperation(datasetAddress, SubscriptionOperation.Type.SUBSCRIBE, null);
        connection.injectCurrentSubscriptionOperation(request);
        request.markAsInProgress(newRequest(connection).getRequestId());

        assertTrue(response.needsSubscriptionChangesProcessed());
        assertEquals(response.getSubscriptionSubscribeCount(), 0);

        final TestSpyEventHandler handler = registerTestSpyEventHandler();

        connector.processSubscriptionChanges();

        assertFalse(response.needsSubscriptionChangesProcessed());
        assertEquals(response.getSubscriptionSubscribeCount(), 1);

        final Subscription subscription =
                Objects.requireNonNull(Replicant.context().findSubscription(datasetAddress));
        assertEquals(subscription.datasetAddress(), datasetAddress);
        safeAction(() -> assertNull(subscription.getFilterParameter()));
        safeAction(() -> assertEquals(subscription.getMode(), SubscriptionMode.EXPLICIT));

        handler.assertEventCount(1);
        handler.assertNextEvent(SubscriptionCreatedEvent.class, e -> {
            assertEquals(e.getSubscription().datasetAddress(), datasetAddress);
            safeAction(() -> assertNull(e.getSubscription().getFilterParameter()));
        });
    }

    @Test
    public void processSubscriptionChanges_unsubscribe() {
        final Connector connector = createConnector();
        connector.pauseMessageScheduler();

        final Connection connection = newConnection(connector);

        final DatasetAddress datasetAddress = new DatasetAddress(1, 0, ValueUtil.randomInt());

        final String[] subscriptionChanges = {"-0." + datasetAddress.datasetRootId()};
        final MessageResponse response = setCurrentMessageResponse(
                connection, ChangeSetMessage.create(null, null, subscriptionChanges, null, null, null));
        response.setParsedSubscriptionChanges(
                Collections.singletonList(SubscriptionChange.from(1, subscriptionChanges[0])));

        final Subscription initialSubscription =
                createSubscription(datasetAddress, ValueUtil.randomString(), SubscriptionMode.EXPLICIT);

        assertTrue(response.needsSubscriptionChangesProcessed());
        assertEquals(response.getSubscriptionUnsubscribeCount(), 0);

        final TestSpyEventHandler handler = registerTestSpyEventHandler();

        connector.processSubscriptionChanges();

        assertFalse(response.needsSubscriptionChangesProcessed());
        assertEquals(response.getSubscriptionUnsubscribeCount(), 1);

        final Subscription subscription = Replicant.context().findSubscription(datasetAddress);
        assertNull(subscription);
        assertTrue(Disposable.isDisposed(initialSubscription));

        handler.assertEventCount(1);
        handler.assertNextEvent(
                SubscriptionDisposedEvent.class,
                e -> assertEquals(e.getSubscription().datasetAddress(), datasetAddress));
    }

    @Test
    public void processSubscriptionChanges_unsubscribe_withAreaOfInterest() {
        final Connector connector = createConnector();
        connector.pauseMessageScheduler();

        final Connection connection = newConnection(connector);

        final DatasetAddress datasetAddress = new DatasetAddress(1, 0, ValueUtil.randomInt());

        final AreaOfInterest areaOfInterest =
                safeAction(() -> Replicant.context().createOrUpdateAreaOfInterest(datasetAddress, null));

        final String[] subscriptionChanges = {"-0." + datasetAddress.datasetRootId()};
        final MessageResponse response = setCurrentMessageResponse(
                connection, ChangeSetMessage.create(null, null, subscriptionChanges, null, null, null));
        response.setParsedSubscriptionChanges(
                Collections.singletonList(SubscriptionChange.from(1, subscriptionChanges[0])));
        final Subscription initialSubscription =
                createSubscription(datasetAddress, ValueUtil.randomString(), SubscriptionMode.EXPLICIT);

        assertTrue(response.needsSubscriptionChangesProcessed());
        assertEquals(response.getSubscriptionUnsubscribeCount(), 0);

        final TestSpyEventHandler handler = registerTestSpyEventHandler();

        connector.processSubscriptionChanges();

        assertFalse(response.needsSubscriptionChangesProcessed());
        assertEquals(response.getSubscriptionUnsubscribeCount(), 1);

        final Subscription subscription = Replicant.context().findSubscription(datasetAddress);
        assertNull(subscription);
        assertTrue(Disposable.isDisposed(initialSubscription));

        assertFalse(Disposable.isDisposed(areaOfInterest));
        assertEquals(areaOfInterest.getStatus(), AreaOfInterest.Status.PENDING);
        assertFalse(areaOfInterest.isDataAvailable());

        handler.assertEventCount(1);
        handler.assertNextEvent(
                SubscriptionDisposedEvent.class,
                e -> assertEquals(e.getSubscription().datasetAddress(), datasetAddress));
    }

    @Test
    public void processSubscriptionChanges_unsubscribe_WithMissingSubscription() {
        final Connector connector = createConnector();
        final Connection connection = newConnection(connector);

        final String[] subscriptionChanges = {"-0.72"};
        final MessageResponse response = setCurrentMessageResponse(
                connection, ChangeSetMessage.create(null, null, subscriptionChanges, null, null, null));
        response.setParsedSubscriptionChanges(
                Collections.singletonList(SubscriptionChange.from(1, subscriptionChanges[0])));
        assertTrue(response.needsSubscriptionChangesProcessed());
        assertEquals(response.getSubscriptionUnsubscribeCount(), 0);

        final TestSpyEventHandler handler = registerTestSpyEventHandler();

        connector.processSubscriptionChanges();

        assertFalse(response.needsSubscriptionChangesProcessed());
        assertEquals(response.getSubscriptionUnsubscribeCount(), 1);

        handler.assertEventCount(0);
    }

    @Test
    public void processSubscriptionChanges_unsubscribe_WithMissingSubscription_butAreaOfInterestPresent() {
        final Connector connector = createConnector();
        final Connection connection = newConnection(connector);

        final DatasetAddress datasetAddress = new DatasetAddress(1, 0, ValueUtil.randomInt());

        final AreaOfInterest areaOfInterest =
                safeAction(() -> Replicant.context().createOrUpdateAreaOfInterest(datasetAddress, null));

        final String[] subscriptionChanges = {"-0." + datasetAddress.datasetRootId()};
        final MessageResponse response = setCurrentMessageResponse(
                connection, ChangeSetMessage.create(null, null, subscriptionChanges, null, null, null));
        response.setParsedSubscriptionChanges(
                Collections.singletonList(SubscriptionChange.from(1, subscriptionChanges[0])));
        assertTrue(response.needsSubscriptionChangesProcessed());
        assertEquals(response.getSubscriptionUnsubscribeCount(), 0);

        final TestSpyEventHandler handler = registerTestSpyEventHandler();

        connector.processSubscriptionChanges();

        assertFalse(response.needsSubscriptionChangesProcessed());
        assertEquals(response.getSubscriptionUnsubscribeCount(), 1);

        assertFalse(Disposable.isDisposed(areaOfInterest));
        assertEquals(areaOfInterest.getStatus(), AreaOfInterest.Status.PENDING);
        assertFalse(areaOfInterest.isDataAvailable());

        handler.assertEventCount(0);
    }

    @Test
    public void processSubscriptionChanges_invalidateDatasetAddress_withMissingSubscription_butAreaOfInterestPresent() {
        final Connector connector = createConnector();
        final Connection connection = newConnection(connector);

        final DatasetAddress datasetAddress = new DatasetAddress(1, 0, ValueUtil.randomInt());

        final AreaOfInterest areaOfInterest =
                safeAction(() -> Replicant.context().createOrUpdateAreaOfInterest(datasetAddress, null));

        final String[] subscriptionChanges = {"!0." + datasetAddress.datasetRootId()};
        final MessageResponse response = setCurrentMessageResponse(
                connection, ChangeSetMessage.create(null, null, subscriptionChanges, null, null, null));
        response.setParsedSubscriptionChanges(
                Collections.singletonList(SubscriptionChange.from(1, subscriptionChanges[0])));
        assertTrue(response.needsSubscriptionChangesProcessed());
        assertEquals(response.getSubscriptionUnsubscribeCount(), 0);

        final TestSpyEventHandler handler = registerTestSpyEventHandler();

        connector.processSubscriptionChanges();

        assertFalse(response.needsSubscriptionChangesProcessed());
        assertEquals(response.getSubscriptionUnsubscribeCount(), 1);

        assertFalse(Disposable.isDisposed(areaOfInterest));
        assertEquals(areaOfInterest.getStatus(), AreaOfInterest.Status.INVALIDATED);

        handler.assertEventCount(0);
    }

    @Test
    public void processSubscriptionChanges_update() {
        final FilterParameterUpdateReplicaMatcher<?> filterParameter = mock(FilterParameterUpdateReplicaMatcher.class);
        final Dataset dataset = new Dataset(
                0,
                ValueUtil.randomString(),
                null,
                Dataset.FilterMode.PARAMETER_FILTERED,
                Dataset.FilterParameterMode.UPDATABLE,
                false,
                filterParameter,
                true,
                true,
                Collections.emptyList());
        final EntityType entityType =
                new EntityType(0, ValueUtil.randomString(), String.class, (i, d) -> "", null, new DatasetLink[0]);
        final SystemSchema systemSchema =
                new SystemSchema(1, ValueUtil.randomString(), new Dataset[] {dataset}, new EntityType[] {entityType});

        final Connector connector = createConnector(systemSchema);
        connector.pauseMessageScheduler();

        final Connection connection = newConnection(connector);

        final DatasetAddress datasetAddress = new DatasetAddress(1, 0, ValueUtil.randomInt());

        final String oldFilterParameter = ValueUtil.randomString();
        final String newFilterParameter = ValueUtil.randomString();
        final SubscriptionChangeMessage[] subscriptionChanges = new SubscriptionChangeMessage[] {
            SubscriptionChangeMessage.create("=0." + datasetAddress.datasetRootId(), newFilterParameter)
        };
        final MessageResponse response = setCurrentMessageResponse(
                connection, ChangeSetMessage.create(null, null, null, subscriptionChanges, null, null));
        response.setParsedSubscriptionChanges(
                Collections.singletonList(SubscriptionChange.from(1, subscriptionChanges[0])));

        final Subscription initialSubscription =
                createSubscription(datasetAddress, oldFilterParameter, SubscriptionMode.EXPLICIT);

        assertTrue(response.needsSubscriptionChangesProcessed());
        assertEquals(response.getSubscriptionUpdateCount(), 0);

        final TestSpyEventHandler handler = registerTestSpyEventHandler();

        connector.processSubscriptionChanges();

        assertFalse(response.needsSubscriptionChangesProcessed());
        assertEquals(response.getSubscriptionUpdateCount(), 1);

        final Subscription subscription = Replicant.context().findSubscription(datasetAddress);
        assertNotNull(subscription);
        assertFalse(Disposable.isDisposed(initialSubscription));

        handler.assertEventCount(0);
    }

    @Test
    public void processSubscriptionChanges_update_withDatasetKey() {
        final FilterParameterUpdateReplicaMatcher<?> filterParameter = mock(FilterParameterUpdateReplicaMatcher.class);
        final Dataset dataset = new Dataset(
                0,
                ValueUtil.randomString(),
                Integer.class,
                Dataset.FilterMode.PARAMETER_FILTERED,
                Dataset.FilterParameterMode.UPDATABLE,
                true,
                filterParameter,
                true,
                true,
                Collections.emptyList());
        final SystemSchema systemSchema =
                new SystemSchema(1, ValueUtil.randomString(), new Dataset[] {dataset}, new EntityType[0]);

        final Connector connector = createConnector(systemSchema);
        connector.pauseMessageScheduler();

        final Connection connection = newConnection(connector);

        final int datasetRootId = ValueUtil.randomInt();
        final DatasetAddress datasetAddress = new DatasetAddress(1, 0, datasetRootId, "fi");

        final String oldFilterParameter = ValueUtil.randomString();
        final String newFilterParameter = ValueUtil.randomString();
        final SubscriptionChangeMessage[] subscriptionChanges = new SubscriptionChangeMessage[] {
            SubscriptionChangeMessage.create("=0." + datasetRootId + "#fi", newFilterParameter)
        };
        final MessageResponse response = setCurrentMessageResponse(
                connection, ChangeSetMessage.create(null, null, null, subscriptionChanges, null, null));
        response.setParsedSubscriptionChanges(
                Collections.singletonList(SubscriptionChange.from(1, subscriptionChanges[0])));

        final Subscription subscription =
                createSubscription(datasetAddress, oldFilterParameter, SubscriptionMode.EXPLICIT);

        assertTrue(response.needsSubscriptionChangesProcessed());
        assertEquals(response.getSubscriptionUpdateCount(), 0);

        connector.processSubscriptionChanges();

        assertFalse(response.needsSubscriptionChangesProcessed());
        assertEquals(response.getSubscriptionUpdateCount(), 1);
        safeAction(() -> assertEquals(subscription.getFilterParameter(), newFilterParameter));
    }

    @Test
    public void processSubscriptionChanges_update_rejectsDatasetWithoutUpdatableFilterParameter() {
        final Dataset dataset = new Dataset(
                0,
                ValueUtil.randomString(),
                null,
                Dataset.FilterMode.UNFILTERED,
                null,
                false,
                null,
                true,
                true,
                Collections.emptyList());
        final SystemSchema systemSchema =
                new SystemSchema(1, ValueUtil.randomString(), new Dataset[] {dataset}, new EntityType[0]);
        final Connector connector = createConnector(systemSchema);
        final Connection connection = newConnection(connector);

        final String oldFilterParameter = ValueUtil.randomString();
        final String newFilterParameter = ValueUtil.randomString();
        final SubscriptionChangeMessage[] subscriptionChanges =
                new SubscriptionChangeMessage[] {SubscriptionChangeMessage.create("=0.2223", newFilterParameter)};
        final MessageResponse response = setCurrentMessageResponse(
                connection, ChangeSetMessage.create(null, null, null, subscriptionChanges, null, null));
        response.setParsedSubscriptionChanges(
                Collections.singletonList(SubscriptionChange.from(1, subscriptionChanges[0])));
        createSubscription(new DatasetAddress(1, 0, 2223), oldFilterParameter, SubscriptionMode.EXPLICIT);

        final IllegalStateException exception =
                expectThrows(IllegalStateException.class, connector::processSubscriptionChanges);

        assertEquals(
                exception.getMessage(),
                "Replicant-0078: Received SubscriptionChange of type UPDATE for Dataset Address 1.0.2223 but the"
                        + " Dataset does not have an updatable Filter Parameter.");
    }

    @Test
    public void processSubscriptionChanges_update_missingSubscription() {
        final Connector connector = createConnector();
        final Connection connection = newConnection(connector);

        final String newFilterParameter = ValueUtil.randomString();
        final SubscriptionChangeMessage[] subscriptionChanges =
                new SubscriptionChangeMessage[] {SubscriptionChangeMessage.create("=0.42", newFilterParameter)};
        final MessageResponse response = setCurrentMessageResponse(
                connection, ChangeSetMessage.create(null, null, null, subscriptionChanges, null, null));
        response.setParsedSubscriptionChanges(
                Collections.singletonList(SubscriptionChange.from(1, subscriptionChanges[0])));
        assertTrue(response.needsSubscriptionChangesProcessed());
        assertEquals(response.getSubscriptionUpdateCount(), 0);

        final TestSpyEventHandler handler = registerTestSpyEventHandler();

        final IllegalStateException exception =
                expectThrows(IllegalStateException.class, connector::processSubscriptionChanges);

        assertTrue(response.needsSubscriptionChangesProcessed());
        assertEquals(response.getSubscriptionUpdateCount(), 0);

        assertEquals(
                exception.getMessage(),
                "Replicant-0033: Received SubscriptionChange of type UPDATE for Dataset Address 1.0.42 but no such"
                        + " subscription exists.");

        handler.assertEventCount(0);
    }

    @Test
    public void transitionSubscriptionsToImplicitMode_preservesSubscriptionState() {
        // Pause the SubscriptionReconciler
        pauseScheduler();

        final Connector connector = createConnector();

        final DatasetAddress datasetAddress1 = new DatasetAddress(1, 1, 1);
        final DatasetAddress datasetAddress2 = new DatasetAddress(1, 1, 2);
        final DatasetAddress datasetAddress3 = new DatasetAddress(1, 1, 3);

        final ArrayList<SubscriptionOperation> requests = new ArrayList<>();
        requests.add(new SubscriptionOperation(datasetAddress1, SubscriptionOperation.Type.UNSUBSCRIBE, null));
        requests.add(new SubscriptionOperation(datasetAddress2, SubscriptionOperation.Type.UNSUBSCRIBE, null));
        requests.add(new SubscriptionOperation(datasetAddress3, SubscriptionOperation.Type.UNSUBSCRIBE, null));

        final String filterParameter = ValueUtil.randomString();
        final Subscription subscription1 =
                createSubscription(datasetAddress1, filterParameter, SubscriptionMode.EXPLICIT);
        final ReplicaEntry replicaEntry = findOrCreateReplicaEntry(String.class, ValueUtil.randomInt());
        safeAction(() -> replicaEntry.linkToSubscription(subscription1));
        // Address2 is already implicit ...
        createSubscription(datasetAddress2, null, SubscriptionMode.IMPLICIT);
        // Address3 has no subscription ... maybe not reconciled yet

        connector.transitionSubscriptionsToImplicitMode(requests);

        safeAction(() -> {
            assertSame(Replicant.context().findSubscription(datasetAddress1), subscription1);
            assertEquals(subscription1.getMode(), SubscriptionMode.IMPLICIT);
            assertEquals(subscription1.getFilterParameter(), filterParameter);
            assertSame(subscription1.findReplicaEntryByTypeAndId(String.class, replicaEntry.getId()), replicaEntry);
        });
    }

    @Test
    public void transitionSubscriptionsToImplicitMode_passedBadAction() {
        // Pause the SubscriptionReconciler
        pauseScheduler();

        final Connector connector = createConnector();

        final DatasetAddress datasetAddress1 = new DatasetAddress(1, 1, 1);

        final ArrayList<SubscriptionOperation> requests = new ArrayList<>();
        requests.add(new SubscriptionOperation(datasetAddress1, SubscriptionOperation.Type.SUBSCRIBE, null));

        createSubscription(datasetAddress1, null, SubscriptionMode.EXPLICIT);

        final IllegalStateException exception = expectThrows(
                IllegalStateException.class,
                () -> safeAction(() -> connector.transitionSubscriptionsToImplicitMode(requests)));
        assertEquals(
                exception.getMessage(),
                "Replicant-0034: Connector.transitionSubscriptionsToImplicitMode() invoked with Subscription Operation"
                        + " with type that is not UNSUBSCRIBE. Operation: "
                        + "SubscriptionOperation[Type=SUBSCRIBE Address=1.1.1]");
    }

    @Test
    public void removeUnneededRemoveRequests_whenInvariantsDisabled() {
        final DatasetAddress datasetAddress1 = new DatasetAddress(1, 1, 1);
        final DatasetAddress datasetAddress2 = new DatasetAddress(1, 1, 2);
        final DatasetAddress datasetAddress3 = new DatasetAddress(1, 1, 3);

        final ArrayList<SubscriptionOperation> requests = new ArrayList<>();
        final SubscriptionOperation request1 =
                new SubscriptionOperation(datasetAddress1, SubscriptionOperation.Type.UNSUBSCRIBE, null);
        final SubscriptionOperation request2 =
                new SubscriptionOperation(datasetAddress2, SubscriptionOperation.Type.UNSUBSCRIBE, null);
        final SubscriptionOperation request3 =
                new SubscriptionOperation(datasetAddress3, SubscriptionOperation.Type.UNSUBSCRIBE, null);
        requests.add(request1);
        requests.add(request2);
        requests.add(request3);

        final Connection connection = createConnection();
        final Connector connector = connection.getConnector();

        final RequestEntry request = newRequest(connection);

        requests.forEach(r -> r.markAsInProgress(request.getRequestId()));

        createSubscription(datasetAddress1, null, SubscriptionMode.EXPLICIT);
        // Address2 is already implicit ...
        createSubscription(datasetAddress2, null, SubscriptionMode.IMPLICIT);
        // Address3 has no subscription ... maybe not reconciled yet

        ReplicantTestUtil.noCheckInvariants();

        connector.removeUnneededRemoveRequests(requests);

        assertEquals(requests.size(), 1);
        assertTrue(requests.contains(request1));
        assertTrue(request1.isInProgress());
        assertFalse(request2.isInProgress());
        assertFalse(request3.isInProgress());
    }

    @Test
    public void removeUnneededRemoveRequests_noSubscription() {
        final Connector connector = createConnector();

        final DatasetAddress datasetAddress1 = new DatasetAddress(1, 1, 1);

        final ArrayList<SubscriptionOperation> requests = new ArrayList<>();
        final SubscriptionOperation request1 =
                new SubscriptionOperation(datasetAddress1, SubscriptionOperation.Type.UNSUBSCRIBE, null);
        requests.add(request1);

        final int requestId = newRequest(newConnection(connector)).getRequestId();
        requests.forEach(r -> r.markAsInProgress(requestId));

        final IllegalStateException exception = expectThrows(
                IllegalStateException.class, () -> safeAction(() -> connector.removeUnneededRemoveRequests(requests)));

        assertEquals(
                exception.getMessage(),
                "Replicant-0046: Request to unsubscribe at Dataset Address 1.1.1 but no Subscription exists.");
    }

    @Test
    public void removeUnneededRemoveRequests_implicitSubscriptionMode() {
        final Connector connector = createConnector();

        final DatasetAddress datasetAddress1 = new DatasetAddress(1, 1, 1);

        final ArrayList<SubscriptionOperation> requests = new ArrayList<>();
        final SubscriptionOperation request1 =
                new SubscriptionOperation(datasetAddress1, SubscriptionOperation.Type.UNSUBSCRIBE, null);
        requests.add(request1);

        final int requestId = newRequest(newConnection(connector)).getRequestId();
        requests.forEach(r -> r.markAsInProgress(requestId));

        createSubscription(datasetAddress1, null, SubscriptionMode.IMPLICIT);

        final IllegalStateException exception = expectThrows(
                IllegalStateException.class, () -> safeAction(() -> connector.removeUnneededRemoveRequests(requests)));

        assertEquals(
                exception.getMessage(),
                "Replicant-0047: Request to unsubscribe at Dataset Address 1.1.1 but Subscription is not in Explicit"
                        + " Subscription Mode.");
    }

    @Test
    public void removeUnneededUpdateRequests_whenInvariantsDisabled() {
        final Connector connector = createConnector();

        final DatasetAddress datasetAddress1 = new DatasetAddress(1, 1, 1);
        final DatasetAddress datasetAddress2 = new DatasetAddress(1, 1, 2);
        final DatasetAddress datasetAddress3 = new DatasetAddress(1, 1, 3);

        final ArrayList<SubscriptionOperation> requests = new ArrayList<>();
        final SubscriptionOperation request1 =
                new SubscriptionOperation(datasetAddress1, SubscriptionOperation.Type.UPDATE, null);
        final SubscriptionOperation request2 =
                new SubscriptionOperation(datasetAddress2, SubscriptionOperation.Type.UPDATE, null);
        final SubscriptionOperation request3 =
                new SubscriptionOperation(datasetAddress3, SubscriptionOperation.Type.UPDATE, null);
        requests.add(request1);
        requests.add(request2);
        requests.add(request3);

        final int requestId = newRequest(newConnection(connector)).getRequestId();
        requests.forEach(r -> r.markAsInProgress(requestId));

        createSubscription(datasetAddress1, null, SubscriptionMode.EXPLICIT);
        // Address2 is already implicit ...
        createSubscription(datasetAddress2, null, SubscriptionMode.IMPLICIT);
        // Address3 has no subscription ... maybe not reconciled yet

        ReplicantTestUtil.noCheckInvariants();

        connector.removeUnneededUpdateRequests(requests);

        assertEquals(requests.size(), 2);
        assertTrue(requests.contains(request1));
        assertTrue(requests.contains(request2));
        assertTrue(request1.isInProgress());
        assertTrue(request2.isInProgress());
        assertFalse(request3.isInProgress());
    }

    @Test
    public void removeUnneededUpdateRequests_noSubscription() {
        final Connector connector = createConnector();

        final DatasetAddress datasetAddress1 = new DatasetAddress(1, 1, 1);

        final ArrayList<SubscriptionOperation> requests = new ArrayList<>();
        final SubscriptionOperation request1 =
                new SubscriptionOperation(datasetAddress1, SubscriptionOperation.Type.UPDATE, null);
        requests.add(request1);

        final int requestId = newRequest(newConnection(connector)).getRequestId();
        requests.forEach(r -> r.markAsInProgress(requestId));

        final IllegalStateException exception = expectThrows(
                IllegalStateException.class, () -> safeAction(() -> connector.removeUnneededUpdateRequests(requests)));

        assertEquals(
                exception.getMessage(),
                "Replicant-0048: Request to update Subscription at Dataset Address 1.1.1 but no Subscription exists.");
    }

    @Test
    public void validateWorld_invalidEntity() {
        final Connector connector = createConnector();
        newConnection(connector);
        final MessageResponse response = setCurrentMessageResponse(
                connector.ensureConnection(), ChangeSetMessage.create(null, null, null, null, null, null));

        assertFalse(response.hasWorldBeenValidated());

        final ReplicaRegistry replicaRegistry = Replicant.context().getReplicaRegistry();
        final ReplicaEntry replicaEntry1 =
                safeAction(() -> replicaRegistry.findOrCreateReplicaEntry("MyEntity/1", MyEntity.class, 1));
        final Exception error = new Exception();
        safeAction(() -> replicaEntry1.setReplica(new MyEntity(error)));

        final IllegalStateException exception = expectThrows(IllegalStateException.class, connector::validateWorld);

        assertEquals(
                exception.getMessage(),
                "Replicant-0065: Replica failed to verify during validation process. Replica Entry = MyEntity/1");

        assertTrue(response.hasWorldBeenValidated());
    }

    @Test
    public void validateWorld_invalidEntity_ignoredIfCompileSettingDisablesValidation() {
        ReplicantTestUtil.noValidateReplicasOnLoad();
        final Connector connector = createConnector();
        newConnection(connector);
        final MessageResponse response = setCurrentMessageResponse(
                connector.ensureConnection(), ChangeSetMessage.create(null, null, null, null, null, null));

        assertTrue(response.hasWorldBeenValidated());

        final ReplicaRegistry replicaRegistry = Replicant.context().getReplicaRegistry();
        final ReplicaEntry replicaEntry1 =
                safeAction(() -> replicaRegistry.findOrCreateReplicaEntry("MyEntity/1", MyEntity.class, 1));
        final Exception error = new Exception();
        safeAction(() -> replicaEntry1.setReplica(new MyEntity(error)));

        connector.validateWorld();

        assertTrue(response.hasWorldBeenValidated());
    }

    @Test
    public void validateWorld_validEntity() {
        final Connector connector = createConnector();
        newConnection(connector);
        final MessageResponse response = setCurrentMessageResponse(
                connector.ensureConnection(), ChangeSetMessage.create(null, null, null, null, null, null));

        assertFalse(response.hasWorldBeenValidated());

        final ReplicaRegistry replicaRegistry = Replicant.context().getReplicaRegistry();
        safeAction(() -> replicaRegistry.findOrCreateReplicaEntry("MyEntity/1", MyEntity.class, 1));

        connector.validateWorld();

        assertTrue(response.hasWorldBeenValidated());
    }

    static class MyEntity implements Verifiable {
        @Nullable
        private final Exception _exception;

        MyEntity() {
            this(null);
        }

        MyEntity(@Nullable final Exception exception) {
            _exception = exception;
        }

        @Override
        public void verify() throws Exception {
            if (null != _exception) {
                throw _exception;
            }
        }
    }

    @Test
    public void completeMessageResponse() {
        final Connector connector = createConnector();
        final Connection connection = newConnection(connector);

        setCurrentMessageResponse(connection, ChangeSetMessage.create(null, null, null, null, null, null));

        final TestSpyEventHandler handler = registerTestSpyEventHandler();

        connector.completeMessageResponse();

        assertNull(connection.getCurrentMessageResponse());

        handler.assertEventCount(1);
        handler.assertNextEvent(MessageProcessedEvent.class, e -> {
            assertEquals(e.getSystemSchemaId(), connector.getSystemSchema().getId());
            assertEquals(e.getSystemSchemaName(), connector.getSystemSchema().getName());
        });
    }

    @Test
    public void completeMessageResponse_hasContent() {
        final Connector connector = createConnector();
        final Connection connection = newConnection(connector);
        safeAction(() -> connector.setState(ConnectorState.CONNECTED));

        final RequestEntry request = newRequest(connection);
        final ChangeSetMessage changeSet =
                ChangeSetMessage.create(request.getRequestId(), null, new String[] {"+1"}, null, null, null);

        setCurrentMessageResponse(connection, changeSet, request);

        final TestSpyEventHandler handler = registerTestSpyEventHandler();

        connector.completeMessageResponse();

        assertNull(connection.getCurrentMessageResponse());

        handler.assertEventCount(2);
        handler.assertNextEvent(MessageProcessedEvent.class, e -> {
            assertEquals(e.getSystemSchemaId(), connector.getSystemSchema().getId());
            assertEquals(e.getSystemSchemaName(), connector.getSystemSchema().getName());
        });
        handler.assertNextEvent(
                SyncRequestEvent.class,
                e -> assertEquals(
                        e.getSystemSchemaId(), connector.getSystemSchema().getId()));
    }

    @Test
    public void completeMessageResponse_datasetCacheEntryStoreFailureDoesNotRemoveSubscription() {
        final Dataset dataset = new Dataset(
                0,
                ValueUtil.randomString(),
                null,
                Dataset.FilterMode.UNFILTERED,
                null,
                false,
                null,
                true,
                true,
                Collections.emptyList());
        final SystemSchema systemSchema =
                new SystemSchema(1, ValueUtil.randomString(), new Dataset[] {dataset}, new EntityType[0]);
        final Connector connector = createConnector(systemSchema);
        final Connection connection = newConnection(connector);
        final DatasetAddress datasetAddress = new DatasetAddress(systemSchema.getId(), dataset.getId());
        final String datasetCacheVersion = ValueUtil.randomString();
        final String[] subscriptionChanges = {"+0"};
        final ChangeSetMessage changeSet =
                ChangeSetMessage.create(null, datasetCacheVersion, subscriptionChanges, null, null, null);
        final MessageResponse response = setCurrentMessageResponse(connection, changeSet);
        response.setParsedSubscriptionChanges(
                Collections.singletonList(SubscriptionChange.from(systemSchema.getId(), subscriptionChanges[0])));
        final DatasetCacheService datasetCacheService = mock(DatasetCacheService.class);
        Replicant.context().setDatasetCacheService(datasetCacheService);
        when(datasetCacheService.storeDatasetCacheEntry(datasetAddress, datasetCacheVersion, changeSet))
                .thenThrow(new IllegalStateException("Unavailable"));

        connector.processSubscriptionChanges();
        final Subscription subscription = Replicant.context().findSubscription(datasetAddress);
        assertNotNull(subscription);

        connector.completeMessageResponse();

        assertSame(Replicant.context().findSubscription(datasetAddress), subscription);
        verify(datasetCacheService).storeDatasetCacheEntry(datasetAddress, datasetCacheVersion, changeSet);
    }

    @Test
    public void completeMessageResponse_stillMessagesPending() {
        final Connector connector = createConnector();
        final Connection connection = newConnection(connector);

        setCurrentMessageResponse(connection, ChangeSetMessage.create(null, null, null, null, null, null));

        connection.enqueueResponse(ChangeSetMessage.create(null, null, null, null, null, null), null);

        connector.completeMessageResponse();

        assertFalse(connector.ensureConnection().getPendingResponses().isEmpty());
    }

    @Test
    public void completeMessageResponse_withPostAction() {
        final Connector connector = createConnector();
        final Connection connection = newConnection(connector);

        setCurrentMessageResponse(connection, ChangeSetMessage.create(null, null, null, null, null, null));

        final AtomicInteger postActionCallCount = new AtomicInteger();
        connector.setPostMessageResponseAction(postActionCallCount::incrementAndGet);

        assertEquals(postActionCallCount.get(), 0);

        connector.completeMessageResponse();

        assertEquals(postActionCallCount.get(), 1);
    }

    @Test
    public void completeMessageResponse_MessageWithRequest_RPCComplete() {
        final Connector connector = createConnector();
        final Connection connection = newConnection(connector);

        final RequestEntry request = newRequest(connection);

        final int requestId = request.getRequestId();

        setCurrentMessageResponse(connection, OkMessage.create(requestId), request);

        final TestSpyEventHandler handler = registerTestSpyEventHandler();

        assertEquals(connection.getRequest(requestId), request);

        connector.completeMessageResponse();

        assertNull(connection.getCurrentMessageResponse());
        assertNull(connection.getRequests().get(requestId));

        handler.assertEventCount(1);
        handler.assertNextEvent(MessageProcessedEvent.class, e -> {
            assertEquals(e.getSystemSchemaId(), connector.getSystemSchema().getId());
            assertEquals(e.getSystemSchemaName(), connector.getSystemSchema().getName());
        });
    }

    @SuppressWarnings({"unchecked"})
    @Test
    public void progressResponseProcessing() {
        /*
         * This test steps through each stage of a message processing.
         */

        final Dataset dataset = new Dataset(
                0,
                ValueUtil.randomString(),
                null,
                Dataset.FilterMode.UNFILTERED,
                null,
                false,
                null,
                false,
                true,
                Collections.emptyList());
        final EntityType.Creator<Linkable> creator = mock(EntityType.Creator.class);
        final EntityType.Updater<Linkable> updater = mock(EntityType.Updater.class);
        final EntityType entityType =
                new EntityType(0, ValueUtil.randomString(), Linkable.class, creator, updater, new DatasetLink[0]);
        final SystemSchema systemSchema =
                new SystemSchema(1, ValueUtil.randomString(), new Dataset[] {dataset}, new EntityType[] {entityType});

        final Connector connector = createConnector(systemSchema);
        final Connection connection = newConnection(connector);

        final ChangeSetMessage message = ChangeSetMessage.create(
                null,
                null,
                new String[] {"+0"},
                null,
                new EntityChange[] {EntityChange.create(0, 1, new String[] {"0"}, new EntityChangeDataImpl())},
                null);
        connection.enqueueResponse(message, null);
        assertNull(connection.getCurrentMessageResponse());
        assertEquals(connection.getPendingResponses().size(), 1);

        final MessageResponse response = connection.getPendingResponses().get(0);

        // Pickup parsed response and set it as current
        assertTrue(connector.progressResponseProcessing());

        assertEquals(connection.getCurrentMessageResponse(), response);
        assertEquals(connection.getPendingResponses().size(), 0);

        {
            assertTrue(response.needsSubscriptionChangesProcessed());

            // Process Subscription Changes in response
            assertTrue(connector.progressResponseProcessing());

            assertFalse(response.needsSubscriptionChangesProcessed());
        }

        {
            assertTrue(response.areEntityChangesPending());

            when(creator.createReplica(anyInt(), any(EntityChangeData.class))).thenReturn(mock(Linkable.class));

            // Process ReplicaEntry Changes in response
            assertTrue(connector.progressResponseProcessing());

            assertFalse(response.areEntityChangesPending());
        }

        {
            assertTrue(response.areReplicaLinksPending());

            // Process ReplicaEntry Links in response
            assertTrue(connector.progressResponseProcessing());

            assertFalse(response.areReplicaLinksPending());
        }

        {
            assertTrue(response.areReplicaUpdateActionsPending());

            // EntityUpdateActions processed
            assertTrue(connector.progressResponseProcessing());

            assertFalse(response.areReplicaUpdateActionsPending());
        }

        {
            assertFalse(response.hasWorldBeenValidated());

            // Validate World
            assertTrue(connector.progressResponseProcessing());

            assertTrue(response.hasWorldBeenValidated());
        }

        {
            assertEquals(connection.getCurrentMessageResponse(), response);

            // Complete message
            assertTrue(connector.progressResponseProcessing());

            assertNull(connection.getCurrentMessageResponse());
        }
    }

    @Test
    public void progressAreaOfInterestAddRequest_onSuccess() {
        final Dataset dataset = new Dataset(
                0,
                ValueUtil.randomString(),
                null,
                Dataset.FilterMode.PARAMETER_FILTERED,
                Dataset.FilterParameterMode.FIXED,
                false,
                null,
                false,
                true,
                Collections.emptyList());
        final SystemSchema systemSchema =
                new SystemSchema(1, ValueUtil.randomString(), new Dataset[] {dataset}, new EntityType[] {});

        final Connector connector = createConnector(systemSchema);
        final Connection connection = newConnection(connector);

        final DatasetAddress datasetAddress = new DatasetAddress(1, 0);
        final String filterParameter = ValueUtil.randomString();
        final SubscriptionOperation request =
                new SubscriptionOperation(datasetAddress, SubscriptionOperation.Type.SUBSCRIBE, filterParameter);

        pauseScheduler();

        connection.injectCurrentSubscriptionOperation(request);

        final TestSpyEventHandler handler = registerTestSpyEventHandler();

        final AtomicInteger callCount = new AtomicInteger();
        doAnswer(i -> {
                    callCount.incrementAndGet();
                    assertEquals(i.getArguments()[0], datasetAddress);
                    assertEquals(i.getArguments()[1], filterParameter);
                    return null;
                })
                .when(connector.getTransport())
                .requestSubscribe(eq(datasetAddress), eq(filterParameter));

        assertEquals(callCount.get(), 0);
        assertFalse(connection.getCurrentSubscriptionOperations().isEmpty());

        connector.progressAreaOfInterestAddRequest(request);

        assertEquals(callCount.get(), 1);
        assertFalse(connection.getCurrentSubscriptionOperations().isEmpty());
        assertNull(Replicant.context().findSubscription(datasetAddress));

        handler.assertEventCount(1);
        handler.assertNextEvent(SubscribeStartedEvent.class, e -> {
            assertEquals(e.getSystemSchemaId(), connector.getSystemSchema().getId());
            assertEquals(e.getDatasetAddress(), datasetAddress);
        });
    }

    @Test
    public void progressAreaOfInterestAddRequest_onSuccess_DatasetCacheEntryNotStoredLocally() {
        final Dataset dataset = new Dataset(
                0,
                ValueUtil.randomString(),
                null,
                Dataset.FilterMode.PARAMETER_FILTERED,
                Dataset.FilterParameterMode.FIXED,
                false,
                null,
                true,
                true,
                Collections.emptyList());
        final SystemSchema systemSchema =
                new SystemSchema(1, ValueUtil.randomString(), new Dataset[] {dataset}, new EntityType[] {});

        final Connector connector = createConnector(systemSchema);
        final Connection connection = newConnection(connector);
        pauseScheduler();
        connector.pauseMessageScheduler();

        final DatasetAddress datasetAddress = new DatasetAddress(1, 0);
        final String filterParameter = ValueUtil.randomString();
        final SubscriptionOperation request =
                new SubscriptionOperation(datasetAddress, SubscriptionOperation.Type.SUBSCRIBE, filterParameter);

        connection.injectCurrentSubscriptionOperation(request);

        Replicant.context().setDatasetCacheService(new TestDatasetCacheService());

        final TestSpyEventHandler handler = registerTestSpyEventHandler();

        final AtomicInteger callCount = new AtomicInteger();
        doAnswer(i -> {
                    callCount.incrementAndGet();
                    return null;
                })
                .when(connector.getTransport())
                .requestSubscribe(eq(datasetAddress), eq(filterParameter));

        assertEquals(callCount.get(), 0);
        assertFalse(connection.getCurrentSubscriptionOperations().isEmpty());

        connector.progressAreaOfInterestAddRequest(request);

        assertEquals(callCount.get(), 1);
        assertFalse(connection.getCurrentSubscriptionOperations().isEmpty());
        assertNull(Replicant.context().findSubscription(datasetAddress));

        handler.assertEventCount(1);
        handler.assertNextEvent(SubscribeStartedEvent.class, e -> {
            assertEquals(e.getSystemSchemaId(), connector.getSystemSchema().getId());
            assertEquals(e.getDatasetAddress(), datasetAddress);
        });
    }

    @Test
    public void progressAreaOfInterestAddRequest_onSuccess_DatasetCacheEntryStoredLocally() {
        final Dataset dataset = new Dataset(
                0,
                ValueUtil.randomString(),
                null,
                Dataset.FilterMode.PARAMETER_FILTERED,
                Dataset.FilterParameterMode.FIXED,
                false,
                null,
                true,
                true,
                Collections.emptyList());
        final SystemSchema systemSchema =
                new SystemSchema(1, ValueUtil.randomString(), new Dataset[] {dataset}, new EntityType[] {});

        final Connector connector = createConnector(systemSchema);
        final Connection connection = newConnection(connector);

        final DatasetAddress datasetAddress = new DatasetAddress(1, 0);
        final String filterParameter = ValueUtil.randomString();
        final SubscriptionOperation request =
                new SubscriptionOperation(datasetAddress, SubscriptionOperation.Type.SUBSCRIBE, filterParameter);

        pauseScheduler();
        connector.pauseMessageScheduler();

        connection.injectCurrentSubscriptionOperation(request);

        final TestDatasetCacheService datasetCacheService = new TestDatasetCacheService();
        Replicant.context().setDatasetCacheService(datasetCacheService);

        final TestSpyEventHandler handler = registerTestSpyEventHandler();

        final String datasetCacheVersion = "";
        datasetCacheService.storeDatasetCacheEntry(
                datasetAddress, datasetCacheVersion, ChangeSetMessage.create(null, null, null, null, null, null));
        final AtomicInteger callCount = new AtomicInteger();
        doAnswer(i -> {
                    callCount.incrementAndGet();
                    return null;
                })
                .when(connector.getTransport())
                .requestSubscribe(eq(datasetAddress), eq(filterParameter));

        assertEquals(callCount.get(), 0);
        assertFalse(connection.getCurrentSubscriptionOperations().isEmpty());

        connector.progressAreaOfInterestAddRequest(request);

        assertEquals(callCount.get(), 1);
        assertFalse(connection.getCurrentSubscriptionOperations().isEmpty());
        assertNull(Replicant.context().findSubscription(datasetAddress));

        handler.assertEventCount(1);
        handler.assertNextEvent(SubscribeStartedEvent.class, e -> {
            assertEquals(e.getSystemSchemaId(), connector.getSystemSchema().getId());
            assertEquals(e.getDatasetAddress(), datasetAddress);
        });

        assertFalse(connector.isSchedulerActive());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void progressBulkAreaOfInterestAddRequests_onSuccess() {
        final Dataset dataset = new Dataset(
                0,
                ValueUtil.randomString(),
                String.class,
                Dataset.FilterMode.PARAMETER_FILTERED,
                Dataset.FilterParameterMode.FIXED,
                false,
                null,
                false,
                true,
                Collections.emptyList());
        final SystemSchema systemSchema =
                new SystemSchema(1, ValueUtil.randomString(), new Dataset[] {dataset}, new EntityType[] {});

        final Connector connector = createConnector(systemSchema);
        final Connection connection = newConnection(connector);

        final DatasetAddress datasetAddress1 = new DatasetAddress(1, 0, 1);
        final DatasetAddress datasetAddress2 = new DatasetAddress(1, 0, 2);
        final DatasetAddress datasetAddress3 = new DatasetAddress(1, 0, 3);
        final String filterParameter = ValueUtil.randomString();
        final SubscriptionOperation request1 =
                new SubscriptionOperation(datasetAddress1, SubscriptionOperation.Type.SUBSCRIBE, filterParameter);
        final SubscriptionOperation request2 =
                new SubscriptionOperation(datasetAddress2, SubscriptionOperation.Type.SUBSCRIBE, filterParameter);
        final SubscriptionOperation request3 =
                new SubscriptionOperation(datasetAddress3, SubscriptionOperation.Type.SUBSCRIBE, filterParameter);

        pauseScheduler();

        final Subscription subscription1 = createSubscription(datasetAddress1, null, SubscriptionMode.EXPLICIT);
        final Subscription subscription2 = createSubscription(datasetAddress2, null, SubscriptionMode.EXPLICIT);
        final Subscription subscription3 = createSubscription(datasetAddress3, null, SubscriptionMode.EXPLICIT);

        connection.injectCurrentSubscriptionOperation(request1);
        connection.injectCurrentSubscriptionOperation(request2);
        connection.injectCurrentSubscriptionOperation(request3);

        final TestSpyEventHandler handler = registerTestSpyEventHandler();

        final AtomicInteger callCount = new AtomicInteger();
        doAnswer(i -> {
                    callCount.incrementAndGet();
                    final List<DatasetAddress> datasetAddresses = (List<DatasetAddress>) i.getArguments()[0];
                    assertTrue(datasetAddresses.contains(datasetAddress1));
                    assertTrue(datasetAddresses.contains(datasetAddress2));
                    assertTrue(datasetAddresses.contains(datasetAddress3));
                    return null;
                })
                .when(connector.getTransport())
                .requestBulkSubscribe(any(), eq(filterParameter));

        assertEquals(callCount.get(), 0);
        assertFalse(connection.getCurrentSubscriptionOperations().isEmpty());

        connector.progressBulkAreaOfInterestAddRequests(Arrays.asList(request1, request2, request3));

        assertEquals(callCount.get(), 1);
        assertFalse(connection.getCurrentSubscriptionOperations().isEmpty());
        safeAction(() -> assertEquals(subscription1.getMode(), SubscriptionMode.EXPLICIT));
        safeAction(() -> assertEquals(subscription2.getMode(), SubscriptionMode.EXPLICIT));
        safeAction(() -> assertEquals(subscription3.getMode(), SubscriptionMode.EXPLICIT));

        handler.assertEventCount(3);
        handler.assertNextEvent(SubscribeStartedEvent.class, e -> {
            assertEquals(e.getSystemSchemaId(), connector.getSystemSchema().getId());
            assertEquals(e.getDatasetAddress(), datasetAddress1);
        });
        handler.assertNextEvent(SubscribeStartedEvent.class, e -> {
            assertEquals(e.getSystemSchemaId(), connector.getSystemSchema().getId());
            assertEquals(e.getDatasetAddress(), datasetAddress2);
        });
        handler.assertNextEvent(SubscribeStartedEvent.class, e -> {
            assertEquals(e.getSystemSchemaId(), connector.getSystemSchema().getId());
            assertEquals(e.getDatasetAddress(), datasetAddress3);
        });
    }

    @Test
    public void progressAreaOfInterestAddRequests_onFailure_zeroRequests() {
        final Dataset dataset = new Dataset(
                0,
                ValueUtil.randomString(),
                String.class,
                Dataset.FilterMode.PARAMETER_FILTERED,
                Dataset.FilterParameterMode.FIXED,
                false,
                null,
                false,
                true,
                Collections.emptyList());
        final SystemSchema systemSchema =
                new SystemSchema(1, ValueUtil.randomString(), new Dataset[] {dataset}, new EntityType[] {});

        final Connector connector = createConnector(systemSchema);
        final Connection connection = newConnection(connector);

        final DatasetAddress datasetAddress1 = new DatasetAddress(1, 0, 1);
        final String filterParameter = ValueUtil.randomString();
        final SubscriptionOperation request1 =
                new SubscriptionOperation(datasetAddress1, SubscriptionOperation.Type.SUBSCRIBE, filterParameter);

        pauseScheduler();

        connection.injectCurrentSubscriptionOperation(request1);

        final TestSpyEventHandler handler = registerTestSpyEventHandler();

        assertFalse(connection.getCurrentSubscriptionOperations().isEmpty());

        final ArrayList<SubscriptionOperation> requests = new ArrayList<>();
        // Pass in empty requests list to simulate that they are all filtered out
        connector.progressAreaOfInterestAddRequests(requests);

        assertTrue(connection.getCurrentSubscriptionOperations().isEmpty());

        handler.assertEventCount(0);
    }

    @Test
    public void progressAreaOfInterestUpdateRequest_onSuccess() {
        final Dataset dataset = new Dataset(
                0,
                ValueUtil.randomString(),
                null,
                Dataset.FilterMode.PARAMETER_FILTERED,
                Dataset.FilterParameterMode.UPDATABLE,
                false,
                mock(FilterParameterUpdateReplicaMatcher.class),
                false,
                true,
                Collections.emptyList());
        final SystemSchema systemSchema =
                new SystemSchema(1, ValueUtil.randomString(), new Dataset[] {dataset}, new EntityType[] {});

        final Connector connector = createConnector(systemSchema);
        final Connection connection = newConnection(connector);

        final DatasetAddress datasetAddress = new DatasetAddress(1, 0);
        final String filterParameter = ValueUtil.randomString();
        final SubscriptionOperation request =
                new SubscriptionOperation(datasetAddress, SubscriptionOperation.Type.UPDATE, filterParameter);

        pauseScheduler();

        final Subscription subscription = createSubscription(datasetAddress, null, SubscriptionMode.EXPLICIT);

        connection.injectCurrentSubscriptionOperation(request);

        final TestSpyEventHandler handler = registerTestSpyEventHandler();

        final AtomicInteger callCount = new AtomicInteger();
        doAnswer(i -> {
                    callCount.incrementAndGet();
                    return null;
                })
                .when(connector.getTransport())
                .requestSubscribe(eq(datasetAddress), eq(filterParameter));

        assertEquals(callCount.get(), 0);
        assertFalse(connection.getCurrentSubscriptionOperations().isEmpty());

        connector.progressAreaOfInterestUpdateRequest(request);

        assertEquals(callCount.get(), 1);
        assertFalse(connection.getCurrentSubscriptionOperations().isEmpty());
        safeAction(() -> assertEquals(subscription.getMode(), SubscriptionMode.EXPLICIT));

        handler.assertEventCount(1);
        handler.assertNextEvent(SubscriptionUpdateStartedEvent.class, e -> {
            assertEquals(e.getSystemSchemaId(), connector.getSystemSchema().getId());
            assertEquals(e.getDatasetAddress(), datasetAddress);
        });
    }

    @SuppressWarnings("unchecked")
    @Test
    public void progressBulkAreaOfInterestUpdateRequests_onSuccess() {
        final Dataset dataset = new Dataset(
                0,
                ValueUtil.randomString(),
                String.class,
                Dataset.FilterMode.PARAMETER_FILTERED,
                Dataset.FilterParameterMode.UPDATABLE,
                false,
                mock(FilterParameterUpdateReplicaMatcher.class),
                false,
                true,
                Collections.emptyList());
        final SystemSchema systemSchema =
                new SystemSchema(1, ValueUtil.randomString(), new Dataset[] {dataset}, new EntityType[] {});

        final Connector connector = createConnector(systemSchema);
        final Connection connection = newConnection(connector);

        final DatasetAddress datasetAddress1 = new DatasetAddress(1, 0, 1);
        final DatasetAddress datasetAddress2 = new DatasetAddress(1, 0, 2);
        final DatasetAddress datasetAddress3 = new DatasetAddress(1, 0, 3);
        final String filterParameter = ValueUtil.randomString();
        final SubscriptionOperation request1 =
                new SubscriptionOperation(datasetAddress1, SubscriptionOperation.Type.UPDATE, filterParameter);
        final SubscriptionOperation request2 =
                new SubscriptionOperation(datasetAddress2, SubscriptionOperation.Type.UPDATE, filterParameter);
        final SubscriptionOperation request3 =
                new SubscriptionOperation(datasetAddress3, SubscriptionOperation.Type.UPDATE, filterParameter);

        pauseScheduler();

        final Subscription subscription1 = createSubscription(datasetAddress1, null, SubscriptionMode.EXPLICIT);
        final Subscription subscription2 = createSubscription(datasetAddress2, null, SubscriptionMode.EXPLICIT);
        final Subscription subscription3 = createSubscription(datasetAddress3, null, SubscriptionMode.EXPLICIT);

        connection.injectCurrentSubscriptionOperation(request1);
        connection.injectCurrentSubscriptionOperation(request2);
        connection.injectCurrentSubscriptionOperation(request3);

        final TestSpyEventHandler handler = registerTestSpyEventHandler();

        final AtomicInteger callCount = new AtomicInteger();
        doAnswer(i -> {
                    callCount.incrementAndGet();
                    final List<DatasetAddress> datasetAddresses = (List<DatasetAddress>) i.getArguments()[0];
                    assertTrue(datasetAddresses.contains(datasetAddress1));
                    assertTrue(datasetAddresses.contains(datasetAddress2));
                    assertTrue(datasetAddresses.contains(datasetAddress3));
                    return null;
                })
                .when(connector.getTransport())
                .requestBulkSubscribe(any(), eq(filterParameter));

        assertEquals(callCount.get(), 0);
        assertFalse(connection.getCurrentSubscriptionOperations().isEmpty());

        connector.progressBulkAreaOfInterestUpdateRequests(Arrays.asList(request1, request2, request3));

        assertEquals(callCount.get(), 1);
        assertFalse(connection.getCurrentSubscriptionOperations().isEmpty());
        safeAction(() -> assertEquals(subscription1.getMode(), SubscriptionMode.EXPLICIT));
        safeAction(() -> assertEquals(subscription2.getMode(), SubscriptionMode.EXPLICIT));
        safeAction(() -> assertEquals(subscription3.getMode(), SubscriptionMode.EXPLICIT));

        handler.assertEventCount(3);
        handler.assertNextEvent(SubscriptionUpdateStartedEvent.class, e -> {
            assertEquals(e.getSystemSchemaId(), connector.getSystemSchema().getId());
            assertEquals(e.getDatasetAddress(), datasetAddress1);
        });
        handler.assertNextEvent(SubscriptionUpdateStartedEvent.class, e -> {
            assertEquals(e.getSystemSchemaId(), connector.getSystemSchema().getId());
            assertEquals(e.getDatasetAddress(), datasetAddress2);
        });
        handler.assertNextEvent(SubscriptionUpdateStartedEvent.class, e -> {
            assertEquals(e.getSystemSchemaId(), connector.getSystemSchema().getId());
            assertEquals(e.getDatasetAddress(), datasetAddress3);
        });
    }

    @Test
    public void progressAreaOfInterestUpdateRequests_onFailure_zeroRequests() {
        final Dataset dataset = new Dataset(
                0,
                ValueUtil.randomString(),
                String.class,
                Dataset.FilterMode.PARAMETER_FILTERED,
                Dataset.FilterParameterMode.UPDATABLE,
                false,
                mock(FilterParameterUpdateReplicaMatcher.class),
                false,
                true,
                Collections.emptyList());
        final SystemSchema systemSchema =
                new SystemSchema(1, ValueUtil.randomString(), new Dataset[] {dataset}, new EntityType[] {});

        final Connector connector = createConnector(systemSchema);
        final Connection connection = newConnection(connector);

        final DatasetAddress datasetAddress1 = new DatasetAddress(1, 0, 1);
        final String filterParameter = ValueUtil.randomString();
        final SubscriptionOperation request1 =
                new SubscriptionOperation(datasetAddress1, SubscriptionOperation.Type.UPDATE, filterParameter);

        pauseScheduler();

        connection.injectCurrentSubscriptionOperation(request1);

        final TestSpyEventHandler handler = registerTestSpyEventHandler();

        assertFalse(connection.getCurrentSubscriptionOperations().isEmpty());

        final ArrayList<SubscriptionOperation> requests = new ArrayList<>();
        // Pass in empty requests list to simulate that they are all filtered out
        connector.progressAreaOfInterestUpdateRequests(requests);

        assertTrue(connection.getCurrentSubscriptionOperations().isEmpty());

        handler.assertEventCount(0);
    }

    @Test
    public void progressAreaOfInterestRemoveRequest_onSuccess() {
        final Dataset dataset = new Dataset(
                0,
                ValueUtil.randomString(),
                null,
                Dataset.FilterMode.UNFILTERED,
                null,
                false,
                null,
                false,
                true,
                Collections.emptyList());
        final SystemSchema systemSchema =
                new SystemSchema(1, ValueUtil.randomString(), new Dataset[] {dataset}, new EntityType[] {});

        final Connector connector = createConnector(systemSchema);
        final Connection connection = newConnection(connector);

        final DatasetAddress datasetAddress = new DatasetAddress(1, 0);
        final SubscriptionOperation request =
                new SubscriptionOperation(datasetAddress, SubscriptionOperation.Type.UNSUBSCRIBE, null);

        pauseScheduler();

        final Subscription subscription = createSubscription(datasetAddress, null, SubscriptionMode.EXPLICIT);

        connection.injectCurrentSubscriptionOperation(request);

        final TestSpyEventHandler handler = registerTestSpyEventHandler();

        final AtomicInteger callCount = new AtomicInteger();
        doAnswer(i -> {
                    callCount.incrementAndGet();
                    return null;
                })
                .when(connector.getTransport())
                .requestUnsubscribe(eq(datasetAddress));

        assertEquals(callCount.get(), 0);
        assertFalse(connection.getCurrentSubscriptionOperations().isEmpty());

        connector.progressAreaOfInterestRemoveRequest(request);

        assertEquals(callCount.get(), 1);
        assertFalse(connection.getCurrentSubscriptionOperations().isEmpty());
        safeAction(() -> assertEquals(subscription.getMode(), SubscriptionMode.EXPLICIT));

        handler.assertEventCount(1);
        handler.assertNextEvent(UnsubscribeStartedEvent.class, e -> {
            assertEquals(e.getSystemSchemaId(), connector.getSystemSchema().getId());
            assertEquals(e.getDatasetAddress(), datasetAddress);
        });
    }

    @SuppressWarnings("unchecked")
    @Test
    public void progressBulkAreaOfInterestRemoveRequests_onSuccess() {
        final Dataset dataset = new Dataset(
                0,
                ValueUtil.randomString(),
                String.class,
                Dataset.FilterMode.UNFILTERED,
                null,
                false,
                null,
                false,
                true,
                Collections.emptyList());
        final SystemSchema systemSchema =
                new SystemSchema(1, ValueUtil.randomString(), new Dataset[] {dataset}, new EntityType[] {});

        final Connector connector = createConnector(systemSchema);
        final Connection connection = newConnection(connector);

        final DatasetAddress datasetAddress1 = new DatasetAddress(1, 0, 1);
        final DatasetAddress datasetAddress2 = new DatasetAddress(1, 0, 2);
        final DatasetAddress datasetAddress3 = new DatasetAddress(1, 0, 3);
        final SubscriptionOperation request1 =
                new SubscriptionOperation(datasetAddress1, SubscriptionOperation.Type.UNSUBSCRIBE, null);
        final SubscriptionOperation request2 =
                new SubscriptionOperation(datasetAddress2, SubscriptionOperation.Type.UNSUBSCRIBE, null);
        final SubscriptionOperation request3 =
                new SubscriptionOperation(datasetAddress3, SubscriptionOperation.Type.UNSUBSCRIBE, null);

        pauseScheduler();

        final Subscription subscription1 = createSubscription(datasetAddress1, null, SubscriptionMode.EXPLICIT);
        final Subscription subscription2 = createSubscription(datasetAddress2, null, SubscriptionMode.EXPLICIT);
        final Subscription subscription3 = createSubscription(datasetAddress3, null, SubscriptionMode.EXPLICIT);

        connection.injectCurrentSubscriptionOperation(request1);
        connection.injectCurrentSubscriptionOperation(request2);
        connection.injectCurrentSubscriptionOperation(request3);

        final TestSpyEventHandler handler = registerTestSpyEventHandler();

        final AtomicInteger callCount = new AtomicInteger();
        doAnswer(i -> {
                    callCount.incrementAndGet();
                    final List<DatasetAddress> datasetAddresses = (List<DatasetAddress>) i.getArguments()[0];
                    assertTrue(datasetAddresses.contains(datasetAddress1));
                    assertTrue(datasetAddresses.contains(datasetAddress2));
                    assertTrue(datasetAddresses.contains(datasetAddress3));
                    return null;
                })
                .when(connector.getTransport())
                .requestBulkUnsubscribe(any());

        assertEquals(callCount.get(), 0);
        assertFalse(connection.getCurrentSubscriptionOperations().isEmpty());

        connector.progressBulkAreaOfInterestRemoveRequests(Arrays.asList(request1, request2, request3));

        assertEquals(callCount.get(), 1);
        assertFalse(connection.getCurrentSubscriptionOperations().isEmpty());
        safeAction(() -> assertEquals(subscription1.getMode(), SubscriptionMode.EXPLICIT));
        safeAction(() -> assertEquals(subscription2.getMode(), SubscriptionMode.EXPLICIT));
        safeAction(() -> assertEquals(subscription3.getMode(), SubscriptionMode.EXPLICIT));

        handler.assertEventCount(3);
        handler.assertNextEvent(UnsubscribeStartedEvent.class, e -> {
            assertEquals(e.getSystemSchemaId(), connector.getSystemSchema().getId());
            assertEquals(e.getDatasetAddress(), datasetAddress1);
        });
        handler.assertNextEvent(UnsubscribeStartedEvent.class, e -> {
            assertEquals(e.getSystemSchemaId(), connector.getSystemSchema().getId());
            assertEquals(e.getDatasetAddress(), datasetAddress2);
        });
        handler.assertNextEvent(UnsubscribeStartedEvent.class, e -> {
            assertEquals(e.getSystemSchemaId(), connector.getSystemSchema().getId());
            assertEquals(e.getDatasetAddress(), datasetAddress3);
        });
    }

    @Test
    public void progressAreaOfInterestRemoveRequests_onFailure_zeroRequests() {
        final Dataset dataset = new Dataset(
                0,
                ValueUtil.randomString(),
                String.class,
                Dataset.FilterMode.UNFILTERED,
                null,
                false,
                null,
                false,
                true,
                Collections.emptyList());
        final SystemSchema systemSchema =
                new SystemSchema(1, ValueUtil.randomString(), new Dataset[] {dataset}, new EntityType[] {});

        final Connector connector = createConnector(systemSchema);
        final Connection connection = newConnection(connector);

        final DatasetAddress datasetAddress1 = new DatasetAddress(1, 0, 1);
        final SubscriptionOperation request1 =
                new SubscriptionOperation(datasetAddress1, SubscriptionOperation.Type.UNSUBSCRIBE, null);

        pauseScheduler();

        connection.injectCurrentSubscriptionOperation(request1);

        final TestSpyEventHandler handler = registerTestSpyEventHandler();

        assertFalse(connection.getCurrentSubscriptionOperations().isEmpty());

        final ArrayList<SubscriptionOperation> requests = new ArrayList<>();
        // Pass in empty requests list to simulate that they are all filtered out
        connector.progressAreaOfInterestRemoveRequests(requests);

        assertTrue(connection.getCurrentSubscriptionOperations().isEmpty());

        handler.assertEventCount(0);
    }

    @Test
    public void progressSubscriptionOperationProcessing_Noop() {
        final Dataset dataset = new Dataset(
                0,
                ValueUtil.randomString(),
                String.class,
                Dataset.FilterMode.PARAMETER_FILTERED,
                Dataset.FilterParameterMode.FIXED,
                false,
                null,
                false,
                true,
                Collections.emptyList());
        final SystemSchema systemSchema =
                new SystemSchema(1, ValueUtil.randomString(), new Dataset[] {dataset}, new EntityType[] {});

        final Connector connector = createConnector(systemSchema);
        final Connection connection = newConnection(connector);

        pauseScheduler();

        final TestSpyEventHandler handler = registerTestSpyEventHandler();

        assertTrue(connection.getCurrentSubscriptionOperations().isEmpty());

        connector.progressSubscriptionOperationProcessing();

        assertTrue(connection.getCurrentSubscriptionOperations().isEmpty());

        handler.assertEventCount(0);
    }

    @Test
    public void progressSubscriptionOperationProcessing_InProgress() {
        final Dataset dataset = new Dataset(
                0,
                ValueUtil.randomString(),
                String.class,
                Dataset.FilterMode.PARAMETER_FILTERED,
                Dataset.FilterParameterMode.FIXED,
                false,
                null,
                false,
                true,
                Collections.emptyList());
        final SystemSchema systemSchema =
                new SystemSchema(1, ValueUtil.randomString(), new Dataset[] {dataset}, new EntityType[] {});

        final Connector connector = createConnector(systemSchema);
        final Connection connection = newConnection(connector);

        final DatasetAddress datasetAddress1 = new DatasetAddress(1, 0, 1);
        final String filterParameter = ValueUtil.randomString();
        final SubscriptionOperation request1 =
                new SubscriptionOperation(datasetAddress1, SubscriptionOperation.Type.SUBSCRIBE, filterParameter);

        pauseScheduler();

        request1.markAsInProgress(newRequest(connection).getRequestId());

        connection.injectCurrentSubscriptionOperation(request1);

        final TestSpyEventHandler handler = registerTestSpyEventHandler();

        assertFalse(connection.getCurrentSubscriptionOperations().isEmpty());

        connector.progressSubscriptionOperationProcessing();

        assertFalse(connection.getCurrentSubscriptionOperations().isEmpty());

        handler.assertEventCount(0);
    }

    @Test
    public void progressSubscriptionOperationProcessing_Add() {
        final Dataset dataset = new Dataset(
                0,
                ValueUtil.randomString(),
                String.class,
                Dataset.FilterMode.PARAMETER_FILTERED,
                Dataset.FilterParameterMode.FIXED,
                false,
                null,
                false,
                true,
                Collections.emptyList());
        final SystemSchema systemSchema =
                new SystemSchema(1, ValueUtil.randomString(), new Dataset[] {dataset}, new EntityType[] {});

        final Connector connector = createConnector(systemSchema);
        final Connection connection = newConnection(connector);

        final DatasetAddress datasetAddress1 = new DatasetAddress(1, 0, 1);
        final String filterParameter = ValueUtil.randomString();
        final SubscriptionOperation request1 =
                new SubscriptionOperation(datasetAddress1, SubscriptionOperation.Type.SUBSCRIBE, filterParameter);

        pauseScheduler();

        connection.injectCurrentSubscriptionOperation(request1);

        final TestSpyEventHandler handler = registerTestSpyEventHandler();

        assertFalse(connection.getCurrentSubscriptionOperations().isEmpty());

        connector.progressSubscriptionOperationProcessing();

        assertFalse(connection.getCurrentSubscriptionOperations().isEmpty());

        handler.assertEventCount(1);
        handler.assertNextEvent(SubscribeStartedEvent.class, e -> assertEquals(e.getDatasetAddress(), datasetAddress1));
    }

    @Test
    public void progressSubscriptionOperationProcessing_Update() {
        final Dataset dataset = new Dataset(
                0,
                ValueUtil.randomString(),
                String.class,
                Dataset.FilterMode.PARAMETER_FILTERED,
                Dataset.FilterParameterMode.UPDATABLE,
                false,
                mock(FilterParameterUpdateReplicaMatcher.class),
                false,
                true,
                Collections.emptyList());
        final SystemSchema systemSchema =
                new SystemSchema(1, ValueUtil.randomString(), new Dataset[] {dataset}, new EntityType[] {});

        final Connector connector = createConnector(systemSchema);
        final Connection connection = newConnection(connector);

        final DatasetAddress datasetAddress1 = new DatasetAddress(1, 0, 1);
        final String filterParameter = ValueUtil.randomString();
        final SubscriptionOperation request1 =
                new SubscriptionOperation(datasetAddress1, SubscriptionOperation.Type.UPDATE, filterParameter);

        pauseScheduler();

        createSubscription(datasetAddress1, null, SubscriptionMode.EXPLICIT);
        connection.injectCurrentSubscriptionOperation(request1);

        final TestSpyEventHandler handler = registerTestSpyEventHandler();

        assertFalse(connection.getCurrentSubscriptionOperations().isEmpty());

        connector.progressSubscriptionOperationProcessing();

        assertFalse(connection.getCurrentSubscriptionOperations().isEmpty());

        handler.assertEventCount(1);
        handler.assertNextEvent(
                SubscriptionUpdateStartedEvent.class, e -> assertEquals(e.getDatasetAddress(), datasetAddress1));
    }

    @Test
    public void progressSubscriptionOperationProcessing_Remove() {
        final Dataset dataset = new Dataset(
                0,
                ValueUtil.randomString(),
                String.class,
                Dataset.FilterMode.UNFILTERED,
                null,
                false,
                null,
                false,
                true,
                Collections.emptyList());
        final SystemSchema systemSchema =
                new SystemSchema(1, ValueUtil.randomString(), new Dataset[] {dataset}, new EntityType[] {});

        final Connector connector = createConnector(systemSchema);
        final Connection connection = newConnection(connector);

        final DatasetAddress datasetAddress1 = new DatasetAddress(1, 0, 1);
        final SubscriptionOperation request1 =
                new SubscriptionOperation(datasetAddress1, SubscriptionOperation.Type.UNSUBSCRIBE, null);

        pauseScheduler();

        createSubscription(datasetAddress1, null, SubscriptionMode.EXPLICIT);

        connection.injectCurrentSubscriptionOperation(request1);

        final TestSpyEventHandler handler = registerTestSpyEventHandler();

        assertFalse(connection.getCurrentSubscriptionOperations().isEmpty());

        connector.progressSubscriptionOperationProcessing();

        assertFalse(connection.getCurrentSubscriptionOperations().isEmpty());

        handler.assertEventCount(1);
        handler.assertNextEvent(
                UnsubscribeStartedEvent.class, e -> assertEquals(e.getDatasetAddress(), datasetAddress1));
    }

    @Test
    public void pauseMessageScheduler() {
        final Connector connector = createConnector();
        newConnection(connector);

        connector.pauseMessageScheduler();

        assertTrue(connector.isSchedulerPaused());
        assertFalse(connector.isSchedulerActive());

        connector.requestSubscribe(new DatasetAddress(1, 0, 1), null);

        assertTrue(connector.isSchedulerActive());

        connector.resumeMessageScheduler();

        assertFalse(connector.isSchedulerPaused());
        assertFalse(connector.isSchedulerActive());

        connector.pauseMessageScheduler();

        assertTrue(connector.isSchedulerPaused());
        assertFalse(connector.isSchedulerActive());

        // No progress
        assertFalse(connector.progressMessages());

        Disposable.dispose(connector);

        assertFalse(connector.isSchedulerActive());
        assertTrue(connector.isSchedulerPaused());

        assertNull(connector.getSchedulerLock());
    }

    @Test
    public void isSynchronized_notConnected() {
        final Connector connector = createConnector();
        safeAction(() -> connector.setState(ConnectorState.DISCONNECTED));
        safeAction(() -> assertFalse(connector.isSynchronized()));
    }

    @Test
    public void isSynchronized_connected() {
        final Connector connector = createConnector();
        newConnection(connector);
        safeAction(() -> connector.setState(ConnectorState.CONNECTED));
        safeAction(() -> assertTrue(connector.isSynchronized()));
    }

    @Test
    public void shouldRequestSync_notConnected() {
        final Connector connector = createConnector();
        safeAction(() -> connector.setState(ConnectorState.DISCONNECTED));
        assertFalse(connector.shouldRequestSync());
    }

    @Test
    public void shouldRequestSync_connected() {
        final Connector connector = createConnector();
        newConnection(connector);
        safeAction(() -> {
            connector.setState(ConnectorState.CONNECTED);
            assertFalse(connector.shouldRequestSync());
        });
    }

    @Test
    public void shouldRequestSync_sentRequest_NotSynced() {
        final Connector connector = createConnector();
        newConnection(connector);
        safeAction(() -> {
            connector.setState(ConnectorState.CONNECTED);
            newRequest(connector.ensureConnection());
            assertFalse(connector.shouldRequestSync());
        });
    }

    @Test
    public void shouldRequestSync_receivedRequestResponse_NotSynced() {
        final Connector connector = createConnector();
        final Connection connection = newConnection(connector);
        safeAction(() -> {
            connector.setState(ConnectorState.CONNECTED);
            connection.removeRequest(newRequest(connection).getRequestId());
            assertTrue(connector.shouldRequestSync());
        });
    }

    @Test
    public void shouldRequestSync_receivedSyncRequestResponse_Synced() {
        final Connector connector = createConnector();
        final Connection connection = newConnection(connector);
        safeAction(() -> {
            connector.setState(ConnectorState.CONNECTED);
            final RequestEntry entry = connection.newRequest(ValueUtil.randomString(), true, null);
            connection.removeRequest(entry.getRequestId());
            assertFalse(connector.shouldRequestSync());
        });
    }

    @Test
    public void shouldRequestSync_receivedSyncRequestResponseButResponsesQueued_NotSynced() {
        final Connector connector = createConnector();
        final Connection connection = newConnection(connector);
        safeAction(() -> {
            connector.setState(ConnectorState.CONNECTED);
            connection.removeRequest(newRequest(connection).getRequestId());
            connection.enqueueResponse(ChangeSetMessage.create(null, null, null, null, null, null), null);
            assertFalse(connector.shouldRequestSync());
        });
    }

    @Test
    public void onInSync() {
        final Connector connector = createConnector();

        final TestSpyEventHandler handler = registerTestSpyEventHandler();

        connector.onInSync();

        handler.assertEventCount(1);
        handler.assertNextEvent(
                InSyncEvent.class,
                e -> assertEquals(
                        e.getSystemSchemaId(), connector.getSystemSchema().getId()));
    }

    @Test
    public void onOutOfSync() {
        final Connector connector = createConnector();

        final TestSpyEventHandler handler = registerTestSpyEventHandler();

        connector.onOutOfSync();

        handler.assertEventCount(1);
        handler.assertNextEvent(
                OutOfSyncEvent.class,
                e -> assertEquals(
                        e.getSystemSchemaId(), connector.getSystemSchema().getId()));
    }

    @Test
    public void requestSync() {
        final Connector connector = createConnector();

        final TestSpyEventHandler handler = registerTestSpyEventHandler();

        connector.requestSync();

        verify(connector.getTransport()).requestSync();

        handler.assertEventCount(1);
        handler.assertNextEvent(
                SyncRequestEvent.class,
                e -> assertEquals(
                        e.getSystemSchemaId(), connector.getSystemSchema().getId()));
    }

    @Test
    public void maybeRequestSync() {
        final Connector connector = createConnector();
        final Connection connection = newConnection(connector);

        safeAction(() -> connector.setState(ConnectorState.CONNECTED));

        assertFalse(connector.shouldRequestSync());
        connector.maybeRequestSync();

        assertTrue(connector.ensureConnection().getRequests().isEmpty());

        connection.removeRequest(newRequest(connection).getRequestId());
        assertTrue(connector.shouldRequestSync());

        connector.maybeRequestSync();

        verify(connector.getTransport()).requestSync();
    }

    @Test
    public void onExecStarted() {
        final Connector connector = createConnector();

        final TestSpyEventHandler handler = registerTestSpyEventHandler();

        final String command = ValueUtil.randomString();
        final int requestId = ValueUtil.randomInt();
        connector.onExecStarted(command, requestId);

        handler.assertEventCount(1);
        handler.assertNextEvent(ExecStartedEvent.class, e -> {
            assertEquals(e.getSystemSchemaId(), connector.getSystemSchema().getId());
            assertEquals(e.getSystemSchemaName(), connector.getSystemSchema().getName());
            assertEquals(e.getCommand(), command);
            assertEquals(e.getRequestId(), requestId);
        });
    }

    @Test
    public void onExecCompleted() {
        final Connector connector = createConnector();

        final TestSpyEventHandler handler = registerTestSpyEventHandler();

        final String command = ValueUtil.randomString();
        final int requestId = ValueUtil.randomInt();
        connector.onExecCompleted(command, requestId);

        handler.assertEventCount(1);
        handler.assertNextEvent(ExecCompletedEvent.class, e -> {
            assertEquals(e.getSystemSchemaId(), connector.getSystemSchema().getId());
            assertEquals(e.getSystemSchemaName(), connector.getSystemSchema().getName());
            assertEquals(e.getCommand(), command);
            assertEquals(e.getRequestId(), requestId);
        });
    }

    @Test
    public void requestExec() {
        final Connector connector = createConnector();
        connector.pauseMessageScheduler();
        newConnection(connector);

        final TestSpyEventHandler handler = registerTestSpyEventHandler();

        final String command = ValueUtil.randomString();
        final Object payload = new Object();
        connector.requestExec(command, payload, null);

        handler.assertEventCount(1);
        handler.assertNextEvent(ExecRequestQueuedEvent.class, e -> {
            assertEquals(e.getSystemSchemaId(), connector.getSystemSchema().getId());
            assertEquals(e.getSystemSchemaName(), connector.getSystemSchema().getName());
            assertEquals(e.getCommand(), command);
        });

        final List<ExecRequest> requests = connector.ensureConnection().getPendingExecRequests();
        assertEquals(requests.size(), 1);
        final ExecRequest request = requests.get(0);
        assertEquals(request.getCommand(), command);
        assertEquals(request.getPayload(), payload);
    }

    @NonNull
    private RequestEntry newRequest(@NonNull final Connection connection) {
        return connection.newRequest(ValueUtil.randomString(), false, null);
    }

    @NonNull
    private MessageResponse setCurrentMessageResponse(
            @NonNull final Connection connection, @NonNull final ServerToClientMessage message) {
        return setCurrentMessageResponse(connection, message, null);
    }

    @NonNull
    private MessageResponse setCurrentMessageResponse(
            @NonNull final Connection connection,
            @NonNull final ServerToClientMessage message,
            @Nullable final RequestEntry request) {
        connection.enqueueResponse(message, request);
        connection.selectNextMessageResponse();
        final MessageResponse response = connection.getCurrentMessageResponse();
        return Objects.requireNonNull(response);
    }
}
