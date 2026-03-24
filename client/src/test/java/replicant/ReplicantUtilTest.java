package replicant;

import java.io.IOException;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class ReplicantUtilTest
  extends AbstractReplicantTest
{
  @Test
  public void safeGetString()
  {
    assertEquals( ReplicantUtil.safeGetString( () -> "My String" ), "My String" );
  }

  @Test
  public void safeGetString_generatesError()
  {
    final var text = ReplicantUtil.safeGetString( () -> {
      throw new RuntimeException( "X" );
    } );
    assertTrue( text.startsWith( "Exception generated whilst attempting to get supplied message.\n" +
                                 "java.lang.RuntimeException: X\n" ) );
  }

  @Test
  public void throwableToString()
  {
    final var text = ReplicantUtil.throwableToString( new RuntimeException( "X" ) );
    assertTrue( text.startsWith( "java.lang.RuntimeException: X\n" ) );
  }

  @Test
  public void throwableToString_with_NestedThrowable()
  {
    final var exception =
      new RuntimeException( "X", new IOException( "Y" ) );
    final var text = ReplicantUtil.throwableToString( exception );
    assertTrue( text.startsWith( "java.lang.RuntimeException: X\n" ) );
    assertTrue( text.contains( "\nCaused by: java.io.IOException: Y\n" ) );
  }
}
