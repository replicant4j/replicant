package replicant;

import static org.realityforge.braincheck.Guards.*;

import arez.ObservableValue;
import arez.annotations.Action;
import arez.annotations.ArezComponent;
import arez.annotations.Feature;
import arez.annotations.Memoize;
import arez.annotations.Observable;
import arez.annotations.ObservableValueRef;
import arez.annotations.Observe;
import arez.component.DisposeNotifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import zemeckis.Zemeckis;

@ArezComponent(disposeNotifier = Feature.DISABLE, requireId = Feature.DISABLE)
abstract class ReplicantRuntime {
    @NonNull
    private final List<ConnectorEntry> _connectors = new ArrayList<>();

    private boolean _active = true;
    /**
     * Token used to authenticate replicant sessions.
     */
    @Nullable
    private String _authToken;

    @NonNull
    static ReplicantRuntime create() {
        return new Arez_ReplicantRuntime();
    }

    /**
     * Request a Synchronization Point with each backend if necessary.
     */
    @Action
    void requestSynchronizationPoint() {
        getConnectors().forEach(c -> c.getConnector().maybeRequestSynchronizationPoint());
    }

    @Action(verifyRequired = false, reportParameters = false)
    public void setAuthToken(@Nullable final String authToken) {
        if (!Objects.equals(_authToken, authToken)) {
            _authToken = authToken;
            final List<ConnectorEntry> connectors = getConnectors();
            for (final ConnectorEntry entry : connectors) {
                final Connector connector = entry.getConnector();
                if (ConnectorState.CONNECTED == connector.getState()) {
                    connector.getTransport().updateAuthToken(authToken);
                }
            }
        }
    }

    /**
     * @return a token used for authentication, if any.
     */
    @Nullable
    String getAuthToken() {
        return _authToken;
    }

    @Action
    void registerConnector(@NonNull final Connector connector) {
        if (Replicant.shouldCheckInvariants()) {
            invariant(
                    () -> _connectors.stream()
                            .noneMatch(e -> e.getConnector().getSystemSchema().getId()
                                    == connector.getSystemSchema().getId()),
                    () -> "Replicant-0015: Invoked registerConnector for System Schema named '"
                            + connector.getSystemSchema().getName()
                            + "' but a Connector for specified System Schema exists.");
        }
        getConnectorsObservableValue().preReportChanged();
        final ConnectorEntry entry = new ConnectorEntry(connector, true);
        _connectors.add(entry);
        DisposeNotifier.asDisposeNotifier(connector)
                .addOnDisposeListener(this, () -> deregisterConnector(connector), true);
        getConnectorsObservableValue().reportChanged();
    }

    void deregisterConnector(@NonNull final Connector connector) {
        getConnectorsObservableValue().preReportChanged();
        detachConnector(connector);
        getConnectorsObservableValue().reportChanged();
    }

    private void detachConnector(@NonNull final Connector connector) {
        if (Replicant.shouldCheckInvariants()) {
            invariant(
                    () -> _connectors.stream()
                            .anyMatch(e -> e.getConnector().getSystemSchema().getId()
                                    == connector.getSystemSchema().getId()),
                    () -> "Replicant-0006: Invoked deregisterConnector for System Schema named '"
                            + connector.getSystemSchema().getName()
                            + "' but no Connector for specified System Schema exists.");
        }
        _connectors.removeIf(e -> e.getConnector().getSystemSchema().getId()
                == connector.getSystemSchema().getId());
        DisposeNotifier.asDisposeNotifier(connector).removeOnDisposeListener(this, true);
    }

    @Observable(expectSetter = false)
    List<ConnectorEntry> getConnectors() {
        // When Arez can wrap @Observable(expectSetter=false) methods correctly, remove this explicit wrap
        return CollectionsUtil.wrap(_connectors);
    }

    @ObservableValueRef
    abstract ObservableValue<?> getConnectorsObservableValue();

    /**
     * Set whether the Connector for the specified System Schema is a Required Connector.
     *
     * @param systemSchemaId the System Schema ID handled by the Connector.
     * @param required true if the Connector is a Required Connector, false otherwise.
     */
    void setConnectorRequired(final int systemSchemaId, final boolean required) {
        getConnectorEntryBySystemSchemaId(systemSchemaId).setRequired(required);
    }

    /**
     * Returns true when it is desired that all connectors should be connected.
     * This is a desired state rather than an actual state. Actual state is represented by {@link #getState()}.
     *
     * @return true if runtime should be connected, false otherwise.
     */
    @Observable
    boolean isActive() {
        return _active;
    }

    void setActive(final boolean active) {
        _active = active;
    }

