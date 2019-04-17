package replicant;

import arez.Disposable;
import java.util.Objects;
import java.util.function.Consumer;
import javax.annotation.Nonnull;

/**
 * Container describing the Container and state in client.
 */
final class ConnectorEntry
  implements Disposable
{
  /**
   * The cost to attempt to modify action on DataLoader.
   */
  private static final int ACTION_COST = 10000;
  static final int REQUIRED_REGEN_PER_SECOND = ACTION_COST;
  static final int OPTIONAL_REGEN_PER_SECOND = REQUIRED_REGEN_PER_SECOND / 5;
  @Nonnull
  private final Connector _connector;
  /**
   * Does the system require this DataLoader to be present to be operational.
   */
  private boolean _required;
  private final RateLimitedValue _rateLimiter;

  ConnectorEntry( @Nonnull final Connector connector, final boolean required )
  {
    _connector = Objects.requireNonNull( connector );
    _required = required;

    final int regenRate = required ? REQUIRED_REGEN_PER_SECOND : OPTIONAL_REGEN_PER_SECOND;
    _rateLimiter = new RateLimitedValue( System.currentTimeMillis(), regenRate, ACTION_COST * 2 );
  }

  boolean attemptAction( @Nonnull final Consumer<Connector> action )
  {
    return getRateLimiter().attempt( System.currentTimeMillis(), ACTION_COST, () -> action.accept( getConnector() ) );
  }

  @Nonnull
  RateLimitedValue getRateLimiter()
  {
    return _rateLimiter;
  }

  /**
   * Return the Connector the entry represents.
   *
   * @return the Connector the entry represents.
   */
  @Nonnull
  Connector getConnector()
  {
    return _connector;
  }

  boolean isRequired()
  {
    return _required;
  }

  void setRequired( final boolean required )
  {
    _required = required;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void dispose()
  {
    Disposable.dispose( _connector );
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isDisposed()
  {
    return Disposable.isDisposed( _connector );
  }
}
