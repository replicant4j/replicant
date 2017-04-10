package org.realityforge.replicant.client.runtime;

import java.util.Objects;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import org.realityforge.replicant.client.transport.DataLoaderService;

/**
 * Container describing the DataLoaderService and state in client.
 */
public final class DataLoaderEntry
{
  /**
   * The cost to attempt to modify action on DataLoader.
   */
  private static final int ACTION_COST = 10000;
  private static final int REQUIRED_REGEN_PER_MILLISECOND = ACTION_COST / 1000;
  private static final int OPTIONAL_REGEN_PER_MILLISECOND = REQUIRED_REGEN_PER_MILLISECOND / 5;

  @Nonnull
  private final DataLoaderService _service;
  /**
   * Does the system require this DataLoader to be present to be operational.
   */
  private final boolean _required;
  private final RateLimitedValue _rateLimiter;

  public DataLoaderEntry( @Nonnull final DataLoaderService service, final boolean required )
  {
    _service = Objects.requireNonNull( service );
    _required = required;

    final int regenRate = required ? REQUIRED_REGEN_PER_MILLISECOND : OPTIONAL_REGEN_PER_MILLISECOND;
    _rateLimiter = new RateLimitedValue( regenRate, ACTION_COST * 2 );
  }

  public boolean attemptAction( @Nonnull final Consumer<DataLoaderService> action )
  {
    return getRateLimiter().attempt( ACTION_COST, () -> action.accept( getService() ) );
  }

  @Nonnull
  final RateLimitedValue getRateLimiter()
  {
    return _rateLimiter;
  }

  @Nonnull
  public DataLoaderService getService()
  {
    return _service;
  }

  public boolean isRequired()
  {
    return _required;
  }
}
