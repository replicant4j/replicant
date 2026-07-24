package replicant.messages;

import java.util.Objects;
import org.jspecify.annotations.NonNull;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;
import replicant.shared.Messages;

/**
 * The message that represents a set of changes to subscriptions and entities that should be applied atomically.
 */
@SuppressWarnings( { "NotNullFieldNotInitialized", "NullAway.Init" } )
@JsType( isNative = true, namespace = JsPackage.GLOBAL, name = "Object" )
public class SessionCreatedMessage
  extends ServerToClientMessage
{
  @JsOverlay
  public static final String TYPE = Messages.S2C_Type.SESSION_CREATED;
  @NonNull
  private String sessionId;

  @JsOverlay
  @NonNull
  public static SessionCreatedMessage create( @NonNull final String sessionId )
  {
    final SessionCreatedMessage changeSet = new SessionCreatedMessage();
    changeSet.type = TYPE;
    changeSet.requestId = null;
    changeSet.sessionId = Objects.requireNonNull( sessionId );
    return changeSet;
  }

  @JsOverlay
  @NonNull
  public final String getSessionId()
  {
    return sessionId;
  }
}
