package replicant;

import static org.testng.Assert.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.testng.annotations.Test;
import replicant.messages.OkMessage;
import replicant.messages.ServerToClientMessage;
import replicant.messages.UseDatasetCacheEntryMessage;
import replicant.spy.RequestStartedEvent;

public class ConnectionTest extends AbstractReplicantTest {
    @Test
    public void construct() {
        final Connection connection = createConnection();

        assertNull(connection.getCurrentMessageProcessing());
        assertThrows(connection::ensureCurrentMessageProcessing);

        final RequestEntry request = connection.newRequest(ValueUtil.randomString(), false, null);
        final MessageProcessing processing =
                new MessageProcessing(1, OkMessage.create(request.getRequestId()), request);
        connection.setCurrentMessageProcessing(processing);

        assertEquals(connection.getCurrentMessageProcessing(), processing);
        assertEquals(connection.ensureCurrentMessageProcessing(), processing);
    }

    @Test
    public void selectNextMessageProcessing_noMessages() {
        final Connection connection = createConnection();

        assertNull(connection.getCurrentMessageProcessing());

        final boolean selectedMessage = connection.selectNextMessageProcessing();

        assertFalse(selectedMessage);
        assertNull(connection.getCurrentMessageProcessing());
    }

    @Test
    public void selectNextMessageProcessing() {
        final Connection connection = createConnection();

        connection.enqueueMessageForProcessing(
                UseDatasetCacheEntryMessage.create(null, "0", ValueUtil.randomString()), null);

        assertNull(connection.getCurrentMessageProcessing());
        assertEquals(connection.getPendingMessageProcessingQueue().size(), 1);

        final boolean selectedMessage = connection.selectNextMessageProcessing();

        assertTrue(selectedMessage);
        final MessageProcessing currentMessageProcessing = connection.getCurrentMessageProcessing();
        assertNotNull(currentMessageProcessing);
        assertEquals(connection.getPendingMessageProcessingQueue().size(), 0);
    }

    @Test
    public void requestCommand() {
        final Connection connection = createConnection();

        assertEquals(connection.getPendingCommands().size(), 0);

        final String commandName = ValueUtil.randomString();
        final Object payload = new Object();
        connection.requestCommand(commandName, payload, null);

        final List<Command> requests = connection.getPendingCommands();
        assertEquals(requests.size(), 1);
        final Command request = requests.get(0);
        assertEquals(request.getName(), commandName);
        assertEquals(request.getPayload(), payload);
    }

    @Test
    public void requestSubscribe() {
        final Connection connection = createConnection();

        assertEquals(connection.getPendingSubscriptionOperations().size(), 0);

        final DatasetAddress datasetAddress1 = new DatasetAddress(1, 0);
        final DatasetAddress datasetAddress2 = new DatasetAddress(1, 1, 23);
        final String filterParameter1 = ValueUtil.randomString();
        final String filterParameter2 = ValueUtil.randomString();

        connection.requestSubscribe(datasetAddress1, filterParameter1);

        assertEquals(connection.getPendingSubscriptionOperations().size(), 1);

        connection.requestSubscribe(datasetAddress2, filterParameter2);

        assertEquals(connection.getPendingSubscriptionOperations().size(), 2);

        final SubscriptionOperation operation1 =
                connection.getPendingSubscriptionOperations().get(0);
        final SubscriptionOperation operation2 =
                connection.getPendingSubscriptionOperations().get(1);

        assertEquals(operation1.getDatasetAddress(), datasetAddress1);
        assertEquals(operation1.getFilterParameter(), filterParameter1);
        assertEquals(operation1.getType(), SubscriptionOperation.Type.SUBSCRIBE);

        assertEquals(operation2.getDatasetAddress(), datasetAddress2);
        assertEquals(operation2.getFilterParameter(), filterParameter2);
        assertEquals(operation2.getType(), SubscriptionOperation.Type.SUBSCRIBE);
    }

