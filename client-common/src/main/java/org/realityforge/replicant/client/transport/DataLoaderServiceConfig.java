package org.realityforge.replicant.client.transport;

public interface DataLoaderServiceConfig
{
  /**
   * @return true if should record key when tracking requests through the system.
   */
  boolean shouldRecordRequestKey();

  /**
   * @return true if a load action should result in the EntityRepository being validated.
   */
  boolean shouldValidateRepositoryOnLoad();

  boolean repositoryDebugOutputEnabled();

  boolean subscriptionsDebugOutputEnabled();

  boolean requestDebugOutputEnabled();
}
