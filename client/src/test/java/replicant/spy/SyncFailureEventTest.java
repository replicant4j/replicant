package replicant.spy;

import java.util.HashMap;
import org.testng.annotations.Test;
import replicant.AbstractReplicantTest;
import static org.testng.Assert.*;

public class SyncFailureEventTest
  extends AbstractReplicantTest
{
  @Test
  public void basicOperation()
  {
    final SyncFailureEvent event = new SyncFailureEvent( 23, new Error( "Some ERROR" ) );

    assertEquals( event.getSchemaId(), 23 );

    final HashMap<String, Object> data = new HashMap<>();
    event.toMap( data );

    assertEquals( data.get( "type" ), "Connector.SyncFailure" );
    assertEquals( data.get( "schema.id" ), 23 );
    assertEquals( data.get( "message" ), "Some ERROR" );
    assertEquals( data.size(), 3 );
  }

  @Test
  public void basicOperation_ThrowableNoMessage()
  {
    final SyncFailureEvent event = new SyncFailureEvent( 23, new NullPointerException() );

    assertEquals( event.getSchemaId(), 23 );

    final HashMap<String, Object> data = new HashMap<>();
    event.toMap( data );

    assertEquals( data.get( "type" ), "Connector.SyncFailure" );
    assertEquals( data.get( "schema.id" ), 23 );
    assertEquals( data.get( "message" ), "java.lang.NullPointerException" );
    assertEquals( data.size(), 3 );
  }
}
