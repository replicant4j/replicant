package org.realityforge.replicant.server.ee;

import javax.annotation.Nullable;

public interface ReplicationRequestManager
{
  /**
   * Start a replication context.
   *
   * @param sessionID the id of the session that initiated change if any.
   * @param requestID the id of the request in the session that initiated change..
   */
  void startReplication( @Nullable String sessionID, @Nullable String requestID );

  /**
   * Set the current call depth in replication request.
   */
  void setReplicationCallDepth( int depth );

  /**
   * Return the current call depth in the replication request.
   */
  int getReplicationCallDepth();

  /**
   * Complete a replication context and submit changes for replication.
   *
   * @return true if the request is complete and did not generate any change messages, false otherwise.
   */
  boolean completeReplication();
}
