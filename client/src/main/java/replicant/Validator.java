package replicant;

import static org.realityforge.braincheck.Guards.*;

import arez.annotations.Action;
import arez.annotations.ArezComponent;
import arez.annotations.Feature;
import arez.component.Verifiable;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Validates that the state owned by one Replicant Context is consistent.
 */
@ArezComponent(disposeNotifier = Feature.DISABLE, requireId = Feature.DISABLE)
abstract class Validator extends ReplicantService {
    @NonNull
    static Validator create(@Nullable final ReplicantContext context) {
        return new Arez_Validator(context);
    }

    Validator(@Nullable final ReplicantContext context) {
        super(context);
    }

    /**
     * Verify that all Replicas contained within the ReplicaRegistry will pass verification.
     * A Replica can be verified by implementing the {@link Verifiable} interface.
     */
    @Action
    void validateReplicas() {
        if (Replicant.shouldCheckInvariants()) {
            for (final Class<?> replicaType : getReplicantContext().findAllReplicaTypes()) {
                for (final ReplicaEntry replicaEntry : getReplicantContext().findAllReplicaEntriesByType(replicaType)) {
                    try {
                        final Object replica = replicaEntry.maybeReplica();
                        if (null != replica) {
                            Verifiable.verify(replica);
                        }
                    } catch (final Exception e) {
                        fail(() ->
                                "Replicant-0065: Replica failed to verify during validation process. Replica Entry = "
                                        + replicaEntry);
                    }
                }
            }
        }
    }
}
