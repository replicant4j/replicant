package org.realityforge.replicant.client;

/**
 * Container that defies a deferred action on listener set.
 */
final class DeferredListenerAction
{
  private final Object _key;
  private final EntityChangeListener _listener;
  private final boolean _remove;

  DeferredListenerAction( final Object key, final EntityChangeListener listener, final boolean remove )
  {
    _key = key;
    _listener = listener;
    _remove = remove;
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
    return _remove;
  }
}
