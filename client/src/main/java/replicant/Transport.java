package replicant;

import java.util.List;
import java.util.Map;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * The transport is responsible for communicating with the backend system.
 */
public interface Transport {
    /**
     * Perform the connection.
     */
    void requestConnect(@NonNull TransportContext context);

    /**
     * This method is invoked by the Connector when the connection
     * disconnects or there is a fatal error. This method disassociates the connection context bound to the transport
     * via the {@link #requestConnect(TransportContext)} method.
     */
    void unbind();

    /**
     * Request disconnection.
     */
    void requestDisconnect();

    /**
     * Request a Synchronization Point.
     * This method talks to the backend and pings it. If the reply returns and there have been no
     * intermediate requests, the Connector has reached the Synchronization Point. Otherwise there
     * is still processing queued on the server or client.
     */
    void requestSynchronizationPoint();

    void updateAuthToken(@Nullable String authToken);

    /**
     * Send the known Dataset Cache Versions and use the response to establish a Synchronization Point.
     */
    void updateDatasetCacheVersionsAndRequestSynchronizationPoint(
            @NonNull Map<String, String> datasetAddressToDatasetCacheVersionMap);

    /**
     * Send a Command to the server.
     */
    void requestCommand(
            @NonNull String commandName, @Nullable Object payload, @Nullable ResponseHandler responseHandler);

    void requestSubscribe(@NonNull DatasetAddress datasetAddress, @Nullable Object filterParameter);

    void requestUnsubscribe(@NonNull DatasetAddress datasetAddress);

    void requestBulkSubscribe(@NonNull List<DatasetAddress> datasetAddresses, @Nullable Object filterParameter);

    void requestBulkUnsubscribe(@NonNull List<DatasetAddress> datasetAddresses);
}
