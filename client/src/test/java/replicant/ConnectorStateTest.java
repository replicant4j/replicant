package replicant;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class ConnectorStateTest
  extends AbstractReplicantTest
{
  @Test
  public void isTransitionState()
  {
    assertTrue( ConnectorState.isTransitionState( ConnectorState.CONNECTING ) );
    assertFalse( ConnectorState.isTransitionState( ConnectorState.CONNECTED ) );
    assertTrue( ConnectorState.isTransitionState( ConnectorState.DISCONNECTING ) );
    assertFalse( ConnectorState.isTransitionState( ConnectorState.DISCONNECTED ) );
    assertFalse( ConnectorState.isTransitionState( ConnectorState.ERROR ) );
  }
}
