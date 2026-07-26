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
import replicant.messages.UseCacheMessage;
import replicant.spy.RequestStartedEvent;

public class ConnectionTest extends AbstractReplicantTest {
    @Test
    public void construct() {
        final Connection connection = createConnection();

        assertNull(connection.getCurrentMessageResponse());
        assertThrows(connection::ensureCurrentMessageResponse);

        final RequestEntry request = connection.newRequest(ValueUtil.randomString(), false, null);
        final MessageResponse response = new MessageResponse(1, OkMessage.create(request.getRequestId()), request);
        connection.setCurrentMessageResponse(response);

        assertEquals(connection.getCurrentMessageResponse(), response);
        assertEquals(connection.ensureCurrentMessageResponse(), response);
    }

    @Test
    public void selectNextMessageResponse_noMessages() {
        final Connection connection = createConnection();

        assertNull(connection.getCurrentMessageResponse());

        final boolean selectedMessage = connection.selectNextMessageResponse();

        assertFalse(selectedMessage);
        assertNull(connection.getCurrentMessageResponse());
    }

    @Test
    public void selectNextMessageResponse() {
        final Connection connection = createConnection();

        connection.enqueueResponse(UseCacheMessage.create(null, "0", ValueUtil.randomString()), null);

        assertNull(connection.getCurrentMessageResponse());
        assertEquals(connection.getPendingResponses().size(), 1);

        final boolean selectedMessage = connection.selectNextMessageResponse();

        assertTrue(selectedMessage);
        final MessageResponse currentMessageResponse = connection.getCurrentMessageResponse();
        assertNotNull(currentMessageResponse);
        assertEquals(connection.getPendingResponses().size(), 0);
    }

    @Test
    public void requestExec() {
        final Connection connection = createConnection();

        assertEquals(connection.getPendingExecRequests().size(), 0);

        final String command = ValueUtil.randomString();
        final Object payload = new Object();
        connection.requestExec(command, payload, null);

        final List<ExecRequest> requests = connection.getPendingExecRequests();
        assertEquals(requests.size(), 1);
        final ExecRequest request = requests.get(0);
        assertEquals(request.getCommand(), command);
        assertEquals(request.getPayload(), payload);
    }

    @Test
    public void requestSubscribe() {
        final Connection connection = createConnection();

        assertEquals(connection.getPendingAreaOfInterestRequests().size(), 0);

        final DatasetAddress datasetAddress1 = new DatasetAddress(1, 0);
        final DatasetAddress datasetAddress2 = new DatasetAddress(1, 1, 23);
        final String filterParameter1 = ValueUtil.randomString();
        final String filterParameter2 = ValueUtil.randomString();

        connection.requestSubscribe(datasetAddress1, filterParameter1);

        assertEquals(connection.getPendingAreaOfInterestRequests().size(), 1);

        connection.requestSubscribe(datasetAddress2, filterParameter2);

        assertEquals(connection.getPendingAreaOfInterestRequests().size(), 2);

        final AreaOfInterestRequest request1 =
                connection.getPendingAreaOfInterestRequests().get(0);
        final AreaOfInterestRequest request2 =
                connection.getPendingAreaOfInterestRequests().get(1);

        assertEquals(request1.getDatasetAddress(), datasetAddress1);
        assertEquals(request1.getFilterParameter(), filterParameter1);
        assertEquals(request1.getType(), AreaOfInterestRequest.Type.ADD);

        assertEquals(request2.getDatasetAddress(), datasetAddress2);
        assertEquals(request2.getFilterParameter(), filterParameter2);
        assertEquals(request2.getType(), AreaOfInterestRequest.Type.ADD);
    }

