package replicant;

import static org.testng.Assert.*;

import arez.component.Verifiable;
import org.jspecify.annotations.Nullable;
import org.testng.annotations.Test;

public class ValidatorTest extends AbstractReplicantTest {
    @Test
    public void noEntities() {
        Validator.create(null).validateReplicas();
    }

    @Test
    public void entitiesAllValid() {
        final ReplicaRegistry replicaRegistry = Replicant.context().getReplicaRegistry();
        final ReplicaEntry replicaEntry1 =
                safeAction(() -> replicaRegistry.findOrCreateReplicaEntry("MyEntity/1", MyEntity.class, 1));
        safeAction(() -> replicaEntry1.setReplica(new MyEntity(null)));
        safeAction(() -> replicaRegistry.findOrCreateReplicaEntry("MyEntity/2", MyEntity.class, 2));

        // Entities fine
        Validator.create(null).validateReplicas();
    }

    @Test
    public void invalidEntity() {
        final ReplicaRegistry replicaRegistry = Replicant.context().getReplicaRegistry();
        final ReplicaEntry replicaEntry1 =
                safeAction(() -> replicaRegistry.findOrCreateReplicaEntry("MyEntity/1", MyEntity.class, 1));
        final Exception error = new Exception();
        safeAction(() -> replicaEntry1.setReplica(new MyEntity(error)));

        final IllegalStateException exception = expectThrows(
                IllegalStateException.class, () -> Validator.create(null).validateReplicas());

        assertEquals(
                exception.getMessage(),
                "Replicant-0065: Replica failed to verify during validation process. Replica Entry = MyEntity/1");
    }

    static class MyEntity implements Verifiable {
        @Nullable
        private final Exception _exception;

        MyEntity(@Nullable final Exception exception) {
            _exception = exception;
        }

        @Override
        public void verify() throws Exception {
            if (null != _exception) {
                throw _exception;
            }
        }
    }
}
