package replicant;

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
  Object getKey()
  {
    return _key;
  }

  @Nullable
  EntityChangeListener getListener()
  {
    return _listener;
  }

  ActionType getActionType()
  {
    return _actionType;
  }

  boolean isRemove()
  {
    return ActionType.REMOVE == getActionType();
  }

  boolean isAdd()
  {
    return ActionType.ADD == getActionType();
  }

  boolean isPurge()
  {
    return ActionType.PURGE == getActionType();
  }
}
