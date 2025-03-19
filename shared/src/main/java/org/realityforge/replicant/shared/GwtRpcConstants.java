package org.realityforge.replicant.shared;

public class GwtRpcConstants
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
}
