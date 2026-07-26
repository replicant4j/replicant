package replicant;

import static org.realityforge.braincheck.Guards.*;

import arez.Arez;
import arez.Disposable;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * The ReplicantContext defines the top-level container of interconnected Subscriptions, Replica Entries, and Areas of
 * Interest.
 */
public final class ReplicantContext {
    @NonNull
    private final AreaOfInterestService _areaOfInterestService;

    @NonNull
    private final ReplicaRegistry _replicaRegistry;

    @NonNull
    private final SubscriptionService _subscriptionService;

    @NonNull
    private final ReplicantRuntime _runtime;

    @NonNull
    private final SubscriptionReconciler _subscriptionReconciler;

    @NonNull
    private final Validator _validator;

    @NonNull
    private final SchemaService _schemaService;
    /**
     * Support infrastructure for spy events.
     */
    @Nullable
    private final SpyImpl _spy;
    /**
     * Service responsible for caching data to avoid hitting the network during requests.
     */
    @Nullable
    private CacheService _cacheService;

    ReplicantContext() {
        assert Arez.context().isSchedulerPaused();
        _areaOfInterestService = AreaOfInterestService.create(Replicant.areZonesEnabled() ? this : null);
        _replicaRegistry = ReplicaRegistry.create(Replicant.areZonesEnabled() ? this : null);
        _subscriptionService = SubscriptionService.create(Replicant.areZonesEnabled() ? this : null);
        _runtime = ReplicantRuntime.create();
        _subscriptionReconciler = SubscriptionReconciler.create(Replicant.areZonesEnabled() ? this : null);
        _validator = Validator.create(Replicant.areZonesEnabled() ? this : null);
        _schemaService = SchemaService.create();
        _spy = Replicant.areSpiesEnabled() ? new SpyImpl() : null;
    }

    public void setAuthToken(@Nullable final String authToken) {
        getRuntime().setAuthToken(authToken);
    }

    /**
     * @return a token used for authentication, if any.
     */
    @Nullable
    public String getAuthToken() {
        return getRuntime().getAuthToken();
    }

    /**
     * Register a connector with specified schema and transport. The transport instance must be unique
     * to this connector but the schema may be shared between multiple connectors.
     *
     * @param schema    the schema defining datasource.
     * @param transport the transport.
     */
    @NonNull
    public Disposable registerConnector(@NonNull final SystemSchema schema, @NonNull final Transport transport) {
        return Disposable.asDisposable(Connector.create(Replicant.areZonesEnabled() ? this : null, schema, transport));
    }

    /**
     * Return the collection of AreaOfInterest that have been declared.
     *
     * @return the collection of AreaOfInterest that have been declared.
     */
    @NonNull
    public List<AreaOfInterest> getAreasOfInterest() {
        return getAreaOfInterestService().getAreasOfInterest();
    }

    /**
     * Return a specific AreaOfInterest that has specified Dataset Address.
     *
     * @param datasetAddress the Dataset Address declared by the Area of Interest
     * @return the AreaOfInterest that matches if any.
     */
    @Nullable
    public AreaOfInterest findAreaOfInterestByDatasetAddress(@NonNull final DatasetAddress datasetAddress) {
        return getAreaOfInterestService().findAreaOfInterestByDatasetAddress(datasetAddress);
    }

    /**
     * Locate an existing AreaOfInterest with specified Dataset Address or create a new AreaOfInterest.
     * The filter is updated, if required, to match the specified parameter.
     *
     * @param datasetAddress the Dataset Address declared by the Area of Interest
     * @param filter  the Filter Parameter used for the Subscription.
     * @return the AreaOfInterest.
     */
    @NonNull
    public AreaOfInterest createOrUpdateAreaOfInterest(
            @NonNull final DatasetAddress datasetAddress, @Nullable final Object filter) {
        return getAreaOfInterestService().createOrUpdateAreaOfInterest(datasetAddress, filter);
    }

    /**
     * Find the Replica Entry by type and id.
     *
     * @param type the type of the Replica.
     * @param id   the Entity identifier.
     * @return the Replica Entry if it exists, null otherwise.
     */
    @Nullable
    public ReplicaEntry findReplicaEntryByTypeAndId(@NonNull final Class<?> type, final int id) {
        return getReplicaRegistry().findReplicaEntryByTypeAndId(type, id);
    }

    @NonNull
    public List<ReplicaEntry> findAllReplicaEntriesByType(@NonNull final Class<?> type) {
        return getReplicaRegistry().findAllReplicaEntriesByType(type);
    }

