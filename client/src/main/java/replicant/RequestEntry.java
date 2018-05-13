package replicant;

import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.braincheck.Guards;

/**
 * A record of a request sent to the server that we need to keep track of.
 */
public class RequestEntry
{
  //TODO: Make this package access after all classes migrated to replicant package
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

  public void setNormalCompletionAction( @Nullable final SafeProcedure completionAction )
  {
    _normalCompletion = true;
    _completionAction = completionAction;
  }

  public void setNonNormalCompletionAction( @Nullable final SafeProcedure completionAction )
  {
    _normalCompletion = false;
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
      Guards.invariant( this::isCompletionDataPresent,
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
      final String name = null == _name ? "?" : _name;
      return "Request(" + name + ")[ID=" + _requestId +
             ( ( null != _cacheKey ? ",Cache=" + _cacheKey : "" ) ) + "]";
    }
    else
    {
      return super.toString();
    }
  }
}
