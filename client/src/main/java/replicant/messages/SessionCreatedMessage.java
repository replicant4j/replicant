package replicant.messages;

import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;
import org.jspecify.annotations.NonNull;
import replicant.shared.Messages;

/**
 * The server-to-client message that supplies the Replicant Session ID of a newly created Replicant Session.
 */
@SuppressWarnings({"NotNullFieldNotInitialized", "NullAway.Init"})
@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
public class SessionCreatedMessage extends ServerToClientMessage {
    @JsOverlay
    public static final String TYPE = Messages.S2C_Type.SESSION_CREATED;

    @NonNull
    private String sessionId;

    @JsOverlay
    @NonNull
    public static SessionCreatedMessage create(@NonNull final String sessionId) {
        final SessionCreatedMessage message = new SessionCreatedMessage();
        assert null != sessionId;
        message.type = TYPE;
        message.requestId = null;
        message.sessionId = sessionId;
        return message;
    }

    @JsOverlay
    @NonNull
    public final String getSessionId() {
        return sessionId;
    }
}
