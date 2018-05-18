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
  private final int _schemaId;
  @Nonnull
  private final String _schemaName;
  @Nonnull
  private final String _requestId;
  @Nonnull
  private final String _name;
  private final boolean _normalCompletion;
  private final boolean _expectingResults;
  private final boolean _resultsArrived;

  public RequestCompletedEvent( final int schemaId,
                                @Nonnull final String schemaName,
                                @Nonnull final String requestId,
                                @Nonnull final String name,
                                final boolean normalCompletion,
                                final boolean expectingResults,
                                final boolean resultsArrived )
  {
    _schemaId = schemaId;
    _schemaName = Objects.requireNonNull( schemaName );
    _requestId = Objects.requireNonNull( requestId );
    _name = Objects.requireNonNull( name );
    _normalCompletion = normalCompletion;
    _expectingResults = expectingResults;
    _resultsArrived = resultsArrived;
  }

  public int getSchemaId()
  {
    return _schemaId;
  }

  @Nonnull
  public String getSchemaName()
  {
    return _schemaName;
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
    map.put( "schema.id", getSchemaId() );
    map.put( "schema.name", getSchemaName() );
    map.put( "requestId", getRequestId() );
    map.put( "name", getName() );
    map.put( "normalCompletion", isNormalCompletion() );
    map.put( "expectingResults", isExpectingResults() );
    map.put( "resultsArrived", haveResultsArrived() );
  }
}
