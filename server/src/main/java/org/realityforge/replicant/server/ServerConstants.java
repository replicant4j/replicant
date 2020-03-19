package org.realityforge.replicant.server;

/**
 * Server-only constants.
 */
public final class ServerConstants
{
  /**
   * Key added to the context when passing through replication method.
   * Used to ensure that there can be at most one replication context active.
   */
  public static final String REPLICATION_INVOCATION_KEY = "ReplicationActive";
  /**
   * Key used to retrieve an opaque identifier for the session from the ReplicantContextHolder.
   * Used to pass data from the servlet to the EJB.
   */
  public static final String SESSION_ID_KEY = "SessionID";
  /**
   * Key used to retrieve an opaque identifier for the request from the ReplicantContextHolder.
   * Used to pass data from the servlet to the EJB.
   */
  public static final String REQUEST_ID_KEY = "RequestID";
  /**
   * Key used to retrieve a flag whether the request produced a changeset relevant for the initiating session.
   * Used to pass data from the EJB to the servlet.
   */
  public static final String REQUEST_COMPLETE_KEY = "RequestComplete";
  /**
   * Key used to flag that a cached result has been sent or that we send a use-cache message.
   * This means that there should be ZERO changes in session changeset and it should be marked as not required.
   */
  public static final String CACHED_RESULT_HANDLED_KEY = "CachedResultSent";
  /**
   * Key used to flag that an action is subscription.
   * This means there are ZERO changes in session changeset.
   */
  public static final String SUBSCRIPTION_REQUEST_KEY = "SubscriptionRequest";

  private ServerConstants()
  {
  }
}