    @Test
    public void requestSubscriptionUpdate() {
        final Connection connection = createConnection();

        assertEquals(connection.getPendingSubscriptionOperations().size(), 0);

        final DatasetAddress datasetAddress1 = new DatasetAddress(1, 0);
        final DatasetAddress datasetAddress2 = new DatasetAddress(1, 1, 23);
        final String filterParameter1 = ValueUtil.randomString();
        final String filterParameter2 = ValueUtil.randomString();

        connection.requestSubscriptionUpdate(datasetAddress1, filterParameter1);

        assertEquals(connection.getPendingSubscriptionOperations().size(), 1);

        connection.requestSubscriptionUpdate(datasetAddress2, filterParameter2);

        assertEquals(connection.getPendingSubscriptionOperations().size(), 2);

        final SubscriptionOperation operation1 =
                connection.getPendingSubscriptionOperations().get(0);
        final SubscriptionOperation operation2 =
                connection.getPendingSubscriptionOperations().get(1);

        assertEquals(operation1.getDatasetAddress(), datasetAddress1);
        assertEquals(operation1.getFilterParameter(), filterParameter1);
        assertEquals(operation1.getType(), SubscriptionOperation.Type.UPDATE);

        assertEquals(operation2.getDatasetAddress(), datasetAddress2);
        assertEquals(operation2.getFilterParameter(), filterParameter2);
        assertEquals(operation2.getType(), SubscriptionOperation.Type.UPDATE);
    }

    @Test
    public void requestUnsubscribe() {
        final Connection connection = createConnection();

        assertEquals(connection.getPendingSubscriptionOperations().size(), 0);

        final DatasetAddress datasetAddress1 = new DatasetAddress(1, 0);
        final DatasetAddress datasetAddress2 = new DatasetAddress(1, 1, 23);

        connection.requestUnsubscribe(datasetAddress1);

        assertEquals(connection.getPendingSubscriptionOperations().size(), 1);

        connection.requestUnsubscribe(datasetAddress2);

        assertEquals(connection.getPendingSubscriptionOperations().size(), 2);

        final SubscriptionOperation operation1 =
                connection.getPendingSubscriptionOperations().get(0);
        final SubscriptionOperation operation2 =
                connection.getPendingSubscriptionOperations().get(1);

        assertEquals(operation1.getDatasetAddress(), datasetAddress1);
        assertNull(operation1.getFilterParameter());
        assertEquals(operation1.getType(), SubscriptionOperation.Type.UNSUBSCRIBE);

        assertEquals(operation2.getDatasetAddress(), datasetAddress2);
        assertNull(operation2.getFilterParameter());
        assertEquals(operation2.getType(), SubscriptionOperation.Type.UNSUBSCRIBE);
    }

    @Test
    public void enqueueMessageForProcessing() {
        final Connection connection = createConnection();

        assertEquals(connection.getPendingSubscriptionOperations().size(), 0);

        final ServerToClientMessage data1 = OkMessage.create(1);
        final ServerToClientMessage data2 = OkMessage.create(1);

        assertEquals(connection.getPendingMessageProcessingQueue().size(), 0);

        connection.enqueueMessageForProcessing(data1, null);

        assertEquals(connection.getPendingMessageProcessingQueue().size(), 1);

        connection.enqueueMessageForProcessing(data2, null);

        assertEquals(connection.getPendingMessageProcessingQueue().size(), 2);

        final MessageProcessing processing1 =
                connection.getPendingMessageProcessingQueue().get(0);
        final MessageProcessing processing2 =
                connection.getPendingMessageProcessingQueue().get(1);

        assertEquals(processing1.getMessage(), data1);
        assertEquals(processing2.getMessage(), data2);
    }

    @Test
    public void basicRequestManagementWorkflow() {
        final Connection connection = createConnection();
        final Connector connector = connection.getConnector();
        final String requestName = ValueUtil.randomString();

        final TestSpyEventHandler handler = registerTestSpyEventHandler();

        final RequestEntry request = connection.newRequest(requestName, false, null);

        assertEquals(request.getName(), requestName);

        handler.assertEventCount(1);
        handler.assertNextEvent(RequestStartedEvent.class, e -> {
            assertEquals(e.getSystemSchemaId(), connector.getSystemSchema().getId());
            assertEquals(e.getRequestId(), request.getRequestId());
            assertEquals(e.getName(), requestName);
        });

        assertEquals(connection.getRequest(request.getRequestId()), request);
        assertEquals(connection.getRequests().get(request.getRequestId()), request);
        assertNull(connection.getRequests().get(ValueUtil.randomInt()));

        connection.removeRequest(request.getRequestId());

        assertNull(connection.getRequests().get(request.getRequestId()));
    }

