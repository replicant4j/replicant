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
  private static boolean ENABLE_NAMES =
    "true".equals( System.getProperty( "replicant.enable_names", PRODUCTION_MODE ? "false" : "true" ) );
  private static boolean ENABLE_ZONES = "true".equals( System.getProperty( "replicant.enable_zones", "false" ) );
  private static boolean VALIDATE_CHANGE_SET_ON_READ =
    "true".equals( System.getProperty( "replicant.validateChangeSetOnRead",
                                       PRODUCTION_MODE ? "false" : "true" ) );
  private static boolean VALIDATE_REPOSITORY_ON_LOAD =
    "true".equals( System.getProperty( "replicant.validateRepositoryOnLoad",
                                       PRODUCTION_MODE ? "false" : "true" ) );
  private static boolean REQUESTS_DEBUG_OUTPUT_ENABLED =
    "true".equals( System.getProperty( "replicant.requestsDebugOutputEnabled", PRODUCTION_MODE ? "false" : "true" ) );
  private static boolean ENABLE_SPIES =
    "true".equals( System.getProperty( "replicant.enable_spies", PRODUCTION_MODE ? "false" : "true" ) );
  /**
   * Valid values are: "none", "basic", "jul" (java.util.logging) and "proxy" (for testing)
   */
  private static final String LOGGER_TYPE =
    System.getProperty( "arez.logger", PRODUCTION_MODE ? "basic" : "proxy" );

  private ReplicantConfig()
  {
  }

  static boolean isProductionMode()
  {
    return PRODUCTION_MODE;
  }

  static boolean areNamesEnabled()
  {
    return ENABLE_NAMES;
  }

  static boolean checkInvariants()
  {
    return CHECK_INVARIANTS;
  }

  static boolean checkApiInvariants()
  {
    return CHECK_API_INVARIANTS;
  }

  static boolean areSpiesEnabled()
  {
    return ENABLE_SPIES;
  }

  static boolean areZonesEnabled()
  {
    return ENABLE_ZONES;
  }

  static boolean shouldValidateRepositoryOnLoad()
  {
    return VALIDATE_REPOSITORY_ON_LOAD;
  }

  static boolean shouldValidateChangeSetOnRead()
  {
    return VALIDATE_CHANGE_SET_ON_READ;
  }

  static boolean canRequestsDebugOutputBeEnabled()
  {
    return REQUESTS_DEBUG_OUTPUT_ENABLED;
  }

  static String loggerType()
  {
    return LOGGER_TYPE;
  }
}