    /**
     * Return the collection of Replica types that exist in the system.
     * Only Replica types that have at least one instance will be returned from this method unless
     * a Replica Entry has been disposed and the scheduler is yet to invoke code to remove the type from the set.
     * This is unlikely to be exposed to normal user code.
     *
     * @return the collection of Replica types.
     */
    @NonNull
    public Collection<Class<?>> findAllReplicaTypes() {
        return getReplicaRegistry().findAllReplicaTypes();
    }

    /**
     * Return the collection of type subscriptions.
     *
     * @return the collection of type subscriptions.
     */
    @NonNull
    public List<Subscription> getTypeSubscriptions() {
        return getSubscriptionService().getTypeSubscriptions();
    }

    /**
     * Return the collection of instance subscriptions.
     *
     * @return the collection of instance subscriptions.
     */
    @NonNull
    public Collection<Subscription> getInstanceSubscriptions() {
        return getSubscriptionService().getInstanceSubscriptions();
    }

    /**
     * Return the collection of instance Subscriptions for the Dataset.
     *
     * @param schemaId  the schema id.
     * @param datasetId the Dataset id.
     * @return the set of ids for all instance Subscriptions to the specified Dataset.
     */
    @NonNull
    public Set<Integer> getInstanceSubscriptionIds(final int schemaId, final int datasetId) {
        return getSubscriptionService().getInstanceSubscriptionIds(schemaId, datasetId);
    }

    /**
     * Return the subscription for the specified Dataset Address.
     * This method will observe the <code>typeSubscriptions</code> or <code>instanceSubscriptions</code>
     * property if not found and the result {@link Subscription} if found. This ensures that if an observer
     * invokes this method then the observer will be rescheduled when the result changes.
     *
     * @param datasetAddress the Dataset Address
     * @return the subscription if it exists, null otherwise.
     */
    @Nullable
    public Subscription findSubscription(@NonNull final DatasetAddress datasetAddress) {
        return getSubscriptionService().findSubscription(datasetAddress);
    }

    /**
     * Return the SystemSchema instances registered with the context.
     *
     * @return the SystemSchema instances registered with the context.
     */
    @NonNull
    public Collection<SystemSchema> getSchemas() {
        return getSchemaService().getSchemas();
    }

    /**
     * Return the schema with the specified schemaId or null if no such schema.
     *
     * @param schemaId the id of the schema.
     * @return the schema or null if no such schema.
     */
    @Nullable
    public SystemSchema findSchemaById(final int schemaId) {
        return getSchemaService().findById(schemaId);
    }

    /**
     * Return the schema with the specified schemaId.
     * This should not be invoked unless the schema with specified id exists.
     *
     * @param schemaId the id of the schema.
     * @return the schema.
     */
    @NonNull
    public SystemSchema getSchemaById(final int schemaId) {
        return getSchemaService().getById(schemaId);
    }

    /**
     * Return true if spy events will be propagated.
     * This means spies are enabled and there is at least one spy event handler present.
     *
     * @return true if spy events will be propagated, false otherwise.
     */
    boolean willPropagateSpyEvents() {
        return Replicant.areSpiesEnabled() && getSpy().willPropagateSpyEvents();
    }

    /**
     * Return the spy associated with context.
     * This method should not be invoked unless {@link Replicant#areSpiesEnabled()} returns true.
     *
     * @return the spy associated with context.
     */
    @NonNull
    public Spy getSpy() {
        if (Replicant.shouldCheckApiInvariants()) {
            apiInvariant(
                    Replicant::areSpiesEnabled,
                    () -> "Replicant-0021: Attempting to get Spy but spies are not enabled.");
        }
        return Objects.requireNonNull(_spy);
    }

    /**
     * Specify the action invoked before Subscription Reconciliation compares desired Areas of Interest with actual
     * Subscriptions.
     * This action is often used when subscriptions in one system trigger subscriptions in another system.
     * This property is an Arez observable.
     *
     * @param preReconciliationAction the action.
     */
    public void setPreReconciliationAction(@Nullable final SafeProcedure preReconciliationAction) {
        getSubscriptionReconciler().setPreReconciliationAction(preReconciliationAction);
    }

    /**
     * Return the pre-reconciliation action. See {@link #setPreReconciliationAction(SafeProcedure)} for further details.
     * This property is an Arez observable.
     *
     * @return the action.
     */
    @Nullable
    public SafeProcedure getPreReconciliationAction() {
        return getSubscriptionReconciler().getPreReconciliationAction();
    }

    /**
     * Specify the action that is invoked after Subscription Reconciliation completes.
     * This property is an Arez observable.
     *
     * @param reconciliationCompleteAction the action.
     */
    public void setReconciliationCompleteAction(@Nullable final SafeProcedure reconciliationCompleteAction) {
        getSubscriptionReconciler().setReconciliationCompleteAction(reconciliationCompleteAction);
    }

