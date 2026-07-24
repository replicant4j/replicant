package replicant;

import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

final class PendingRequest
{
  @Nullable
  private final String _name;
  @NonNull
  private final SafeProcedure _callback;
  @Nullable
  private final ResponseHandler _responseHandler;

  PendingRequest( @Nullable final String name,
                  @NonNull final SafeProcedure callback,
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

  @NonNull
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
