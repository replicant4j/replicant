package replicant.spy;

import java.util.HashMap;
import org.realityforge.guiceyloops.shared.ValueUtil;
import org.testng.annotations.Test;
import replicant.AbstractReplicantTest;
import static org.testng.Assert.*;

public class RequestCompletedEventTest
  extends AbstractReplicantTest
{
  @Test
  public void basicOperation()
  {
    final int requestId = ValueUtil.randomInt();
    final String name = ValueUtil.randomString();
    final boolean expectingResults = ValueUtil.randomBoolean();
    final boolean resultsArrived = ValueUtil.randomBoolean();
    final RequestCompletedEvent event =
      new RequestCompletedEvent( 23, "Rose", requestId, name, expectingResults, resultsArrived );

    assertEquals( event.getRequestId(), requestId );
    assertEquals( event.getName(), name );
    assertEquals( event.isExpectingResults(), expectingResults );
    assertEquals( event.haveResultsArrived(), resultsArrived );

    final HashMap<String, Object> data = new HashMap<>();
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
