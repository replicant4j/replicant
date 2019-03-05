package org.realityforge.replicant.shared;

public final class SharedConstants
{
  /**
   * HTTP request header to indicate the session id.
   */
  public static final String CONNECTION_ID_HEADER = "X-Replicant-SessionID";
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
  public static final String CONNECTION_URL_FRAGMENT = "/session";
  /**
   * The url relative to the session that will generate a request that is returned to the client.
   */
  public static final String PING_URL_FRAGMENT = "/ping";
  /**
   * The url relative to the session that controls the channel subscriptions.
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

  public static final char CHANNEL_ACTION_ADD = '+';
  public static final char CHANNEL_ACTION_REMOVE = '-';
  public static final char CHANNEL_ACTION_UPDATE = '=';
  // Delete indicates the instance channel has been deleted and will never be a valid channel to subscribe to.
  public static final char CHANNEL_ACTION_DELETE = '!';

  private SharedConstants()
  {
  }
}