    @Test
    public void requestSubscriptionUpdate() {
        final Connection connection = createConnection();

        assertEquals(connection.getPendingAreaOfInterestRequests().size(), 0);

        final DatasetAddress datasetAddress1 = new DatasetAddress(1, 0);
        final DatasetAddress datasetAddress2 = new DatasetAddress(1, 1, 23);
        final String filterParameter1 = ValueUtil.randomString();
        final String filterParameter2 = ValueUtil.randomString();

        connection.requestSubscriptionUpdate(datasetAddress1, filterParameter1);

        assertEquals(connection.getPendingAreaOfInterestRequests().size(), 1);

        connection.requestSubscriptionUpdate(datasetAddress2, filterParameter2);

        assertEquals(connection.getPendingAreaOfInterestRequests().size(), 2);

        final AreaOfInterestRequest request1 =
                connection.getPendingAreaOfInterestRequests().get(0);
        final AreaOfInterestRequest request2 =
                connection.getPendingAreaOfInterestRequests().get(1);

        assertEquals(request1.getDatasetAddress(), datasetAddress1);
        assertEquals(request1.getFilterParameter(), filterParameter1);
        assertEquals(request1.getType(), AreaOfInterestRequest.Type.UPDATE);

        assertEquals(request2.getDatasetAddress(), datasetAddress2);
        assertEquals(request2.getFilterParameter(), filterParameter2);
        assertEquals(request2.getType(), AreaOfInterestRequest.Type.UPDATE);
    }

    @Test
    public void requestUnsubscribe() {
        final Connection connection = createConnection();

        assertEquals(connection.getPendingAreaOfInterestRequests().size(), 0);

        final DatasetAddress datasetAddress1 = new DatasetAddress(1, 0);
        final DatasetAddress datasetAddress2 = new DatasetAddress(1, 1, 23);

        connection.requestUnsubscribe(datasetAddress1);

        assertEquals(connection.getPendingAreaOfInterestRequests().size(), 1);

        connection.requestUnsubscribe(datasetAddress2);

        assertEquals(connection.getPendingAreaOfInterestRequests().size(), 2);

        final AreaOfInterestRequest request1 =
                connection.getPendingAreaOfInterestRequests().get(0);
        final AreaOfInterestRequest request2 =
                connection.getPendingAreaOfInterestRequests().get(1);

        assertEquals(request1.getDatasetAddress(), datasetAddress1);
        assertNull(request1.getFilterParameter());
        assertEquals(request1.getType(), AreaOfInterestRequest.Type.REMOVE);

        assertEquals(request2.getDatasetAddress(), datasetAddress2);
        assertNull(request2.getFilterParameter());
        assertEquals(request2.getType(), AreaOfInterestRequest.Type.REMOVE);
    }

