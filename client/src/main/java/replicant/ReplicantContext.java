package replicant;

import arez.Disposable;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import static org.realityforge.braincheck.Guards.*;

/**
 * The ReplicantContext defines the top level container of interconnected subscriptions, entities and areas of interest.
 */
public final class ReplicantContext
{
  private final AreaOfInterestService _areaOfInterestService =
    AreaOfInterestService.create( Replicant.areZonesEnabled() ? this : null );
  private final EntityService _entityService = EntityService.create( Replicant.areZonesEnabled() ? this : null );
  private final SubscriptionService _subscriptionService =
    SubscriptionService.create( Replicant.areZonesEnabled() ? this : null );
  private final ReplicantRuntime _runtime = ReplicantRuntime.create();
  private final Converger _converger = Converger.create( Replicant.areZonesEnabled() ? this : null );
  private final Validator _validator = Validator.create( Replicant.areZonesEnabled() ? this : null );
  private final SchemaService _schemaService = SchemaService.create();
  /**
   * Service responsible for caching data to avoid hitting the network during requests.
   */
  @Nullable
  private CacheService _cacheService;
  /**
   * Support infrastructure for spy events.
   */
  @Nullable
  private final SpyImpl _spy = Replicant.areSpiesEnabled() ? new SpyImpl() : null;

  public ReplicantContext()
  {
  }

  /**
   * Register a connector with specified schema and transport. The transport instance must be unique
   * to this connector but the schema may be shared between multiple connectors.
   *
   * @param schema    the schema defining datasource.
   * @param transport the transport.
   */
  @Nonnull
  public Disposable registerConnector( @Nonnull final SystemSchema schema, @Nonnull final Transport transport )
  {
    final Connector connector = Connector.create( Replicant.areZonesEnabled() ? this : null, schema, transport );
    return Disposable.asDisposable( connector );
  }

  /**
   * Return the collection of AreaOfInterest that have been declared.
   *
   * @return the collection of AreaOfInterest that have been declared.
   */
  @Nonnull
  public List<AreaOfInterest> getAreasOfInterest()
  {
    return getAreaOfInterestService().getAreasOfInterest();
  }

  /**
   * Return a specific AreaOfInterest that has specified address.
   *
   * @param address the address of the channel that AreaOfInterest is about.
   * @return the AreaOfInterest that matches if any.
   */
  @Nullable
  public AreaOfInterest findAreaOfInterestByAddress( @Nonnull final ChannelAddress address )
  {
    return getAreaOfInterestService().findAreaOfInterestByAddress( address );
  }

  /**
   * Locate an existing AreaOfInterest with specified address or create a new AreaOfInterest.
   * The filter is updated, if required, to match the specified parameter.
   *
   * @param address the address of the channel that AreaOfInterest is about.
   * @param filter  the filter that is used to define the channel.
   * @return the AreaOfInterest.
   */
  @Nonnull
  public AreaOfInterest createOrUpdateAreaOfInterest( @Nonnull final ChannelAddress address,
                                                      @Nullable final Object filter )
  {
    return getAreaOfInterestService().createOrUpdateAreaOfInterest( address, filter );
  }

  /**
   * Find the Entity by type and id.
   *
   * @param type the type of the entity.
   * @param id   the id of the entity.
   * @return the Entity if it exists, null otherwise.
   */
  @Nullable
  public Entity findEntityByTypeAndId( @Nonnull final Class<?> type, final int id )
  {
    return getEntityService().findEntityByTypeAndId( type, id );
  }

  @Nonnull
  public List<Entity> findAllEntitiesByType( @Nonnull final Class<?> type )
  {
    return getEntityService().findAllEntitiesByType( type );
  }

  /**
   * Return the collection of entity types that exist in the system.
   * Only entity types that have at least one instance will be returned from this method unless
   * an Entity has been disposed and the scheduler is yet to invoke code to remove type from set.
   * This is a unlikely to be exposed to normal user code.
   *
   * @return the collection of entity types.
   */
  @Nonnull
  public Collection<Class<?>> findAllEntityTypes()
  {
    return getEntityService().findAllEntityTypes();
  }

