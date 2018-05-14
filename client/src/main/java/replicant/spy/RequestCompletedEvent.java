package replicant.spy;

import arez.spy.SerializableEvent;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;

/**
 * Notification when a Request completes.
 */
public final class RequestCompletedEvent
  implements SerializableEvent
{
  @Nonnull
  private final Class<?> _systemType;
  @Nonnull
  private final String _requestId;
  @Nonnull
  private final String _name;
  private final boolean _normalCompletion;
  private final boolean _expectingResults;
  private final boolean _resultsArrived;

  public RequestCompletedEvent( @Nonnull final Class<?> systemType,
                                @Nonnull final String requestId,
                                @Nonnull final String name,
                                final boolean normalCompletion,
                                final boolean expectingResults,
                                final boolean resultsArrived )
  {
    _systemType = Objects.requireNonNull( systemType );
    _requestId = Objects.requireNonNull( requestId );
    _name = Objects.requireNonNull( name );
    _normalCompletion = normalCompletion;
    _expectingResults = expectingResults;
    _resultsArrived = resultsArrived;
  }

  @Nonnull
  public Class<?> getSystemType()
  {
    return _systemType;
  }

  @Nonnull
  public String getRequestId()
  {
    return _requestId;
  }

  @Nonnull
  public String getName()
  {
    return _name;
  }

  public boolean isNormalCompletion()
  {
    return _normalCompletion;
  }

  public boolean isExpectingResults()
  {
    return _expectingResults;
  }

  public boolean haveResultsArrived()
  {
    return _resultsArrived;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void toMap( @Nonnull final Map<String, Object> map )
  {
    map.put( "type", "Connector.RequestCompleted" );
    map.put( "systemType", getSystemType().getSimpleName() );
    map.put( "requestId", getRequestId() );
    map.put( "name", getName() );
    map.put( "normalCompletion", isNormalCompletion() );
    map.put( "expectingResults", isExpectingResults() );
    map.put( "resultsArrived", haveResultsArrived() );
  }
}
