package org.realityforge.replicant.shared.transport;

public final class ReplicantContext
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
   * Key used to retrieve a flag whether the request produced a changeset relevant for the initiating session..
   * Used to pass data from the EJB to the servlet.
   */
  public static final String REQUEST_COMPLETE_KEY = "RequestComplete";

  /**
   * HTTP request header to indicate the session id.
   */
  public static final String SESSION_ID_HEADER = "X-Replicant-SessionID";
  /**
   * HTTP request header to indicate the request id.
   */
  public static final String REQUEST_ID_HEADER = "X-Replicant-RequestID";
  /**
   * HTTP request header to indicate the cache key if any.
   */
  public static final String ETAG_HEADER = "X-Replicant-ETag";
  /**
   * HTTP response header to indicate the whether the request is complete or a change set is expected.
   */
  public static final String REQUEST_COMPLETE_HEADER = "X-Replicant-RequestComplete";
  /**
   * The url relative to base from which replicant event stream data is retrieved.
   */
  public static final String REPLICANT_URL_FRAGMENT = "/replicant";
  /**
   * The url relative to base from which initial token is generated and the replicant session created.
   */
  public static final String SESSION_URL_FRAGMENT = "/session";
  /**
   * The url relative to the session from which controls the channel subscriptions.
   */
  public static final String CHANNEL_URL_FRAGMENT = "/channel";
  /**
   * The query parameter used to identify the last received packet sequence.
   */
  public static final String RECEIVE_SEQUENCE_PARAM = "rx";
  /**
   * The query parameter used to identify a sub-channel id on a bulk change url.
   */
  public static final String SUB_CHANNEL_ID_PARAM = "scid";

  /**
   * The duration of the long polls before returning in seconds.
   */
  public static final int MAX_POLL_TIME_IN_SECONDS = 30;

  private ReplicantContext()
  {
  }
}
