package org.realityforge.replicant.client.transport;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

final class AreaOfInterestAction<T extends Enum>
{
  enum Action
  {
    ADD, REMOVE, UPDATE
  }

  @Nonnull
  private final T _graph;
  @Nonnull
  private final Action _action;
  @Nullable
  private final String _cacheKey;
  @Nullable
  private final Object _id;
  @Nullable
  private final Object _filterParameter;
  @Nullable
  private final Runnable _userAction;
  private boolean _inProgress;

  AreaOfInterestAction( @Nonnull final T graph,
                        @Nonnull final Action action,
                        @Nullable final String cacheKey,
                        @Nullable final Object id,
                        @Nullable final Object filterParameter,
                        @Nullable final Runnable userAction )
  {
    _graph = graph;
    _action = action;
    _cacheKey = cacheKey;
    _id = id;
    _filterParameter = filterParameter;
    _userAction = userAction;
  }

  @Nonnull
  T getGraph()
  {
    return _graph;
  }

  @Nonnull
  Action getAction()
  {
    return _action;
  }

  @Nullable
  String getCacheKey()
  {
    return _cacheKey;
  }

  @Nullable
  Object getId()
  {
    return _id;
  }

  @Nullable
  Object getFilterParameter()
  {
    return _filterParameter;
  }

  @Nullable
  Runnable getUserAction()
  {
    return _userAction;
  }

  boolean isInProgress()
  {
    return _inProgress;
  }

  void markAsInProgress()
  {
    _inProgress = true;
  }

  void markAsComplete()
  {
    _inProgress = false;
  }

  @Override
  public String toString()
  {
    return "AOI[" +
           "Graph=" + _graph +
           ",id=" + _id +
           ",CacheKey=" + _cacheKey +
           ",filter=" + _filterParameter +
           "]" + ( _inProgress ? "(InProgress)" : "" );
  }
}