    @Test
    public void commandLifecycle() {
        final Connection connection = createConnection();

        assertEquals(connection.getActiveCommands().size(), 0);
        assertEquals(connection.getPendingCommands().size(), 0);

        final String commandName = ValueUtil.randomString();
        final Object payload = new Object();

        // Request Command
        {
            connection.requestCommand(commandName, payload, null);

            assertEquals(connection.getActiveCommands().size(), 0);
            final List<Command> requests = connection.getPendingCommands();
            assertEquals(requests.size(), 1);
            final Command request = requests.get(0);
            assertEquals(request.getName(), commandName);
            assertEquals(request.getPayload(), payload);
        }

        {
            final Command request = Objects.requireNonNull(connection.nextCommand());
            assertEquals(request.getName(), commandName);
            assertEquals(request.getPayload(), payload);
            assertEquals(request.getRequestId(), -1);

            assertEquals(connection.getPendingCommands().size(), 0);
            assertEquals(connection.getActiveCommands().size(), 0);

            final int requestId = ValueUtil.randomInt();
            request.markAsInProgress(requestId);

            assertEquals(request.getRequestId(), requestId);

            connection.recordActiveCommand(request);

            assertEquals(connection.getPendingCommands().size(), 0);
            assertEquals(connection.getActiveCommands().size(), 1);
            assertEquals(connection.getActiveCommand(requestId), request);
            assertTrue(request.isInProgress());
            assertEquals(request.getRequestId(), requestId);

            connection.markCommandAsComplete(requestId);

            assertEquals(connection.getPendingCommands().size(), 0);
            assertEquals(connection.getActiveCommands().size(), 0);
            assertNull(connection.getActiveCommand(requestId));
            assertFalse(request.isInProgress());
            assertEquals(request.getRequestId(), -1);
        }

        {
            assertNull(connection.nextCommand());
        }
    }

    @Test
    public void removeRequestWhenNoRequest() {
        final Connection connection = createConnection();

        final IllegalStateException exception =
                expectThrows(IllegalStateException.class, () -> connection.removeRequest(789));
        assertEquals(
                exception.getMessage(),
                "Replicant-0067: Attempted to remove request with id 789 from Replicant Session '"
                        + connection.getReplicantSessionId() + "' but no such request exists.");
    }

    @Test
    public void completeSubscriptionOperation() {
        final Connection connection = createConnection();

        final DatasetAddress datasetAddress1 = new DatasetAddress(1, 0, 1);
        final DatasetAddress datasetAddress2 = new DatasetAddress(1, 0, 2);

        final Object filterParameter1 = null;
        final Object filterParameter2 = null;

        final SubscriptionOperation operation1 =
                new SubscriptionOperation(datasetAddress1, SubscriptionOperation.Type.SUBSCRIBE, filterParameter1);
        final SubscriptionOperation operation2 =
                new SubscriptionOperation(datasetAddress2, SubscriptionOperation.Type.SUBSCRIBE, filterParameter2);
        connection.injectCurrentSubscriptionOperation(operation1);
        connection.injectCurrentSubscriptionOperation(operation2);

        operation1.markAsInProgress(1);
        operation2.markAsInProgress(2);

        assertTrue(operation1.isInProgress());
        assertTrue(operation2.isInProgress());
        assertEquals(connection.getCurrentSubscriptionOperations().size(), 2);

        connection.completeSubscriptionOperation();

        assertFalse(operation1.isInProgress());
        assertFalse(operation2.isInProgress());
        assertEquals(connection.getCurrentSubscriptionOperations().size(), 0);
    }

    @Test
    public void completeSubscriptionOperation_whenNoRequests() {
        final Connection connection = createConnection();

        final IllegalStateException exception =
                expectThrows(IllegalStateException.class, connection::completeSubscriptionOperation);
        assertEquals(
                exception.getMessage(),
                "Replicant-0023: Connection.completeSubscriptionOperation() invoked when there are no current"
                        + " Subscription Operations.");
    }

