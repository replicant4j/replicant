package org.realityforge.replicant.client.transport;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class RequestEntryTest
{
  @Test
  public void basicWorkflow()
  {
    final RequestEntry e = new RequestEntry( "a1", true );

    assertEquals( e.getRequestID(), "a1" );
    assertEquals( e.isBulkLoad(), true );

    assertEquals( e.isCompletionDataPresent(), false );
    e.setNormalCompletionAction( null );
    assertEquals( e.isCompletionDataPresent(), true );
    assertEquals( e.isNormalCompletion(), true );

    assertEquals( e.isCompleted(), false );
    e.complete();
    assertEquals( e.isCompleted(), true );
  }
}
