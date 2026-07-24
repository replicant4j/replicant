package replicant.spy;

import java.util.HashMap;
import org.testng.annotations.Test;
import replicant.AbstractReplicantTest;
import static org.testng.Assert.*;

public class DisconnectFailureEventTest
  extends AbstractReplicantTest
{
  @Test
  public void basicOperation()
  {
    final DisconnectFailureEvent event =
      new DisconnectFailureEvent( 23, "Rose" );

    assertEquals( event.getSchemaId(), 23 );
    assertEquals( event.getSchemaName(), "Rose" );

    final HashMap<String, Object> data = new HashMap<>();
    event.toMap( data );

    assertEquals( data.get( "type" ), "Connector.DisconnectFailure" );
    assertEquals( data.get( "schema.id" ), 23 );
    assertEquals( data.get( "schema.name" ), "Rose" );
    assertEquals( data.size(), 3 );
  }
}
