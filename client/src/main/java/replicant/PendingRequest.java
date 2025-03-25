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
  @Nullable
  private final ResponseHandler _responseHandler;

  PendingRequest( @Nullable final String name,
                  @Nonnull final SafeProcedure callback,
                  @Nullable final ResponseHandler responseHandler )
  {
    _name = name;
    _callback = Objects.requireNonNull( callback );
    _responseHandler = responseHandler;
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

  @Nullable
  ResponseHandler getResponseHandler()
  {
    return _responseHandler;
  }
}
