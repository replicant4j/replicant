package org.realityforge.replicant.client.ee;

import java.util.Objects;
import javax.annotation.Nonnull;

public abstract class AbstractDataLoaderErrorEvent
  extends AbstractDataLoaderEvent
{
  private final Throwable _throwable;

  public AbstractDataLoaderErrorEvent( @Nonnull final String systemKey, @Nonnull final Throwable throwable )
  {
    super( systemKey );
    _throwable = Objects.requireNonNull( throwable );
  }

  @Nonnull
  public final Throwable getThrowable()
  {
    return _throwable;
  }
}
