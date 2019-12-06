package org.realityforge.replicant.server;

public final class ServerConstants
{
  /**
   * Key used to retrieve an opaque identifier for the session from the ReplicantContextHolder.
   * Used to pass data from the servlet to the EJB.
   */
  public static final String SESSION_ID_KEY = "SessionID";
  /**
   * Key used to retrieve an opaque identifier for the request from the ReplicantContextHolder.
   * Used to pass data from the servlet to the EJB.
   */
  public static final String REQUEST_ID_KEY = "RequestId";
  /**
   * Key used to retrieve a flag whether the request produced a changeset relevant for the initiating session..
   * Used to pass data from the EJB to the servlet.
   */
  public static final String REQUEST_COMPLETE_KEY = "RequestComplete";

  private ServerConstants()
  {
  }
}
