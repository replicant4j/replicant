package replicant.server.transport;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import replicant.server.EntityChangeCandidate;

public interface ReplicantChangeRecorder {
    /**
     * Converts the given object into an appropriate {@link EntityChangeCandidate}.
     *
     * @param object   the source object to be converted; must not be null
     * @param isUpdate a boolean indicating if the conversion is for an update
     * @return the converted {@link EntityChangeCandidate}, or null if the conversion cannot be performed
     */
    @Nullable
    EntityChangeCandidate convertToEntityChangeCandidate(@NonNull final Object object, final boolean isUpdate);
}
