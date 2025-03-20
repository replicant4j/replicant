package replicant.spy;

import java.util.HashMap;
import org.realityforge.guiceyloops.shared.ValueUtil;
import org.testng.annotations.Test;
import replicant.AbstractReplicantTest;
import static org.testng.Assert.*;

public final class ExecStartedEventTest
  extends AbstractReplicantTest
{
  @Test
  public void basicOperation()
  {
    final String command = ValueUtil.randomString();
    final int schemaId = ValueUtil.randomInt();
    final String schemaName = ValueUtil.randomString();
    final ExecStartedEvent event = new ExecStartedEvent( schemaId, schemaName, command );

    assertEquals( event.getSchemaId(), schemaId );
    assertEquals( event.getSchemaName(), schemaName );
    assertEquals( event.getCommand(), command );

    final HashMap<String, Object> data = new HashMap<>();
    event.toMap( data );

    assertEquals( data.get( "type" ), "Connector.ExecStarted" );
    assertEquals( data.get( "schema.id" ), schemaId );
    assertEquals( data.get( "schema.name" ), schemaName );
    assertEquals( data.get( "command" ), command );

    assertEquals( data.size(), 4 );
  }
}
