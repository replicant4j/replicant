package org.realityforge.replicant.server.ee;

import java.io.Serializable;
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
    final Serializable v2 = ReplicantContextHolder.get( key );
    assertEquals( v2, value );

    final Serializable[] result = new Serializable[ 1 ];

    final Thread thread = new Thread()
    {
      @Override
      public void run()
      {
        result[ 0 ] = ReplicantContextHolder.get( key );
      }
    };
    thread.start();
    thread.join();

    assertNull( result[ 0 ] );

    ReplicantContextHolder.clean();
    assertNull( ReplicantContextHolder.get( key ) );
  }
}
