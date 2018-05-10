package replicant;

import arez.Arez;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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
                  "Replicant-0124: Converger passed a context but Replicant.areZonesEnabled() is false" );
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

  @Test
  public void preConvergeAction()
  {
    final Converger c = Replicant.context().getConverger();

    // Pause scheduler so Autoruns don't auto-converge
    Arez.context().pauseScheduler();

    // should do nothing ...
    Arez.context().safeAction( c::preConverge );

    final AtomicInteger callCount = new AtomicInteger();

    Arez.context().safeAction( () -> c.setPreConvergeAction( callCount::incrementAndGet ) );

    Arez.context().safeAction( c::preConverge );

    assertEquals( callCount.get(), 1 );

    Arez.context().safeAction( c::preConverge );

    assertEquals( callCount.get(), 2 );

    Arez.context().safeAction( () -> c.setPreConvergeAction( null ) );

    Arez.context().safeAction( c::preConverge );

    assertEquals( callCount.get(), 2 );
  }

  @Test
  public void convergeCompleteAction()
  {
    final Converger c = Replicant.context().getConverger();

    // Pause scheduler so Autoruns don't auto-converge
    Arez.context().pauseScheduler();

    // should do nothing ...
    Arez.context().safeAction( c::convergeComplete );

    final AtomicInteger callCount = new AtomicInteger();

    Arez.context().safeAction( () -> c.setConvergeCompleteAction( callCount::incrementAndGet ) );

    Arez.context().safeAction( c::convergeComplete );

    assertEquals( callCount.get(), 1 );

    Arez.context().safeAction( c::convergeComplete );

    assertEquals( callCount.get(), 2 );

    Arez.context().safeAction( () -> c.setConvergeCompleteAction( null ) );

    Arez.context().safeAction( c::convergeComplete );

    assertEquals( callCount.get(), 2 );
  }

  @Nonnull
  private AreaOfInterest createAreaOfInterest( @Nonnull final ChannelAddress address, @Nullable final Object filter )
  {
    return Replicant.context().createOrUpdateAreaOfInterest( address, filter );
  }

  private enum G
  {
    G1, G2
  }
}
