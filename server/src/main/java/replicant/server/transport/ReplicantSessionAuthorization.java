package replicant.server.transport;

import java.io.IOException;
import org.jspecify.annotations.NonNull;

public interface ReplicantSessionAuthorization {
    boolean runIfValid(@NonNull Action action) throws IOException;

    /**
     * Return the Principal associated with the authorized Replicant Session.
     * Replicant treats the Principal as an opaque application identity.
     *
     * @return the Principal.
     */
    @NonNull
    Object getPrincipal();

    void touchActivity();

    void close();

    @FunctionalInterface
    interface Action {
        void run() throws IOException;
    }
}
