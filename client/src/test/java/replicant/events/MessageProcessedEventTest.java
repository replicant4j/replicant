package replicant.events;

import org.testng.annotations.Test;
import replicant.AbstractReplicantTest;
import static org.testng.Assert.*;

public class MessageProcessedEventTest
  extends AbstractReplicantTest
{
  @Test
  public void basicOperation()
  {
    final int schemaId = 23;
    final String schemaName = "Rose";
    final MessageProcessedEvent event = new MessageProcessedEvent( schemaId, schemaName );
    assertEquals( event.getSchemaId(), schemaId );
    assertEquals( event.getSchemaName(), schemaName );
  }
}
