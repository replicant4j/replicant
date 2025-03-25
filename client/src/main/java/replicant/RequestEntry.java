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
  private Boolean _normalCompletion;
  private boolean _expectingResults;
  private boolean _resultsArrived;
  private SafeProcedure _completionAction;

  RequestEntry( final int requestId, @Nullable final String name, final boolean syncRequest, @Nullable final ResponseHandler responseHandler )
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
  SafeProcedure getCompletionAction()
  {
    return _completionAction;
  }

  void setCompletionAction( @Nonnull final SafeProcedure completionAction )
  {
    _completionAction = completionAction;
  }

  void setNormalCompletion( final boolean normalCompletion )
  {
    _normalCompletion = normalCompletion;
  }

  boolean hasCompleted()
  {
    return null != _normalCompletion;
  }

  boolean isNormalCompletion()
  {
    if ( Replicant.shouldCheckInvariants() )
    {
      invariant( this::hasCompleted,
                 () -> "Replicant-0008: isNormalCompletion invoked before completion data specified." );
    }
    return _normalCompletion;
  }

  boolean isExpectingResults()
  {
    return _expectingResults;
  }

  void setExpectingResults( final boolean expectingResults )
  {
    _expectingResults = expectingResults;
  }

  boolean haveResultsArrived()
  {
    return _resultsArrived;
  }

  void markResultsAsArrived()
  {
    _resultsArrived = true;
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
