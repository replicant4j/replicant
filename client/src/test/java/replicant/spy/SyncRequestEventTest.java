package replicant.spy;

import java.util.HashMap;
import org.testng.annotations.Test;
import replicant.AbstractReplicantTest;
import static org.testng.Assert.*;

public class SyncRequestEventTest
  extends AbstractReplicantTest
{
  @Test
  public void basicOperation()
  {
    final var event = new SyncRequestEvent( 23 );

    assertEquals( event.getSchemaId(), 23 );

    final var data = new HashMap<String, Object>();
    event.toMap( data );

    assertEquals( data.get( "type" ), "Connector.SyncRequest" );
    assertEquals( data.get( "schema.id" ), 23 );
    assertEquals( data.size(), 2 );
  }
}
