package replicant.spy;

import java.util.HashMap;
import org.testng.annotations.Test;
import replicant.AbstractReplicantTest;
import static org.testng.Assert.*;

public class ConnectFailureEventTest
  extends AbstractReplicantTest
{
  @Test
  public void basicOperation()
  {
    final ConnectFailureEvent event =
      new ConnectFailureEvent( 23, "Rose", new Error( "Some ERROR" ) );

    assertEquals( event.getSchemaId(), 23 );
    assertEquals( event.getSchemaName(), "Rose" );

    final HashMap<String, Object> data = new HashMap<>();
    event.toMap( data );

    assertEquals( data.get( "type" ), "Connector.ConnectFailure" );
    assertEquals( data.get( "schema.id" ), 23 );
    assertEquals( data.get( "schema.name" ), "Rose" );
    assertEquals( data.get( "message" ), "Some ERROR" );
    assertEquals( data.size(), 4 );
  }

  @Test
  public void basicOperation_ThrowableNoMessage()
  {
    final ConnectFailureEvent event =
      new ConnectFailureEvent( 23, "Rose", new NullPointerException() );

    assertEquals( event.getSchemaId(), 23 );
    assertEquals( event.getSchemaName(), "Rose" );

    final HashMap<String, Object> data = new HashMap<>();
    event.toMap( data );

    assertEquals( data.get( "type" ), "Connector.ConnectFailure" );
    assertEquals( data.get( "schema.id" ), 23 );
    assertEquals( data.get( "schema.name" ), "Rose" );
    assertEquals( data.get( "message" ), "java.lang.NullPointerException" );
    assertEquals( data.size(), 4 );
  }
}
