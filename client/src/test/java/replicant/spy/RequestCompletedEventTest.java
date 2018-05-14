package replicant.spy;

import arez.Arez;
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
    final String requestId = ValueUtil.randomString();
    final String name = ValueUtil.randomString();
    final boolean normalCompletion = ValueUtil.randomBoolean();
    final boolean expectingResults = ValueUtil.randomBoolean();
    final boolean resultsArrived = ValueUtil.randomBoolean();
    final RequestCompletedEvent event =
      new RequestCompletedEvent( requestId, name, normalCompletion, expectingResults, resultsArrived );

    assertEquals( event.getRequestId(), requestId );
    assertEquals( event.getName(), name );
    assertEquals( event.isNormalCompletion(), normalCompletion );
    assertEquals( event.isExpectingResults(), expectingResults );
    assertEquals( event.haveResultsArrived(), resultsArrived );

    final HashMap<String, Object> data = new HashMap<>();
    Arez.context().safeAction( () -> event.toMap( data ) );

    assertEquals( data.get( "type" ), "Connector.RequestCompleted" );
    assertEquals( data.get( "requestId" ), requestId );
    assertEquals( data.get( "name" ), name );
    assertEquals( data.get( "normalCompletion" ), normalCompletion );
    assertEquals( data.get( "expectingResults" ), expectingResults );
    assertEquals( data.get( "resultsArrived" ), resultsArrived );

    assertEquals( data.size(), 6 );
  }
}
