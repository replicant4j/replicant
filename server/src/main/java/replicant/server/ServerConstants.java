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
     * Key used to retrieve the Replicant Session ID from the ReplicantRequestContextHolder.
     * Used to pass data from the servlet to the EJB.
     */
    public static final String REPLICANT_SESSION_ID_KEY = "ReplicantSessionId";
    /**
     * Key used to retrieve an opaque identifier for the request from the ReplicantRequestContextHolder.
     * Used to pass data from the servlet to the EJB.
     */
    public static final String REQUEST_ID_KEY = "RequestID";
    /**
     * Key used to retrieve a flag indicating whether the request produced a Change Set relevant to the initiating
     * Replicant Session.
     * Used to pass data from the EJB to the servlet.
     */
    public static final String REQUEST_COMPLETE_KEY = "RequestComplete";
    /**
     * Key used to retrieve the encoded Command Result relevant to the initiating Replicant Session.
     * Used to pass data from the EJB to the servlet.
     */
    public static final String COMMAND_RESULT_KEY = "CommandResult";
    /**
     * Key used to flag that a Cacheable Dataset Change Set or use-dataset-cache-entry message has been queued.
     * This means there should be no other changes in the Initiating Session Change Set and it should be marked as not
     * required.
     */
    public static final String DATASET_CACHE_ENTRY_HANDLED_KEY = "DatasetCacheEntryHandled";
    /**
     * Key used to flag that an action is subscription.
     * This means there are zero changes in the Initiating Session Change Set.
     */
    public static final String SUBSCRIPTION_REQUEST_KEY = "SubscriptionRequest";

    private ServerConstants() {}
}
