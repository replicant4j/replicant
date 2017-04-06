package org.realityforge.replicant.client.transport;

import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

final class AreaOfInterestEntry
{
  @Nonnull
  private final Enum _graph;
  @Nonnull
  private final AreaOfInterestAction _action;
  @Nullable
  private final Object _id;
  @Nullable
  private final Object _filterParameter;
  @Nullable
  private final Runnable _userAction;
  private boolean _inProgress;

  AreaOfInterestEntry( @Nonnull final Enum graph,
                       @Nonnull final AreaOfInterestAction action,
                       @Nullable final Object id,
                       @Nullable final Object filterParameter,
                       @Nullable final Runnable userAction )
  {
    _graph = Objects.requireNonNull( graph );
    _action = Objects.requireNonNull( action );
    _id = id;
    _filterParameter = filterParameter;
    _userAction = userAction;
  }

  @Nonnull
  Enum getGraph()
  {
    return _graph;
  }

  @Nonnull
  AreaOfInterestAction getAction()
  {
    return _action;
  }

  @Nonnull
  String getCacheKey()
  {
    return null == _id ? getGraph().name() : getGraph().name() + "/" + _id;
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
           ",filter=" + _filterParameter +
           "]" + ( _inProgress ? "(InProgress)" : "" );
  }
}
