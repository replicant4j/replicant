package replicant.spy;

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
    final int requestId = ValueUtil.randomInt();
    final String name = ValueUtil.randomString();
    final RequestStartedEvent event =
      new RequestStartedEvent( 23, "Rose", requestId, name );

    assertEquals( event.getSchemaId(), 23 );
    assertEquals( event.getSchemaName(), "Rose" );
    assertEquals( event.getRequestId(), requestId );
    assertEquals( event.getName(), name );

    final HashMap<String, Object> data = new HashMap<>();
    safeAction( () -> event.toMap( data ) );

    assertEquals( data.get( "type" ), "Connector.RequestStarted" );
    assertEquals( data.get( "schema.id" ), 23 );
    assertEquals( data.get( "schema.name" ), "Rose" );
    assertEquals( data.get( "requestId" ), requestId );
    assertEquals( data.get( "name" ), name );

    assertEquals( data.size(), 5 );
  }
}
