package org.realityforge.replicant.client.transport;

import org.testng.annotations.Test;
import replicant.AbstractReplicantTest;
import replicant.ReplicantTestUtil;
import static org.testng.Assert.*;

public class InvalidHttpResponseExceptionTest
  extends AbstractReplicantTest
{
  @Test
  public void basicOperation()
  {
    final InvalidHttpResponseException exception = new InvalidHttpResponseException( 200, "OK" );
    assertEquals( exception.getStatusCode(), 200 );
    assertEquals( exception.getStatusLine(), "OK" );
    assertEquals( exception.toString(), "InvalidHttpResponseException[StatusCode=200,StatusLine='OK']" );
    ReplicantTestUtil.disableNames();

    assertEquals( exception.toString(),
                  "org.realityforge.replicant.client.transport.InvalidHttpResponseException" );
  }
}