  /**
   * Return the collection of type subscriptions.
   *
   * @return the collection of type subscriptions.
   */
  @Nonnull
  public List<Subscription> getTypeSubscriptions()
  {
    return getSubscriptionService().getTypeSubscriptions();
  }

  /**
   * Return the collection of instance subscriptions.
   *
   * @return the collection of instance subscriptions.
   */
  @Nonnull
  public Collection<Subscription> getInstanceSubscriptions()
  {
    return getSubscriptionService().getInstanceSubscriptions();
  }

  /**
   * Return the collection of instance subscriptions for channel.
   *
   * @param systemId  the system id.
   * @param channelId the channel id.
   * @return the set of ids for all instance subscriptions with specified channel type.
   */
  @Nonnull
  public Set<Object> getInstanceSubscriptionIds( final int systemId, final int channelId )
  {
    return getSubscriptionService().getInstanceSubscriptionIds( systemId, channelId );
  }

  /**
   * Return the subscription for the specified address.
   * This method will observe the <code>typeSubscriptions</code> or <code>instanceSubscriptions</code>
   * property if not found and the result {@link Subscription} if found. This ensures that if an observer
   * invokes this method then the observer will be rescheduled when the result changes.
   *
   * @param address the channel address.
   * @return the subscription if it exists, null otherwise.
   */
  @Nullable
  public final Subscription findSubscription( @Nonnull final ChannelAddress address )
  {
    return getSubscriptionService().findSubscription( address );
  }

  /**
   * Return the SystemSchema instances registered with the context.
   *
   * @return the SystemSchema instances registered with the context.
   */
  @Nonnull
  public Collection<SystemSchema> getSchemas()
  {
    return getSchemaService().getSchemas();
  }

  /**
   * Return true if spy events will be propagated.
   * This means spies are enabled and there is at least one spy event handler present.
   *
   * @return true if spy events will be propagated, false otherwise.
   */
  boolean willPropagateSpyEvents()
  {
    return Replicant.areSpiesEnabled() && getSpy().willPropagateSpyEvents();
  }

  /**
   * Return the spy associated with context.
   * This method should not be invoked unless {@link Replicant#areSpiesEnabled()} returns true.
   *
   * @return the spy associated with context.
   */
  @Nonnull
  public Spy getSpy()
  {
    if ( Replicant.shouldCheckApiInvariants() )
    {
      apiInvariant( Replicant::areSpiesEnabled,
                    () -> "Replicant-0021: Attempting to get Spy but spies are not enabled." );
    }
    assert null != _spy;
    return _spy;
  }

  /**
   * Specify the action that invoked prior to converging the desired AreaOfInterest to actual Subscriptions.
   * This action is often used when subscriptions in one system trigger subscriptions in another system.
   * This property is an Arez observable.
   *
   * @param preConvergeAction the action.
   */
  public void setPreConvergeAction( @Nullable final SafeProcedure preConvergeAction )
  {
    getConverger().setPreConvergeAction( preConvergeAction );
  }

  /**
   * Return the pre-converge action. See {@link #setPreConvergeAction(SafeProcedure)} for further details.
   * This property is an Arez observable.
   *
   * @return the action.
   */
  @Nullable
  public SafeProcedure getPreConvergeAction()
  {
    return getConverger().getPreConvergeAction();
  }

  /**
   * Specify the action that is invoked after all the subscriptions converge.
   * This property is an Arez observable.
   *
   * @param convergeCompleteAction the action.
   */
  public void setConvergeCompleteAction( @Nullable final SafeProcedure convergeCompleteAction )
  {
    getConverger().setConvergeCompleteAction( convergeCompleteAction );
  }

  /**
   * Return the converge complete action. See {@link #setConvergeCompleteAction(SafeProcedure)} for further details.
   * This property is an Arez observable.
   *
   * @return the action.
   */
  @Nullable
  public SafeProcedure getConvergeCompleteAction()
  {
    return getConverger().getConvergeCompleteAction();
  }