    @Test
    public void canGroupSubscriptionOperations() {
        final Connection connection = createConnection();

        final DatasetAddress datasetAddressA = new DatasetAddress(1, 0);
        final DatasetAddress datasetAddressB = new DatasetAddress(1, 1, 1);
        final DatasetAddress datasetAddressC = new DatasetAddress(1, 1, 2);
        final DatasetAddress datasetAddressD = new DatasetAddress(1, 1, 3);
        final DatasetAddress datasetAddressE = new DatasetAddress(1, 1, 4);

        final String filterParameterP = null;
        final String filterParameterQ = "F1";
        final String filterParameterR = "F2";

        final SubscriptionOperation operation1 =
                new SubscriptionOperation(datasetAddressA, SubscriptionOperation.Type.SUBSCRIBE, filterParameterP);
        final SubscriptionOperation operation2 =
                new SubscriptionOperation(datasetAddressA, SubscriptionOperation.Type.UNSUBSCRIBE, filterParameterP);
        final SubscriptionOperation operation3 =
                new SubscriptionOperation(datasetAddressA, SubscriptionOperation.Type.UPDATE, filterParameterP);
        final SubscriptionOperation operation4 =
                new SubscriptionOperation(datasetAddressA, SubscriptionOperation.Type.SUBSCRIBE, filterParameterP);

        final SubscriptionOperation operation10 =
                new SubscriptionOperation(datasetAddressB, SubscriptionOperation.Type.SUBSCRIBE, filterParameterQ);
        final SubscriptionOperation operation11 =
                new SubscriptionOperation(datasetAddressC, SubscriptionOperation.Type.SUBSCRIBE, filterParameterQ);
        final SubscriptionOperation operation12 =
                new SubscriptionOperation(datasetAddressD, SubscriptionOperation.Type.UNSUBSCRIBE, null);
        final SubscriptionOperation operation13 =
                new SubscriptionOperation(datasetAddressE, SubscriptionOperation.Type.UNSUBSCRIBE, null);
        final SubscriptionOperation operation14 =
                new SubscriptionOperation(datasetAddressE, SubscriptionOperation.Type.UPDATE, filterParameterQ);
        final SubscriptionOperation operation15 =
                new SubscriptionOperation(datasetAddressE, SubscriptionOperation.Type.SUBSCRIBE, filterParameterP);
        final SubscriptionOperation operation16 =
                new SubscriptionOperation(datasetAddressE, SubscriptionOperation.Type.UPDATE, filterParameterP);
        final SubscriptionOperation operation17 =
                new SubscriptionOperation(datasetAddressE, SubscriptionOperation.Type.UNSUBSCRIBE, null);
        final SubscriptionOperation operation18 =
                new SubscriptionOperation(datasetAddressE, SubscriptionOperation.Type.UPDATE, filterParameterP);
        final SubscriptionOperation operation19 =
                new SubscriptionOperation(datasetAddressE, SubscriptionOperation.Type.UPDATE, filterParameterR);

        final List<SubscriptionOperation> operations = Arrays.asList(
                operation1,
                operation2,
                operation3,
                operation4,
                operation10,
                operation11,
                operation12,
                operation13,
                operation14,
                operation15,
                operation16,
                operation17,
                operation18,
                operation19);

        final HashMap<String, String> groupingPairs = new HashMap<>();

        groupingPairs.put(operation10.toString(), operation11.toString());
        groupingPairs.put(operation12.toString(), operation13.toString());
        groupingPairs.put(operation12.toString(), operation17.toString());
        groupingPairs.put(operation13.toString(), operation17.toString());
        groupingPairs.put(operation16.toString(), operation18.toString());

        for (final SubscriptionOperation r1 : operations) {
            for (final SubscriptionOperation r2 : operations) {
                final boolean expected =
                        (r1 == r2 && null != r1.getDatasetAddress().datasetRootId())
                                || Objects.equals(String.valueOf(groupingPairs.get(r1.toString())), r2.toString())
                                || Objects.equals(String.valueOf(groupingPairs.get(r2.toString())), r1.toString());
                assertEquals(
                        connection.canGroupSubscriptionOperations(r1, r2),
                        expected,
                        "Comparing " + r1 + " versus " + r2);
            }
        }
    }

    @Test
    public void canGroupSubscriptionOperations_presentInCache() {
        final Connection connection = createConnection();

        final DatasetAddress datasetAddressA = new DatasetAddress(1, 1, 1);
        final DatasetAddress datasetAddressB = new DatasetAddress(1, 1, 2);

        final SubscriptionOperation operationA =
                new SubscriptionOperation(datasetAddressA, SubscriptionOperation.Type.SUBSCRIBE, null);
        final SubscriptionOperation operationB =
                new SubscriptionOperation(datasetAddressB, SubscriptionOperation.Type.SUBSCRIBE, null);

        assertTrue(connection.canGroupSubscriptionOperations(operationA, operationB));
        assertTrue(connection.canGroupSubscriptionOperations(operationB, operationA));

        final TestDatasetCacheService datasetCacheService = new TestDatasetCacheService();
        Replicant.context().setDatasetCacheService(datasetCacheService);

        datasetCacheService.storeDatasetCacheEntry(
                datasetAddressA,
                ValueUtil.randomString(),
                replicant.messages.ChangeSetMessage.create(null, null, null, null, null, null));

        assertFalse(connection.canGroupSubscriptionOperations(operationA, operationB));
        assertFalse(connection.canGroupSubscriptionOperations(operationB, operationA));
    }

