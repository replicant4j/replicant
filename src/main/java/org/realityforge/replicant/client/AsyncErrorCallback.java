package org.realityforge.replicant.client;

/**
 * Interface to receive notification of calls that fail to complete normally.
 */
public interface AsyncErrorCallback
{
  /**
   * Called when an asynchronous call fails to complete normally.
   * This includes transport layer errors as well as exceptions throw by the actual service method.
   *
   * @param caught failure encountered while executing a remote procedure call
   */
  void onFailure( Throwable caught );
}