  /**
   * Set the desired state of the context as "active" and start to converge Connectors to being CONNECTED.
   * The desired state of the context is accessible via {@link #isActive()} while the actual state of the
   * context is accessible via {@link #getState()}.
   */
  public void activate()
  {
    getRuntime().activate();
  }

  /**
   * Set the desired state of the context as "inactive" and start to converge Connectors to being DISCONNECTED.
   * The desired state of the context is accessible via {@link #isActive()} while the actual state of the
   * context is accessible via {@link #getState()}.
   */
  public void deactivate()
  {
    getRuntime().deactivate();
  }

  /**
   * Return true if the desired state of the system is "active", false otherwise.
   * This property is Arez observable.
   *
   * @return true if the desired state of the system is "active", false otherwise.
   */
  public boolean isActive()
  {
    return getRuntime().isActive();
  }

  /**
   * Return the actual state of the context.
   *
   * @return the actual state of the context.
   */
  public RuntimeState getState()
  {
    return getRuntime().getState();
  }

  /**
   * Set the "required" flag for connector for specified type.
   * NOTE: It is expected that the way this is done will change in the future.
   *
   * @param schemaId the id of the schema handled by connector.
   * @param required true if connector is required for the context to be active, false otherwise.
   */
  public void setConnectorRequired( final int schemaId, final boolean required )
  {
    getRuntime().setConnectorRequired( schemaId, required );
  }

  /**
   * Create a new request abstraction.
   * This generates a requestId for connection but it is the responsibility of the caller to perform request and
   * invoke the {@link Request#onSuccess(boolean, SafeProcedure)} or {@link Request#onFailure(SafeProcedure)} method
   * when the request completes.
   *
   * @param schemaId the id of the schema of connector where request created.
   * @param name     the name of the request. This should be null if {@link Replicant#areNamesEnabled()} returns false, otherwise it should be non-null.
   */
  @Nonnull
  public Request newRequest( final int schemaId, @Nullable final String name )
  {
    return getRuntime().getConnector( schemaId ).ensureConnection().newRequest( name );
  }

  /**
   * Return the CacheService associated with context if any.
   *
   * @return the CacheService associated with context if any.
   */
  @Nullable
  public CacheService getCacheService()
  {
    return _cacheService;
  }

  /**
   * Specify the CacheService used by context if any.
   *
   * @param cacheService the CacheService.
   */
  public void setCacheService( @Nullable final CacheService cacheService )
  {
    _cacheService = cacheService;
  }

  /**
   * Return the underlying AreaOfInterestService.
   *
   * @return the underlying AreaOfInterestService.
   */
  @Nonnull
  private AreaOfInterestService getAreaOfInterestService()
  {
    return _areaOfInterestService;
  }

  /**
   * Return the underlying EntityService.
   *
   * @return the underlying EntityService.
   */
  @Nonnull
  final EntityService getEntityService()
  {
    return _entityService;
  }

  /**
   * Return the underlying SubscriptionService.
   *
   * @return the underlying SubscriptionService.
   */
  @Nonnull
  final SubscriptionService getSubscriptionService()
  {
    return _subscriptionService;
  }

  /**
   * Return the underlying ReplicantRuntime.
   *
   * @return the underlying ReplicantRuntime.
   */
  @Nonnull
  final ReplicantRuntime getRuntime()
  {
    return _runtime;
  }

  /**
   * Return the underlying Converger.
   *
   * @return the underlying Converger.
   */
  @Nonnull
  final Converger getConverger()
  {
    return _converger;
  }

  /**
   * Return the underlying SchemaService.
   *
   * @return the underlying SchemaService.
   */
  @Nonnull
  final SchemaService getSchemaService()
  {
    return _schemaService;
  }

  /**
   * Return the underlying Validator.
   *
   * @return the underlying Validator.
   */
  @Nonnull
  final Validator getValidator()
  {
    return _validator;
  }
}
