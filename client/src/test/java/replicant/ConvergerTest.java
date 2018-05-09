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

  @Test
  public void getReplicantContext()
  {
    final ReplicantContext context = Replicant.context();
    final Converger converger = context.getConverger();
    assertEquals( converger.getReplicantContext(), context );
    assertEquals( getFieldValue( converger, "_context" ), null );
  }

  @Test
  public void getReplicantContext_zonesEnabled()
  {
    ReplicantTestUtil.enableZones();
    ReplicantTestUtil.resetState();

    final ReplicantContext context = Replicant.context();
    final Converger converger = context.getConverger();
    assertEquals( converger.getReplicantContext(), context );
    assertEquals( getFieldValue( converger, "_context" ), context );
  }
}