    @Test
    public void getCurrentSubscriptionOperations() {
        final Connection connection = createConnection();

        final DatasetAddress datasetAddressA = new DatasetAddress(1, 1, 1);
        final DatasetAddress datasetAddressB = new DatasetAddress(1, 1, 2);
        final DatasetAddress datasetAddressC = new DatasetAddress(1, 1, 3);

        assertEquals(connection.getCurrentSubscriptionOperations().size(), 0);

        connection.requestSubscribe(datasetAddressA, null);
        connection.requestSubscribe(datasetAddressB, null);

        assertEquals(connection.getPendingSubscriptionOperations().size(), 2);

        // This should transfer the above two and group them
        assertEquals(connection.getCurrentSubscriptionOperations().size(), 2);

        assertEquals(connection.getPendingSubscriptionOperations().size(), 0);

        // These should all go to pending queue
        connection.requestSubscribe(datasetAddressA, null);
        connection.requestSubscribe(datasetAddressB, null);
        connection.requestSubscribe(datasetAddressC, null);

        assertEquals(connection.getCurrentSubscriptionOperations().size(), 2);

        assertEquals(connection.getPendingSubscriptionOperations().size(), 3);
    }

    @Test
    public void lastIndexOfPendingSubscriptionOperation_passingNonnullFilterParameterForUnsubscribe() {
        final Connection connection = createConnection();

        final DatasetAddress datasetAddress = new DatasetAddress(1, 0);
        final String filterParameter = "MyFilter";

        final IllegalStateException exception = expectThrows(
                IllegalStateException.class,
                () -> connection.lastIndexOfPendingSubscriptionOperation(
                        SubscriptionOperation.Type.UNSUBSCRIBE, datasetAddress, filterParameter));
        assertEquals(
                exception.getMessage(),
                "Replicant-0024: Connection.lastIndexOfPendingSubscriptionOperation passed an UNSUBSCRIBE operation"
                        + " for Dataset Address '1.0' with a non-null Filter Parameter 'MyFilter'.");
    }

    @Test
    public void isSubscriptionOperationPending_passingNonnullFilterParameterForUnsubscribe() {
        final Connection connection = createConnection();

        final DatasetAddress datasetAddress = new DatasetAddress(1, 0);
        final String filterParameter = "MyFilter";

        final IllegalStateException exception = expectThrows(
                IllegalStateException.class,
                () -> connection.isSubscriptionOperationPending(
                        SubscriptionOperation.Type.UNSUBSCRIBE, datasetAddress, filterParameter));
        assertEquals(
                exception.getMessage(),
                "Replicant-0025: Connection.isSubscriptionOperationPending passed an UNSUBSCRIBE operation for Dataset"
                        + " Address '1.0' with a non-null Filter Parameter 'MyFilter'.");
    }

    @Test
    public void pendingSubscriptionOperationQueries_noOperationsInConnection() {
        final Connection connection = createConnection();

        final DatasetAddress datasetAddress1 = new DatasetAddress(1, 0);

        assertRequestPending(connection, SubscriptionOperation.Type.SUBSCRIBE, datasetAddress1, null, false);
        assertRequestPending(connection, SubscriptionOperation.Type.UNSUBSCRIBE, datasetAddress1, null, false);
        assertRequestPending(connection, SubscriptionOperation.Type.UPDATE, datasetAddress1, null, false);
        assertRequestPendingIndex(connection, SubscriptionOperation.Type.SUBSCRIBE, datasetAddress1, null, -1);
        assertRequestPendingIndex(connection, SubscriptionOperation.Type.UNSUBSCRIBE, datasetAddress1, null, -1);
        assertRequestPendingIndex(connection, SubscriptionOperation.Type.UPDATE, datasetAddress1, null, -1);
    }

