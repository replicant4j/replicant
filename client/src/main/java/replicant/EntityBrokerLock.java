package replicant;

import java.util.Objects;
import javax.annotation.Nonnull;

public final class EntityBrokerLock
{
  @Nonnull
  private final SafeProcedure _action;
  private boolean _released;

  EntityBrokerLock( @Nonnull final SafeProcedure action )
  {
    _action = Objects.requireNonNull( action );
  }

  public void release()
  {
    if ( !_released )
    {
      _released = true;
      _action.call();
    }
  }
}
