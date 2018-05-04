package replicant;

/**
 * Location of all compile time configuration settings for framework.
 */
final class ReplicantConfig
{
  /**
   * Valid values are: "production" and "development".
   */
  private static final boolean PRODUCTION_MODE =
    "production".equals( System.getProperty( "replicant.environment", "production" ) );
  private static boolean CHECK_INVARIANTS =
    "true".equals( System.getProperty( "replicant.check_invariants", PRODUCTION_MODE ? "false" : "true" ) );
  private static boolean CHECK_API_INVARIANTS =
    "true".equals( System.getProperty( "replicant.check_api_invariants", PRODUCTION_MODE ? "false" : "true" ) );
  private static boolean ENABLE_ZONES = "true".equals( System.getProperty( "replicant.enable_zones", "false" ) );
  private static boolean RECORD_REQUEST_KEY =
    "true".equals( System.getProperty( "replicant.recordRequestKey", PRODUCTION_MODE ? "false" : "true" ) );
  private static boolean VALIDATE_REPOSITORY_ON_LOAD =
    "true".equals( System.getProperty( "replicant.validateRepositoryOnLoad",
                                       PRODUCTION_MODE ? "false" : "true" ) );
  private static boolean REQUESTS_DEBUG_OUTPUT_ENABLED =
    "true".equals( System.getProperty( "replicant.requestsDebugOutputEnabled", PRODUCTION_MODE ? "false" : "true" ) );
  private static boolean SUBSCRIPTION_DEBUG_OUTPUT_ENABLED =
    "true".equals( System.getProperty( "replicant.subscriptionsDebugOutputEnabled",
                                       PRODUCTION_MODE ? "false" : "true" ) );

  private ReplicantConfig()
  {
  }

  static boolean isProductionMode()
  {
    return PRODUCTION_MODE;
  }

  static boolean checkInvariants()
  {
    return CHECK_INVARIANTS;
  }

  static boolean checkApiInvariants()
  {
    return CHECK_API_INVARIANTS;
  }

  static boolean areZonesEnabled()
  {
    return ENABLE_ZONES;
  }

  static boolean shouldRecordRequestKey()
  {
    return RECORD_REQUEST_KEY;
  }

  static boolean shouldValidateRepositoryOnLoad()
  {
    return VALIDATE_REPOSITORY_ON_LOAD;
  }

  static boolean canRequestsDebugOutputBeEnabled()
  {
    return REQUESTS_DEBUG_OUTPUT_ENABLED;
  }

  static boolean canSubscriptionsDebugOutputBeEnabled()
  {
    return SUBSCRIPTION_DEBUG_OUTPUT_ENABLED;
  }
}
