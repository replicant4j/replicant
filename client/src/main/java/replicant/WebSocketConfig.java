package replicant;

import java.util.function.Consumer;
import java.util.function.Supplier;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import replicant.shared.SharedConstants;

public final class WebSocketConfig
{
  /**
   * The url of websocket. Typically something like ws://example.com/myapp/api
   */
  @NonNull
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

  @NonNull
  public static WebSocketConfig create( @NonNull final String baseURL )
  {
    return create( baseURL, null, null );
  }

  @NonNull
  public static WebSocketConfig create( @NonNull final String baseURL,
                                        @Nullable final Consumer<Runnable> remoteCallWrapper,
                                        @Nullable final Supplier<String> authenticationTokenGenerator )
  {
    return new WebSocketConfig( baseURL, remoteCallWrapper, authenticationTokenGenerator );
  }

  private WebSocketConfig( @NonNull final String url,
                           @Nullable final Consumer<Runnable> remoteCallWrapper,
                           @Nullable final Supplier<String> authenticationTokenGenerator )
  {
    _url = url.replace( "https://", "wss://" ).replace( "http://", "ws://" ) + SharedConstants.REPLICANT_URL_FRAGMENT;
    _remoteCallWrapper = remoteCallWrapper;
    _authenticationTokenGenerator = authenticationTokenGenerator;
  }

  @NonNull
  public String getUrl()
  {
    return _url;
  }

  @Nullable
  public String getAuthenticationToken()
  {
    return null != _authenticationTokenGenerator ? _authenticationTokenGenerator.get() : null;
  }

  public void remote( @NonNull final Runnable action )
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
}
