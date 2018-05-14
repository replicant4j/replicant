package replicant.spy;

import arez.Arez;
import java.util.HashMap;
import org.realityforge.guiceyloops.shared.ValueUtil;
import org.testng.annotations.Test;
import replicant.AbstractReplicantTest;
import static org.testng.Assert.*;

public class RequestStartedEventTest
  extends AbstractReplicantTest
{
  @Test
  public void basicOperation()
  {
    final String requestId = ValueUtil.randomString();
    final String name = ValueUtil.randomString();
    final RequestStartedEvent event =
      new RequestStartedEvent( G.class, requestId, name );

    assertEquals( event.getRequestId(), requestId );
    assertEquals( event.getName(), name );

    final HashMap<String, Object> data = new HashMap<>();
    Arez.context().safeAction( () -> event.toMap( data ) );

    assertEquals( data.get( "type" ), "Connector.RequestStarted" );
    assertEquals( data.get( "systemType" ), "G" );
    assertEquals( data.get( "requestId" ), requestId );
    assertEquals( data.get( "name" ), name );

    assertEquals( data.size(), 4 );
  }

  enum G
  {
    G1
  }
}
