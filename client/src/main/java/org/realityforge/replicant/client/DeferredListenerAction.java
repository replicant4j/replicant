package org.realityforge.replicant.client;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Container that defies a deferred action on listener set.
 */
final class DeferredListenerAction
{
  enum ActionType
  {
    ADD, REMOVE, PURGE
  }

  private final Object _key;
  private final EntityChangeListener _listener;
  private final ActionType _actionType;

  DeferredListenerAction( @Nullable final Object key,
                          @Nullable final EntityChangeListener listener,
                          @Nonnull final ActionType actionType )
  {
    _key = key;
    _listener = listener;
    _actionType = actionType;
  }

  @Nullable
  public Object getKey()
  {
    return _key;
  }

  @Nullable
  public EntityChangeListener getListener()
  {
    return _listener;
  }

  public boolean isRemove()
  {
    return _actionType == ActionType.REMOVE;
  }

  public boolean isAdd()
  {
    return _actionType == ActionType.ADD;
  }

  public boolean isPurge()
  {
    return _actionType == ActionType.PURGE;
  }
}
