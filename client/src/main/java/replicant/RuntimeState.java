package replicant;

/**
 * An enum describing the state of the runtime.
 */
public enum RuntimeState
{
  /// The service is not yet connected or has been disconnected
  DISCONNECTED,
  /// The service has started connecting but connection has not completed.
  CONNECTING,
  /// The service is connected.
  CONNECTED,
  /// The service has started disconnecting but disconnection has not completed.
  DISCONNECTING,
  /// The service is in error state. This error may occur during connection, disconnection or in normal operation.
  ERROR
}
