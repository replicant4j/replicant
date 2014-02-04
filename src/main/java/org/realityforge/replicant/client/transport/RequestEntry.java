package org.realityforge.replicant.client.transport;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A record of a request sent to the server that we need to keep track of.
 */
public class RequestEntry
{
  private final String _requestID;
  @Nullable
  private final String _cacheKey;
  private final boolean _bulkLoad;
  private Boolean _normalCompletion;
  private boolean _expectingResults;
  private boolean _resultsArrived;
  private Runnable _runnable;

  public RequestEntry( @Nonnull final String requestID, @Nullable final String cacheKey, final boolean bulkLoad )
  {
    _requestID = requestID;
    _cacheKey = cacheKey;
    _bulkLoad = bulkLoad;
  }

  public String getRequestID()
  {
    return _requestID;
  }

  @Nullable
  public String getCacheKey()
  {
    return _cacheKey;
  }

  public boolean isBulkLoad()
  {
    return _bulkLoad;
  }

  public boolean haveResultsArrived()
  {
    return _resultsArrived;
  }

  public void setNormalCompletionAction( @Nullable final Runnable runnable )
  {
    _normalCompletion = true;
    _runnable = runnable;
  }

  public void setNonNormalCompletionAction( @Nullable final Runnable runnable )
  {
    _normalCompletion = false;
    _runnable = runnable;
  }

  public boolean isCompletionDataPresent()
  {
    return null != _normalCompletion;
  }

  public boolean isNormalCompletion()
  {
    if ( !isCompletionDataPresent() )
    {
      throw new IllegalStateException( "Completion data not yet specified" );
    }
    return _normalCompletion;
  }

  public boolean isExpectingResults()
  {
    return _expectingResults;
  }

  @Nullable
  public Runnable getRunnable()
  {
    return _runnable;
  }

  public void setExpectingResults( final boolean expectingResults )
  {
    _expectingResults = expectingResults;
  }

  public void markResultsAsArrived()
  {
    _resultsArrived = true;
  }

  @Override
  public String toString()
  {
    return "Request[ID=" + _requestID+ ",Cache=" + _cacheKey + ",Bulk=" + _bulkLoad + "]";
  }
}