    @Test
    public void enqueueResponse() {
        final Connection connection = createConnection();

        assertEquals(connection.getPendingAreaOfInterestRequests().size(), 0);

        final ServerToClientMessage data1 = OkMessage.create(1);
        final ServerToClientMessage data2 = OkMessage.create(1);

        assertEquals(connection.getPendingResponses().size(), 0);

        connection.enqueueResponse(data1, null);

        assertEquals(connection.getPendingResponses().size(), 1);

        connection.enqueueResponse(data2, null);

        assertEquals(connection.getPendingResponses().size(), 2);

        final MessageResponse response1 = connection.getPendingResponses().get(0);
        final MessageResponse response2 = connection.getPendingResponses().get(1);

        assertEquals(response1.getMessage(), data1);
        assertEquals(response2.getMessage(), data2);
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
            assertEquals(e.getSchemaId(), connector.getSchema().getId());
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
    public void execLifecycle() {
        final Connection connection = createConnection();

        assertEquals(connection.getActiveExecRequests().size(), 0);
        assertEquals(connection.getPendingExecRequests().size(), 0);

        final String command = ValueUtil.randomString();
        final Object payload = new Object();

        // Request Exec
        {
            connection.requestExec(command, payload, null);

            assertEquals(connection.getActiveExecRequests().size(), 0);
            final List<ExecRequest> requests = connection.getPendingExecRequests();
            assertEquals(requests.size(), 1);
            final ExecRequest request = requests.get(0);
            assertEquals(request.getCommand(), command);
            assertEquals(request.getPayload(), payload);
        }

        {
            final ExecRequest request = Objects.requireNonNull(connection.nextExecRequest());
            assertEquals(request.getCommand(), command);
            assertEquals(request.getPayload(), payload);
            assertEquals(request.getRequestId(), -1);

            assertEquals(connection.getPendingExecRequests().size(), 0);
            assertEquals(connection.getActiveExecRequests().size(), 0);

            final int requestId = ValueUtil.randomInt();
            request.markAsInProgress(requestId);

            assertEquals(request.getRequestId(), requestId);

            connection.recordActiveExecRequest(request);

            assertEquals(connection.getPendingExecRequests().size(), 0);
            assertEquals(connection.getActiveExecRequests().size(), 1);
            assertEquals(connection.getActiveExecRequest(requestId), request);
            assertTrue(request.isInProgress());
            assertEquals(request.getRequestId(), requestId);

            connection.markExecRequestAsComplete(requestId);

            assertEquals(connection.getPendingExecRequests().size(), 0);
            assertEquals(connection.getActiveExecRequests().size(), 0);
            assertNull(connection.getActiveExecRequest(requestId));
            assertFalse(request.isInProgress());
            assertEquals(request.getRequestId(), -1);
        }

        {
            assertNull(connection.nextExecRequest());
        }
    }

    @Test
    public void removeRequestWhenNoRequest() {
        final Connection connection = createConnection();

        final IllegalStateException exception =
                expectThrows(IllegalStateException.class, () -> connection.removeRequest(789));
        assertEquals(
                exception.getMessage(),
                "Replicant-0067: Attempted to remove request with id 789 from connection with id '"
                        + connection.getConnectionId() + "' but no such request exists.");
    }

    @Test
    public void completeAreaOfInterestRequest() {
        final Connection connection = createConnection();

        final DatasetAddress datasetAddress1 = new DatasetAddress(1, 0, 1);
        final DatasetAddress datasetAddress2 = new DatasetAddress(1, 0, 2);

        final Object filterParameter1 = null;
        final Object filterParameter2 = null;

        final AreaOfInterestRequest request1 =
                new AreaOfInterestRequest(datasetAddress1, AreaOfInterestRequest.Type.ADD, filterParameter1);
        final AreaOfInterestRequest request2 =
                new AreaOfInterestRequest(datasetAddress2, AreaOfInterestRequest.Type.ADD, filterParameter2);
        connection.injectCurrentAreaOfInterestRequest(request1);
        connection.injectCurrentAreaOfInterestRequest(request2);

        request1.markAsInProgress(1);
        request2.markAsInProgress(2);

        assertTrue(request1.isInProgress());
        assertTrue(request2.isInProgress());
        assertEquals(connection.getCurrentAreaOfInterestRequests().size(), 2);

        connection.completeAreaOfInterestRequest();

        assertFalse(request1.isInProgress());
        assertFalse(request2.isInProgress());
        assertEquals(connection.getCurrentAreaOfInterestRequests().size(), 0);
    }

    @Test
    public void completeAreaOfInterestRequest_whenNoRequests() {
        final Connection connection = createConnection();

        final IllegalStateException exception =
                expectThrows(IllegalStateException.class, connection::completeAreaOfInterestRequest);
        assertEquals(
                exception.getMessage(),
                "Replicant-0023: Connection.completeAreaOfInterestRequest() invoked when there is no current"
                        + " AreaOfInterest requests.");
    }

    @Test
    public void canGroupRequests() {
        final Connection connection = createConnection();

        final DatasetAddress datasetAddressA = new DatasetAddress(1, 0);
        final DatasetAddress datasetAddressB = new DatasetAddress(1, 1, 1);
        final DatasetAddress datasetAddressC = new DatasetAddress(1, 1, 2);
        final DatasetAddress datasetAddressD = new DatasetAddress(1, 1, 3);
        final DatasetAddress datasetAddressE = new DatasetAddress(1, 1, 4);

        final String filterParameterP = null;
        final String filterParameterQ = "F1";
        final String filterParameterR = "F2";

        final AreaOfInterestRequest request1 =
                new AreaOfInterestRequest(datasetAddressA, AreaOfInterestRequest.Type.ADD, filterParameterP);
        final AreaOfInterestRequest request2 =
                new AreaOfInterestRequest(datasetAddressA, AreaOfInterestRequest.Type.REMOVE, filterParameterP);
        final AreaOfInterestRequest request3 =
                new AreaOfInterestRequest(datasetAddressA, AreaOfInterestRequest.Type.UPDATE, filterParameterP);
        final AreaOfInterestRequest request4 =
                new AreaOfInterestRequest(datasetAddressA, AreaOfInterestRequest.Type.ADD, filterParameterP);

        final AreaOfInterestRequest request10 =
                new AreaOfInterestRequest(datasetAddressB, AreaOfInterestRequest.Type.ADD, filterParameterQ);
        final AreaOfInterestRequest request11 =
                new AreaOfInterestRequest(datasetAddressC, AreaOfInterestRequest.Type.ADD, filterParameterQ);
        final AreaOfInterestRequest request12 =
                new AreaOfInterestRequest(datasetAddressD, AreaOfInterestRequest.Type.REMOVE, null);
        final AreaOfInterestRequest request13 =
                new AreaOfInterestRequest(datasetAddressE, AreaOfInterestRequest.Type.REMOVE, null);
        final AreaOfInterestRequest request14 =
                new AreaOfInterestRequest(datasetAddressE, AreaOfInterestRequest.Type.UPDATE, filterParameterQ);
        final AreaOfInterestRequest request15 =
                new AreaOfInterestRequest(datasetAddressE, AreaOfInterestRequest.Type.ADD, filterParameterP);
        final AreaOfInterestRequest request16 =
                new AreaOfInterestRequest(datasetAddressE, AreaOfInterestRequest.Type.UPDATE, filterParameterP);
        final AreaOfInterestRequest request17 =
                new AreaOfInterestRequest(datasetAddressE, AreaOfInterestRequest.Type.REMOVE, null);
        final AreaOfInterestRequest request18 =
                new AreaOfInterestRequest(datasetAddressE, AreaOfInterestRequest.Type.UPDATE, filterParameterP);
        final AreaOfInterestRequest request19 =
                new AreaOfInterestRequest(datasetAddressE, AreaOfInterestRequest.Type.UPDATE, filterParameterR);

        final List<AreaOfInterestRequest> requests = Arrays.asList(
                request1, request2, request3, request4, request10, request11, request12, request13, request14,
                request15, request16, request17, request18, request19);

        final HashMap<String, String> groupingPairs = new HashMap<>();

        groupingPairs.put(request10.toString(), request11.toString());
        groupingPairs.put(request12.toString(), request13.toString());
        groupingPairs.put(request12.toString(), request17.toString());
        groupingPairs.put(request13.toString(), request17.toString());
        groupingPairs.put(request16.toString(), request18.toString());

        for (final AreaOfInterestRequest r1 : requests) {
            for (final AreaOfInterestRequest r2 : requests) {
                final boolean expected =
                        (r1 == r2 && null != r1.getDatasetAddress().datasetRootId())
                                || Objects.equals(String.valueOf(groupingPairs.get(r1.toString())), r2.toString())
                                || Objects.equals(String.valueOf(groupingPairs.get(r2.toString())), r1.toString());
                assertEquals(connection.canGroupRequests(r1, r2), expected, "Comparing " + r1 + " versus " + r2);
            }
        }
    }

    @Test
    public void canGroupRequests_presentInCache() {
        final Connection connection = createConnection();

        final DatasetAddress datasetAddressA = new DatasetAddress(1, 1, 1);
        final DatasetAddress datasetAddressB = new DatasetAddress(1, 1, 2);

        final AreaOfInterestRequest requestA =
                new AreaOfInterestRequest(datasetAddressA, AreaOfInterestRequest.Type.ADD, null);
        final AreaOfInterestRequest requestB =
                new AreaOfInterestRequest(datasetAddressB, AreaOfInterestRequest.Type.ADD, null);

        assertTrue(connection.canGroupRequests(requestA, requestB));
        assertTrue(connection.canGroupRequests(requestB, requestA));

        final TestCacheService cacheService = new TestCacheService();
        Replicant.context().setCacheService(cacheService);

        cacheService.store(datasetAddressA, ValueUtil.randomString(), ValueUtil.randomString());

        assertFalse(connection.canGroupRequests(requestA, requestB));
        assertFalse(connection.canGroupRequests(requestB, requestA));
    }

    @Test
    public void getCurrentAreaOfInterestRequests() {
        final Connection connection = createConnection();

        final DatasetAddress datasetAddressA = new DatasetAddress(1, 1, 1);
        final DatasetAddress datasetAddressB = new DatasetAddress(1, 1, 2);
        final DatasetAddress datasetAddressC = new DatasetAddress(1, 1, 3);

        assertEquals(connection.getCurrentAreaOfInterestRequests().size(), 0);

        connection.requestSubscribe(datasetAddressA, null);
        connection.requestSubscribe(datasetAddressB, null);

        assertEquals(connection.getPendingAreaOfInterestRequests().size(), 2);

        // This should transfer the above two and group them
        assertEquals(connection.getCurrentAreaOfInterestRequests().size(), 2);

        assertEquals(connection.getPendingAreaOfInterestRequests().size(), 0);

        // These should all go to pending queue
        connection.requestSubscribe(datasetAddressA, null);
        connection.requestSubscribe(datasetAddressB, null);
        connection.requestSubscribe(datasetAddressC, null);

        assertEquals(connection.getCurrentAreaOfInterestRequests().size(), 2);

        assertEquals(connection.getPendingAreaOfInterestRequests().size(), 3);
    }

    @Test
    public void lastIndexOfPendingAreaOfInterestRequest_passingNonnullFilterParameterForDelete() {
        final Connection connection = createConnection();

        final DatasetAddress datasetAddress = new DatasetAddress(1, 0);
        final String filterParameter = "MyFilter";

        final IllegalStateException exception = expectThrows(
                IllegalStateException.class,
                () -> connection.lastIndexOfPendingAreaOfInterestRequest(
                        AreaOfInterestRequest.Type.REMOVE, datasetAddress, filterParameter));
        assertEquals(
                exception.getMessage(),
                "Replicant-0024: Connection.lastIndexOfPendingAreaOfInterestRequest passed a REMOVE request for"
                        + " Dataset Address '1.0' with a non-null Filter Parameter 'MyFilter'.");
    }

    @Test
    public void isAreaOfInterestRequestPending_passingNonnullFilterParameterForDelete() {
        final Connection connection = createConnection();

        final DatasetAddress datasetAddress = new DatasetAddress(1, 0);
        final String filterParameter = "MyFilter";

        final IllegalStateException exception = expectThrows(
                IllegalStateException.class,
                () -> connection.isAreaOfInterestRequestPending(
                        AreaOfInterestRequest.Type.REMOVE, datasetAddress, filterParameter));
        assertEquals(
                exception.getMessage(),
                "Replicant-0025: Connection.isAreaOfInterestRequestPending passed a REMOVE request for Dataset Address"
                        + " '1.0' with a non-null Filter Parameter 'MyFilter'.");
    }

    @Test
    public void pendingAreaOfInterestRequestQueries_noRequestsInConnection() {
        final Connection connection = createConnection();

        final DatasetAddress datasetAddress1 = new DatasetAddress(1, 0);

        assertRequestPending(connection, AreaOfInterestRequest.Type.ADD, datasetAddress1, null, false);
        assertRequestPending(connection, AreaOfInterestRequest.Type.REMOVE, datasetAddress1, null, false);
        assertRequestPending(connection, AreaOfInterestRequest.Type.UPDATE, datasetAddress1, null, false);
        assertRequestPendingIndex(connection, AreaOfInterestRequest.Type.ADD, datasetAddress1, null, -1);
        assertRequestPendingIndex(connection, AreaOfInterestRequest.Type.REMOVE, datasetAddress1, null, -1);
        assertRequestPendingIndex(connection, AreaOfInterestRequest.Type.UPDATE, datasetAddress1, null, -1);
    }

    @Test
    public void pendingAreaOfInterestRequestQueries_requestPending() {
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
    public void pendingAreaOfInterestRequestQueries_currentPending() {
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

        connection.injectCurrentAreaOfInterestRequest(
                new AreaOfInterestRequest(datasetAddress1, AreaOfInterestRequest.Type.ADD, filterParameter1));

        assertRequestPendingState(connection, datasetAddress1, filterParameter1, true, false, false, 0, -1, -1);
        assertRequestPendingState(connection, datasetAddress2, filterParameter2, false, false, false, -1, -1, -1);
        assertRequestPendingState(connection, datasetAddress3, filterParameter3, false, false, false, -1, -1, -1);

        connection.injectCurrentAreaOfInterestRequest(
                new AreaOfInterestRequest(datasetAddress2, AreaOfInterestRequest.Type.ADD, filterParameter2));

        assertRequestPendingState(connection, datasetAddress1, filterParameter1, true, false, false, 0, -1, -1);
        assertRequestPendingState(connection, datasetAddress2, filterParameter2, true, false, false, 0, -1, -1);
        assertRequestPendingState(connection, datasetAddress3, filterParameter3, false, false, false, -1, -1, -1);
    }

    @Test
    public void pendingAreaOfInterestRequestQueries_jumbledAggregate() {
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

        connection.injectCurrentAreaOfInterestRequest(
                new AreaOfInterestRequest(datasetAddress1, AreaOfInterestRequest.Type.ADD, filterParameter1));
        connection.injectCurrentAreaOfInterestRequest(
                new AreaOfInterestRequest(datasetAddress4, AreaOfInterestRequest.Type.ADD, filterParameter4));

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
        assertRequestPending(connection, AreaOfInterestRequest.Type.ADD, datasetAddress, filterParameter, hasAdd);
        assertRequestPending(connection, AreaOfInterestRequest.Type.UPDATE, datasetAddress, filterParameter, hasUpdate);
        assertRequestPending(connection, AreaOfInterestRequest.Type.REMOVE, datasetAddress, null, hasRemove);
        assertRequestPendingIndex(
                connection, AreaOfInterestRequest.Type.ADD, datasetAddress, filterParameter, addIndex);
        assertRequestPendingIndex(
                connection, AreaOfInterestRequest.Type.UPDATE, datasetAddress, filterParameter, updateIndex);
        assertRequestPendingIndex(connection, AreaOfInterestRequest.Type.REMOVE, datasetAddress, null, removeIndex);
    }

    private void assertRequestPendingIndex(
            @NonNull final Connection connection,
            final AreaOfInterestRequest.@NonNull Type action,
            @NonNull final DatasetAddress datasetAddress,
            @Nullable final Object filterParameter,
            final int expected) {
        assertEquals(
                connection.lastIndexOfPendingAreaOfInterestRequest(action, datasetAddress, filterParameter), expected);
    }

    private void assertRequestPending(
            @NonNull final Connection connection,
            final AreaOfInterestRequest.@NonNull Type action,
            @NonNull final DatasetAddress datasetAddress,
            @Nullable final Object filterParameter,
            final boolean expected) {
        assertEquals(connection.isAreaOfInterestRequestPending(action, datasetAddress, filterParameter), expected);
    }
}
