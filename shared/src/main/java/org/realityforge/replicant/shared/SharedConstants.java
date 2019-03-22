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
   * HTTP response header to indicate the whether the request is complete or a change set is expected.
   */
  public static final String REQUEST_COMPLETE_HEADER = "X-Replicant-RequestComplete";
  /**
   * The url relative to base from which replicant event stream data is retrieved.
   */
  @SuppressWarnings( "unused" )
  public static final String REPLICANT_URL_FRAGMENT = "/replicant";
  /**
   * The url relative to base from which initial token is generated and the replicant session created.
   */
  public static final String CONNECTION_URL_FRAGMENT = "/session";
  /**
   * The url relative to the session that controls the channel subscriptions.
   */
  public static final String CHANNEL_URL_FRAGMENT = "/channel";

  public static final char CHANNEL_ACTION_ADD = '+';
  public static final char CHANNEL_ACTION_REMOVE = '-';
  public static final char CHANNEL_ACTION_UPDATE = '=';
  // Delete indicates the instance channel has been deleted and will never be a valid channel to subscribe to.
  public static final char CHANNEL_ACTION_DELETE = '!';

  private SharedConstants()
  {
  }
}
