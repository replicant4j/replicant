package replicant.server.ee;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class ReplicantContextHolderTest
{
  @Test
  public void basicWorkflow()
    throws Exception
  {
    final var key = "X";
    final var value = "1";

    ReplicantContextHolder.clean();
    assertNull( ReplicantContextHolder.get( key ) );
    ReplicantContextHolder.put( key, value );
    final var v2 = ReplicantContextHolder.get( key );
    assertEquals( v2, value );

    final var result = new Object[ 1 ];

    final var thread = new Thread( () -> result[ 0 ] = ReplicantContextHolder.get( key ) );
    thread.start();
    thread.join();

    assertNull( result[ 0 ] );

    ReplicantContextHolder.remove( key );
    assertNull( ReplicantContextHolder.get( key ) );

    ReplicantContextHolder.put( key, value );
    assertNotNull( ReplicantContextHolder.get( key ) );

    ReplicantContextHolder.clean();

    assertNull( ReplicantContextHolder.get( key ) );
  }
}
