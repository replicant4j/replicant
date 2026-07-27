package replicant.server.ee;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.transaction.TransactionSynchronizationRegistry;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import replicant.server.EntityChangeCandidate;
import replicant.server.runtime.EntityChangeCandidateCacheUtil;
import replicant.server.runtime.TransactionSynchronizationRegistryUtil;
import replicant.server.transport.ReplicantChangeRecorder;

public class ReplicantEntityChangeListenerTest {
    @BeforeMethod
    public void setup() {
        RegistryUtil.bind();
        EntityChangeCandidateCacheUtil.removeEntityChangeCandidateSet();
    }

    @AfterMethod
    public void teardown() {
        RegistryUtil.unbind();
    }

    @Test
    public void postUpdate_mergesEntityChangeCandidateWhenPresent() {
        final var registry = TransactionSynchronizationRegistryUtil.lookup();
        final var recorder = mock(ReplicantChangeRecorder.class);
        final var listener = newListener(registry, recorder);
        final var entity = new Object();

        when(recorder.convertToEntityChangeCandidate(entity, true))
                .thenReturn(new EntityChangeCandidate(11, 7, 0L, new HashMap<>(), Map.of("a", "b")));

        listener.postUpdate(entity);

        final var set = EntityChangeCandidateCacheUtil.lookupEntityChangeCandidateSet();
        assertNotNull(set);
        assertTrue(Objects.requireNonNull(set).containsEntityChangeCandidate(7, 11));
        verify(recorder).convertToEntityChangeCandidate(entity, true);
    }

    @Test
    public void postUpdate_ignoresEventWhenRollbackOnly() {
        final var registry = TransactionSynchronizationRegistryUtil.lookup();
        registry.setRollbackOnly();

        final var recorder = mock(ReplicantChangeRecorder.class);
        final var listener = newListener(registry, recorder);

        listener.postUpdate(new Object());

        assertNull(EntityChangeCandidateCacheUtil.lookupEntityChangeCandidateSet());
        verifyNoInteractions(recorder);
    }

    @Test
    public void preRemove_mergesEntityChangeCandidateWhenPresent() {
        final var registry = TransactionSynchronizationRegistryUtil.lookup();
        final var recorder = mock(ReplicantChangeRecorder.class);
        final var listener = newListener(registry, recorder);
        final var entity = new Object();
        final var routingKeys = new HashMap<String, Serializable>();

        when(recorder.convertToEntityChangeCandidate(entity, false))
                .thenReturn(new EntityChangeCandidate(12, 8, 0L, routingKeys, null));

        listener.preRemove(entity);

        final var set = EntityChangeCandidateCacheUtil.lookupEntityChangeCandidateSet();
        assertNotNull(set);
        assertTrue(Objects.requireNonNull(set).containsEntityChangeCandidate(8, 12));
        verify(recorder).convertToEntityChangeCandidate(entity, false);
    }

    @Test
    public void preRemove_ignoresNullEntityChangeCandidate() {
        final var registry = TransactionSynchronizationRegistryUtil.lookup();
        final var recorder = mock(ReplicantChangeRecorder.class);
        final var listener = newListener(registry, recorder);
        final var entity = new Object();

        when(recorder.convertToEntityChangeCandidate(entity, false)).thenReturn(null);

        listener.preRemove(entity);

        assertNull(EntityChangeCandidateCacheUtil.lookupEntityChangeCandidateSet());
        verify(recorder).convertToEntityChangeCandidate(entity, false);
    }

    @NonNull
    private ReplicantEntityChangeListener newListener(
            @NonNull final TransactionSynchronizationRegistry registry,
            @NonNull final ReplicantChangeRecorder recorder) {
        final var listener = new ReplicantEntityChangeListener();
        setField(listener, "_registry", registry);
        setField(listener, "_recorder", recorder);
        return listener;
    }

    private void setField(@NonNull final Object target, @NonNull final String name, @Nullable final Object value) {
        try {
            final var field = ReplicantEntityChangeListener.class.getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, value);
        } catch (final Exception e) {
            throw new AssertionError(e);
        }
    }
}
