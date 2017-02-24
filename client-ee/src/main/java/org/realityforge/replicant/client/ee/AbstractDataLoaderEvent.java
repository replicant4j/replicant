package org.realityforge.replicant.client.ee;

import javax.annotation.Nonnull;

public abstract class AbstractDataLoaderEvent
{
  private final String _systemKey;

  @SuppressWarnings( "ConstantConditions" )
  public AbstractDataLoaderEvent( @Nonnull final String systemKey )
  {
    if ( null == systemKey )
    {
      throw new IllegalArgumentException( "systemKey is null" );
    }
    _systemKey = systemKey;
  }

  @Nonnull
  public final String getSystemKey()
  {
    return _systemKey;
  }
}
