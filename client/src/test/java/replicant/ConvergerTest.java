package replicant;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class ConvergerTest
  extends AbstractReplicantTest
{
  @Test
  public void construct_withUnnecessaryContext()
  {
    final ReplicantContext context = Replicant.context();
    final IllegalStateException exception =
      expectThrows( IllegalStateException.class, () -> Converger.create( context ) );
    assertEquals( exception.getMessage(),
                  "Replicant-0124: SubscriptioConvergernService passed a context but Replicant.areZonesEnabled() is false" );
  }
}
