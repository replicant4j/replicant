package replicant.server.ee;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class ReplicantContextHolderTest
{
  @Test
  public void basicWorkflow()
    throws Exception
  {
    final String key = "X";
    final String value = "1";

    ReplicantContextHolder.clean();
    assertNull( ReplicantContextHolder.get( key ) );
    ReplicantContextHolder.put( key, value );
    final Object v2 = ReplicantContextHolder.get( key );
    assertEquals( v2, value );

    final Object[] result = new Object[ 1 ];

    final Thread thread = new Thread( () -> result[ 0 ] = ReplicantContextHolder.get( key ) );
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
