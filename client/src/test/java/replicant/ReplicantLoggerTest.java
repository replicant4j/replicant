package replicant;

import java.util.List;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class ReplicantLoggerTest
  extends AbstractReplicantTest
{
  @Test
  public void log()
  {
    final var message1 = ValueUtil.randomString();
    final var message2 = ValueUtil.randomString();
    ReplicantLogger.log( message1, null );
    final var throwable = new Throwable();
    ReplicantLogger.log( message2, throwable );

    final var entries = getTestLogger().getEntries();
    assertEquals( entries.size(), 2 );
    final var entry1 = entries.get( 0 );
    assertEquals( entry1.getMessage(), message1 );
    assertNull( entry1.getThrowable() );
    final var entry2 = entries.get( 1 );
    assertEquals( entry2.getMessage(), message2 );
    assertEquals( entry2.getThrowable(), throwable );
  }
}
