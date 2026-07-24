package replicant.server.transport;

import java.io.IOException;
import org.jspecify.annotations.NonNull;

public interface ReplicantSessionAuthorization {
    boolean runIfValid(@NonNull Action action) throws IOException;

    @NonNull
    Object getPrincipal();

    void touchActivity();

    void close();

    @FunctionalInterface
    interface Action {
        void run() throws IOException;
    }
}
