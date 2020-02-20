package replicant;

import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

final class PendingRequest
{
  @Nullable
  private final String _name;
  @Nonnull
  private final SafeProcedure _callback;

  PendingRequest( @Nullable final String name, @Nonnull final SafeProcedure callback )
  {
    _name = name;
    _callback = Objects.requireNonNull( callback );
  }

  @Nullable
  String getName()
  {
    return _name;
  }

  @Nonnull
  SafeProcedure getCallback()
  {
    return _callback;
  }
}
