package replicant;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

import arez.Disposable;
import java.util.Collections;
import java.util.List;
import org.testng.annotations.Test;

public class ReplicantContextTest extends AbstractReplicantTest {
    @Test
    public void stateIsIsolatedBetweenContexts() {
        ReplicantTestUtil.enableZones();
        ReplicantTestUtil.resetState();
        pauseScheduler();

        final Dataset dataset1 = new Dataset(
                0,
                ValueUtil.randomString(),
                null,
                Dataset.FilterMode.UNFILTERED,
                null,
                false,
                null,
                false,
                Dataset.Visibility.INTERNAL,
                Collections.emptyList());
        final Dataset dataset2 = new Dataset(
                1,
                ValueUtil.randomString(),
                null,
                Dataset.FilterMode.UNFILTERED,
                null,
                false,
                null,
                false,
                Dataset.Visibility.INTERNAL,
                Collections.emptyList());
        final SystemSchema systemSchema =
                new SystemSchema(42, ValueUtil.randomString(), new Dataset[] {dataset1, dataset2}, new EntityType[0]);
        final SystemSchema additionalSystemSchema =
                new SystemSchema(43, ValueUtil.randomString(), new Dataset[0], new EntityType[0]);
        final DatasetAddress datasetAddress1 = new DatasetAddress(systemSchema.getId(), dataset1.getId());
        final DatasetAddress datasetAddress2 = new DatasetAddress(systemSchema.getId(), dataset2.getId());
        final Zone zone1 = Replicant.createZone();
        final Zone zone2 = Replicant.createZone();
        final ReplicantContext[] contexts = new ReplicantContext[2];
        final AreaOfInterest[] areasOfInterest = new AreaOfInterest[2];
        final Subscription[] subscriptions = new Subscription[2];
        final ReplicaEntry[] replicaEntries = new ReplicaEntry[2];
        final DatasetCacheService[] datasetCacheServices =
                new DatasetCacheService[] {mock(DatasetCacheService.class), mock(DatasetCacheService.class)};
        final Disposable[] connectorRegistrations = new Disposable[3];

        zone1.safeRun(() -> safeAction(() -> {
            final ReplicantContext context = Replicant.context();
            contexts[0] = context;
            context.setDatasetCacheService(datasetCacheServices[0]);
            connectorRegistrations[0] = context.registerConnector(systemSchema, mock(Transport.class));
            connectorRegistrations[1] = context.registerConnector(additionalSystemSchema, mock(Transport.class));
            areasOfInterest[0] = context.createOrUpdateAreaOfInterest(datasetAddress1, null);
            subscriptions[0] = context.getSubscriptionService()
                    .createSubscription(datasetAddress1, null, SubscriptionMode.EXPLICIT);
            final Subscription secondSubscription = context.getSubscriptionService()
                    .createSubscription(datasetAddress2, null, SubscriptionMode.EXPLICIT);
            replicaEntries[0] = context.getReplicaRegistry().findOrCreateReplicaEntry("Replica/7", A.class, 7);
            replicaEntries[0].linkToSubscription(subscriptions[0]);
            replicaEntries[0].linkToSubscription(secondSubscription);

            assertSame(context.findReplicaEntryByTypeAndId(A.class, 7), replicaEntries[0]);
            assertEquals(replicaEntries[0].getSubscriptions().size(), 2);
        }));

        zone2.safeRun(() -> safeAction(() -> {
            final ReplicantContext context = Replicant.context();
            contexts[1] = context;
            context.setDatasetCacheService(datasetCacheServices[1]);
            connectorRegistrations[2] = context.registerConnector(systemSchema, mock(Transport.class));
            areasOfInterest[1] = context.createOrUpdateAreaOfInterest(datasetAddress1, null);
            subscriptions[1] = context.getSubscriptionService()
                    .createSubscription(datasetAddress1, null, SubscriptionMode.EXPLICIT);
            replicaEntries[1] = context.getReplicaRegistry().findOrCreateReplicaEntry("Replica/7", A.class, 7);
            replicaEntries[1].linkToSubscription(subscriptions[1]);
        }));

        safeAction(() -> {
            assertNotSame(contexts[0], contexts[1]);
            assertEquals(contexts[0].getSystemSchemas().size(), 2);
            assertTrue(contexts[0].getSystemSchemas().contains(systemSchema));
            assertTrue(contexts[0].getSystemSchemas().contains(additionalSystemSchema));
            assertEquals(contexts[1].getSystemSchemas().size(), 1);
            assertTrue(contexts[1].getSystemSchemas().contains(systemSchema));
            assertFalse(contexts[1].getSystemSchemas().contains(additionalSystemSchema));
            assertNotSame(areasOfInterest[0], areasOfInterest[1]);
            assertSame(contexts[0].findAreaOfInterestByDatasetAddress(datasetAddress1), areasOfInterest[0]);
            assertSame(contexts[1].findAreaOfInterestByDatasetAddress(datasetAddress1), areasOfInterest[1]);
            assertNotSame(subscriptions[0], subscriptions[1]);
            assertSame(contexts[0].findSubscription(datasetAddress1), subscriptions[0]);
            assertSame(contexts[1].findSubscription(datasetAddress1), subscriptions[1]);
            assertNotSame(replicaEntries[0], replicaEntries[1]);
            assertSame(contexts[0].findReplicaEntryByTypeAndId(A.class, 7), replicaEntries[0]);
            assertSame(contexts[1].findReplicaEntryByTypeAndId(A.class, 7), replicaEntries[1]);
            assertSame(contexts[0].getDatasetCacheService(), datasetCacheServices[0]);
            assertSame(contexts[1].getDatasetCacheService(), datasetCacheServices[1]);

            connectorRegistrations[0].dispose();

            assertEquals(contexts[0].getSystemSchemas().size(), 1);
            assertFalse(contexts[0].getSystemSchemas().contains(systemSchema));
            assertTrue(contexts[0].getSystemSchemas().contains(additionalSystemSchema));
            assertTrue(contexts[1].getSystemSchemas().contains(systemSchema));
            assertEquals(contexts[0].getRuntime().getConnectors().size(), 1);
            assertEquals(contexts[1].getRuntime().getConnectors().size(), 1);
        });
    }

