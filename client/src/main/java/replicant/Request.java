package replicant;

import java.util.Objects;
import javax.annotation.Nonnull;

public final class Request
{
  @Nonnull
  private final Connection _connection;
  @Nonnull
  private final RequestEntry _entry;

  Request( @Nonnull final Connection connection, @Nonnull final RequestEntry entry )
  {
    _connection = Objects.requireNonNull( connection );
    _entry = Objects.requireNonNull( entry );
  }

  @Nonnull
  public String getConnectionId()
  {
    return _connection.ensureConnectionId();
  }

  public int getRequestId()
  {
    return _entry.getRequestId();
  }
}
