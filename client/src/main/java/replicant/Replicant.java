package replicant;

import org.realityforge.braincheck.BrainCheckConfig;

/**
 * Provide access to global configuration settings.
 */
public final class Replicant
{
  private Replicant()
  {
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
}
