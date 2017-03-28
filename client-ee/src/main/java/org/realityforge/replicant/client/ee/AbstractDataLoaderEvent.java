package org.realityforge.replicant.client.ee;

import java.util.Objects;
import javax.annotation.Nonnull;

public abstract class AbstractDataLoaderEvent
{
  private final String _systemKey;

  public AbstractDataLoaderEvent( @Nonnull final String systemKey )
  {
    _systemKey = Objects.requireNonNull( systemKey );
  }

  @Nonnull
  public final String getSystemKey()
  {
    return _systemKey;
  }
}
