package replicant;

import java.util.Objects;
import javax.annotation.Nonnull;
import static org.realityforge.braincheck.Guards.*;

public final class Request
{
  @Nonnull
  private final Connection _connection;
  @Nonnull
  private final RequestEntry _entry;

  public Request( @Nonnull final Connection connection, @Nonnull final RequestEntry entry )
  {
    _connection = Objects.requireNonNull( connection );
    _entry = Objects.requireNonNull( entry );
  }

  @Nonnull
  public String getConnectionId()
  {
    return _connection.getConnectionId();
  }

  public int getRequestId()
  {
    return _entry.getRequestId();
  }

  public final void onSuccess( final boolean messageComplete, @Nonnull final SafeProcedure onSuccess )
  {
    if ( Replicant.shouldCheckApiInvariants() )
    {
      apiInvariant( () -> !_entry.hasCompleted(),
                    () -> "Replicant-0073: Request.onSuccess invoked on completed request " + _entry + "." );
    }
    _entry.setNormalCompletion( true );
    _entry.setExpectingResults( !messageComplete );
    _connection.completeRequest( _entry, Objects.requireNonNull( onSuccess ) );
  }

  public void onFailure( @Nonnull final SafeProcedure onError )
  {
    if ( Replicant.shouldCheckApiInvariants() )
    {
      apiInvariant( () -> !_entry.hasCompleted(),
                    () -> "Replicant-0074: Request.onFailure invoked on completed request " + _entry + "." );
    }
    _entry.setNormalCompletion( false );
    _entry.setExpectingResults( false );
    _connection.completeRequest( _entry, Objects.requireNonNull( onError ) );
  }

  @Nonnull
  RequestEntry getEntry()
  {
    return _entry;
  }
}
