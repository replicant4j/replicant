package org.realityforge.replicant.client.transport;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A record of a request sent to the server that we need to keep track of.
 */
public class RequestEntry
{
  private final Integer _requestId;
  @Nullable
  private final String _requestKey;
  @Nullable
  private final String _cacheKey;
  private Boolean _normalCompletion;
  private boolean _expectingResults;
  private boolean _resultsArrived;
  private Runnable _completionAction;

  public RequestEntry( @Nonnull final Integer requestId,
                       @Nullable final String requestKey,
                       @Nullable final String cacheKey )
  {
    _requestId = requestId;
    _requestKey = requestKey;
    _cacheKey = cacheKey;
  }

  @Nonnull
  public Integer getRequestId()
  {
    return _requestId;
  }

  @Nullable
  public String getRequestKey()
  {
    return _requestKey;
  }

  @Nullable
  public String getCacheKey()
  {
    return _cacheKey;
  }

  public boolean haveResultsArrived()
  {
    return _resultsArrived;
  }

  public void setNormalCompletionAction( @Nullable final Runnable completionAction )
  {
    _normalCompletion = true;
    _completionAction = completionAction;
  }

  public void setNonNormalCompletionAction( @Nullable final Runnable completionAction )
  {
    _normalCompletion = false;
    _completionAction = completionAction;
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
  public Runnable getCompletionAction()
  {
    return _completionAction;
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
    final String requestKey = null == _requestKey ? "?" : _requestKey;
    return "Request(" + requestKey + ")[ID=" + _requestId +
           ( ( null != _cacheKey ? ",Cache=" + _cacheKey : "" ) ) + "]";
  }
}
