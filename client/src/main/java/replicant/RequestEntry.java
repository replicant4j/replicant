package replicant;

import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import static org.realityforge.braincheck.Guards.*;

/**
 * A record of a request sent to the server that we need to keep track of.
 */
public class RequestEntry
{
  //TODO: Make this package access after all classes migrated to replicant package
  @Nonnull
  private final String _requestId;
  @Nullable
  private final String _name;
  @Nullable
  private final String _cacheKey;
  private Boolean _normalCompletion;
  private boolean _expectingResults;
  private boolean _resultsArrived;
  private SafeProcedure _completionAction;

  public RequestEntry( @Nonnull final String requestId,
                       @Nullable final String name,
                       @Nullable final String cacheKey )
  {
    if ( Replicant.shouldCheckInvariants() )
    {
      invariant( () -> Replicant.areNamesEnabled() || null == name,
                 () -> "Replicant-0064: RequestEntry passed a name '" + name +
                       "' but Replicant.areNamesEnabled() is false" );
    }
    _requestId = Objects.requireNonNull( requestId );
    _name = Replicant.areNamesEnabled() ? Objects.requireNonNull( name ) : null;
    _cacheKey = cacheKey;
  }

  @Nonnull
  public String getRequestId()
  {
    return _requestId;
  }

  @Nonnull
  public String getName()
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
  public String getCacheKey()
  {
    return _cacheKey;
  }

  @Nullable
  public SafeProcedure getCompletionAction()
  {
    return _completionAction;
  }

  public void setNormalCompletion( final boolean normalCompletion )
  {
    _normalCompletion = normalCompletion;
  }

  public void setCompletionAction( @Nonnull final SafeProcedure completionAction )
  {
    _completionAction = completionAction;
  }

  public boolean isCompletionDataPresent()
  {
    return null != _normalCompletion;
  }

  public boolean isNormalCompletion()
  {
    if ( Replicant.shouldCheckInvariants() )
    {
      invariant( this::isCompletionDataPresent,
                 () -> "Replicant-0008: isNormalCompletion invoked before completion data specified." );
    }
    return _normalCompletion;
  }

  public boolean isExpectingResults()
  {
    return _expectingResults;
  }

  public void setExpectingResults( final boolean expectingResults )
  {
    _expectingResults = expectingResults;
  }

  public boolean haveResultsArrived()
  {
    return _resultsArrived;
  }

  public void markResultsAsArrived()
  {
    _resultsArrived = true;
  }

  @Override
  public String toString()
  {
    if ( Replicant.areNamesEnabled() )
    {
      return "Request(" + getName() + ")[ID=" + _requestId +
             ( ( null != _cacheKey ? ",Cache=" + _cacheKey : "" ) ) + "]";
    }
    else
    {
      return super.toString();
    }
  }
}
