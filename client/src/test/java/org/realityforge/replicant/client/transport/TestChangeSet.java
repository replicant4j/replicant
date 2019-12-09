package org.realityforge.replicant.client.transport;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.replicant.client.Change;
import org.realityforge.replicant.client.ChangeSet;
import org.realityforge.replicant.client.ChannelAction;

final class TestChangeSet
  implements ChangeSet
{
  private final int _sequence;
  private final Runnable _runnable;
  private final ChannelAction[] _actions;
  private Integer _requestId;
  private String _cacheKey;
  private String _etag;
  private final Change[] _changes;

  TestChangeSet( final int sequence, @Nullable final Runnable runnable, final Change[] changes )
  {
    this( sequence, runnable, changes, new ChannelAction[ 0 ] );
  }


  TestChangeSet( final int sequence,
                 @Nullable final Runnable runnable,
                 final Change[] changes,
                 final ChannelAction[] actions )
  {
    _sequence = sequence;
    _runnable = runnable;
    _changes = changes;
    _actions = actions;
  }

  String getCacheKey()
  {
    return _cacheKey;
  }

  void setCacheKey( @Nonnull final String cacheKey )
  {
    _cacheKey = cacheKey;
  }

  void setRequestId( final Integer requestId )
  {
    _requestId = requestId;
  }

  void setEtag( @Nonnull final String etag )
  {
    _etag = etag;
  }

  @Nullable
  @Override
  public String getETag()
  {
    return _etag;
  }

  boolean isResponseToRequest()
  {
    return null != _runnable;
  }

  Runnable getRunnable()
  {
    return _runnable;
  }

  @Override
  public int getSequence()
  {
    return _sequence;
  }

  @Override
  @Nullable
  public Integer getRequestId()
  {
    return _requestId;
  }

  @Override
  public int getChangeCount()
  {
    return _changes.length;
  }

  @Nonnull
  @Override
  public Change getChange( final int index )
  {
    return _changes[ index ];
  }

  @Override
  public int getChannelActionCount()
  {
    return _actions.length;
  }

  @Nonnull
  @Override
  public ChannelAction getChannelAction( final int index )
  {
    return _actions[ index ];
  }

  @Override
  public String toString()
  {
    return "ChangeSet:" + System.identityHashCode( this );
  }
}
