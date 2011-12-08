package org.realityforge.replicant.client;

/**
 * Container that defies a deferred action on listener set.
 */
final class DeferredListenerAction
{
  static enum ActionType
  {
    ADD, REMOVE, PURGE
  }

  private final Object _key;
  private final EntityChangeListener _listener;
  private final ActionType _actionType;

  DeferredListenerAction( final Object key, final EntityChangeListener listener, final ActionType actionType )
  {
    _key = key;
    _listener = listener;
    _actionType = actionType;
  }

  public Object getKey()
  {
    return _key;
  }

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