    @Test
    public void schemas() {
        final int systemSchemaId = 22;

        final ReplicantContext context = Replicant.context();
        assertEquals(context.getSystemSchemas().size(), 0);
        assertNull(context.findSystemSchemaById(systemSchemaId));

        final IllegalStateException exception = expectThrows(
                IllegalStateException.class, () -> safeAction(() -> context.getSystemSchemaById(systemSchemaId)));
        assertEquals(exception.getMessage(), "Replicant-0059: Unable to locate System Schema with id 22");

        final SystemSchema systemSchema =
                new SystemSchema(systemSchemaId, ValueUtil.randomString(), new Dataset[0], new EntityType[0]);

        context.getSystemSchemaService().registerSystemSchema(systemSchema);

        assertEquals(context.getSystemSchemas().size(), 1);
        assertTrue(context.getSystemSchemas().contains(systemSchema));

        assertEquals(context.findSystemSchemaById(systemSchemaId), systemSchema);
        assertEquals(context.getSystemSchemaById(systemSchemaId), systemSchema);
    }

    @Test
    public void areasOfInterest() {
        // Pause scheduler to prevent automatic subscription reconciliation
        pauseScheduler();

        final ReplicantContext context = Replicant.context();
        final DatasetAddress datasetAddress = new DatasetAddress(1, 0);
        final String filterParameter = ValueUtil.randomString();
        final String filterParameter2 = ValueUtil.randomString();

        safeAction(() -> {
            assertEquals(context.getAreasOfInterest().size(), 0);
            assertNull(context.findAreaOfInterestByDatasetAddress(datasetAddress));

            final AreaOfInterest areaOfInterest = context.createOrUpdateAreaOfInterest(datasetAddress, filterParameter);

            assertEquals(areaOfInterest.getFilterParameter(), filterParameter);
            assertEquals(context.getAreasOfInterest().size(), 1);
            assertEquals(context.findAreaOfInterestByDatasetAddress(datasetAddress), areaOfInterest);

            final AreaOfInterest areaOfInterest2 =
                    context.createOrUpdateAreaOfInterest(datasetAddress, filterParameter2);

            assertEquals(areaOfInterest2, areaOfInterest);
            assertEquals(areaOfInterest2.getFilterParameter(), filterParameter2);
            assertEquals(context.getAreasOfInterest().size(), 1);
            assertEquals(context.findAreaOfInterestByDatasetAddress(datasetAddress), areaOfInterest2);
            assertEquals(context.findAreaOfInterestByDatasetAddress(datasetAddress), areaOfInterest2);
        });
    }

