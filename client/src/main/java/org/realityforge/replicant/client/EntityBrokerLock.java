package org.realityforge.replicant.client;

public final class EntityBrokerLock
{
  private final boolean _disable;

  EntityBrokerLock( final boolean disable )
  {
    _disable = disable;
  }

  public boolean isDisableAction()
  {
    return _disable;
  }

  public boolean isPauseAction()
  {
    return !isDisableAction();
  }
}
