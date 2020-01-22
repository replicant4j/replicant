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
    final MessageProcessedEvent event = new MessageProcessedEvent( schemaId );
    assertEquals( event.getSchemaId(), schemaId );
  }
}
