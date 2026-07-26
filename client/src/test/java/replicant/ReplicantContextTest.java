package replicant;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

import arez.Disposable;
import java.util.List;
import org.testng.annotations.Test;

public class ReplicantContextTest extends AbstractReplicantTest {
    @Test
    public void schemas() {
        final int schemaId = 22;

        final ReplicantContext context = Replicant.context();
        assertEquals(context.getSchemas().size(), 0);
        assertNull(context.findSchemaById(schemaId));

        final IllegalStateException exception =
                expectThrows(IllegalStateException.class, () -> safeAction(() -> context.getSchemaById(schemaId)));
        assertEquals(exception.getMessage(), "Replicant-0059: Unable to locate SystemSchema with id 22");

        final SystemSchema schema1 =
                new SystemSchema(schemaId, ValueUtil.randomString(), new Dataset[0], new EntityType[0]);

        context.getSchemaService().registerSchema(schema1);

        assertEquals(context.getSchemas().size(), 1);
        assertTrue(context.getSchemas().contains(schema1));

        assertEquals(context.findSchemaById(schemaId), schema1);
        assertEquals(context.getSchemaById(schemaId), schema1);
    }

    @Test
    public void areasOfInterest() {
        // Pause scheduler to prevent automatic subscription reconciliation
        pauseScheduler();

        final ReplicantContext context = Replicant.context();
        final DatasetAddress datasetAddress = new DatasetAddress(1, 0);
        final String filter = ValueUtil.randomString();
        final String filter2 = ValueUtil.randomString();

        safeAction(() -> {
            assertEquals(context.getAreasOfInterest().size(), 0);
            assertNull(context.findAreaOfInterestByDatasetAddress(datasetAddress));

            final AreaOfInterest areaOfInterest = context.createOrUpdateAreaOfInterest(datasetAddress, filter);

            assertEquals(areaOfInterest.getFilter(), filter);
            assertEquals(context.getAreasOfInterest().size(), 1);
            assertEquals(context.findAreaOfInterestByDatasetAddress(datasetAddress), areaOfInterest);

            final AreaOfInterest areaOfInterest2 = context.createOrUpdateAreaOfInterest(datasetAddress, filter2);

            assertEquals(areaOfInterest2, areaOfInterest);
            assertEquals(areaOfInterest2.getFilter(), filter2);
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
        final String filter1 = null;
        final String filter2 = ValueUtil.randomString();
        final String filter3 = ValueUtil.randomString();
        final boolean explicitSubscription1 = true;
        final boolean explicitSubscription2 = true;
        final boolean explicitSubscription3 = false;

        safeAction(() -> {
            assertEquals(context.getTypeDatasetSubscriptions().size(), 0);
            assertEquals(context.getInstanceDatasetSubscriptions().size(), 0);
            assertEquals(context.getInstanceDatasetSubscriptionIds(1, 1).size(), 0);
            assertNull(context.findSubscription(datasetAddress1));
            assertNull(context.findSubscription(datasetAddress2));
            assertNull(context.findSubscription(datasetAddress3));

            final Subscription subscription1 = createSubscription(datasetAddress1, filter1, explicitSubscription1);

            assertEquals(subscription1.datasetAddress(), datasetAddress1);
            assertEquals(subscription1.getFilter(), filter1);
            assertEquals(subscription1.isExplicitSubscription(), explicitSubscription1);

            assertEquals(context.getTypeDatasetSubscriptions().size(), 1);
            assertEquals(context.getInstanceDatasetSubscriptions().size(), 0);
            assertEquals(context.getInstanceDatasetSubscriptionIds(1, 1).size(), 0);
            assertEquals(context.findSubscription(datasetAddress1), subscription1);
            assertNull(context.findSubscription(datasetAddress2));
            assertNull(context.findSubscription(datasetAddress3));

            final Subscription subscription2 = createSubscription(datasetAddress2, filter2, explicitSubscription2);

            assertEquals(subscription2.datasetAddress(), datasetAddress2);
            assertEquals(subscription2.getFilter(), filter2);
            assertEquals(subscription2.isExplicitSubscription(), explicitSubscription2);

            assertEquals(context.getTypeDatasetSubscriptions().size(), 1);
            assertEquals(context.getInstanceDatasetSubscriptions().size(), 1);
            assertEquals(context.getInstanceDatasetSubscriptionIds(1, 1).size(), 1);
            assertEquals(context.findSubscription(datasetAddress1), subscription1);
            assertEquals(context.findSubscription(datasetAddress2), subscription2);
            assertNull(context.findSubscription(datasetAddress3));

            final Subscription subscription3 = createSubscription(datasetAddress3, filter3, explicitSubscription3);

            assertEquals(subscription3.datasetAddress(), datasetAddress3);
            assertEquals(subscription3.getFilter(), filter3);
            assertEquals(subscription3.isExplicitSubscription(), explicitSubscription3);

            assertEquals(context.getTypeDatasetSubscriptions().size(), 1);
            assertEquals(context.getInstanceDatasetSubscriptions().size(), 2);
            assertEquals(context.getInstanceDatasetSubscriptionIds(1, 1).size(), 2);
            assertEquals(context.findSubscription(datasetAddress1), subscription1);
            assertEquals(context.findSubscription(datasetAddress2), subscription2);
            assertEquals(context.findSubscription(datasetAddress3), subscription3);

            Disposable.dispose(subscription2);
            Disposable.dispose(subscription3);

            assertEquals(context.getTypeDatasetSubscriptions().size(), 1);
            assertEquals(context.getInstanceDatasetSubscriptions().size(), 0);
            assertEquals(context.getInstanceDatasetSubscriptionIds(1, 1).size(), 0);
            assertEquals(context.findSubscription(datasetAddress1), subscription1);
            assertNull(context.findSubscription(datasetAddress2));
            assertNull(context.findSubscription(datasetAddress3));
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
        final SystemSchema schema = newSchema();

        createConnector(schema);

        final ReplicantContext context = Replicant.context();

        final int schemaId = schema.getId();
        assertTrue(context.getRuntime().getConnectorEntryBySchemaId(schemaId).isRequired());
        context.setConnectorRequired(schemaId, false);
        assertFalse(context.getRuntime().getConnectorEntryBySchemaId(schemaId).isRequired());
    }

    @SuppressWarnings("ConstantValue")
    @Test
    public void setCacheService() {
        createConnector();

        final ReplicantContext context = Replicant.context();
        final CacheService cacheService = mock(CacheService.class);

        assertNull(context.getCacheService());

        context.setCacheService(cacheService);

        assertEquals(context.getCacheService(), cacheService);

        context.setCacheService(null);

        assertNull(context.getCacheService());
    }

    @Test
    public void exec() {
        final Connector connector = createConnector();
        connector.pauseMessageScheduler();
        final Connection connection = newConnection(connector);

        final String command = ValueUtil.randomString();
        final Object payload = new Object();

        Replicant.context().exec(connector.getSchema().getId(), command, payload, null);

        final List<ExecRequest> requests = connection.getPendingExecRequests();
        assertEquals(requests.size(), 1);
        final ExecRequest request = requests.get(0);
        assertEquals(request.getCommand(), command);
        assertEquals(request.getPayload(), payload);
    }

    @Test
    public void findConnectionId() {
        final Connector connector = createConnector();
        final int schemaId = connector.getSchema().getId();

        assertNull(Replicant.context().findConnectionId(schemaId));

        final Connection connection = newConnection(connector);

        assertEquals(Replicant.context().findConnectionId(schemaId), connection.getConnectionId());
    }

    @Test
    public void registerConnector() {
        safeAction(() ->
                assertEquals(Replicant.context().getRuntime().getConnectors().size(), 0));
        assertEquals(Replicant.context().getSchemas().size(), 0);

        final Disposable disposable = Replicant.context().registerConnector(newSchema(), mock(Transport.class));

        safeAction(() ->
                assertEquals(Replicant.context().getRuntime().getConnectors().size(), 1));
        assertEquals(Replicant.context().getSchemas().size(), 1);

        disposable.dispose();

        safeAction(() ->
                assertEquals(Replicant.context().getRuntime().getConnectors().size(), 0));
        assertEquals(Replicant.context().getSchemas().size(), 0);
    }

    static class A {}

    private static class B {}
}
