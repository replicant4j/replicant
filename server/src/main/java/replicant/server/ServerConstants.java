package replicant.server;

/**
 * Server-only constants.
 */
public final class ServerConstants {
    /**
     * Key added to the request context during a Replication Invocation.
     * Used to ensure that at most one Replication Invocation is active.
     */
    public static final String REPLICATION_INVOCATION_KEY = "ReplicationInvocation";
    /**
     * Key used to retrieve an opaque identifier for the session from the ReplicantRequestContextHolder.
     * Used to pass data from the servlet to the EJB.
     */
    public static final String SESSION_ID_KEY = "SessionID";
    /**
     * Key used to retrieve an opaque identifier for the request from the ReplicantRequestContextHolder.
     * Used to pass data from the servlet to the EJB.
     */
    public static final String REQUEST_ID_KEY = "RequestID";
    /**
     * Key used to retrieve a flag whether the request produced a changeset relevant for the initiating session.
     * Used to pass data from the EJB to the servlet.
     */
    public static final String REQUEST_COMPLETE_KEY = "RequestComplete";
    /**
     * Key used to retrieve the encoded response for the request relevant for the initiating session.
     * Used to pass data from the EJB to the servlet.
     */
    public static final String REQUEST_RESPONSE_KEY = "RequestResponse";
    /**
     * Key used to flag that a Cacheable Dataset Change Set or use-dataset-cache-entry message has been queued.
     * This means there should be no other changes in the session Change Set and it should be marked as not required.
     */
    public static final String DATASET_CACHE_ENTRY_HANDLED_KEY = "DatasetCacheEntryHandled";
    /**
     * Key used to flag that an action is subscription.
     * This means there are ZERO changes in session changeset.
     */
    public static final String SUBSCRIPTION_REQUEST_KEY = "SubscriptionRequest";

    private ServerConstants() {}
}
