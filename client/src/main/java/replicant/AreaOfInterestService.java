package replicant;

import arez.Arez;
import arez.Disposable;
import arez.ObservableValue;
import arez.annotations.ArezComponent;
import arez.annotations.Feature;
import arez.annotations.Observable;
import arez.annotations.ObservableValueRef;
import arez.annotations.PreDispose;
import arez.component.CollectionsUtil;
import arez.component.DisposeNotifier;
import arez.component.Identifiable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import replicant.spy.AreaOfInterestCreatedEvent;
import replicant.spy.AreaOfInterestFilterUpdatedEvent;
import static org.realityforge.braincheck.Guards.*;

/**
 * The AreaOfInterestService is responsible for managing AreaOfInterest instance.
 * An {@link AreaOfInterest} represents a declaration of a desire for a
 * {@link Subscription}. The intention
 * is that user code defines the desired state as instances of {@link AreaOfInterest}
 * and the {@link Converger} converges
 * the actual state towards the desired state.
 */
@ArezComponent( disposeNotifier = Feature.DISABLE )
abstract class AreaOfInterestService
  extends ReplicantService
{
  /**
   * A map of all the entities ArezId to entity.
   */
  @Nonnull
  private final Map<Integer, AreaOfInterest> _areasOfInterest = new HashMap<>();

  AreaOfInterestService( @Nullable final ReplicantContext context )
  {
    super( context );
  }

  /**
   * Create an instance of the AreaOfInterestService.
   *
   * @return an instance of the AreaOfInterestService.
   */
  @Nonnull
  static AreaOfInterestService create( @Nullable final ReplicantContext context )
  {
    return new Arez_AreaOfInterestService( context );
  }

  /**
   * Return the collection of AreaOfInterest that have been declared.
   *
   * @return the collection of AreaOfInterest that have been declared.
   */
  @Nonnull
  List<AreaOfInterest> getAreasOfInterest()
  {
    return CollectionsUtil.asList( areasOfInterest() );
  }

  /**
   * Return a specific AreaOfInterest that has specified address.
   *
   * @param address the address of the channel that AreaOfInterest is about.
   * @return the AreaOfInterest that matches if any.
   */
  @Nullable
  AreaOfInterest findAreaOfInterestByAddress( @Nonnull final ChannelAddress address )
  {
    return areasOfInterest().filter( e -> e.getAddress().equals( address ) ).findAny().orElse( null );
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
  AreaOfInterest createOrUpdateAreaOfInterest( @Nonnull final ChannelAddress address, @Nullable final Object filter )
  {
    final AreaOfInterest areaOfInterest = findAreaOfInterestByAddress( address );
    if ( null != areaOfInterest )
    {
      if ( !FilterUtil.filtersEqual( areaOfInterest.getFilter(), filter ) )
      {
        areaOfInterest.setFilter( filter );
        if ( Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents() )
        {
          getReplicantContext().getSpy().reportSpyEvent( new AreaOfInterestFilterUpdatedEvent( areaOfInterest ) );
        }
      }
      return areaOfInterest;
    }
    else
    {
      final AreaOfInterest newAreaOfInterest =
        AreaOfInterest.create( Replicant.areZonesEnabled() ? getReplicantContext() : null,
                               address,
                               filter );
      attach( newAreaOfInterest );
      if ( Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents() )
      {
        getReplicantContext().getSpy().reportSpyEvent( new AreaOfInterestCreatedEvent( newAreaOfInterest ) );
      }
      return newAreaOfInterest;
    }
  }

  /**
   * Attach specified entity to the set of entities managed by the container.
   * This should not be invoked if the entity is already attached to the repository.
   *
   * @param entity the entity to register.
   */
  @SuppressWarnings( "SuspiciousMethodCalls" )
  protected void attach( @Nonnull final AreaOfInterest entity )
  {
    if ( Arez.shouldCheckApiInvariants() )
    {
      apiInvariant( () -> Disposable.isNotDisposed( entity ),
                    () -> "Arez-0168: Called attach() passing an entity that is disposed. Entity: " + entity );
      apiInvariant( () -> !_areasOfInterest.containsKey( Identifiable.getArezId( entity ) ),
                    () -> "Arez-0136: Called attach() passing an entity that is already attached " +
                          "to the container. Entity: " + entity );
    }
    getAreasOfInterestObservableValue().preReportChanged();
    attachEntity( entity );
    _areasOfInterest.put( Identifiable.getArezId( entity ), entity );
    getAreasOfInterestObservableValue().reportChanged();
  }

  /**
   * Dispose or detach all the entities associated with the container.
   */
  @PreDispose
  void preDispose()
  {
    _areasOfInterest.values().forEach( entry -> detachEntity( entry, true ) );
    _areasOfInterest.clear();
  }

  /**
   * Return true if the specified entity is contained in the container.
   *
   * @param entity the entity.
   * @return true if the specified entity is contained in the container, false otherwise.
   */
  protected boolean contains( @Nonnull final AreaOfInterest entity )
  {
    getAreasOfInterestObservableValue().reportObserved();
    return _areasOfInterest.containsKey( Identifiable.<Integer>getArezId( entity ) );
  }

  /**
   * Detach entity from container without disposing entity.
   * The entity must be attached to the container.
   *
   * @param entity the entity to detach.
   */
  private void detach( @Nonnull final AreaOfInterest entity )
  {
    // This method has been extracted to try and avoid GWT inlining into invoker
    final AreaOfInterest removed = _areasOfInterest.remove( Identifiable.<Integer>getArezId( entity ) );
    if ( null != removed )
    {
      getAreasOfInterestObservableValue().preReportChanged();
      detachEntity( entity, false );
      getAreasOfInterestObservableValue().reportChanged();
    }
    else
    {
      fail( () -> "Arez-0157: Called detach() passing an entity that was not attached to the container. Entity: " +
                  entity );
    }
  }

  /**
   * Return a stream of all entities in the container.
   *
   * @return the underlying entities.
   */
  @Observable( expectSetter = false )
  @Nonnull
  public Stream<AreaOfInterest> areasOfInterest()
  {
    return _areasOfInterest.values().stream();
  }

  @ObservableValueRef
  @Nonnull
  abstract ObservableValue<Stream<AreaOfInterest>> getAreasOfInterestObservableValue();

  private void attachEntity( @Nonnull final AreaOfInterest entity )
  {
    DisposeNotifier
      .asDisposeNotifier( entity )
      .addOnDisposeListener( this, () -> {
        getAreasOfInterestObservableValue().preReportChanged();
        detach( entity );
        getAreasOfInterestObservableValue().reportChanged();
      } );
  }

  private void detachEntity( @Nonnull final AreaOfInterest entity, final boolean disposeOnDetach )
  {
    DisposeNotifier.asDisposeNotifier( entity ).removeOnDisposeListener( this );
    if ( disposeOnDetach )
    {
      Disposable.dispose( entity );
    }
  }
}
