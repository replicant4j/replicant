package replicant;

import java.util.concurrent.atomic.AtomicReference;
import org.realityforge.guiceyloops.shared.ValueUtil;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class RequestTest
  extends AbstractReplicantTest
{
  @Test
  public void construct()
  {
    final Connection connection = createConnection();
    final String name = ValueUtil.randomString();
    final RequestEntry entry = connection.newRequest( name, false );
    final Request request = new Request( connection, entry );

    assertEquals( request.getConnectionId(), connection.getConnectionId() );
    assertEquals( request.getRequestId(), entry.getRequestId() );
  }

  @Test
  public void onSuccess_messageIncomplete()
  {
    final AtomicReference<Object> result = new AtomicReference<>();
    final Connection connection = createConnection();
    final RequestEntry entry = connection.newRequest( ValueUtil.randomString(), false );
    final Request request = new Request( connection, entry );

    assertNull( entry.getCompletionAction() );
    assertFalse( entry.hasCompleted() );

    request.onSuccess( false, () -> result.set( true ) );

    assertNull( result.get() );
    assertTrue( entry.hasCompleted() );
    assertNotNull( entry.getCompletionAction() );
    assertTrue( entry.isNormalCompletion() );
    assertTrue( entry.isExpectingResults() );
  }

  @Test
  public void onSuccess()
  {
    final AtomicReference<Object> result = new AtomicReference<>();
    final Connection connection = createConnection();
    final RequestEntry entry = connection.newRequest( ValueUtil.randomString(), false );
    final Request request = new Request( connection, entry );

    assertNull( entry.getCompletionAction() );
    assertFalse( entry.hasCompleted() );

    request.onSuccess( true, () -> result.set( true ) );

    assertEquals( result.get(), true );
    assertTrue( entry.hasCompleted() );

    // Completion action is null as it has already run
    assertNull( entry.getCompletionAction() );
    assertTrue( entry.isNormalCompletion() );
    assertFalse( entry.isExpectingResults() );
  }

  @Test
  public void onSuccess_alreadyCompleted()
  {
    final Connection connection = createConnection();
    final RequestEntry entry = connection.newRequest( "DoStuff", false );
    final Request request = new Request( connection, entry );

    final SafeProcedure onSuccess = () -> {
    };
    request.onSuccess( ValueUtil.randomBoolean(), onSuccess );

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class, () -> request.onSuccess( ValueUtil.randomBoolean(), onSuccess ) );

    assertEquals( exception.getMessage(),
                  "Replicant-0073: Request.onSuccess invoked on completed request Request(DoStuff)[Id=1]." );
  }

  @Test
  public void onFailure()
  {
    final AtomicReference<Object> result = new AtomicReference<>();
    final Connection connection = createConnection();
    final RequestEntry entry = connection.newRequest( ValueUtil.randomString(), false );
    final Request request = new Request( connection, entry );

    assertNull( request.getEntry().getCompletionAction() );
    assertFalse( request.getEntry().hasCompleted() );

    request.onFailure( () -> result.set( true ) );

    assertEquals( result.get(), true );
    assertTrue( request.getEntry().hasCompleted() );

    // Completion action is null as it has already run
    assertFalse( request.getEntry().isNormalCompletion() );
    assertFalse( request.getEntry().isExpectingResults() );
  }

  @Test
  public void onFailure_alreadyCompleted()
  {
    final Connection connection = createConnection();
    final RequestEntry entry = connection.newRequest( "DoStuff", false );
    final Request request = new Request( connection, entry );

    final SafeProcedure onFailure = () -> {
    };
    request.onFailure( onFailure );

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class, () -> request.onFailure( onFailure ) );

    assertEquals( exception.getMessage(),
                  "Replicant-0074: Request.onFailure invoked on completed request Request(DoStuff)[Id=1]." );
  }
}