    @Test
    public void pendingSubscriptionOperationQueries_operationPending() {
        final Connection connection = createConnection();

        final DatasetAddress datasetAddress1 = new DatasetAddress(1, 0, 1);
        final DatasetAddress datasetAddress2 = new DatasetAddress(1, 0, 2);
        final DatasetAddress datasetAddress3 = new DatasetAddress(1, 0, 3);

        final Object filterParameter1 = null;
        final Object filterParameter2 = ValueUtil.randomString();
        final Object filterParameter3 = null;

        assertRequestPendingState(connection, datasetAddress1, filterParameter1, false, false, false, -1, -1, -1);
        assertRequestPendingState(connection, datasetAddress2, filterParameter2, false, false, false, -1, -1, -1);
        assertRequestPendingState(connection, datasetAddress3, filterParameter3, false, false, false, -1, -1, -1);

        connection.requestSubscribe(datasetAddress1, null);

        assertRequestPendingState(connection, datasetAddress1, filterParameter1, true, false, false, 1, -1, -1);
        assertRequestPendingState(connection, datasetAddress2, filterParameter2, false, false, false, -1, -1, -1);
        assertRequestPendingState(connection, datasetAddress3, filterParameter3, false, false, false, -1, -1, -1);

        connection.requestSubscriptionUpdate(datasetAddress2, filterParameter2);

        assertRequestPendingState(connection, datasetAddress1, filterParameter1, true, false, false, 1, -1, -1);
        assertRequestPendingState(connection, datasetAddress2, filterParameter2, false, true, false, -1, 2, -1);
        assertRequestPendingState(connection, datasetAddress3, filterParameter3, false, false, false, -1, -1, -1);

        connection.requestUnsubscribe(datasetAddress3);

        assertRequestPendingState(connection, datasetAddress1, filterParameter1, true, false, false, 1, -1, -1);
        assertRequestPendingState(connection, datasetAddress2, filterParameter2, false, true, false, -1, 2, -1);
        assertRequestPendingState(connection, datasetAddress3, filterParameter3, false, false, true, -1, -1, 3);
    }

    @Test
    public void pendingSubscriptionOperationQueries_currentPending() {
        final Connection connection = createConnection();

        final DatasetAddress datasetAddress1 = new DatasetAddress(1, 0, 1);
        final DatasetAddress datasetAddress2 = new DatasetAddress(1, 0, 2);
        final DatasetAddress datasetAddress3 = new DatasetAddress(1, 0, 3);

        final Object filterParameter1 = null;
        final Object filterParameter2 = null;
        final Object filterParameter3 = null;

        assertRequestPendingState(connection, datasetAddress1, filterParameter1, false, false, false, -1, -1, -1);
        assertRequestPendingState(connection, datasetAddress2, filterParameter2, false, false, false, -1, -1, -1);
        assertRequestPendingState(connection, datasetAddress3, filterParameter3, false, false, false, -1, -1, -1);

        connection.injectCurrentSubscriptionOperation(
                new SubscriptionOperation(datasetAddress1, SubscriptionOperation.Type.SUBSCRIBE, filterParameter1));

        assertRequestPendingState(connection, datasetAddress1, filterParameter1, true, false, false, 0, -1, -1);
        assertRequestPendingState(connection, datasetAddress2, filterParameter2, false, false, false, -1, -1, -1);
        assertRequestPendingState(connection, datasetAddress3, filterParameter3, false, false, false, -1, -1, -1);

        connection.injectCurrentSubscriptionOperation(
                new SubscriptionOperation(datasetAddress2, SubscriptionOperation.Type.SUBSCRIBE, filterParameter2));

        assertRequestPendingState(connection, datasetAddress1, filterParameter1, true, false, false, 0, -1, -1);
        assertRequestPendingState(connection, datasetAddress2, filterParameter2, true, false, false, 0, -1, -1);
        assertRequestPendingState(connection, datasetAddress3, filterParameter3, false, false, false, -1, -1, -1);
    }

