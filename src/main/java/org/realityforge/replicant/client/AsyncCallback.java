package org.realityforge.replicant.client;

/**
 * Interface to receive notification of successful call completion.
 *
 * @param <T> the type of the return value.
 */
public interface AsyncCallback<T>
{
  /**
   * Called when an asynchronous call completes successfully.
   *
   * @param result the return value of the remote produced call
   */
  void onSuccess( T result );
}