    @Test
    public void replicaEntries() {
        final ReplicantContext context = Replicant.context();

        safeAction(() -> assertEquals(context.findAllReplicaTypes().size(), 0));
        safeAction(
                () -> assertEquals(context.findAllReplicaEntriesByType(A.class).size(), 0));
        safeAction(
                () -> assertEquals(context.findAllReplicaEntriesByType(B.class).size(), 0));
        safeAction(() -> assertNull(context.findReplicaEntryByTypeAndId(A.class, 1)));
        safeAction(() -> assertNull(context.findReplicaEntryByTypeAndId(A.class, 2)));
        safeAction(() -> assertNull(context.findReplicaEntryByTypeAndId(B.class, 47)));

        final ReplicaEntry replicaEntry1 = findOrCreateReplicaEntry(A.class, 1);

        assertEquals(replicaEntry1.getName(), "A/1");
        safeAction(() -> assertEquals(context.findAllReplicaTypes().size(), 1));
        safeAction(
                () -> assertEquals(context.findAllReplicaEntriesByType(A.class).size(), 1));
        safeAction(
                () -> assertEquals(context.findAllReplicaEntriesByType(B.class).size(), 0));
        safeAction(() -> assertEquals(context.findReplicaEntryByTypeAndId(A.class, 1), replicaEntry1));
        safeAction(() -> assertNull(context.findReplicaEntryByTypeAndId(A.class, 2)));
        safeAction(() -> assertNull(context.findReplicaEntryByTypeAndId(B.class, 47)));

        final ReplicaEntry replicaEntry2 =
                safeAction(() -> context.getReplicaRegistry().findOrCreateReplicaEntry("Super-dee-duper", A.class, 2));

        assertEquals(replicaEntry2.getName(), "Super-dee-duper");
        safeAction(() -> assertEquals(context.findAllReplicaTypes().size(), 1));
        safeAction(
                () -> assertEquals(context.findAllReplicaEntriesByType(A.class).size(), 2));
        safeAction(
                () -> assertEquals(context.findAllReplicaEntriesByType(B.class).size(), 0));
        safeAction(() -> assertEquals(context.findReplicaEntryByTypeAndId(A.class, 1), replicaEntry1));
        safeAction(() -> assertEquals(context.findReplicaEntryByTypeAndId(A.class, 2), replicaEntry2));
        safeAction(() -> assertNull(context.findReplicaEntryByTypeAndId(B.class, 47)));

        final ReplicaEntry replicaEntry3 = findOrCreateReplicaEntry(B.class, 47);

        safeAction(() -> assertEquals(context.findAllReplicaTypes().size(), 2));
        safeAction(
                () -> assertEquals(context.findAllReplicaEntriesByType(A.class).size(), 2));
        safeAction(
                () -> assertEquals(context.findAllReplicaEntriesByType(B.class).size(), 1));
        safeAction(() -> assertEquals(context.findReplicaEntryByTypeAndId(A.class, 1), replicaEntry1));
        safeAction(() -> assertEquals(context.findReplicaEntryByTypeAndId(A.class, 2), replicaEntry2));
        safeAction(() -> assertEquals(context.findReplicaEntryByTypeAndId(B.class, 47), replicaEntry3));

        Disposable.dispose(replicaEntry1);

        safeAction(() -> assertEquals(context.findAllReplicaTypes().size(), 2));
        safeAction(
                () -> assertEquals(context.findAllReplicaEntriesByType(A.class).size(), 1));
        safeAction(
                () -> assertEquals(context.findAllReplicaEntriesByType(B.class).size(), 1));
        safeAction(() -> assertNull(context.findReplicaEntryByTypeAndId(A.class, 1)));
        safeAction(() -> assertEquals(context.findReplicaEntryByTypeAndId(A.class, 2), replicaEntry2));
        safeAction(() -> assertEquals(context.findReplicaEntryByTypeAndId(B.class, 47), replicaEntry3));

        Disposable.dispose(replicaEntry2);

        safeAction(() -> assertEquals(context.findAllReplicaTypes().size(), 1));
        safeAction(
                () -> assertEquals(context.findAllReplicaEntriesByType(A.class).size(), 0));
        safeAction(
                () -> assertEquals(context.findAllReplicaEntriesByType(B.class).size(), 1));
        safeAction(() -> assertNull(context.findReplicaEntryByTypeAndId(A.class, 1)));
        safeAction(() -> assertNull(context.findReplicaEntryByTypeAndId(A.class, 2)));
        safeAction(() -> assertEquals(context.findReplicaEntryByTypeAndId(B.class, 47), replicaEntry3));

        Disposable.dispose(replicaEntry3);

        safeAction(() -> assertEquals(context.findAllReplicaTypes().size(), 0));
        safeAction(
                () -> assertEquals(context.findAllReplicaEntriesByType(A.class).size(), 0));
        safeAction(
                () -> assertEquals(context.findAllReplicaEntriesByType(B.class).size(), 0));
        safeAction(() -> assertNull(context.findReplicaEntryByTypeAndId(A.class, 1)));
        safeAction(() -> assertNull(context.findReplicaEntryByTypeAndId(A.class, 2)));
        safeAction(() -> assertNull(context.findReplicaEntryByTypeAndId(B.class, 47)));
    }

