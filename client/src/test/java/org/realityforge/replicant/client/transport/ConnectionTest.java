package org.realityforge.replicant.client.transport;

import org.realityforge.guiceyloops.shared.ValueUtil;
import org.testng.annotations.Test;
import replicant.AbstractReplicantTest;
import replicant.RequestEntry;
import replicant.SafeProcedure;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class ConnectionTest
  extends AbstractReplicantTest
{
  @Test
  public void basicRequestManagementWorkflow()
  {
    final Connection rm = new Connection( ValueUtil.randomString() );
    final RequestEntry e = rm.newRequest( "Y", "X" );
    assertEquals( e.getName(), "Y" );
    assertEquals( e.getCacheKey(), "X" );

    assertEquals( rm.getRequest( e.getRequestId() ), e );
    assertEquals( rm.getRequests().get( e.getRequestId() ), e );
    assertEquals( rm.getRequest( "NotHere" + e.getRequestId() ), null );

    assertTrue( rm.removeRequest( e.getRequestId() ) );
    assertFalse( rm.removeRequest( e.getRequestId() ) );

    assertEquals( rm.getRequest( e.getRequestId() ), null );
  }

  @Test
  public void completeNormalRequest()
  {
    final Connection rm = new Connection( ValueUtil.randomString() );
    final RequestEntry e = rm.newRequest( "Y", "X" );
    final SafeProcedure action = mock( SafeProcedure.class );

    rm.completeNormalRequest( e, action );

    verify( action ).call();
    assertFalse( e.isCompletionDataPresent() );
    assertNull( rm.getRequest( e.getRequestId() ) );
  }

  @Test
  public void completeNormalRequest_expectingResults()
  {
    final Connection rm = new Connection( ValueUtil.randomString() );
    final RequestEntry e = rm.newRequest( "Y", "X" );
    final SafeProcedure action = mock( SafeProcedure.class );

    e.setExpectingResults( true );

    rm.completeNormalRequest( e, action );

    verify( action, never() ).call();
    assertTrue( e.isCompletionDataPresent() );
    assertTrue( e.isNormalCompletion() );
    assertEquals( e.getCompletionAction(), action );
    assertNotNull( rm.getRequest( e.getRequestId() ) );
  }

  @Test
  public void completeNormalRequest_resultsArrived()
  {
    final Connection rm = new Connection( ValueUtil.randomString() );
    final RequestEntry e = rm.newRequest( "Y", "X" );
    final SafeProcedure action = mock( SafeProcedure.class );

    e.setExpectingResults( true );
    e.markResultsAsArrived();

    rm.completeNormalRequest( e, action );

    verify( action ).call();
    assertFalse( e.isCompletionDataPresent() );
    assertNull( rm.getRequest( e.getRequestId() ) );
  }

  @Test
  public void completeNonNormalRequest()
  {
    final Connection rm = new Connection( ValueUtil.randomString() );
    final RequestEntry e = rm.newRequest( "Y", "X" );
    final SafeProcedure action = mock( SafeProcedure.class );

    rm.completeNonNormalRequest( e, action );

    verify( action ).call();
    assertFalse( e.isCompletionDataPresent() );
    assertNull( rm.getRequest( e.getRequestId() ) );
  }

  @Test
  public void completeNonNormalRequest_expectingResults()
  {
    final Connection rm = new Connection( ValueUtil.randomString() );
    final RequestEntry e = rm.newRequest( "Y", "X" );
    final SafeProcedure action = mock( SafeProcedure.class );

    e.setExpectingResults( true );

    rm.completeNonNormalRequest( e, action );

    verify( action, never() ).call();
    assertTrue( e.isCompletionDataPresent() );
    assertFalse( e.isNormalCompletion() );
    assertEquals( e.getCompletionAction(), action );
    assertNotNull( rm.getRequest( e.getRequestId() ) );
  }

  @Test
  public void completeNonNormalRequest_resultsArrived()
  {
    final Connection rm = new Connection( ValueUtil.randomString() );
    final RequestEntry e = rm.newRequest( "Y", "X" );
    final SafeProcedure action = mock( SafeProcedure.class );

    e.setExpectingResults( true );
    e.markResultsAsArrived();

    rm.completeNonNormalRequest( e, action );

    verify( action ).call();
    assertFalse( e.isCompletionDataPresent() );
    assertNull( rm.getRequest( e.getRequestId() ) );
  }
}
