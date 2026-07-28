package replicant.server.ee;

import javax.annotation.Priority;
import javax.inject.Inject;
import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import org.jetbrains.annotations.VisibleForTesting;
import org.jspecify.annotations.NonNull;
import replicant.server.ServerConstants;
import replicant.server.runtime.ReplicantRequestContextHolder;
import replicant.server.transport.ReplicantSessionManager;

/**
 * Intercepts a Replication Invocation to capture Entity Change Candidates and submit them after successful
 * transaction completion.
 */
@Interceptor
@Priority(Interceptor.Priority.LIBRARY_BEFORE + 100)
@ReplicationInvocation
public class ReplicationInvocationInterceptor {
    @VisibleForTesting
    @Inject
    ReplicantSessionManager _sessionManager;

    @AroundInvoke
    public Object businessIntercept(final InvocationContext context) throws Exception {
        final var sessionId = (String) ReplicantRequestContextHolder.get(ServerConstants.SESSION_ID_KEY);
        final var requestId = (Integer) ReplicantRequestContextHolder.get(ServerConstants.REQUEST_ID_KEY);
        final var session = null != sessionId ? _sessionManager.getSession(sessionId) : null;
        ReplicantRequestContextHolder.clean();
        return _sessionManager.runReplicationInvocation(
                getReplicationInvocationKey(context), session, requestId, context::proceed);
    }

    @NonNull
    private String getReplicationInvocationKey(@NonNull final InvocationContext context) {
        final var method = context.getMethod();
        if (null != method) {
            return method.getDeclaringClass().getName() + "." + method.getName();
        }
        final var constructor = context.getConstructor();
        if (null != constructor) {
            return constructor.getDeclaringClass().getName() + "." + constructor.getName();
        }
        return "Unknown";
    }
}