    @Test
    public void subscriptions() {
        // Pause scheduler to prevent automatic subscription reconciliation
        pauseScheduler();

        final ReplicantContext context = Replicant.context();
        final DatasetAddress datasetAddress1 = new DatasetAddress(1, 0);
        final DatasetAddress datasetAddress2 = new DatasetAddress(1, 1, 1);
        final DatasetAddress datasetAddress3 = new DatasetAddress(1, 1, 2);
        final String filterParameter1 = null;
        final String filterParameter2 = ValueUtil.randomString();
        final String filterParameter3 = ValueUtil.randomString();
        final SubscriptionMode mode1 = SubscriptionMode.EXPLICIT;
        final SubscriptionMode mode2 = SubscriptionMode.EXPLICIT;
        final SubscriptionMode mode3 = SubscriptionMode.IMPLICIT;

        safeAction(() -> {
            assertEquals(context.getTypeDatasetSubscriptions().size(), 0);
            assertEquals(context.getInstanceDatasetSubscriptions().size(), 0);
            assertEquals(context.getInstanceDatasetSubscriptionIds(1, 1).size(), 0);
            assertNull(context.findSubscription(datasetAddress1));
            assertNull(context.findSubscription(datasetAddress2));
            assertNull(context.findSubscription(datasetAddress3));
            assertFalse(context.isDataAvailable(datasetAddress1));
            assertFalse(context.isDataAvailable(datasetAddress2));
            assertFalse(context.isDataAvailable(datasetAddress3));

            final Subscription subscription1 = createSubscription(datasetAddress1, filterParameter1, mode1);

            assertEquals(subscription1.datasetAddress(), datasetAddress1);
            assertEquals(subscription1.getFilterParameter(), filterParameter1);
            assertEquals(subscription1.getMode(), mode1);

            assertEquals(context.getTypeDatasetSubscriptions().size(), 1);
            assertEquals(context.getInstanceDatasetSubscriptions().size(), 0);
            assertEquals(context.getInstanceDatasetSubscriptionIds(1, 1).size(), 0);
            assertEquals(context.findSubscription(datasetAddress1), subscription1);
            assertNull(context.findSubscription(datasetAddress2));
            assertNull(context.findSubscription(datasetAddress3));
            assertTrue(context.isDataAvailable(datasetAddress1));
            assertFalse(context.isDataAvailable(datasetAddress2));
            assertFalse(context.isDataAvailable(datasetAddress3));

            final Subscription subscription2 = createSubscription(datasetAddress2, filterParameter2, mode2);

            assertEquals(subscription2.datasetAddress(), datasetAddress2);
            assertEquals(subscription2.getFilterParameter(), filterParameter2);
            assertEquals(subscription2.getMode(), mode2);

            assertEquals(context.getTypeDatasetSubscriptions().size(), 1);
            assertEquals(context.getInstanceDatasetSubscriptions().size(), 1);
            assertEquals(context.getInstanceDatasetSubscriptionIds(1, 1).size(), 1);
            assertEquals(context.findSubscription(datasetAddress1), subscription1);
            assertEquals(context.findSubscription(datasetAddress2), subscription2);
            assertNull(context.findSubscription(datasetAddress3));
            assertTrue(context.isDataAvailable(datasetAddress1));
            assertTrue(context.isDataAvailable(datasetAddress2));
            assertFalse(context.isDataAvailable(datasetAddress3));

            final Subscription subscription3 = createSubscription(datasetAddress3, filterParameter3, mode3);

            assertEquals(subscription3.datasetAddress(), datasetAddress3);
            assertEquals(subscription3.getFilterParameter(), filterParameter3);
            assertEquals(subscription3.getMode(), mode3);

            assertEquals(context.getTypeDatasetSubscriptions().size(), 1);
            assertEquals(context.getInstanceDatasetSubscriptions().size(), 2);
            assertEquals(context.getInstanceDatasetSubscriptionIds(1, 1).size(), 2);
            assertEquals(context.findSubscription(datasetAddress1), subscription1);
            assertEquals(context.findSubscription(datasetAddress2), subscription2);
            assertEquals(context.findSubscription(datasetAddress3), subscription3);
            assertTrue(context.isDataAvailable(datasetAddress1));
            assertTrue(context.isDataAvailable(datasetAddress2));
            assertTrue(context.isDataAvailable(datasetAddress3));

            Disposable.dispose(subscription2);
            Disposable.dispose(subscription3);

            assertEquals(context.getTypeDatasetSubscriptions().size(), 1);
            assertEquals(context.getInstanceDatasetSubscriptions().size(), 0);
            assertEquals(context.getInstanceDatasetSubscriptionIds(1, 1).size(), 0);
            assertEquals(context.findSubscription(datasetAddress1), subscription1);
            assertNull(context.findSubscription(datasetAddress2));
            assertNull(context.findSubscription(datasetAddress3));
            assertTrue(context.isDataAvailable(datasetAddress1));
            assertFalse(context.isDataAvailable(datasetAddress2));
            assertFalse(context.isDataAvailable(datasetAddress3));
        });
    }

