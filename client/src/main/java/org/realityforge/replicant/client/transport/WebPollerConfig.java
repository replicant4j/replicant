package org.realityforge.replicant.client.transport;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class WebPollerConfig
{
  /**
   * The base url of rest services. Typically something like http://example.com/myapp/api
   */
  @Nonnull
  private final String _baseUrl;
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

  public WebPollerConfig( @Nonnull final String baseUrl,
                          @Nonnull final Consumer<Runnable> remoteCallWrapper,
                          @Nullable final Supplier<String> authenticationTokenGenerator )
  {
    _baseUrl = Objects.requireNonNull( baseUrl );
    _remoteCallWrapper = Objects.requireNonNull( remoteCallWrapper );
    _authenticationTokenGenerator = authenticationTokenGenerator;
  }

  @Nonnull
  public String getBaseUrl()
  {
    return _baseUrl;
  }

  @Nullable
  public Supplier<String> getAuthenticationTokenGenerator()
  {
    return _authenticationTokenGenerator;
  }

  @Nonnull
  public Consumer<Runnable> getRemoteCallWrapper()
  {
    return _remoteCallWrapper;
  }
}
