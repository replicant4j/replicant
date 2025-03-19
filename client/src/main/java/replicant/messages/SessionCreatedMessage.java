package replicant.messages;

import java.util.Objects;
import javax.annotation.Nonnull;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;
import org.realityforge.replicant.shared.Messages;

/**
 * The message that represents a set of changes to subscriptions and entities that should be applied atomically.
 */
@SuppressWarnings( "NotNullFieldNotInitialized" )
@SuppressFBWarnings( "EI_EXPOSE_REP" )
@JsType( isNative = true, namespace = JsPackage.GLOBAL, name = "Object" )
public class SessionCreatedMessage
  extends ServerToClientMessage
{
  @JsOverlay
  public static final String TYPE = Messages.S2C_Type.SESSION_CREATED;

  @Nonnull
  private String sessionId;

  @GwtIncompatible
  @Nonnull
  public static SessionCreatedMessage create( @Nonnull final String sessionId )
  {
    final SessionCreatedMessage changeSet = new SessionCreatedMessage();
    changeSet.type = TYPE;
    changeSet.requestId = null;
    changeSet.sessionId = Objects.requireNonNull( sessionId );
    return changeSet;
  }

  @JsOverlay
  @Nonnull
  public final String getSessionId()
  {
    return sessionId;
  }
}