    @Test
    public void getSpy_whenSpiesDisabled() {
        ReplicantTestUtil.disableSpies();
        ReplicantTestUtil.resetState();

        assertEquals(
                expectThrows(IllegalStateException.class, Replicant.context()::getSpy)
                        .getMessage(),
                "Replicant-0021: Attempting to get Spy but spies are not enabled.");
    }

    @Test
    public void getSpy() {
        final ReplicantContext context = Replicant.context();

        assertFalse(context.willPropagateSpyEvents());

        final Spy spy = context.getSpy();

        spy.addSpyEventHandler(new TestSpyEventHandler());

        assertTrue(spy.willPropagateSpyEvents());
        assertTrue(context.willPropagateSpyEvents());

        ReplicantTestUtil.disableSpies();

        assertFalse(spy.willPropagateSpyEvents());
        assertFalse(context.willPropagateSpyEvents());
    }

    @Test
    public void preReconciliationAction() {
        safeAction(() -> {
            final SafeProcedure action = () -> {};
            assertNull(Replicant.context().getPreReconciliationAction());
            Replicant.context().setPreReconciliationAction(action);
            assertEquals(Replicant.context().getPreReconciliationAction(), action);
        });
    }

    @Test
    public void reconciliationCompleteAction() {
        safeAction(() -> {
            final SafeProcedure action = () -> {};
            assertNull(Replicant.context().getReconciliationCompleteAction());
            Replicant.context().setReconciliationCompleteAction(action);
            assertEquals(Replicant.context().getReconciliationCompleteAction(), action);
        });
    }

