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

    assertEquals( e.hasReturned(), false );
    e.returned();
    assertEquals( e.hasReturned(), true );

    assertEquals( e.isCompleted(), false );
    e.complete();
    assertEquals( e.isCompleted(), true );
  }

  @Test
  public void completedMarksAsReturned()
  {
    final RequestEntry e = new RequestEntry( "a1", true );

    assertEquals( e.hasReturned(), false );
    assertEquals( e.isCompleted(), false );
    e.complete();
    assertEquals( e.hasReturned(), true );
    assertEquals( e.isCompleted(), true );
  }
}
