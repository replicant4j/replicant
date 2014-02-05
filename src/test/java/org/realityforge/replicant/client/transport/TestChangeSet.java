package org.realityforge.replicant.client.transport;

import javax.annotation.Nullable;
import org.realityforge.replicant.client.Change;
import org.realityforge.replicant.client.ChangeSet;

final class TestChangeSet
  implements ChangeSet
{
  private final int _sequence;
  private final boolean _bulkChange;
  private final Runnable _runnable;
  private String _requestID;
  private String _cacheKey;
  private String _etag;
  private final Change[] _changes;

  TestChangeSet( final int sequence,
                 @Nullable final Runnable runnable,
                 final boolean bulkChange,
                 final Change[] changes )
  {
    _sequence = sequence;
    _runnable = runnable;
    _bulkChange = bulkChange;
    _changes = changes;
  }

  public String getCacheKey()
  {
    return _cacheKey;
  }

  public void setCacheKey( final String cacheKey )
  {
    _cacheKey = cacheKey;
  }

  public void setRequestID( final String requestID )
  {
    _requestID = requestID;
  }

  public void setEtag( final String etag )
  {
    _etag = etag;
  }

  @Nullable
  @Override
  public String getETag()
  {
    return _etag;
  }

  public boolean isResponseToRequest()
  {
    return null != _runnable;
  }

  public Runnable getRunnable()
  {
    return _runnable;
  }

  public boolean isBulkChange()
  {
    return _bulkChange;
  }

  @Override
  public int getSequence()
  {
    return _sequence;
  }

  @Override
  public String getRequestID()
  {
    return _requestID;
  }

  @Override
  public int getChangeCount()
  {
    return _changes.length;
  }

  @Override
  public Change getChange( final int index )
  {
    return _changes[ index ];
  }

  @Override
  public String toString()
  {
    return "ChangeSet:" + System.identityHashCode( this );
  }
}
