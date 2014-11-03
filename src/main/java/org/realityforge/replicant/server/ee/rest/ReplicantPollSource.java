package org.realityforge.replicant.server.ee.rest;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Interface implemented by the service responsible
 * for providing data to the clients via polling.
 */
public interface ReplicantPollSource
{
  /**
   * Check if there is any data for specified session and return data if any immediately.
   * This method should not block.
   *
   * @param sessionID  the session identifier.
   * @param rxSequence the sequence of the last message received by the session.
   * @return data for session if any.
   * @throws java.lang.Exception if session is unknown, unavailable or the sequence makes no sense.
   */
  @Nullable
  String poll( @Nonnull String sessionID, int rxSequence )
    throws Exception;
}
