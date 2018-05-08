package org.realityforge.replicant.client.runtime;

import java.util.Objects;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import org.realityforge.replicant.client.transport.DataLoaderService;

/**
 * Container describing the DataLoaderService and state in client.
 */
final class DataLoaderEntry
{
  /**
   * The cost to attempt to modify action on DataLoader.
   */
  private static final int ACTION_COST = 10000;
  static final int REQUIRED_REGEN_PER_MILLISECOND = ACTION_COST;
  static final int OPTIONAL_REGEN_PER_MILLISECOND = REQUIRED_REGEN_PER_MILLISECOND / 5;

  @Nonnull
  private final DataLoaderService _service;
  /**
   * Does the system require this DataLoader to be present to be operational.
   */
  private boolean _required;
  private final RateLimitedValue _rateLimiter;

  DataLoaderEntry( @Nonnull final DataLoaderService service, final boolean required )
  {
    _service = Objects.requireNonNull( service );
    _required = required;

    final int regenRate = required ? REQUIRED_REGEN_PER_MILLISECOND : OPTIONAL_REGEN_PER_MILLISECOND;
    _rateLimiter = new RateLimitedValue( regenRate, ACTION_COST * 2 );
  }

  boolean attemptAction( @Nonnull final Consumer<DataLoaderService> action )
  {
    return getRateLimiter().attempt( ACTION_COST, () -> action.accept( getService() ) );
  }

  @Nonnull
  final RateLimitedValue getRateLimiter()
  {
    return _rateLimiter;
  }

  @Nonnull
  DataLoaderService getService()
  {
    return _service;
  }

  boolean isRequired()
  {
    return _required;
  }

  void setRequired( final boolean required )
  {
    _required = required;
  }
}
