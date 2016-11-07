package org.realityforge.replicant.client;

import javax.annotation.Nonnull;

public final class EntityBrokerTransaction
{
  @Nonnull
  private final String _key;
  private final boolean _disable;

  public EntityBrokerTransaction( @Nonnull final String key, final boolean disable )
  {
    _key = key;
    _disable = disable;
  }

  @Nonnull
  public String getKey()
  {
    return _key;
  }

  public boolean isDisableAction()
  {
    return _disable;
  }

  public boolean isPauseAction()
  {
    return !isDisableAction();
  }

  @Override
  public String toString()
  {
    return "Transaction[key=" + getKey() + ",isDisable=" + isDisableAction() + "]";
  }
}
