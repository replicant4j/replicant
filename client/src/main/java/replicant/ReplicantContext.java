package replicant;

import arez.Arez;
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
  private final EntityService _entityService = EntityService.create();
  private final SubscriptionService _subscriptionService =
    SubscriptionService.create( Replicant.areZonesEnabled() ? this : null );
  /**
   * Support infrastructure for spy events.
   */
  @Nullable
  private final SpyImpl _spy = Arez.areSpiesEnabled() ? new SpyImpl() : null;

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
   * Return the entity specified by type and id, creating an Entity if one does not already exist.
   *
   * @param type the type of the entity.
   * @param id   the id of the entity.
   * @return the existing Entity if it exists, otherwise the newly created entity.
   */
  @Nonnull
  public Entity findOrCreateEntity( @Nonnull final Class<?> type, final int id )
  {
    return findOrCreateEntity( Replicant.areNamesEnabled() ? type.getSimpleName() + "/" + id : null,
                               type,
                               id );
  }

  /**
   * Return the entity specified by type and id, creating an Entity if one does not already exist.
   *
   * @param name the name of the entity. This value should be null if and only if {@link Replicant#areNamesEnabled()} returns false.
   * @param type the type of the entity.
   * @param id   the id of the entity.
   * @return the existing Entity if it exists, otherwise the newly created entity.
   */
  @Nonnull
  public Entity findOrCreateEntity( @Nullable final String name,
                                    @Nonnull final Class<?> type,
                                    final int id )
  {
    return getEntityService().findOrCreateEntity( name, type, id );
  }

  /**
   * Find the Entity by type and id.
   *
   * @param type the type of the entity.
   * @param id   the id of the entity.
   * @return the Entity if it exists, null otherwise.
   */
  @Nullable
  public Entity findEntityByTypeAndId( @Nonnull final Class<?> type, @Nonnull final Integer id )
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
   * Only entity types that have at least one instance will be returned from this method.
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
   * @param channelType the channel type.
   * @return the set of ids for all instance subscriptions with specified channel type.
   */
  @Nonnull
  public Set<Object> getInstanceSubscriptionIds( @Nonnull final Enum channelType )
  {
    return getSubscriptionService().getInstanceSubscriptionIds( channelType );
  }

  /**
   * Create a subscription.
   * This method should not be invoked if a subscription with the existing name already exists.
   *
   * @param address              the channel address.
   * @param filter               the filter if subscription is filterable.
   * @param explicitSubscription if subscription was explicitly requested by the client.
   * @return the subscription.
   */
  @Nonnull
  public final Subscription createSubscription( @Nonnull final ChannelAddress address,
                                                @Nullable final Object filter,
                                                final boolean explicitSubscription )
  {
    return getSubscriptionService().createSubscription( address, filter, explicitSubscription );
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
   * Return the underlying AreaOfInterestService implementation.
   *
   * @return the underlying AreaOfInterestService implementation.
   */
  @Nonnull
  final AreaOfInterestService getAreaOfInterestService()
  {
    return _areaOfInterestService;
  }

  /**
   * Return the underlying EntityService implementation.
   *
   * @return the underlying EntityService implementation.
   */
  @Nonnull
  final EntityService getEntityService()
  {
    return _entityService;
  }

  /**
   * Return the underlying SubscriptionService implementation.
   *
   * @return the underlying SubscriptionService implementation.
   */
  @Nonnull
  final SubscriptionService getSubscriptionService()
  {
    return _subscriptionService;
  }
}
