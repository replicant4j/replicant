package replicant;

import arez.Arez;
import arez.Disposable;
import arez.ObservableValue;
import arez.annotations.ArezComponent;
import arez.annotations.Feature;
import arez.annotations.Observable;
import arez.annotations.ObservableValueRef;
import arez.annotations.PreDispose;
import arez.component.DisposeNotifier;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
   * A set of all the AreasOfInterest.
   */
  @Nonnull
  private final Set<AreaOfInterest> _areasOfInterest = new HashSet<>();

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
   * Attach specified areaOfInterest to the set of areasOfInterest managed by the container.
   * This should not be invoked if the areaOfInterest is already attached to the repository.
   *
   * @param areaOfInterest the areaOfInterest to register.
   */
  protected void attach( @Nonnull final AreaOfInterest areaOfInterest )
  {
    if ( Arez.shouldCheckApiInvariants() )
    {
      apiInvariant( () -> Disposable.isNotDisposed( areaOfInterest ),
                    () -> "Replicant-0093: Called attach() passing an areaOfInterest that is disposed. " +
                          "AreaOfInterest: " + areaOfInterest );
      apiInvariant( () -> !_areasOfInterest.contains( areaOfInterest ),
                    () -> "Replicant-0094: Called attach() passing an areaOfInterest that is already attached to " +
                          "the container. AreaOfInterest: " + areaOfInterest );
    }
    getAreasOfInterestObservableValue().preReportChanged();
    doAttach( areaOfInterest );
    _areasOfInterest.add( areaOfInterest );
    getAreasOfInterestObservableValue().reportChanged();
  }

  /**
   * Dispose or detach all the areasOfInterest associated with the container.
   */
  @PreDispose
  void preDispose()
  {
    _areasOfInterest.forEach( entry -> doDetach( entry, true ) );
    _areasOfInterest.clear();
  }

  /**
   * Return true if the specified areaOfInterest is contained in the container.
   *
   * @param areaOfInterest the areaOfInterest.
   * @return true if the specified areaOfInterest is contained in the container, false otherwise.
   */
  protected boolean contains( @Nonnull final AreaOfInterest areaOfInterest )
  {
    getAreasOfInterestObservableValue().reportObserved();
    return _areasOfInterest.contains( areaOfInterest );
  }

  /**
   * Detach areaOfInterest from container without disposing areaOfInterest.
   * The areaOfInterest must be attached to the container.
   *
   * @param areaOfInterest the areaOfInterest to detach.
   */
  private void detach( @Nonnull final AreaOfInterest areaOfInterest )
  {
    // This method has been extracted to try and avoid GWT inlining into invoker
    if ( _areasOfInterest.remove( areaOfInterest ) )
    {
      getAreasOfInterestObservableValue().preReportChanged();
      doDetach( areaOfInterest, false );
      getAreasOfInterestObservableValue().reportChanged();
    }
    else
    {
      fail( () -> "Replicant-0095: Called detach() passing an areaOfInterest that was not attached to the container. " +
                  "AreaOfInterest: " + areaOfInterest );
    }
  }

  /**
   * Return a stream of all areasOfInterest in the container.
   *
   * @return the underlying areasOfInterest.
   */
  @Observable( expectSetter = false )
  @Nonnull
  public Stream<AreaOfInterest> areasOfInterest()
  {
    return _areasOfInterest.stream();
  }

  @ObservableValueRef
  @Nonnull
  abstract ObservableValue<Stream<AreaOfInterest>> getAreasOfInterestObservableValue();

  private void doAttach( @Nonnull final AreaOfInterest areaOfInterest )
  {
    DisposeNotifier
      .asDisposeNotifier( areaOfInterest )
      .addOnDisposeListener( this, () -> {
        getAreasOfInterestObservableValue().preReportChanged();
        detach( areaOfInterest );
        getAreasOfInterestObservableValue().reportChanged();
      } );
  }

  private void doDetach( @Nonnull final AreaOfInterest areaOfInterest, final boolean disposeOnDetach )
  {
    DisposeNotifier.asDisposeNotifier( areaOfInterest ).removeOnDisposeListener( this );
    if ( disposeOnDetach )
    {
      Disposable.dispose( areaOfInterest );
    }
    final ReplicantContext context = getReplicantContext();
    if ( RuntimeState.CONNECTED == context.getState() )
    {
      final ChannelAddress address = areaOfInterest.getAddress();
      final Subscription subscription = context.findSubscription( address );
      if ( null != subscription )
      {
        context.getRuntime().getConnector( address.getSystemId() ).requestSync();
      }
    }
  }
}
