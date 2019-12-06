package org.realityforge.replicant.client.transport;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class RequestEntryTest
{
  @Test
  public void basicWorkflow()
  {
    final RequestEntry e = new RequestEntry( "a1", "MyOperation", "X" );

    assertEquals( e.getRequestId(), "a1" );
    assertEquals( e.getCacheKey(), "X" );

    assertFalse( e.isCompletionDataPresent() );
    e.setNormalCompletionAction( null );
    assertTrue( e.isCompletionDataPresent() );
    assertTrue( e.isNormalCompletion() );

    assertFalse( e.haveResultsArrived() );
    e.markResultsAsArrived();
    assertTrue( e.haveResultsArrived() );
  }
}
