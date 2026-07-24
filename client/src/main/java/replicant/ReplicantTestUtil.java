package replicant;

/**
 * Utility class for interacting with Replicant config settings in tests.
 */
@SuppressWarnings("WeakerAccess")
public final class ReplicantTestUtil {
    private ReplicantTestUtil() {}

    /**
     * Reset the state of Replicant config to either production or development state.
     *
     * @param productionMode true to set it to production mode configuration, false to set it to development mode config.
     */
    public static void resetConfig(final boolean productionMode) {
        if (Replicant.isProductionMode()) {
            /*
             * This should really never happen but if it does add assertion (so code stops in debugger) or
             * failing that throw an exception.
             */
            assert !Replicant.isProductionMode();
            throw new IllegalStateException("Unable to reset config as Replicant is in production mode");
        }

        if (productionMode) {
            disableNames();
            noValidateChangeSetOnRead();
            noValidateEntitiesOnLoad();
            disableSpies();
            noCheckInvariants();
            noCheckApiInvariants();
        } else {
            enableNames();
            validateChangeSetOnRead();
            validateEntitiesOnLoad();
            enableSpies();
            checkInvariants();
            checkApiInvariants();
        }
        disableZones();
        ((ReplicantLogger.ProxyLogger) ReplicantLogger.getLogger()).setLogger(null);
        resetState();
    }

    /**
     * Reset the state of Replicant context and zone information to align with the current configuration settings.
     * This will clear all existing state and should be used with caution.
     */
    public static void resetState() {
        ReplicantContextHolder.reset();
        ReplicantZoneHolder.reset();
    }

    /**
     * Set `replicant.enable_names` setting to true.
     */
    public static void enableNames() {
        ReplicantConfig.setEnableNames(true);
    }

    /**
     * Set `replicant.enable_names` setting to false.
     */
    public static void disableNames() {
        ReplicantConfig.setEnableNames(false);
    }

    /**
     * Set `replicant.validateChangeSetOnRead` setting to true.
     */
    public static void validateChangeSetOnRead() {
        ReplicantConfig.setValidateChangeSetOnRead(true);
    }

    /**
     * Set `replicant.validateChangeSetOnRead` setting to false.
     */
    public static void noValidateChangeSetOnRead() {
        ReplicantConfig.setValidateChangeSetOnRead(false);
    }

    /**
     * Set `replicant.validateEntitiesOnLoad` setting to true.
     */
    public static void validateEntitiesOnLoad() {
        ReplicantConfig.setValidateEntitiesOnLoad(true);
    }

    /**
     * Set `replicant.validateEntitiesOnLoad` setting to false.
     */
    public static void noValidateEntitiesOnLoad() {
        ReplicantConfig.setValidateEntitiesOnLoad(false);
    }

    /**
     * Set `replicant.check_invariants` setting to true.
     */
    public static void checkInvariants() {
        ReplicantConfig.setCheckInvariants(true);
    }

    /**
     * Set the `replicant.check_invariants` setting to false.
     */
    public static void noCheckInvariants() {
        ReplicantConfig.setCheckInvariants(false);
    }

    /**
     * Set `replicant.check_api_invariants` setting to true.
     */
    public static void checkApiInvariants() {
        ReplicantConfig.setCheckApiInvariants(true);
    }

    /**
     * Set the `replicant.check_api_invariants` setting to false.
     */
    public static void noCheckApiInvariants() {
        ReplicantConfig.setCheckApiInvariants(false);
    }

    /**
     * Set `replicant.enable_spies` setting to true.
     */
    public static void enableSpies() {
        ReplicantConfig.setEnableSpies(true);
    }

    /**
     * Set `replicant.enable_spies` setting to false.
     */
    public static void disableSpies() {
        ReplicantConfig.setEnableSpies(false);
    }

    /**
     * Set `replicant.enable_zones` setting to true.
     */
    public static void enableZones() {
        ReplicantConfig.setEnableZones(true);
    }

    /**
     * Set `replicant.enable_zones` setting to false.
     */
    public static void disableZones() {
        ReplicantConfig.setEnableZones(false);
    }
}
