package org.realityforge.replicant.client.ee;

import javax.annotation.Nonnull;

public final class DataLoadFailureEvent
  extends AbstractDataLoaderErrorEvent
{
  public DataLoadFailureEvent( @Nonnull final String systemKey, @Nonnull final Throwable throwable )
  {
    super( systemKey, throwable );
  }

  public String toString()
  {
    return "DataLoadFailure[systemKey=" + getSystemKey() + ", Throwable=" + getThrowable() + "]";
  }
}
