package replicant.server.runtime;

import java.util.HashMap;
import java.util.Map;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Passes Replicant request metadata from the servlet tier to the EJB tier.
 *
 * <p>The expectation is that an interceptor in the ejb tier will inspect and utilize
 * the request metadata. The implementation uses thread-locals as it assumes that at least
 * the first interceptor will be invoked in the thread that initiates the request.</p>
 *
 * <p>This server-side request context is unrelated to the client-side Replicant Context and does not own System
 * Schemas, Areas of Interest, Subscriptions, or Replicas.
 */
public final class ReplicantRequestContextHolder {
    @NonNull
    private static final ThreadLocal<Map<String, Object>> c_context = new ThreadLocal<>();

    private ReplicantRequestContextHolder() {}

    /**
     * Specify request metadata for a particular key.
     *
     * @param key  the key.
     * @param data the data.
     */
    public static void put(@NonNull final String key, @Nullable final Object data) {
        if (null == c_context.get()) {
            if (null == data) {
                return;
            }
            c_context.set(new HashMap<>());
        }

        final var context = c_context.get();
        if (null == data) {
            context.remove(key);
        } else {
            context.put(key, data);
        }
    }

    /**
     * Retrieve request metadata specified for key.
     *
     * @param key the key.
     * @return the request metadata if any, else null.
     */
    @Nullable
    public static Object get(@NonNull final String key) {
        final var map = c_context.get();
        return null == map ? null : map.get(key);
    }

    /**
     * Cleanup and remove any metadata associated with the current request.
     * This should be invoked by the outer interceptor.
     */
    public static void clean() {
        c_context.remove();
    }
}
