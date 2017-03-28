package org.realityforge.replicant.client.ee;

import java.util.Objects;
import javax.annotation.Nonnull;
import org.realityforge.replicant.client.transport.DataLoadStatus;

public final class DataLoadCompleteEvent
  extends AbstractDataLoaderEvent
{
  private final DataLoadStatus _status;

  public DataLoadCompleteEvent( @Nonnull final DataLoadStatus status )
  {
    super( status.getSystemKey() );
    _status = Objects.requireNonNull( status );
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
