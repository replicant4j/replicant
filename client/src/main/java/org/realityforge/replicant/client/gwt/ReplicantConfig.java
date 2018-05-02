package org.realityforge.replicant.client.gwt;

public final class ReplicantConfig
{
  private ReplicantConfig()
  {
  }

  public static boolean shouldRecordRequestKey()
  {
    return System.getProperty( "replicant.shouldRecordRequestKey", "false" ).equals( "true" );
  }

  public static boolean shouldValidateRepositoryOnLoad()
  {
    return System.getProperty( "replicant.shouldValidateRepositoryOnLoad", "false" ).equals( "true" );
  }

  public static boolean canRequestDebugOutputBeEnabled()
  {
    return System.getProperty( "replicant.requestDebugOutputEnabled", "false" ).equals( "true" );
  }

  public static boolean canSubscriptionsDebugOutputBeEnabled()
  {
    return System.getProperty( "replicant.subscriptionsDebugOutputEnabled", "false" ).equals( "true" );
  }
}
