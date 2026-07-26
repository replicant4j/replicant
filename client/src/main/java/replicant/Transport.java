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
     * Request a synchronization point.
     * This method talks to the back-end and pings it. If the reply returns and there has been no
     * intermediate requests then the connection is considered synchronized to that point. If there
     * has been requests in the meantime (i.e. the sequence number of sync is not 1 behind) then
     * there is still processing queued on the server (or client).
     */
    void requestSync();

    void updateAuthToken(@Nullable String authToken);

    void updateEtagsSync(@NonNull Map<String, String> datasetAddressToEtagMap);

    void requestExec(@NonNull String command, @Nullable Object payload, @Nullable ResponseHandler responseHandler);

    void requestSubscribe(@NonNull DatasetAddress datasetAddress, @Nullable Object filter);

    void requestUnsubscribe(@NonNull DatasetAddress datasetAddress);

    void requestBulkSubscribe(@NonNull List<DatasetAddress> datasetAddresses, @Nullable Object filter);

    void requestBulkUnsubscribe(@NonNull List<DatasetAddress> datasetAddresses);
}
