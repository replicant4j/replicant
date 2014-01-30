package org.realityforge.replicant.server.transport;

/**
 * Base class for sessions within replicant.
 */
public abstract class ReplicantSession
  extends org.realityforge.ssf.SimpleSessionInfo
{
  private final PacketQueue _queue = new PacketQueue();

  public ReplicantSession( @javax.annotation.Nonnull final String sessionID )
  {
    super( sessionID );
  }

  public final PacketQueue getQueue()
  {
    return _queue;
  }
}
