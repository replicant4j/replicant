package replicant;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import replicant.messages.ServerToClientMessage;

public interface TransportContext {
    /**
     * Create a new request abstraction.
     *
     * @param name        the name of the request. This should be null if {@link Replicant#areNamesEnabled()} returns false, otherwise it should be non-null.
     * @param synchronizationPointRequest true if the request establishes a Synchronization Point when processed.
     */
    int newRequestId(
            @Nullable String name,
            boolean synchronizationPointRequest,
            @Nullable CommandResultHandler commandResultHandler);

    /**
     * Notify the Connector that a message was received.
     *
     * @param message the message.
     */
    void onMessageReceived(@NonNull ServerToClientMessage message);

    /**
     * Notify the Connector that there was an error from the Transport.
     */
    void onError();

    /**
     * Notify the Connector that the Transport has disconnected.
     */
    void onDisconnect();
}