    /**
     * Return the reconciliation-complete action. See {@link #setReconciliationCompleteAction(SafeProcedure)} for
     * further details.
     * This property is an Arez observable.
     *
     * @return the action.
     */
    @Nullable
    public SafeProcedure getReconciliationCompleteAction() {
        return getSubscriptionReconciler().getReconciliationCompleteAction();
    }

    /**
     * Set the desired state of the context as "active" and start driving Connectors toward CONNECTED.
     * The desired state of the context is accessible via {@link #isActive()} while the actual state of the
     * context is accessible via {@link #getState()}.
     */
    public void activate() {
        final ReplicantRuntime runtime = getRuntime();
        runtime.activate();
        runtime.requestSync();
    }

    /**
     * Set the desired state of the context as "inactive" and start driving Connectors toward DISCONNECTED.
     * The desired state of the context is accessible via {@link #isActive()} while the actual state of the
     * context is accessible via {@link #getState()}.
     */
    public void deactivate() {
        getRuntime().deactivate();
    }

    /**
     * Return true if the desired state of the system is "active", false otherwise.
     * This property is Arez observable.
     *
     * @return true if the desired state of the system is "active", false otherwise.
     */
    public boolean isActive() {
        return getRuntime().isActive();
    }

    /**
     * Return the actual state of the context.
     *
     * @return the actual state of the context.
     */
    @NonNull
    public RuntimeState getState() {
        return getRuntime().getState();
    }

    /**
     * Set the "required" flag for connector for specified type.
     * NOTE: It is expected that the way this is done will change in the future.
     *
     * @param schemaId the id of the schema handled by connector.
     * @param required true if connector is required for the context to be active, false otherwise.
     */
    public void setConnectorRequired(final int schemaId, final boolean required) {
        getRuntime().setConnectorRequired(schemaId, required);
    }

    /**
     * Get the connection id from the connector for specified schema if the connector has established a connection else return null.
     *
     * @param schemaId the id of the schema.
     */
    @Nullable
    public String findConnectionId(final int schemaId) {
        final Connector connector = getRuntime().getConnector(schemaId);
        final Connection connection = connector.getConnection();
        return null == connection ? null : connection.getConnectionId();
    }

    /**
     * Schedule an "Exec" request to the server.
     * The request is identified by a command and a payload (a.k.a. parameters) and may return a result.
     * The mapping of the command and payload to behaviour is abstracted away by server and is outside
     * the scope of this api.
     *
     * @param schemaId        the id of the schema.
     * @param command         the command string. It uniquely identifies a call.
     * @param payload         the payload or parameters of the payload.
     * @param responseHandler the ResponseHandler invoked when a response is received.
     */
    public void exec(
            final int schemaId,
            @NonNull final String command,
            @Nullable final Object payload,
            @Nullable final ResponseHandler responseHandler) {
        getRuntime().getConnector(schemaId).requestExec(command, payload, responseHandler);
    }

    /**
     * Return the CacheService associated with context if any.
     *
     * @return the CacheService associated with context if any.
     */
    @Nullable
    public CacheService getCacheService() {
        return _cacheService;
    }

    /**
     * Specify the CacheService used by context if any.
     *
     * @param cacheService the CacheService.
     */
    public void setCacheService(@Nullable final CacheService cacheService) {
        _cacheService = cacheService;
    }

    /**
     * Return the underlying AreaOfInterestService.
     *
     * @return the underlying AreaOfInterestService.
     */
    @NonNull
    AreaOfInterestService getAreaOfInterestService() {
        return _areaOfInterestService;
    }

    /**
     * Return the underlying ReplicaRegistry.
     *
     * @return the underlying ReplicaRegistry.
     */
    @NonNull
    ReplicaRegistry getReplicaRegistry() {
        return _replicaRegistry;
    }

    /**
     * Return the underlying SubscriptionService.
     *
     * @return the underlying SubscriptionService.
     */
    @NonNull
    SubscriptionService getSubscriptionService() {
        return _subscriptionService;
    }

    /**
     * Return the underlying ReplicantRuntime.
     *
     * @return the underlying ReplicantRuntime.
     */
    @NonNull
    ReplicantRuntime getRuntime() {
        return _runtime;
    }

    /**
     * Return the underlying SubscriptionReconciler.
     *
     * @return the underlying SubscriptionReconciler.
     */
    @NonNull
    SubscriptionReconciler getSubscriptionReconciler() {
        return _subscriptionReconciler;
    }

    /**
     * Return the underlying SchemaService.
     *
     * @return the underlying SchemaService.
     */
    @NonNull
    SchemaService getSchemaService() {
        return _schemaService;
    }

    /**
     * Return the underlying Validator.
     *
     * @return the underlying Validator.
     */
    @NonNull
    Validator getValidator() {
        return _validator;
    }
}
