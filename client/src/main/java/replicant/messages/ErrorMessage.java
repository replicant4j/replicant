package replicant.messages;

import java.util.HashSet;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;
import org.realityforge.replicant.shared.Messages;
import replicant.Replicant;
import static org.realityforge.braincheck.Guards.*;

/**
 * The message that represents a set of changes to subscriptions and entities that should be applied atomically.
 */
@SuppressFBWarnings( "EI_EXPOSE_REP" )
@JsType( isNative = true, namespace = JsPackage.GLOBAL, name = "Object" )
public class ErrorMessage
  extends ServerToClientMessage
{
  @JsOverlay
  public static final String TYPE = Messages.S2C_Type.ERROR;

  @Nullable
  private String message;

  @JsOverlay
  @Nullable
  public final String getMessage()
  {
    return message;
  }
}
