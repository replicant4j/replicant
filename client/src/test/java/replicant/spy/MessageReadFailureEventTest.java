package replicant.spy;

import arez.Arez;
import java.util.HashMap;
import org.testng.annotations.Test;
import replicant.AbstractReplicantTest;
import static org.testng.Assert.*;

public class MessageReadFailureEventTest
  extends AbstractReplicantTest
{
  @Test
  public void basicOperation()
  {
    final MessageReadFailureEvent event =
      new MessageReadFailureEvent( G.class, new Error( "Some ERROR" ) );

    assertEquals( event.getSystemType(), G.class );

    final HashMap<String, Object> data = new HashMap<>();
    Arez.context().safeAction( () -> event.toMap( data ) );

    assertEquals( data.get( "type" ), "Connector.MessageReadFailure" );
    assertEquals( data.get( "systemType" ), "G" );
    assertEquals( data.get( "message" ), "Some ERROR" );
    assertEquals( data.size(), 3 );
  }

  @Test
  public void basicOperation_ThrowableNoMessage()
  {
    final MessageReadFailureEvent event =
      new MessageReadFailureEvent( G.class, new NullPointerException() );

    assertEquals( event.getSystemType(), G.class );

    final HashMap<String, Object> data = new HashMap<>();
    Arez.context().safeAction( () -> event.toMap( data ) );

    assertEquals( data.get( "type" ), "Connector.MessageReadFailure" );
    assertEquals( data.get( "systemType" ), "G" );
    assertEquals( data.get( "message" ), "java.lang.NullPointerException" );
    assertEquals( data.size(), 3 );
  }

  enum G
  {
    G1
  }
}
