package replicant;

import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nonnull;
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
    final Request request = connection.newRequest( ValueUtil.randomString() );

    assertEquals( request.getConnectionId(), connection.getConnectionId() );
    assertEquals( request.getRequestId(), request.getEntry().getRequestId() );
  }

  @Test
  public void onSuccess_messageIncomplete()
  {
    final AtomicReference<Object> result = new AtomicReference<>();
    final Connection connection = createConnection();
    final Request request = connection.newRequest( ValueUtil.randomString() );

    assertNull( request.getEntry().getCompletionAction() );
    assertFalse( request.getEntry().hasCompleted() );

    request.onSuccess( false, () -> result.set( true ) );

    assertNull( result.get() );
    assertTrue( request.getEntry().hasCompleted() );
    assertNotNull( request.getEntry().getCompletionAction() );
    assertTrue( request.getEntry().isNormalCompletion() );
    assertTrue( request.getEntry().isExpectingResults() );
  }

  @Test
  public void onSuccess_messageComplete()
  {
    final AtomicReference<Object> result = new AtomicReference<>();
    final Connection connection = createConnection();
    final Request request = connection.newRequest( ValueUtil.randomString() );

    assertNull( request.getEntry().getCompletionAction() );
    assertFalse( request.getEntry().hasCompleted() );

    request.onSuccess( true, () -> result.set( true ) );

    assertEquals( result.get(), true );
    assertTrue( request.getEntry().hasCompleted() );

    // Completion action is null as it has already run
    assertNull( request.getEntry().getCompletionAction() );
    assertTrue( request.getEntry().isNormalCompletion() );
    assertFalse( request.getEntry().isExpectingResults() );
  }

  @Test
  public void onSuccess_alreadyCompleted()
  {
    final Connection connection = createConnection();
    final Request request = connection.newRequest( "DoStuff" );

    final SafeProcedure onSuccess = () -> {
    };
    request.onSuccess( ValueUtil.randomBoolean(), onSuccess );

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class, () -> request.onSuccess( ValueUtil.randomBoolean(), onSuccess ) );

    assertEquals( exception.getMessage(),
                  "Replicant-0073: Request.onSuccess invoked on completed request Request(DoStuff)[Id=1]." );
  }

  @Test
  public void onSuccess_failureMessage()
  {
    final AtomicReference<Object> result = new AtomicReference<>();
    final Connection connection = createConnection();
    final Request request = connection.newRequest( ValueUtil.randomString() );

    assertNull( request.getEntry().getCompletionAction() );
    assertFalse( request.getEntry().hasCompleted() );

    request.onFailure( () -> result.set( true ) );

    assertEquals( result.get(), true );
    assertTrue( request.getEntry().hasCompleted() );

    // Completion action is null as it has already run
    assertNull( request.getEntry().getCompletionAction() );
    assertFalse( request.getEntry().isNormalCompletion() );
    assertFalse( request.getEntry().isExpectingResults() );
  }

  @Test
  public void onFailure_alreadyCompleted()
  {
    final Connection connection = createConnection();
    final Request request = connection.newRequest( "DoStuff" );

    final SafeProcedure onFailure = () -> {
    };
    request.onFailure( onFailure );

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class, () -> request.onFailure( onFailure ) );

    assertEquals( exception.getMessage(),
                  "Replicant-0074: Request.onFailure invoked on completed request Request(DoStuff)[Id=1]." );
  }

  @Nonnull
  private Connection createConnection()
  {
    return new Connection( createConnector(), ValueUtil.randomString() );
  }
}