    @Test
    public void pendingSubscriptionOperationQueries_jumbledAggregate() {
        final Connection connection = createConnection();

        final DatasetAddress datasetAddress1 = new DatasetAddress(1, 0, 1);
        final DatasetAddress datasetAddress2 = new DatasetAddress(1, 0, 2);
        final DatasetAddress datasetAddress3 = new DatasetAddress(1, 0, 3);
        final DatasetAddress datasetAddress4 = new DatasetAddress(1, 1);

        final Object filterParameter1 = null;
        final Object filterParameter2 = ValueUtil.randomString();
        final Object filterParameter3 = null;
        final Object filterParameter4 = null;

        assertRequestPendingState(connection, datasetAddress1, filterParameter1, false, false, false, -1, -1, -1);
        assertRequestPendingState(connection, datasetAddress2, filterParameter2, false, false, false, -1, -1, -1);
        assertRequestPendingState(connection, datasetAddress3, filterParameter3, false, false, false, -1, -1, -1);
        assertRequestPendingState(connection, datasetAddress4, filterParameter4, false, false, false, -1, -1, -1);

        // Same Dataset Address with multiple chained requests
        connection.requestUnsubscribe(datasetAddress1);
        connection.requestSubscribe(datasetAddress1, filterParameter1);
        connection.requestSubscriptionUpdate(datasetAddress1, filterParameter1);

        // Same Dataset Address - multiple update requests
        connection.requestSubscriptionUpdate(datasetAddress2, filterParameter2);
        connection.requestSubscriptionUpdate(datasetAddress2, filterParameter2);

        // Same Dataset Address - bad requests sequence
        connection.requestUnsubscribe(datasetAddress3);
        connection.requestSubscriptionUpdate(datasetAddress3, filterParameter3);

        // Same Dataset Address - bad requests sequence
        connection.requestUnsubscribe(datasetAddress3);
        connection.requestSubscriptionUpdate(datasetAddress3, filterParameter3);

        // Back to the first Dataset Address
        connection.requestSubscribe(datasetAddress1, filterParameter1);

        connection.injectCurrentSubscriptionOperation(
                new SubscriptionOperation(datasetAddress1, SubscriptionOperation.Type.SUBSCRIBE, filterParameter1));
        connection.injectCurrentSubscriptionOperation(
                new SubscriptionOperation(datasetAddress4, SubscriptionOperation.Type.SUBSCRIBE, filterParameter4));

        assertRequestPendingState(connection, datasetAddress1, filterParameter1, true, true, true, 10, 3, 1);
        assertRequestPendingState(connection, datasetAddress2, filterParameter2, false, true, false, -1, 5, -1);
        assertRequestPendingState(connection, datasetAddress3, filterParameter3, false, true, true, -1, 9, 8);
        assertRequestPendingState(connection, datasetAddress4, filterParameter4, true, false, false, 0, -1, -1);
    }

    private void assertRequestPendingState(
            final Connection connection,
            final DatasetAddress datasetAddress,
            @Nullable final Object filterParameter,
            final boolean hasAdd,
            final boolean hasUpdate,
            final boolean hasRemove,
            final int addIndex,
            final int updateIndex,
            final int removeIndex) {
        assertRequestPending(connection, SubscriptionOperation.Type.SUBSCRIBE, datasetAddress, filterParameter, hasAdd);
        assertRequestPending(connection, SubscriptionOperation.Type.UPDATE, datasetAddress, filterParameter, hasUpdate);
        assertRequestPending(connection, SubscriptionOperation.Type.UNSUBSCRIBE, datasetAddress, null, hasRemove);
        assertRequestPendingIndex(
                connection, SubscriptionOperation.Type.SUBSCRIBE, datasetAddress, filterParameter, addIndex);
        assertRequestPendingIndex(
                connection, SubscriptionOperation.Type.UPDATE, datasetAddress, filterParameter, updateIndex);
        assertRequestPendingIndex(
                connection, SubscriptionOperation.Type.UNSUBSCRIBE, datasetAddress, null, removeIndex);
    }

    private void assertRequestPendingIndex(
            @NonNull final Connection connection,
            final SubscriptionOperation.@NonNull Type type,
            @NonNull final DatasetAddress datasetAddress,
            @Nullable final Object filterParameter,
            final int expected) {
        assertEquals(
                connection.lastIndexOfPendingSubscriptionOperation(type, datasetAddress, filterParameter), expected);
    }

    private void assertRequestPending(
            @NonNull final Connection connection,
            final SubscriptionOperation.@NonNull Type type,
            @NonNull final DatasetAddress datasetAddress,
            @Nullable final Object filterParameter,
            final boolean expected) {
        assertEquals(connection.isSubscriptionOperationPending(type, datasetAddress, filterParameter), expected);
    }
}
