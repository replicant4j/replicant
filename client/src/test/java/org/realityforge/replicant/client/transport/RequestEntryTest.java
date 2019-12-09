package org.realityforge.replicant.client.transport;

import org.realityforge.guiceyloops.shared.ValueUtil;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class RequestEntryTest
{
  @Test
  public void basicWorkflow()
  {
    final int requestId = ValueUtil.randomInt();
    final RequestEntry e = new RequestEntry( requestId, "MyOperation", "X" );

    assertEquals( e.getRequestId(), (Integer) requestId );
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
