package replicant.spy;

import arez.Arez;
import java.util.HashMap;
import org.testng.annotations.Test;
import replicant.AbstractReplicantTest;
import static org.testng.Assert.*;

public class DisconnectedEventTest
  extends AbstractReplicantTest
{
  @Test
  public void basicOperation()
  {
    final DisconnectedEvent event = new DisconnectedEvent( G.class );

    assertEquals( event.getSystemType(), G.class );

    final HashMap<String, Object> data = new HashMap<>();
    Arez.context().safeAction( () -> event.toMap( data ) );

    assertEquals( data.get( "type" ), "DataLoader.Disconnect" );
    assertEquals( data.get( "systemType" ), "G" );
    assertEquals( data.size(), 2 );
  }

  enum G
  {
    G1
  }
}
