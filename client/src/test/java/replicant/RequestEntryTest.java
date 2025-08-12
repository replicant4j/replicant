package replicant;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class RequestEntryTest
  extends AbstractReplicantTest
{
  @Test
  public void construct()
  {
    final int requestId = 321;
    final String requestKey = "ABC.go";
    final RequestEntry e = new RequestEntry( requestId, requestKey, false, null );

    assertEquals( e.getRequestId(), requestId );
    assertEquals( e.getName(), requestKey );
    assertEquals( e.toString(), "Request(ABC.go)[Id=321]" );
  }

  @Test
  public void construct_MissingName()
  {
    assertThrows( NullPointerException.class, () -> new RequestEntry( 123, null, false, null ) );
  }

  @Test
  public void construct_NameWhenNamesDisabled()
  {
    ReplicantTestUtil.disableNames();
    final IllegalStateException exception =
      expectThrows( IllegalStateException.class, () -> new RequestEntry( 123, "ABC", false, null ) );
    assertEquals( exception.getMessage(),
                  "Replicant-0041: RequestEntry passed a name 'ABC' but Replicant.areNamesEnabled() is false" );
  }

  @Test
  public void minimalToString()
  {
    final int requestId = 123;
    final String name = "MyMagicMethod";
    final RequestEntry e = new RequestEntry( requestId, name, false, null );

    assertEquals( e.toString(), "Request(MyMagicMethod)[Id=123]" );
  }

  @Test
  public void toString_WhenNameDisabled()
  {
    ReplicantTestUtil.disableNames();
    final RequestEntry e = new RequestEntry( 123, null, false, null );

    assertEquals( e.toString(), "replicant.RequestEntry@" + Integer.toHexString( System.identityHashCode( e ) ) );
  }

  @Test
  public void getName_WhenNameDisabled()
  {
    ReplicantTestUtil.disableNames();
    final RequestEntry e = new RequestEntry( 123, null, false, null );
    final IllegalStateException exception = expectThrows( IllegalStateException.class, e::getName );

    assertEquals( exception.getMessage(),
                  "Replicant-0043: RequestEntry.getName() invoked when Replicant.areNamesEnabled() is false" );
  }
}
