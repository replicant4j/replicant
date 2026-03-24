package replicant;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class RequestTest
  extends AbstractReplicantTest
{
  @Test
  public void construct()
  {
    final var connection = createConnection();
    final var name = ValueUtil.randomString();
    final var entry = connection.newRequest( name, false, null );
    final var request = new Request( connection, entry );

    assertEquals( request.getConnectionId(), connection.getConnectionId() );
    assertEquals( request.getRequestId(), entry.getRequestId() );
  }
}
