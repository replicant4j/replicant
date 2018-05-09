package replicant;

import java.lang.reflect.Field;
import javax.annotation.Nonnull;
import org.realityforge.anodoc.TestOnly;

/**
 * Utility class for interacting with Replicant config settings in tests.
 */
@SuppressWarnings( "WeakerAccess" )
@TestOnly
@GwtIncompatible
public final class ReplicantTestUtil
{
  private ReplicantTestUtil()
  {
  }

  /**
   * Reset the state of Replicant config to either production or development state.
   *
   * @param productionMode true to set it to production mode configuration, false to set it to development mode config.
   */
  public static void resetConfig( final boolean productionMode )
  {
    if ( ReplicantConfig.isProductionMode() )
    {
      /*
       * This should really never happen but if it does add assertion (so code stops in debugger) or
       * failing that throw an exception.
       */
      assert !ReplicantConfig.isProductionMode();
      throw new IllegalStateException( "Unable to reset config as Replicant is in production mode" );
    }

    if ( productionMode )
    {
      disableNames();
      noRecordRequestKey();
      noValidateRepositoryOnLoad();
      noRequestsDebugOutputEnabled();
      disableSpies();
      noCheckInvariants();
      noCheckApiInvariants();
    }
    else
    {
      enableNames();
      recordRequestKey();
      validateRepositoryOnLoad();
      requestsDebugOutputEnabled();
      enableSpies();
      checkInvariants();
      checkApiInvariants();
    }
    disableZones();
    ( (ReplicantLogger.ProxyLogger) ReplicantLogger.getLogger() ).setLogger( null );
    resetState();
  }

  /**
   * Reset the state of Replicant context and zone information to align with the current configuration settings.
   * This will clear all existing state and should be used with caution.
   */
  public static void resetState()
  {
    ReplicantContextHolder.reset();
    ReplicantZoneHolder.reset();
  }

  /**
   * Set `replicant.enable_names` setting to true.
   */
  public static void enableNames()
  {
    setEnableNames( true );
  }

  /**
   * Set `replicant.enable_names` setting to false.
   */
  public static void disableNames()
  {
    setEnableNames( false );
  }

  /**
   * Configure the `replicant.enable_names` setting.
   *
   * @param value the setting.
   */
  private static void setEnableNames( final boolean value )
  {
    setConstant( "ENABLE_NAMES", value );
  }

  /**
   * Set `replicant.recordRequestKey` setting to true.
   */
  public static void recordRequestKey()
  {
    setShouldRecordRequestKey( true );
  }

  /**
   * Set `replicant.recordRequestKey` setting to false.
   */
  public static void noRecordRequestKey()
  {
    setShouldRecordRequestKey( false );
  }

  /**
   * Configure the `replicant.enable_names` setting.
   *
   * @param value the setting.
   */
  private static void setShouldRecordRequestKey( final boolean value )
  {
    setConstant( "RECORD_REQUEST_KEY", value );
  }

  /**
   * Set `replicant.validateRepositoryOnLoad` setting to true.
   */
  public static void validateRepositoryOnLoad()
  {
    setValidateRepositoryOnLoad( true );
  }

  /**
   * Set `replicant.validateRepositoryOnLoad` setting to false.
   */
  public static void noValidateRepositoryOnLoad()
  {
    setValidateRepositoryOnLoad( false );
  }

  /**
   * Configure the `replicant.validateRepositoryOnLoad` setting.
   *
   * @param value the setting.
   */
  private static void setValidateRepositoryOnLoad( final boolean value )
  {
    setConstant( "VALIDATE_REPOSITORY_ON_LOAD", value );
  }

  /**
   * Set `replicant.requestsDebugOutputEnabled` setting to true.
   */
  public static void requestsDebugOutputEnabled()
  {
    setRequestsDebugOutputEnabled( true );
  }

  /**
   * Set `replicant.requestsDebugOutputEnabled` setting to false.
   */
  public static void noRequestsDebugOutputEnabled()
  {
    setRequestsDebugOutputEnabled( false );
  }

  /**
   * Configure the `replicant.requestsDebugOutputEnabled` setting.
   *
   * @param value the setting.
   */
  private static void setRequestsDebugOutputEnabled( final boolean value )
  {
    setConstant( "REQUESTS_DEBUG_OUTPUT_ENABLED", value );
  }

  /**
   * Set `replicant.check_invariants` setting to true.
   */
  public static void checkInvariants()
  {
    setCheckInvariants( true );
  }

  /**
   * Set the `replicant.check_invariants` setting to false.
   */
  public static void noCheckInvariants()
  {
    setCheckInvariants( false );
  }

  /**
   * Configure the `replicant.check_invariants` setting.
   *
   * @param checkInvariants the "check invariants" setting.
   */
  private static void setCheckInvariants( final boolean checkInvariants )
  {
    setConstant( "CHECK_INVARIANTS", checkInvariants );
  }

  /**
   * Set `replicant.check_api_invariants` setting to true.
   */
  public static void checkApiInvariants()
  {
    setCheckApiInvariants( true );
  }

  /**
   * Set the `replicant.check_api_invariants` setting to false.
   */
  public static void noCheckApiInvariants()
  {
    setCheckApiInvariants( false );
  }

  /**
   * Configure the `replicant.check_api_invariants` setting.
   *
   * @param checkApiInvariants the "check invariants" setting.
   */
  private static void setCheckApiInvariants( final boolean checkApiInvariants )
  {
    setConstant( "CHECK_API_INVARIANTS", checkApiInvariants );
  }

  /**
   * Set `replicant.enable_spies` setting to true.
   */
  public static void enableSpies()
  {
    setEnableSpies( true );
  }

  /**
   * Set `replicant.enable_spies` setting to false.
   */
  public static void disableSpies()
  {
    setEnableSpies( false );
  }

  /**
   * Configure the "replicant.enable_spies" setting.
   *
   * @param value the setting.
   */
  private static void setEnableSpies( final boolean value )
  {
    setConstant( "ENABLE_SPIES", value );
  }

  /**
   * Set `replicant.enable_zones` setting to true.
   */
  public static void enableZones()
  {
    setEnableZones( true );
  }

  /**
   * Set `replicant.enable_zones` setting to false.
   */
  public static void disableZones()
  {
    setEnableZones( false );
  }

  /**
   * Configure the `replicant.enable_zones` setting.
   *
   * @param value the setting.
   */
  private static void setEnableZones( final boolean value )
  {
    setConstant( "ENABLE_ZONES", value );
  }

  /**
   * Set the specified field name on ReplicantConfig.
   */
  @SuppressWarnings( "NonJREEmulationClassesInClientCode" )
  private static void setConstant( @Nonnull final String fieldName, final boolean value )
  {
    if ( !ReplicantConfig.isProductionMode() )
    {
      try
      {
        final Field field = ReplicantConfig.class.getDeclaredField( fieldName );
        field.setAccessible( true );
        field.set( null, value );
      }
      catch ( final NoSuchFieldException | IllegalAccessException e )
      {
        throw new IllegalStateException( "Unable to change constant " + fieldName, e );
      }
    }
    else
    {
      /*
       * This should not happen but if it does then just fail with an assertion or error.
       */
      assert !ReplicantConfig.isProductionMode();
      throw new IllegalStateException( "Unable to change constant " + fieldName +
                                       " as Replicant is in production mode" );
    }
  }
}
