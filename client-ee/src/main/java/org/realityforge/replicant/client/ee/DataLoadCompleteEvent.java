package org.realityforge.replicant.client.ee;

import javax.annotation.Nonnull;
import org.realityforge.replicant.client.transport.DataLoadStatus;

public final class DataLoadCompleteEvent
{
  private final DataLoadStatus _status;

  @SuppressWarnings( "ConstantConditions" )
  public DataLoadCompleteEvent( @Nonnull final DataLoadStatus status )
  {
    if ( null == status )
    {
      throw new IllegalArgumentException( "status is null" );
    }

    _status = status;
  }

  @Nonnull
  public DataLoadStatus getStatus()
  {
    return _status;
  }

  public String toString()
  {
    return "DataLoadComplete[Status=" + _status + "]";
  }
}
