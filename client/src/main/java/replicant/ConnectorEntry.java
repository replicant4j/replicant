package replicant;

import arez.Disposable;
import arez.Observer;
import java.util.Objects;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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
  static final int REQUIRED_REGEN_PER_MILLISECOND = ACTION_COST;
  static final int OPTIONAL_REGEN_PER_MILLISECOND = REQUIRED_REGEN_PER_MILLISECOND / 5;

  @Nonnull
  private final Connector _connector;
  /**
   * The monitor observer that will remove connector from context if it is independent disposed.
   */
  @Nullable
  private Observer _monitor;
  /**
   * Does the system require this DataLoader to be present to be operational.
   */
  private boolean _required;
  private final RateLimitedValue _rateLimiter;

  ConnectorEntry( @Nonnull final Connector connector, final boolean required )
  {
    _connector = Objects.requireNonNull( connector );
    _required = required;

    final int regenRate = required ? REQUIRED_REGEN_PER_MILLISECOND : OPTIONAL_REGEN_PER_MILLISECOND;
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
   * Set the monitor that will remove Connector from the ReplicantRuntime when the Connector is disposed.
   *
   * @param monitor the observer that monitors Connector for dispose.
   */
  void setMonitor( @Nonnull final Observer monitor )
  {
    _monitor = Objects.requireNonNull( monitor );
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void dispose()
  {
    if ( null != _monitor )
    {
      _monitor.dispose();
      _monitor = null;
    }
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