    @Test
    public void active() {
        final ReplicantContext context = Replicant.context();
        assertEquals(context.getState(), RuntimeState.CONNECTED);
        safeAction(() -> assertTrue(context.isActive()));
        context.deactivate();
        assertEquals(context.getState(), RuntimeState.DISCONNECTED);
        safeAction(() -> assertFalse(context.isActive()));
        context.activate();
        assertEquals(context.getState(), RuntimeState.CONNECTED);
        safeAction(() -> assertTrue(context.isActive()));
    }

    @Test
    public void setConnectorRequired() {
        final SystemSchema systemSchema = newSystemSchema();

        createConnector(systemSchema);

        final ReplicantContext context = Replicant.context();

        final int systemSchemaId = systemSchema.getId();
        assertTrue(context.getRuntime()
                .getConnectorEntryBySystemSchemaId(systemSchemaId)
                .isRequired());
        context.setConnectorRequired(systemSchemaId, false);
        assertFalse(context.getRuntime()
                .getConnectorEntryBySystemSchemaId(systemSchemaId)
                .isRequired());
    }

    @SuppressWarnings("ConstantValue")
    @Test
    public void setDatasetCacheService() {
        createConnector();

        final ReplicantContext context = Replicant.context();
        final DatasetCacheService datasetCacheService = mock(DatasetCacheService.class);

        assertNull(context.getDatasetCacheService());

        context.setDatasetCacheService(datasetCacheService);

        assertEquals(context.getDatasetCacheService(), datasetCacheService);

        context.setDatasetCacheService(null);

        assertNull(context.getDatasetCacheService());
    }

    @Test
    public void exec() {
        final Connector connector = createConnector();
        connector.pauseMessageScheduler();
        final Connection connection = newConnection(connector);

        final String command = ValueUtil.randomString();
        final Object payload = new Object();

        Replicant.context().exec(connector.getSystemSchema().getId(), command, payload, null);

        final List<ExecRequest> requests = connection.getPendingExecRequests();
        assertEquals(requests.size(), 1);
        final ExecRequest request = requests.get(0);
        assertEquals(request.getCommand(), command);
        assertEquals(request.getPayload(), payload);
    }

    @Test
    public void findConnectionId() {
        final Connector connector = createConnector();
        final int systemSchemaId = connector.getSystemSchema().getId();

        assertNull(Replicant.context().findConnectionId(systemSchemaId));

        final Connection connection = newConnection(connector);

        assertEquals(Replicant.context().findConnectionId(systemSchemaId), connection.getConnectionId());
    }

    @Test
    public void registerConnector() {
        safeAction(() ->
                assertEquals(Replicant.context().getRuntime().getConnectors().size(), 0));
        assertEquals(Replicant.context().getSystemSchemas().size(), 0);

        final Disposable disposable = Replicant.context().registerConnector(newSystemSchema(), mock(Transport.class));

        safeAction(() ->
                assertEquals(Replicant.context().getRuntime().getConnectors().size(), 1));
        assertEquals(Replicant.context().getSystemSchemas().size(), 1);

        disposable.dispose();

        safeAction(() ->
                assertEquals(Replicant.context().getRuntime().getConnectors().size(), 0));
        assertEquals(Replicant.context().getSystemSchemas().size(), 0);
    }

    static class A {}

    private static class B {}
}
