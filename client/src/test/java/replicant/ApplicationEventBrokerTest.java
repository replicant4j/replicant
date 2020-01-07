package replicant;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class ApplicationEventBrokerTest
  extends AbstractReplicantTest
{
  @Test
  public void basicOperation()
  {
    ReplicantTestUtil.enableEvents();
    ReplicantTestUtil.resetState();

    final ApplicationEventBroker broker = new ApplicationEventBroker();

    final Object event = new Object();

    final AtomicInteger callCount = new AtomicInteger();

    final ApplicationEventHandler handler = e -> {
      callCount.incrementAndGet();
      assertEquals( e, event );
    };

    assertFalse( broker.willPropagateApplicationEvents() );

    broker.addApplicationEventHandler(  handler );

    assertTrue( broker.willPropagateApplicationEvents() );

    assertEquals( broker.getApplicationEventHandlers().size(), 1 );
    assertTrue( broker.getApplicationEventHandlers().contains( handler ) );

    assertEquals( callCount.get(), 0 );

    broker.reportApplicationEvent( event );

    assertEquals( callCount.get(), 1 );

    broker.removeApplicationEventHandler( handler );

    assertFalse( broker.willPropagateApplicationEvents() );

    assertEquals( broker.getApplicationEventHandlers().size(), 0 );
  }

  @Test
  public void reportApplicationEvent_whenNoListeners()
  {
    ReplicantTestUtil.enableEvents();
    ReplicantTestUtil.resetState();

    final ApplicationEventBroker broker = new ApplicationEventBroker();

    assertFalse( broker.willPropagateApplicationEvents() );

    final Object event = new Object();

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class, () -> broker.reportApplicationEvent( event ) );

    assertEquals( exception.getMessage(),
                  "Replicant-0091: Attempting to report ApplicationEvent '" + event +
                  "' but willPropagateApplicationEvents() returns false." );
  }

  @Test
  public void addApplicationEventHandler_alreadyExists()
  {
    ReplicantTestUtil.enableEvents();
    ReplicantTestUtil.resetState();

    final ApplicationEventBroker broker = new ApplicationEventBroker();

    final ApplicationEventHandler handler = new TestApplicationEventHandler();
    broker.addApplicationEventHandler( handler );

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class, () -> broker.addApplicationEventHandler( handler ) );

    assertEquals( exception.getMessage(),
                  "Replicant-0089: Attempting to add handler " + handler +
                  " that is already in the list of application handlers." );
  }

  @Test
  public void removeApplicationEventHandler_noExists()
  {
    ReplicantTestUtil.enableEvents();
    ReplicantTestUtil.resetState();

    final ApplicationEventBroker broker = new ApplicationEventBroker();

    final ApplicationEventHandler handler = new TestApplicationEventHandler();

    final IllegalStateException exception =
      expectThrows( IllegalStateException.class, () -> broker.removeApplicationEventHandler( handler ) );

    assertEquals( exception.getMessage(),
                  "Replicant-0090: Attempting to remove handler " + handler +
                  " that is not in the list of application handlers." );
  }

  @Test
  public void multipleHandlers()
  {
    ReplicantTestUtil.enableEvents();
    ReplicantTestUtil.resetState();

    final ApplicationEventBroker broker = new ApplicationEventBroker();

    final Object event = new Object();

    final AtomicInteger callCount1 = new AtomicInteger();
    final AtomicInteger callCount2 = new AtomicInteger();
    final AtomicInteger callCount3 = new AtomicInteger();

    final ApplicationEventHandler handler1 = e -> callCount1.incrementAndGet();
    final ApplicationEventHandler handler2 = e -> callCount2.incrementAndGet();
    final ApplicationEventHandler handler3 = e -> callCount3.incrementAndGet();
    broker.addApplicationEventHandler( handler1 );
    broker.addApplicationEventHandler( handler2 );
    broker.addApplicationEventHandler( handler3 );

    assertEquals( broker.getApplicationEventHandlers().size(), 3 );

    broker.reportApplicationEvent( event );

    assertEquals( callCount1.get(), 1 );
    assertEquals( callCount2.get(), 1 );
    assertEquals( callCount3.get(), 1 );

    broker.reportApplicationEvent( event );

    assertEquals( callCount1.get(), 2 );
    assertEquals( callCount2.get(), 2 );
    assertEquals( callCount3.get(), 2 );
  }

  @Test
  public void onApplicationEvent_whereOneHandlerGeneratesError()
  {
    ReplicantTestUtil.enableEvents();
    ReplicantTestUtil.resetState();

    final ApplicationEventBroker broker = new ApplicationEventBroker();

    final Object event = new Object();

    final AtomicInteger callCount1 = new AtomicInteger();
    final AtomicInteger callCount3 = new AtomicInteger();

    final RuntimeException exception = new RuntimeException( "X" );

    final ApplicationEventHandler handler1 = e -> callCount1.incrementAndGet();
    final ApplicationEventHandler handler2 = e -> {
      throw exception;
    };
    final ApplicationEventHandler handler3 = e -> callCount3.incrementAndGet();
    broker.addApplicationEventHandler( handler1 );
    broker.addApplicationEventHandler( handler2 );
    broker.addApplicationEventHandler( handler3 );

    broker.reportApplicationEvent( event );

    assertEquals( callCount1.get(), 1 );
    assertEquals( callCount3.get(), 1 );

    final List<TestLogger.LogEntry> entries = getTestLogger().getEntries();
    assertEquals( entries.size(), 1 );
    final TestLogger.LogEntry entry1 = entries.get( 0 );
    assertEquals( entry1.getMessage(),
                  "Exception when notifying application handler '" + handler2 + "' of '" + event + "' event." );
    assertEquals( entry1.getThrowable(), exception );

    broker.reportApplicationEvent( event );

    assertEquals( callCount1.get(), 2 );
    assertEquals( callCount3.get(), 2 );

    assertEquals( getTestLogger().getEntries().size(), 2 );
  }
}
