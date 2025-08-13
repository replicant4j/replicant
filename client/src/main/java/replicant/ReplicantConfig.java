package replicant;

import grim.annotations.OmitType;
import javax.annotation.Nonnull;

/**
 * Location of all compile time configuration settings for framework.
 */
@SuppressWarnings( "FieldMayBeFinal" )
@OmitType
final class ReplicantConfig
{
  @Nonnull
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
  @Nonnull
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

  static boolean shouldValidateEntitiesOnLoad()
  {
    return VALIDATE_ENTITIES_ON_LOAD;
  }

  static boolean shouldValidateChangeSetOnRead()
  {
    return VALIDATE_CHANGE_SET_ON_READ;
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
    @Nonnull
    @Override
    String loggerType()
    {
      return System.getProperty( "replicant.logger", PRODUCTION_MODE ? "basic" : "proxy" );
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

    @Nonnull
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
