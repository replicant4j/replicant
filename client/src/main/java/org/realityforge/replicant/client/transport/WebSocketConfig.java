package org.realityforge.replicant.client.transport;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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
  @Nonnull
  private final Consumer<Runnable> _remoteCallWrapper;
  /**
   * Mechanism via which an authentication token is generated, if any.
   */
  @Nullable
  private final Supplier<String> _authenticationTokenGenerator;

  public WebSocketConfig( @Nonnull final String url,
                          @Nonnull final Consumer<Runnable> remoteCallWrapper,
                          @Nullable final Supplier<String> authenticationTokenGenerator )
  {
    _url = Objects.requireNonNull( url );
    _remoteCallWrapper = Objects.requireNonNull( remoteCallWrapper );
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
    _remoteCallWrapper.accept( action );
  }
}
