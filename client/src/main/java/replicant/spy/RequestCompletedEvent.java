package replicant.spy;

import arez.spy.SerializableEvent;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.NonNull;

/**
 * Notification when a Request completes.
 */
public final class RequestCompletedEvent
  implements SerializableEvent
{
  private final int _schemaId;
  @NonNull
  private final String _schemaName;
  private final int _requestId;
  @NonNull
  private final String _name;
  private final boolean _expectingResults;
  private final boolean _resultsArrived;

  public RequestCompletedEvent( final int schemaId,
                                @NonNull final String schemaName,
                                final int requestId,
                                @NonNull final String name,
                                final boolean expectingResults,
                                final boolean resultsArrived )
  {
    _schemaId = schemaId;
    _schemaName = Objects.requireNonNull( schemaName );
    _requestId = requestId;
    _name = Objects.requireNonNull( name );
    _expectingResults = expectingResults;
    _resultsArrived = resultsArrived;
  }

  public int getSchemaId()
  {
    return _schemaId;
  }

  @NonNull
  public String getSchemaName()
  {
    return _schemaName;
  }

  public int getRequestId()
  {
    return _requestId;
  }

  @NonNull
  public String getName()
  {
    return _name;
  }

  public boolean isExpectingResults()
  {
    return _expectingResults;
  }

  public boolean haveResultsArrived()
  {
    return _resultsArrived;
  }

  @Override
  public void toMap( @NonNull final Map<String, Object> map )
  {
    map.put( "type", "Connector.RequestCompleted" );
    map.put( "schema.id", getSchemaId() );
    map.put( "schema.name", getSchemaName() );
    map.put( "requestId", getRequestId() );
    map.put( "name", getName() );
    map.put( "expectingResults", isExpectingResults() );
    map.put( "resultsArrived", haveResultsArrived() );
  }
}
