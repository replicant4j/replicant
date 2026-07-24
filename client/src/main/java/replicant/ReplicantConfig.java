package replicant;

import grim.annotations.OmitType;
import org.jspecify.annotations.NonNull;

/**
 * Location of all compile time configuration settings for framework.
 */
@SuppressWarnings( { "FieldMayBeFinal", "ConstantField", "FieldCanBeFinal" } )
@OmitType
final class ReplicantConfig
{
  @NonNull
  private static final ConfigProvider PROVIDER = new ConfigProvider();
  private static final boolean JVM = PROVIDER.isJvm();
  private static boolean PRODUCTION_MODE = PROVIDER.isProductionMode();
  private static boolean CHECK_INVARIANTS = PROVIDER.checkInvariants();
  private static boolean CHECK_API_INVARIANTS = PROVIDER.checkApiInvariants();
  private static boolean ENABLE_NAMES = PROVIDER.areNamesEnabled();
  private static boolean ENABLE_ZONES = PROVIDER.enableZones();
  private static boolean VALIDATE_CHANGE_SET_ON_READ = PROVIDER.validateChangeSetOnRead();
  private static boolean VALIDATE_ENTITIES_ON_LOAD = PROVIDER.validateEntitiesOnLoad();
  private static boolean ENABLE_SPIES = PROVIDER.enableSpies();
  @NonNull
  private static final String LOGGER_TYPE = PROVIDER.loggerType();

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

  static void setEnableNames( final boolean enableNames )
  {
    ENABLE_NAMES = enableNames;
  }

  static boolean checkInvariants()
  {
    return CHECK_INVARIANTS;
  }

  static void setCheckInvariants( final boolean checkInvariants )
  {
    CHECK_INVARIANTS = checkInvariants;
  }

  static boolean checkApiInvariants()
  {
    return CHECK_API_INVARIANTS;
  }

  static void setCheckApiInvariants( final boolean checkApiInvariants )
  {
    CHECK_API_INVARIANTS = checkApiInvariants;
  }

  static boolean areSpiesEnabled()
  {
    return ENABLE_SPIES;
  }

  static void setEnableSpies( final boolean enableSpies )
  {
    ENABLE_SPIES = enableSpies;
  }

  static boolean areZonesEnabled()
  {
    return ENABLE_ZONES;
  }

  static void setEnableZones( final boolean enableZones )
  {
    ENABLE_ZONES = enableZones;
  }

  static boolean shouldValidateEntitiesOnLoad()
  {
    return VALIDATE_ENTITIES_ON_LOAD;
  }

  static void setValidateEntitiesOnLoad( final boolean validateEntitiesOnLoad )
  {
    VALIDATE_ENTITIES_ON_LOAD = validateEntitiesOnLoad;
  }

  static boolean shouldValidateChangeSetOnRead()
  {
    return VALIDATE_CHANGE_SET_ON_READ;
  }

  static void setValidateChangeSetOnRead( final boolean validateChangeSetOnRead )
  {
    VALIDATE_CHANGE_SET_ON_READ = validateChangeSetOnRead;
  }

  static String loggerType()
  {
    return LOGGER_TYPE;
  }

  static boolean isJvm()
  {
    return JVM;
  }

  private static final class ConfigProvider
    extends AbstractConfigProvider
  {
    @GwtIncompatible
    @Override
    boolean isProductionMode()
    {
      return "production".equals( System.getProperty( "replicant.environment", "production" ) );
    }

    @GwtIncompatible
    @Override
    boolean areNamesEnabled()
    {
      return "true".equals( System.getProperty( "replicant.enable_names", PRODUCTION_MODE ? "false" : "true" ) );
    }

    @GwtIncompatible
    @Override
    boolean checkInvariants()
    {
      return "true".equals( System.getProperty( "replicant.check_invariants", PRODUCTION_MODE ? "false" : "true" ) );
    }

    @GwtIncompatible
    @Override
    boolean checkApiInvariants()
    {
      return "true".equals( System.getProperty( "replicant.check_api_invariants",
                                                PRODUCTION_MODE ? "false" : "true" ) );
    }

    @GwtIncompatible
    @Override
    boolean enableZones()
    {
      return "true".equals( System.getProperty( "replicant.enable_zones", "false" ) );
    }

    @GwtIncompatible
    @Override
    boolean validateChangeSetOnRead()
    {
      return "true".equals( System.getProperty( "replicant.validateChangeSetOnRead",
                                                PRODUCTION_MODE ? "false" : "true" ) );
    }

    @GwtIncompatible
    @Override
    boolean validateEntitiesOnLoad()
    {
      return "true".equals( System.getProperty( "replicant.validateEntitiesOnLoad",
                                                PRODUCTION_MODE ? "false" : "true" ) );
    }

    @GwtIncompatible
    @Override
    boolean enableSpies()
    {
      return "true".equals( System.getProperty( "replicant.enable_spies", PRODUCTION_MODE ? "false" : "true" ) );
    }

    @GwtIncompatible
    @NonNull
    @Override
    String loggerType()
    {
      return System.getProperty( "replicant.logger", PRODUCTION_MODE ? "console" : "proxy" );
    }

    @GwtIncompatible
    @Override
    boolean isJvm()
    {
      return true;
    }
  }

  @SuppressWarnings( { "unused", "StringEquality" } )
  private static abstract class AbstractConfigProvider
  {
    boolean isProductionMode()
    {
      return true;
    }

    boolean areNamesEnabled()
    {
      return "true" == System.getProperty( "replicant.enable_names" );
    }

    boolean checkInvariants()
    {
      return "true" == System.getProperty( "replicant.check_invariants" );
    }

    boolean checkApiInvariants()
    {
      return "true" == System.getProperty( "replicant.check_api_invariants" );
    }

    boolean enableZones()
    {
      return "true" == System.getProperty( "replicant.enable_zones" );
    }

    boolean validateChangeSetOnRead()
    {
      return "true" == System.getProperty( "replicant.validateChangeSetOnRead" );
    }

    boolean validateEntitiesOnLoad()
    {
      return "true" == System.getProperty( "replicant.validateEntitiesOnLoad" );
    }

    boolean enableSpies()
    {
      return "true" == System.getProperty( "replicant.enable_spies" );
    }

    @NonNull
    String loggerType()
    {
      /*
       * Valid values are: "none", "console" and "proxy" (for testing)
       */
      return System.getProperty( "replicant.logger" );
    }

    boolean isJvm()
    {
      return false;
    }
  }
}
