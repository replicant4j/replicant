package org.realityforge.replicant.client.ee;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class AbstractDataLoaderErrorEvent
  extends AbstractDataLoaderEvent
{
  private final Throwable _throwable;

  @SuppressWarnings( "ConstantConditions" )
  public AbstractDataLoaderErrorEvent( @Nonnull final String systemKey,
                                       @Nonnull final Throwable throwable )
  {
    super( systemKey );
    if ( null == throwable )
    {
      throw new IllegalArgumentException( "throwable is null" );
    }
    _throwable = throwable;
  }

  @Nonnull
  public final Throwable getThrowable()
  {
    return _throwable;
  }
}
