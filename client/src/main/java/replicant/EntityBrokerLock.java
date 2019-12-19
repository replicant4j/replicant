package replicant;

import javax.annotation.Nonnull;

public final class EntityBrokerLock
{
  @Nonnull
  private final EntityChangeBroker _changeBroker;
  private final boolean _disable;
  private boolean _released;

  EntityBrokerLock( @Nonnull final EntityChangeBroker changeBroker, final boolean disable )
  {
    _changeBroker = changeBroker;
    _disable = disable;
  }

  public void release()
  {
    if ( !_released )
    {
      _released = true;
      if ( _disable )
      {
        _changeBroker.enable();
      }
      else
      {
        _changeBroker.resume();
      }
    }
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
