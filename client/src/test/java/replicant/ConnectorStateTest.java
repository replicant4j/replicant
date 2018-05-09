package replicant;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class ConnectorStateTest
  extends AbstractReplicantTest
{
  @Test
  public void isTransitionState()
    throws Exception
  {
    assertEquals( ConnectorState.isTransitionState( ConnectorState.CONNECTING ), true );
    assertEquals( ConnectorState.isTransitionState( ConnectorState.CONNECTED ), false );
    assertEquals( ConnectorState.isTransitionState( ConnectorState.DISCONNECTING ), true );
    assertEquals( ConnectorState.isTransitionState( ConnectorState.DISCONNECTED ), false );
    assertEquals( ConnectorState.isTransitionState( ConnectorState.ERROR ), false );
  }
}
