package org.realityforge.replicant.client.transport;

import javax.annotation.Nonnull;

/**
 * A record of a request sent to the server that we need to keep track of.
 */
public class RequestEntry
{
  private final String _requestID;
  private final boolean _bulkLoad;
  private boolean _returned;
  private boolean _completed;

  public RequestEntry( @Nonnull final String requestID, final boolean bulkLoad )
  {
    _requestID = requestID;
    _bulkLoad = bulkLoad;
  }

  public String getRequestID()
  {
    return _requestID;
  }

  public boolean isBulkLoad()
  {
    return _bulkLoad;
  }

  public boolean hasReturned()
  {
    return _returned;
  }

  public void returned()
  {
    _returned = true;
  }

  public boolean isCompleted()
  {
    return _completed;
  }

  public void complete()
  {
    returned();
    _completed = true;
  }
}
