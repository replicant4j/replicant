package org.realityforge.replicant.client.transport;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class RequestEntryTest
{
  @Test
  public void basicWorkflow()
  {
    final RequestEntry e = new RequestEntry( "a1", "MyOperation", "X" );

    assertEquals( e.getRequestID(), "a1" );
    assertEquals( e.getCacheKey(), "X" );

    assertEquals( e.isCompletionDataPresent(), false );
    e.setNormalCompletionAction( null );
    assertEquals( e.isCompletionDataPresent(), true );
    assertEquals( e.isNormalCompletion(), true );

    assertEquals( e.haveResultsArrived(), false );
    e.markResultsAsArrived();
    assertEquals( e.haveResultsArrived(), true );
  }
}
