package replicant;

import static org.realityforge.braincheck.Guards.*;

import arez.Arez;
import arez.SchedulerLock;
import org.jspecify.annotations.NonNull;
import org.realityforge.braincheck.BrainCheckConfig;

/**
 * Provide access to global configuration settings.
 */
public final class Replicant {
    private Replicant() {}

    /**
     * Return true if replicant is in production mode.
     * Production mode means that certain development only features are present and it
     * changes the defaults of some other settings.
     *
     * @return true if replicant is in production mode.
     */
    public static boolean isProductionMode() {
        return ReplicantConfig.isProductionMode();
    }

    /**
     * Return true if user should pass names into API methods, false if should pass null.
     *
     * @return true if user should pass names into API methods, false if should pass null.
     */
    public static boolean areNamesEnabled() {
        return ReplicantConfig.areNamesEnabled();
    }

    /**
     * Return true if zones are enabled, false otherwise.
     *
     * @return true if zones are enabled, false otherwise.
     */
    public static boolean areZonesEnabled() {
        return ReplicantConfig.areZonesEnabled();
    }

    /**
     * Return true if spies are enabled.
     *
     * @return true if spies are enabled, false otherwise.
     */
    public static boolean areSpiesEnabled() {
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
    public static boolean shouldCheckInvariants() {
        return ReplicantConfig.checkInvariants() && BrainCheckConfig.checkInvariants();
    }

    /**
     * Return true if apiInvariants will be checked.
     *
     * @return true if apiInvariants will be checked.
     */
    public static boolean shouldCheckApiInvariants() {
        return ReplicantConfig.checkApiInvariants() && BrainCheckConfig.checkApiInvariants();
    }

    /**
     * Return true if ChangeSet messages should be validated prior to processing, false otherwise.
     *
     * @return true if ChangeSet messages should be validated prior to processing, false otherwise.
     */
    public static boolean shouldValidateChangeSetOnRead() {
        return shouldCheckInvariants() && ReplicantConfig.shouldValidateChangeSetOnRead();
    }

    /**
     * Return true if Replicas should be validated after a message has been processed, false otherwise.
     *
     * @return true if Replicas should be validated after a message has been processed, false otherwise.
     */
    public static boolean shouldValidateReplicasAfterMessageProcessing() {
        return shouldCheckInvariants() && ReplicantConfig.shouldValidateReplicasAfterMessageProcessing();
    }

    /**
     * Return the current Replicant Context.
     *
     * <p>When Zones are disabled this returns the singleton Context. When Zones are enabled this returns the Context
     * selected by the active Zone. Replicant state is isolated between the Contexts selected by different Zones.
     *
     * @return the current Replicant Context.
     */
    @NonNull
    public static ReplicantContext context() {
        return areZonesEnabled() ? ReplicantZoneHolder.context() : ReplicantContextHolder.context();
    }

    /**
     * Create a new Zone containing an isolated Replicant Context.
     * This Zone is not yet activated.
     *
     * @return the new Zone.
     */
    @NonNull
    public static Zone createZone() {
        if (shouldCheckApiInvariants()) {
            apiInvariant(
                    Replicant::areZonesEnabled,
                    () -> "Replicant-0001: Invoked Replicant.createZone() but zones are not enabled.");
        }
        final SchedulerLock lock = Arez.context().pauseScheduler();
        final Zone zone = new Zone();
        lock.dispose();
        return zone;
    }

    /**
     * Save the old zone and make the specified zone the current zone.
     */
    static void activateZone(@NonNull final Zone zone) {
        ReplicantZoneHolder.activateZone(zone);
    }

    /**
     * Restore the old zone.
     * This takes the zone that was current when {@link #activateZone(Zone)} was called for the active zone
     * and restores it to being the current zone.
     */
    static void deactivateZone(@NonNull final Zone zone) {
        ReplicantZoneHolder.deactivateZone(zone);
    }

    /**
     * Return the current zone.
     *
     * @return the current zone.
     */
    @NonNull
    static Zone currentZone() {
        return ReplicantZoneHolder.currentZone();
    }
}
