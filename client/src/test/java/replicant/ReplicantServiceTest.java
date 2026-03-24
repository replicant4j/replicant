package replicant;

import javax.annotation.Nullable;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class ReplicantServiceTest
  extends AbstractReplicantTest
{
  static class TestReplicantService
    extends ReplicantService
  {
    TestReplicantService( @Nullable final ReplicantContext context )
    {
      super( context );
    }
  }

  @Test
  public void construct()
  {
    final var replicantService = new TestReplicantService( null );
    assertEquals( replicantService.getReplicantContext(), Replicant.context() );
  }

  @Test
  public void construct_whenZonesEnabled()
  {
    ReplicantTestUtil.enableZones();
    final var context = Replicant.context();
    final var replicantService = new TestReplicantService( context );
    assertEquals( replicantService.getReplicantContext(), context );
  }

  @Test
  public void construct_withUnnecessaryContext()
  {
    final var context = Replicant.context();
    final var exception =
      expectThrows( IllegalStateException.class, () -> new TestReplicantService( context ) );
    assertEquals( exception.getMessage(),
                  "Replicant-0037: ReplicantService passed a context but Replicant.areZonesEnabled() is false" );
  }
}
