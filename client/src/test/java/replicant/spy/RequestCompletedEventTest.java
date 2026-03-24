package replicant.spy;

import java.util.HashMap;
import org.testng.annotations.Test;
import replicant.AbstractReplicantTest;
import replicant.ValueUtil;
import static org.testng.Assert.*;

public class RequestCompletedEventTest
  extends AbstractReplicantTest
{
  @Test
  public void basicOperation()
  {
    final var requestId = ValueUtil.randomInt();
    final var name = ValueUtil.randomString();
    final var expectingResults = ValueUtil.randomBoolean();
    final var resultsArrived = ValueUtil.randomBoolean();
    final var event =
      new RequestCompletedEvent( 23, "Rose", requestId, name, expectingResults, resultsArrived );

    assertEquals( event.getRequestId(), requestId );
    assertEquals( event.getName(), name );
    assertEquals( event.isExpectingResults(), expectingResults );
    assertEquals( event.haveResultsArrived(), resultsArrived );

    final var data = new HashMap<String, Object>();
    event.toMap( data );

    assertEquals( data.get( "type" ), "Connector.RequestCompleted" );
    assertEquals( data.get( "schema.id" ), 23 );
    assertEquals( data.get( "schema.name" ), "Rose" );
    assertEquals( data.get( "requestId" ), requestId );
    assertEquals( data.get( "name" ), name );
    assertEquals( data.get( "expectingResults" ), expectingResults );
    assertEquals( data.get( "resultsArrived" ), resultsArrived );

    assertEquals( data.size(), 7 );
  }
}
