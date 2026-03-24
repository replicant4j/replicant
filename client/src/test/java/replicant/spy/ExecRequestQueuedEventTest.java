package replicant.spy;

import java.util.HashMap;
import org.testng.annotations.Test;
import replicant.AbstractReplicantTest;
import replicant.ValueUtil;
import static org.testng.Assert.*;

public final class ExecRequestQueuedEventTest
  extends AbstractReplicantTest
{
  @Test
  public void basicOperation()
  {
    final var command = ValueUtil.randomString();
    final var schemaId = ValueUtil.randomInt();
    final var schemaName = ValueUtil.randomString();
    final var event = new ExecRequestQueuedEvent( schemaId, schemaName, command );

    assertEquals( event.getSchemaId(), schemaId );
    assertEquals( event.getSchemaName(), schemaName );
    assertEquals( event.getCommand(), command );

    final var data = new HashMap<String, Object>();
    event.toMap( data );

    assertEquals( data.get( "type" ), "Connector.ExecRequestQueued" );
    assertEquals( data.get( "schema.id" ), schemaId );
    assertEquals( data.get( "schema.name" ), schemaName );
    assertEquals( data.get( "command" ), command );

    assertEquals( data.size(), 4 );
  }
}
