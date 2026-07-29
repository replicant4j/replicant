package replicant;

/**
 * The aggregate connection lifecycle state of a Replicant Context.
 */
public enum ReplicantContextState {
    /**
     * The Replicant Context is not yet connected or has been disconnected.
     */
    DISCONNECTED,
    /**
     * The Replicant Context has started connecting but connection has not completed.
     */
    CONNECTING,
    /**
     * The Replicant Context is connected.
     */
    CONNECTED,
    /**
     * The Replicant Context has started disconnecting but disconnection has not completed.
     */
    DISCONNECTING,
    /**
     * The Replicant Context is in an error state. This error may occur during connection, disconnection or normal
     * operation.
     */
    ERROR,
    /**
     * The Replicant Context is in a fatal error state. The client should not attempt a retry
     * until the error has been addressed. This often indicates a security error.
     */
    FATAL_ERROR
}
