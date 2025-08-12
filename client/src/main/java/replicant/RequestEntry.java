package replicant;

import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import static org.realityforge.braincheck.Guards.*;

/**
 * A record of a request sent to the server that we need to keep track of.
 */
final class RequestEntry
{
  private final int _requestId;
  @Nullable
  private final String _name;
  private final boolean _syncRequest;
  @Nullable
  private final ResponseHandler _responseHandler;

  RequestEntry( final int requestId,
                @Nullable final String name,
                final boolean syncRequest,
                @Nullable final ResponseHandler responseHandler )
  {
    _responseHandler = responseHandler;
    if ( Replicant.shouldCheckInvariants() )
    {
      invariant( () -> Replicant.areNamesEnabled() || null == name,
                 () -> "Replicant-0041: RequestEntry passed a name '" + name +
                       "' but Replicant.areNamesEnabled() is false" );
    }
    _requestId = requestId;
    _name = Replicant.areNamesEnabled() ? Objects.requireNonNull( name ) : null;
    _syncRequest = syncRequest;
  }

  int getRequestId()
  {
    return _requestId;
  }

  boolean isSyncRequest()
  {
    return _syncRequest;
  }

  @Nonnull
  String getName()
  {
    if ( Replicant.shouldCheckApiInvariants() )
    {
      apiInvariant( Replicant::areNamesEnabled,
                    () -> "Replicant-0043: RequestEntry.getName() invoked when Replicant.areNamesEnabled() is false" );
    }
    assert null != _name;
    return _name;
  }

  @Nullable
  ResponseHandler getResponseHandler()
  {
    return _responseHandler;
  }

  @Override
  public String toString()
  {
    if ( Replicant.areNamesEnabled() )
    {
      return "Request(" + getName() + ")[Id=" + _requestId + "]";
    }
    else
    {
      return super.toString();
    }
  }
}
