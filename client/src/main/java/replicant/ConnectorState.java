package replicant;

import javax.annotation.Nonnull;

/**
 * Enum describing possible states of a Connector.
 */
public enum ConnectorState
{
  /**
   * The service is not yet connected or has been disconnected
   */
  DISCONNECTED,
  /**
   * The service has started connecting but connection has not completed.
   */
  CONNECTING,
  /**
   * The service is connected.
   */
  CONNECTED,
  /**
   * The service has started disconnecting but disconnection has not completed.
   */
  DISCONNECTING,
  /**
   * The service is in error state. This error may occur during connection, disconnection or in normal operation.
   */
  ERROR;

  /**
   * Return true if state is one of <code>DISCONNECTING</code> or <code>CONNECTING</code>.
   *
   * @return true if state is one of <code>DISCONNECTING</code> or <code>CONNECTING</code>.
   */
  public static boolean isTransitionState( @Nonnull final ConnectorState state )
  {
    return DISCONNECTING == state || CONNECTING == state;
  }
}
