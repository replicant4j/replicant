package replicant.spy.tools;

import static org.testng.Assert.*;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.jspecify.annotations.NonNull;
import org.testng.annotations.Test;
import replicant.AbstractReplicantTest;
import replicant.spy.RequestCompletedEvent;
import replicant.spy.RequestStartedEvent;

public class SpyEventProcessorTest extends AbstractReplicantTest {
    private static class TestSpyEventProcessor extends AbstractSpyEventProcessor {
        int _handleUnhandledEventCallCount;

        @Override
        protected void handleUnhandledEvent(@NonNull final Object event) {
            super.handleUnhandledEvent(event);
            _handleUnhandledEventCallCount += 1;
        }
    }

    private static class FakeEvent {}

    private static class TestConsoleSpyEventProcessor extends ConsoleSpyEventProcessor {
        private String _message = "";

        @Override
        protected void log(@NonNull final String message, @NonNull final String styling) {
            _message = message;
        }
    }

    @Test
    public void handleUnhandledEvent() {
        final TestSpyEventProcessor processor = new TestSpyEventProcessor();

        final Object event = new Object();
        processor.onSpyEvent(event);

        assertEquals(processor._handleUnhandledEventCallCount, 1);
    }

    @Test
    public void handleEvent() {
        final TestSpyEventProcessor processor = new TestSpyEventProcessor();

        final AtomicInteger callCount = new AtomicInteger();
        processor.on(FakeEvent.class, e -> callCount.incrementAndGet());

        final FakeEvent event = new FakeEvent();

        assertEquals(callCount.get(), 0);
        processor.onSpyEvent(event);
        assertEquals(callCount.get(), 1);
    }

    @Test
    public void onFailsOnDuplicates() {
        final TestSpyEventProcessor processor = new TestSpyEventProcessor();

        final Consumer<FakeEvent> handler = e -> {};
        processor.on(FakeEvent.class, handler);
        final IllegalStateException exception =
                expectThrows(IllegalStateException.class, () -> processor.on(FakeEvent.class, handler));
        assertEquals(
                exception.getMessage(),
                "Replicant-0036: Attempting to call AbstractSpyEventProcessor.on() to register a processor for type"
                        + " class replicant.spy.tools.SpyEventProcessorTest$FakeEvent but an existing processor already"
                        + " exists for type");
    }

    @Test
    public void requestLifecycleMessages() {
        final TestConsoleSpyEventProcessor processor = new TestConsoleSpyEventProcessor();

        processor.onSpyEvent(new RequestStartedEvent(23, "Rose", 42, "Load"));
        assertEquals(processor._message, "%cRequest started. System Schema: Rose Request: Load Request ID: 42");

        processor.onSpyEvent(new RequestCompletedEvent(23, "Rose", 42, "Load"));
        assertEquals(processor._message, "%cRequest completed. System Schema: Rose Request: Load Request ID: 42");
    }
}
