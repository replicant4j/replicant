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
   * Reset the state of Arez config to either production or development state.
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
      noRecordRequestKey();
      noValidateRepositoryOnLoad();
      noRequestsDebugOutputEnabled();
      noSubscriptionsDebugOutputEnabled();
      noCheckInvariants();
      noCheckApiInvariants();
    }
    else
    {
      recordRequestKey();
      validateRepositoryOnLoad();
      requestsDebugOutputEnabled();
      subscriptionsDebugOutputEnabled();
      checkInvariants();
      checkApiInvariants();
    }
    disableZones();
    ReplicantZoneHolder.reset();
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
   * Configure the `replicant.requestDebugOutputEnabled` setting.
   *
   * @param value the setting.
   */
  private static void setRequestsDebugOutputEnabled( final boolean value )
  {
    setConstant( "REQUESTS_DEBUG_OUTPUT_ENABLED", value );
  }

  /**
   * Set `replicant.subscriptionsDebugOutputEnabled` setting to true.
   */
  public static void subscriptionsDebugOutputEnabled()
  {
    setSubscriptionsDebugOutputEnabled( true );
  }

  /**
   * Set `replicant.subscriptionsDebugOutputEnabled` setting to false.
   */
  public static void noSubscriptionsDebugOutputEnabled()
  {
    setSubscriptionsDebugOutputEnabled( false );
  }

  /**
   * Configure the `replicant.subscriptionsDebugOutputEnabled` setting.
   *
   * @param value the setting.
   */
  private static void setSubscriptionsDebugOutputEnabled( final boolean value )
  {
    setConstant( "SUBSCRIPTION_DEBUG_OUTPUT_ENABLED", value );
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
