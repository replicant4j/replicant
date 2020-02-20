package replicant;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.gwt.keycloak.Keycloak;
import org.realityforge.replicant.shared.SharedConstants;

public final class WebSocketConfig
{
  /**
   * The url of websocket. Typically something like ws://example.com/myapp/api
   */
  @Nonnull
  private final String _url;
  /**
   * The remote call accepts a runnable that is used to invoke the remote invocation.
   * This runnable may be invoked immediately or deferred until later (such as after
   * authentication token has been generated).
   */
  @Nullable
  private final Consumer<Runnable> _remoteCallWrapper;
  /**
   * Mechanism via which an authentication token is generated, if any.
   */
  @Nullable
  private final Supplier<String> _authenticationTokenGenerator;

  @Nonnull
  public static WebSocketConfig create( @Nonnull final String baseURL )
  {
    return new WebSocketConfig( toWebSocketProtocol( baseURL ) + SharedConstants.REPLICANT_URL_FRAGMENT, null, null );
  }

  @Nonnull
  public static WebSocketConfig create( @Nonnull final String baseURL, @Nonnull final Keycloak keycloak )
  {
    return new WebSocketConfig( toWebSocketProtocol( baseURL ) + SharedConstants.REPLICANT_URL_FRAGMENT,
                                keycloak::updateTokenAndExecute,
                                keycloak::getToken );
  }

  private WebSocketConfig( @Nonnull final String url,
                           @Nullable final Consumer<Runnable> remoteCallWrapper,
                           @Nullable final Supplier<String> authenticationTokenGenerator )
  {
    _url = Objects.requireNonNull( url );
    _remoteCallWrapper = remoteCallWrapper;
    _authenticationTokenGenerator = authenticationTokenGenerator;
  }

  @Nonnull
  public String getUrl()
  {
    return _url;
  }

  @Nullable
  public String getAuthenticationToken()
  {
    return null != _authenticationTokenGenerator ? _authenticationTokenGenerator.get() : null;
  }

  public void remote( @Nonnull final Runnable action )
  {
    if ( null == _remoteCallWrapper )
    {
      action.run();
    }
    else
    {
      _remoteCallWrapper.accept( action );
    }
  }

  @Nonnull
  private static String toWebSocketProtocol( @Nonnull final String baseURL )
  {
    return baseURL.replace( "https://", "wss://" ).replace( "http://", "ws://" );
  }
}
