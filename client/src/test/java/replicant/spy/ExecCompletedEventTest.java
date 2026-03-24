package replicant.spy;

import java.util.HashMap;
import org.testng.annotations.Test;
import replicant.AbstractReplicantTest;
import replicant.ValueUtil;
import static org.testng.Assert.*;

public final class ExecCompletedEventTest
  extends AbstractReplicantTest
{
  @Test
  public void basicOperation()
  {
    final var command = ValueUtil.randomString();
    final var schemaId = ValueUtil.randomInt();
    final var schemaName = ValueUtil.randomString();
    final var requestId = ValueUtil.randomInt();
    final var event = new ExecCompletedEvent( schemaId, schemaName, command, requestId );

    assertEquals( event.getSchemaId(), schemaId );
    assertEquals( event.getSchemaName(), schemaName );
    assertEquals( event.getCommand(), command );
    assertEquals( event.getRequestId(), requestId );

    final var data = new HashMap<String, Object>();
    event.toMap( data );

    assertEquals( data.get( "type" ), "Connector.ExecCompleted" );
    assertEquals( data.get( "schema.id" ), schemaId );
    assertEquals( data.get( "schema.name" ), schemaName );
    assertEquals( data.get( "command" ), command );
    assertEquals( data.get( "requestId" ), requestId );

    assertEquals( data.size(), 5 );
  }
}
