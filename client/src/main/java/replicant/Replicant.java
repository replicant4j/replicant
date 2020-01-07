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
   * Return true if user should pass names into API methods, false if should pass null.
   *
   * @return true if user should pass names into API methods, false if should pass null.
   */
  public static boolean areNamesEnabled()
  {
    return ReplicantConfig.areNamesEnabled();
  }

  /**
   * Return true if the {@link EntityChangeBroker} infrastructure is enabled, false otherwise.
   * This functionality should only be enabled to support legacy systems as it will be removed in the future.
   *
   * @return true if the entity change broker is enabled, false otherwise.
   */
  public static boolean isChangeBrokerEnabled()
  {
    return ReplicantConfig.isChangeBrokerEnabled();
  }

  /**
   * Return true if the {@link ApplicationEventHandler} infrastructure is enabled, false otherwise.
   * This functionality should only be enabled to support legacy systems as it will be removed in the future.
   *
   * @return true if the replicant will generate events, false otherwise.
   */
  public static boolean areEventsEnabled()
  {
    return ReplicantConfig.areEventsEnabled();
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
   * Return true if spies are enabled.
   *
   * @return true if spies are enabled, false otherwise.
   */
  public static boolean areSpiesEnabled()
  {
    /*
     * Spy's use debug names so we can not enable spies without names.
     */
    return areNamesEnabled() && ReplicantConfig.areSpiesEnabled();
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
   * Return true if ChangeSet messages should be validated prior to processing, false otherwise.
   *
   * @return true if ChangeSet messages should be validated prior to processing, false otherwise.
   */
  public static boolean shouldValidateChangeSetOnRead()
  {
    return shouldCheckInvariants() && ReplicantConfig.shouldValidateChangeSetOnRead();
  }

  /**
   * Return true if entities should be validated after message has been processed, false otherwise.
   *
   * @return true if entities should be validated after message has been processed, false otherwise.
   */
  public static boolean shouldValidateEntitiesOnLoad()
  {
    return shouldCheckInvariants() && ReplicantConfig.shouldValidateEntitiesOnLoad();
  }

  /**
   * Return the ReplicantContext from the provider.
   *
   * @return the ReplicantContext.
   */
  @Nonnull
  public static ReplicantContext context()
  {
    return areZonesEnabled() ? ReplicantZoneHolder.context() : ReplicantContextHolder.context();
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
      apiInvariant( Replicant::areZonesEnabled,
                    () -> "Replicant-0001: Invoked Replicant.createZone() but zones are not enabled." );
    }
    return new Zone();
  }

  /**
   * Save the old zone and make the specified zone the current zone.
   */
  static void activateZone( @Nonnull final Zone zone )
  {
    ReplicantZoneHolder.activateZone( zone );
  }

  /**
   * Restore the old zone.
   * This takes the zone that was current when {@link #activateZone(Zone)} was called for the active zone
   * and restores it to being the current zone.
   */
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
