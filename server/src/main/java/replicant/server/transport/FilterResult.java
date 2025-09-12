package replicant.server.transport;

/**
 * A enum controlling thw way we filter EntityMessage.
 */
public enum FilterResult
{
  /**
   * Message should be kept
   */
  KEEP,
  /**
   * Message should be kept but translated into a "DELETE" as the entity has moved out of scope
   */
  DELETE,
  /**
   * Message should be filtered out
   */
  DISCARD
}
