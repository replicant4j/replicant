package org.realityforge.replicant.server.transport;

import javax.annotation.Nonnull;
import javax.json.stream.JsonGenerator;
import org.realityforge.rest.field_filter.FieldFilter;

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
  public abstract void emitStatus( @Nonnull final JsonGenerator g, @Nonnull FieldFilter filter );
}
