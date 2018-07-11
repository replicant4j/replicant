package replicant.spy;

import java.util.HashMap;
import org.testng.annotations.Test;
import replicant.AbstractReplicantTest;
import static org.testng.Assert.*;

public class RestartEventTest
  extends AbstractReplicantTest
{
  @Test
  public void basicOperation()
  {
    final Error error = new Error( "Some ERROR" );
    final RestartEvent event = new RestartEvent( 23, "Rose", error );

    assertEquals( event.getSchemaId(), 23 );
    assertEquals( event.getSchemaName(), "Rose" );
    assertEquals( event.getError(), error );

    final HashMap<String, Object> data = new HashMap<>();
    event.toMap( data );

    assertEquals( data.get( "type" ), "Connector.Restart" );
    assertEquals( data.get( "schema.id" ), 23 );
    assertEquals( data.get( "schema.name" ), "Rose" );
    assertEquals( data.get( "message" ), "Some ERROR" );
    assertEquals( data.size(), 4 );
  }

  @Test
  public void basicOperation_ThrowableNoMessage()
  {
    final NullPointerException error = new NullPointerException();
    final RestartEvent event = new RestartEvent( 23, "Rose", error );

    assertEquals( event.getSchemaId(), 23 );
    assertEquals( event.getSchemaName(), "Rose" );
    assertEquals( event.getError(), error );

    final HashMap<String, Object> data = new HashMap<>();
    event.toMap( data );

    assertEquals( data.get( "type" ), "Connector.Restart" );
    assertEquals( data.get( "schema.id" ), 23 );
    assertEquals( data.get( "schema.name" ), "Rose" );
    assertEquals( data.get( "message" ), "java.lang.NullPointerException" );
    assertEquals( data.size(), 4 );
  }
}