    /**
     * Return the aggregate Replicant Context State.
     *
     * @return the Replicant Context State.
     */
    @NonNull
    @Memoize(readOutsideTransaction = Feature.ENABLE)
    ReplicantContextState getState() {
        // Are any required connecting?
        boolean connecting = false;
        // Are any required disconnecting?
        boolean disconnecting = false;
        // Are any required disconnecting?
        boolean disconnected = false;
        // Are any required in fatal error?
        boolean fatalError = false;
        // Are any required in error?
        boolean error = false;

        final List<ConnectorEntry> connectors = getConnectors();
        if (connectors.isEmpty()) {
            // If there are no connectors then we just mirror the desired state (i.e. isActive flag)
            // to the actual state
            if (isActive()) {
                return ReplicantContextState.CONNECTED;
            } else {
                return ReplicantContextState.DISCONNECTED;
            }
        } else {
            for (final ConnectorEntry entry : connectors) {
                if (entry.isRequired()) {
                    final ConnectorState state = entry.getConnector().getState();
                    if (ConnectorState.DISCONNECTED == state) {
                        disconnected = true;
                    } else if (ConnectorState.DISCONNECTING == state) {
                        disconnecting = true;
                    } else if (ConnectorState.CONNECTING == state) {
                        connecting = true;
                    } else if (ConnectorState.ERROR == state) {
                        error = true;
                    } else if (ConnectorState.FATAL_ERROR == state) {
                        fatalError = true;
                    }
                }
            }
        }
        if (fatalError) {
            return ReplicantContextState.FATAL_ERROR;
        } else if (error) {
            return ReplicantContextState.ERROR;
        } else if (disconnected) {
            return ReplicantContextState.DISCONNECTED;
        } else if (disconnecting) {
            return ReplicantContextState.DISCONNECTING;
        } else if (connecting) {
            return ReplicantContextState.CONNECTING;
        } else {
            return ReplicantContextState.CONNECTED;
        }
    }

    /**
     * Mark the Replicant Context as active and begin transitioning to CONNECTED.
     */
    @Action
    void activate() {
        setActive(true);
    }

    /**
     * Mark the Replicant Context as inactive and begin transitioning to DISCONNECTED.
     */
    @Action
    void deactivate() {
        setActive(false);
    }

    /**
     * Retrieve the Connector service associated with the System Schema.
     */
    @NonNull
    Connector getConnector(final int systemSchemaId) {
        return getConnectorEntryBySystemSchemaId(systemSchemaId).getConnector();
    }

    @NonNull
    ConnectorEntry getConnectorEntryBySystemSchemaId(final int systemSchemaId) {
        final ConnectorEntry entry = findConnectorEntryBySystemSchemaId(systemSchemaId);
        if (Replicant.shouldCheckInvariants()) {
            invariant(
                    () -> null != entry,
                    () -> "Replicant-0007: Unable to locate Connector by System Schema ID " + systemSchemaId);
        }
        return Objects.requireNonNull(entry);
    }

    @Nullable
    private ConnectorEntry findConnectorEntryBySystemSchemaId(final int systemSchemaId) {
        for (final ConnectorEntry entry : _connectors) {
            if (entry.getConnector().getSystemSchema().getId() == systemSchemaId) {
                return entry;
            }
        }
        return null;
    }

    @Observable(readOutsideTransaction = Feature.ENABLE, writeOutsideTransaction = Feature.ENABLE)
    abstract int retryGeneration();

    abstract void setRetryGeneration(int value);

    /// Called from timer that will trigger a change so that reflectActiveState() is reactivated
    @Action(skipIfDisposed = Feature.ENABLE)
    void incrementRetryGeneration() {
        setRetryGeneration(retryGeneration() + 1);
    }

    @Observe(mutation = true)
    void reflectActiveState() {
        // Need to watch retryGeneration so that observer is re-triggered when it is changed
        retryGeneration();
        final boolean active = isActive();
        for (final ConnectorEntry entry : getConnectors()) {
            final Connector connector = entry.getConnector();
            final ConnectorState state = connector.getState();
            if (ConnectorState.FATAL_ERROR != state && !ConnectorState.isTransitionState(state)) {
                if (active && ConnectorState.CONNECTED != state) {
                    if (!entry.attemptAction(Connector::connect)) {
                        final int delay = (ConnectorEntry.REGEN_TIME_IN_SECONDS * 1000) + 50;
                        Zemeckis.delayedTask(
                                Zemeckis.areNamesEnabled() ? "reflectActiveState" : null,
                                this::incrementRetryGeneration,
                                delay);
                    }
                } else if (!active && ConnectorState.DISCONNECTED != state && ConnectorState.ERROR != state) {
                    entry.attemptAction(Connector::disconnect);
                }
            }
        }
    }
}
