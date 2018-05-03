package replicant;

import javax.annotation.Nonnull;
import org.realityforge.braincheck.BrainCheckConfig;
import static org.realityforge.braincheck.Guards.*;

/**
 * Provide access to global configuration settings.
 */
public final class Replicant
{
  private Replicant()
  {
  }

  /**
   * Return true if zones are enabled, false otherwise.
   *
   * @return true if zones are enabled, false otherwise.
   */
  public static boolean areZonesEnabled()
  {
    return ReplicantConfig.areZonesEnabled();
  }

  /**
   * Return true if invariants will be checked.
   *
   * @return true if invariants will be checked.
   */
  public static boolean shouldCheckInvariants()
  {
    return ReplicantConfig.checkInvariants() && BrainCheckConfig.checkInvariants();
  }

  /**
   * Return true if apiInvariants will be checked.
   *
   * @return true if apiInvariants will be checked.
   */
  public static boolean shouldCheckApiInvariants()
  {
    return ReplicantConfig.checkApiInvariants() && BrainCheckConfig.checkApiInvariants();
  }

  /**
   * Return true if should record key when tracking requests through the system, false otherwise.
   *
   * @return true if should record key when tracking requests through the system, false otherwise.
   */
  public static boolean shouldRecordRequestKey()
  {
    return ReplicantConfig.shouldRecordRequestKey();
  }

  /**
   * Return true if a data load action should result in the local entity state being validated, false otherwise.
   *
   * @return true if a data load action should result in the local entity state being validated, false otherwise.
   */
  public static boolean shouldValidateRepositoryOnLoad()
  {
    return shouldCheckInvariants() && ReplicantConfig.shouldValidateRepositoryOnLoad();
  }

  /**
   * Return true if request debugging can be enabled at runtime, false otherwise.
   *
   * @return true if request debugging can be enabled at runtime, false otherwise.
   */
  public static boolean canRequestDebugOutputBeEnabled()
  {
    return ReplicantConfig.canRequestDebugOutputBeEnabled();
  }

  /**
   * Return true if subscription debugging can be enabled at runtime, false otherwise.
   *
   * @return true if subscription debugging can be enabled at runtime, false otherwise.
   */
  public static boolean canSubscriptionsDebugOutputBeEnabled()
  {
    return ReplicantConfig.canSubscriptionsDebugOutputBeEnabled();
  }

  /**
   * Create a new zone.
   * This zone is not yet activated.
   *
   * @return the new zone.
   */
  @Nonnull
  public static Zone createZone()
  {
    if ( shouldCheckApiInvariants() )
    {
      apiInvariant( Replicant::areZonesEnabled, () -> "Replicant-0001: Invoked Replicant.createZone() but zones are not enabled." );
    }
    return new Zone();
  }

  /**
   * Save the old zone and make the specified zone the current zone.
   */
  @SuppressWarnings( "ConstantConditions" )
  static void activateZone( @Nonnull final Zone zone )
  {
    ReplicantZoneHolder.activateZone( zone );
  }

  /**
   * Restore the old zone.
   * This takes the zone that was current when {@link #activateZone(Zone)} was called for the active zone
   * and restores it to being the current zone.
   */
  @SuppressWarnings( "ConstantConditions" )
  static void deactivateZone( @Nonnull final Zone zone )
  {
    ReplicantZoneHolder.deactivateZone( zone );
  }

  /**
   * Return the current zone.
   *
   * @return the current zone.
   */
  @Nonnull
  static Zone currentZone()
  {
    return ReplicantZoneHolder.currentZone();
  }
}
