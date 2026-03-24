package replicant.spy.tools;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import org.testng.annotations.Test;
import replicant.AbstractReplicantTest;
import static org.testng.Assert.*;

public class SpyEventProcessorTest
  extends AbstractReplicantTest
{
  private static class TestSpyEventProcessor
    extends AbstractSpyEventProcessor
  {
    int _handleUnhandledEventCallCount;

    @Override
    protected void handleUnhandledEvent( @Nonnull final Object event )
    {
      super.handleUnhandledEvent( event );
      _handleUnhandledEventCallCount += 1;
    }
  }

  private static class FakeEvent
  {
  }

  @Test
  public void handleUnhandledEvent()
  {
    final var processor = new TestSpyEventProcessor();

    final var event = new Object();
    processor.onSpyEvent( event );

    assertEquals( processor._handleUnhandledEventCallCount, 1 );
  }

  @Test
  public void handleEvent()
  {
    final var processor = new TestSpyEventProcessor();

    final var callCount = new AtomicInteger();
    processor.on( FakeEvent.class, e -> callCount.incrementAndGet() );

    final var event = new FakeEvent();

    assertEquals( callCount.get(), 0 );
    processor.onSpyEvent( event );
    assertEquals( callCount.get(), 1 );
  }

  @Test
  public void onFailsOnDuplicates()
  {
    final var processor = new TestSpyEventProcessor();

    final var handler = (Consumer<FakeEvent>) e -> {
    };
    processor.on( FakeEvent.class, handler );
    final var exception =
      expectThrows( IllegalStateException.class, () -> processor.on( FakeEvent.class, handler ) );
    assertEquals( exception.getMessage(),
                  "Replicant-0036: Attempting to call AbstractSpyEventProcessor.on() to register a processor for type class replicant.spy.tools.SpyEventProcessorTest$FakeEvent but an existing processor already exists for type" );
  }
}
