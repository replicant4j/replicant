package replicant;

import static org.realityforge.braincheck.Guards.*;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * The registry of all System Schemas in a Replicant Context.
 */
final class SystemSchemaService {
    private final Map<Integer, SystemSchema> _systemSchemas = new HashMap<>();

    static SystemSchemaService create() {
        return new SystemSchemaService();
    }

    /**
     * Return the System Schemas associated with the service.
     *
     * @return the System Schemas associated with the service.
     */
    @NonNull
    Collection<SystemSchema> getSystemSchemas() {
        return CollectionsUtil.wrap(_systemSchemas.values());
    }

    /**
     * Return the System Schema with the specified systemSchemaId or null if no such System Schema.
     *
     * @param systemSchemaId the id of the System Schema.
     * @return the System Schema or null if no such System Schema.
     */
    @Nullable
    SystemSchema findById(final int systemSchemaId) {
        return _systemSchemas.get(systemSchemaId);
    }

    /**
     * Return the System Schema with the specified systemSchemaId.
     * This should not be invoked unless the System Schema with specified id exists.
     *
     * @param systemSchemaId the id of the System Schema.
     * @return the System Schema.
     */
    @NonNull
    SystemSchema getById(final int systemSchemaId) {
        final SystemSchema systemSchema = findById(systemSchemaId);
        if (Replicant.shouldCheckInvariants()) {
            invariant(
                    () -> null != systemSchema,
                    () -> "Replicant-0059: Unable to locate System Schema with id " + systemSchemaId);
        }
        return Objects.requireNonNull(systemSchema);
    }

    /**
     * Register specified System Schema in list of System Schemas managed by the container.
     * The System Schema should NOT already be registered in service.
     *
     * @param systemSchema the System Schema to register.
     */
    void registerSystemSchema(@NonNull final SystemSchema systemSchema) {
        final int systemSchemaId = systemSchema.getId();
        if (Replicant.shouldCheckInvariants()) {
            invariant(
                    () -> !_systemSchemas.containsKey(systemSchemaId),
                    () -> "Replicant-0070: Attempted to register System Schema with id " + systemSchemaId
                            + " when a System Schema with id already exists: " + _systemSchemas.get(systemSchemaId));
        }
        _systemSchemas.put(systemSchemaId, systemSchema);
    }

    void deregisterSystemSchema(@NonNull final SystemSchema systemSchema) {
        final int systemSchemaId = systemSchema.getId();
        if (Replicant.shouldCheckInvariants()) {
            invariant(
                    () -> _systemSchemas.containsKey(systemSchemaId),
                    () -> "Replicant-0085: Attempted to deregister System Schema with id " + systemSchemaId
                            + " but no such System Schema exists.");
        }
        _systemSchemas.remove(systemSchemaId);
    }
}
