package org.realityforge.replicant.server.transport;

import javax.annotation.Nonnull;
import javax.json.stream.JsonGenerator;
import org.realityforge.rest.field_filter.FieldFilter;
import org.realityforge.ssf.SimpleSessionInfo;

/**
 * Base class for sessions within replicant.
 */
public abstract class ReplicantSession
  extends SimpleSessionInfo
{
  private final PacketQueue _queue = new PacketQueue();

  public ReplicantSession( @Nonnull final String sessionID )
  {
    super( sessionID );
  }

  @Nonnull
  public final PacketQueue getQueue()
  {
    return _queue;
  }

  /**
   * Emit replication status of the session.
   * This should include details of subscriptions.
   *
   * @param g      the json generator in which to serialize details.
   * @param filter the filter to use when determining which details to emit.
   */
}
