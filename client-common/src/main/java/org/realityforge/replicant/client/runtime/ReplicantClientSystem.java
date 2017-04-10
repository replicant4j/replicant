package org.realityforge.replicant.client.runtime;

import java.util.List;
import javax.annotation.Nonnull;
import org.realityforge.replicant.client.transport.DataLoaderService;

public interface ReplicantClientSystem
{
  enum State
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

  /**
   * Returns true if the system is expected to be active and connected to all data sources.
   * This is a desired state rather than an actual state that is represented by {@link #getState()}
   */
  boolean isActive();

  /**
   * Return the actual state of the system.
   */
  State getState();

  /**
   * Mark the client system as active and start to converge to being CONNECTED.
   */
  void activate();

  /**
   * Mark the client system as inactive and start to converge to being DISCONNECTED.
   */
  void deactivate();

  /**
   * Attempt to converge the state of the system towards the desired state.
   * This should be invoked periodically.
   */
  void converge();

  /**
   * Add a listener. Return true if actually added, false if listener was already present.
   */
  boolean addReplicantSystemListener( @Nonnull ReplicantSystemListener listener );

  /**
   * Remove a listener. Return true if actually removed, false if listener was not present.
   */
  boolean removeReplicantSystemListener( @Nonnull ReplicantSystemListener listener );

  /**
   * Retrieve the dataloader service associated with the graph.
   */
  @Nonnull
  DataLoaderService getDataLoaderService( @Nonnull Enum graph )
    throws IllegalArgumentException;

  @Nonnull
  List<DataLoaderEntry> getDataLoaders();
}
