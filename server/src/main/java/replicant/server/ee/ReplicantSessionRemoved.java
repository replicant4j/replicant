package replicant.server.ee;

import org.jspecify.annotations.NonNull;

public record ReplicantSessionRemoved(@NonNull String clientSessionId) {}
