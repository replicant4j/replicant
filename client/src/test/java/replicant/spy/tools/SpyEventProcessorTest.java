package replicant.spy.tools;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import org.testng.annotations.Test;
import replicant.AbstractReplicantTest;
import replicant.AreaOfInterest;
import replicant.Channel;
import replicant.ChannelAddress;
import replicant.spy.AreaOfInterestCreatedEvent;
import static org.testng.Assert.*;

public class SpyEventProcessorTest
  extends AbstractReplicantTest
{
  private class TestSpyEventProcessor
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

  @Test
  public void handleUnhandledEvent()
    throws Throwable
  {
    final TestSpyEventProcessor processor = new TestSpyEventProcessor();

    final Object event = new Object();
    processor.onSpyEvent( event );

    assertEquals( processor._handleUnhandledEventCallCount, 1 );
  }

  @Test
  public void handleEvent()
    throws Throwable
  {
    final TestSpyEventProcessor processor = new TestSpyEventProcessor();

    final AtomicInteger callCount = new AtomicInteger();
    processor.on( AreaOfInterestCreatedEvent.class, e -> callCount.incrementAndGet() );

    final AreaOfInterestCreatedEvent event =
      new AreaOfInterestCreatedEvent( AreaOfInterest.create( Channel.create( new ChannelAddress( G.G1 ) ) ) );

    assertEquals( callCount.get(), 0 );
    processor.onSpyEvent( event );
    assertEquals( callCount.get(), 1 );
  }

  @Test
  public void onFailsOnDuplicates()
    throws Throwable
  {
    final TestSpyEventProcessor processor = new TestSpyEventProcessor();

    final Consumer<AreaOfInterestCreatedEvent> handler = e -> {
    };
    processor.on( AreaOfInterestCreatedEvent.class, handler );
    final IllegalStateException exception =
      expectThrows( IllegalStateException.class, () -> processor.on( AreaOfInterestCreatedEvent.class, handler ) );
    assertEquals( exception.getMessage(),
                  "Replicant-0157: Attempting to call AbstractSpyEventProcessor.on() to register a processor for type class replicant.spy.AreaOfInterestCreatedEvent but an existing processor already exists for type" );
  }

  enum G
  {
    G1
  }
}
