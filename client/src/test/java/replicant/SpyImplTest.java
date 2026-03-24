package replicant;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class SpyImplTest
  extends AbstractReplicantTest
{
  @Test
  public void basicOperation()
  {
    final var spy = new SpyImpl();

    final var event = new Object();

    final var callCount = new AtomicInteger();

    final var handler = (SpyEventHandler) e -> {
      callCount.incrementAndGet();
      assertEquals( e, event );
    };

    assertFalse( spy.willPropagateSpyEvents() );

    spy.addSpyEventHandler( handler );

    assertTrue( spy.willPropagateSpyEvents() );

    assertEquals( spy.getSpyEventHandlers().size(), 1 );
    assertTrue( spy.getSpyEventHandlers().contains( handler ) );

    assertEquals( callCount.get(), 0 );

    spy.reportSpyEvent( event );

    assertEquals( callCount.get(), 1 );

    spy.removeSpyEventHandler( handler );

    assertFalse( spy.willPropagateSpyEvents() );

    assertEquals( spy.getSpyEventHandlers().size(), 0 );
  }

  @Test
  public void reportSpyEvent_whenNoListeners()
  {
    final var spy = new SpyImpl();

    assertFalse( spy.willPropagateSpyEvents() );

    final var event = new Object();

    final var exception =
      expectThrows( IllegalStateException.class, () -> spy.reportSpyEvent( event ) );

    assertEquals( exception.getMessage(),
                  "Replicant-0038: Attempting to report SpyEvent '" + event +
                  "' but willPropagateSpyEvents() returns false." );
  }

  @Test
  public void addSpyEventHandler_alreadyExists()
  {
    final var support = new SpyImpl();

    final var handler = new TestSpyEventHandler();
    support.addSpyEventHandler( handler );

    final var exception =
      expectThrows( IllegalStateException.class, () -> support.addSpyEventHandler( handler ) );

    assertEquals( exception.getMessage(),
                  "Replicant-0040: Attempting to add handler " +
                  handler +
                  " that is already in the list of spy handlers." );
  }

  @Test
  public void removeSpyEventHandler_noExists()
  {
    final var support = new SpyImpl();

    final var handler = new TestSpyEventHandler();

    final var exception =
      expectThrows( IllegalStateException.class, () -> support.removeSpyEventHandler( handler ) );

    assertEquals( exception.getMessage(),
                  "Replicant-0039: Attempting to remove handler " +
                  handler +
                  " that is not in the list of spy handlers." );
  }

  @Test
  public void multipleHandlers()
  {
    final var support = new SpyImpl();

    final var event = new Object();

    final var callCount1 = new AtomicInteger();
    final var callCount2 = new AtomicInteger();
    final var callCount3 = new AtomicInteger();

    final var handler1 = (SpyEventHandler) e -> callCount1.incrementAndGet();
    final var handler2 = (SpyEventHandler) e -> callCount2.incrementAndGet();
    final var handler3 = (SpyEventHandler) e -> callCount3.incrementAndGet();
    support.addSpyEventHandler( handler1 );
    support.addSpyEventHandler( handler2 );
    support.addSpyEventHandler( handler3 );

    assertEquals( support.getSpyEventHandlers().size(), 3 );

    support.reportSpyEvent( event );

    assertEquals( callCount1.get(), 1 );
    assertEquals( callCount2.get(), 1 );
    assertEquals( callCount3.get(), 1 );

    support.reportSpyEvent( event );

    assertEquals( callCount1.get(), 2 );
    assertEquals( callCount2.get(), 2 );
    assertEquals( callCount3.get(), 2 );
  }

  @Test
  public void onSpyEvent_whereOneHandlerGeneratesError()
  {
    final var support = new SpyImpl();

    final var event = new Object();

    final var callCount1 = new AtomicInteger();
    final var callCount3 = new AtomicInteger();

    final var exception = new RuntimeException( "X" );

    final var handler1 = (SpyEventHandler) e -> callCount1.incrementAndGet();
    final var handler2 = (SpyEventHandler) e -> {
      throw exception;
    };
    final var handler3 = (SpyEventHandler) e -> callCount3.incrementAndGet();
    support.addSpyEventHandler( handler1 );
    support.addSpyEventHandler( handler2 );
    support.addSpyEventHandler( handler3 );

    support.reportSpyEvent( event );

    assertEquals( callCount1.get(), 1 );
    assertEquals( callCount3.get(), 1 );

    final var entries = getTestLogger().getEntries();
    assertEquals( entries.size(), 1 );
    final var entry1 = entries.get( 0 );
    assertEquals( entry1.getMessage(),
                  "Exception when notifying spy handler '" + handler2 + "' of '" + event + "' event." );
    assertEquals( entry1.getThrowable(), exception );

    support.reportSpyEvent( event );

    assertEquals( callCount1.get(), 2 );
    assertEquals( callCount3.get(), 2 );

    assertEquals( getTestLogger().getEntries().size(), 2 );
  }
}
