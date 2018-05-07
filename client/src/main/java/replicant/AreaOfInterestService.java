package replicant;

import arez.annotations.ArezComponent;
import arez.component.AbstractContainer;
import arez.component.RepositoryUtil;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Singleton;
import org.realityforge.replicant.client.converger.Converger;
import replicant.spy.AreaOfInterestCreatedEvent;
import static org.realityforge.braincheck.Guards.*;

/**
 * The AreaOfInterestService is responsible for managing AreaOfInterest instance.
 * An {@link AreaOfInterest} represents a delcaration of a desire for a
 * {@link Subscription}. The intention
 * is that user code defines the desired state as instances of {@link AreaOfInterest}
 * and the {@link Converger} converges
 * the actual state towards the desired state.
 */
@Singleton
@ArezComponent
abstract class AreaOfInterestService
  extends AbstractContainer<ChannelAddress, AreaOfInterest>
{
  /**
   * Reference to the context to which this service belongs.
   */
  @Nullable
  private final ReplicantContext _context;

  AreaOfInterestService( @Nullable final ReplicantContext context )
  {
    if ( Replicant.shouldCheckInvariants() )
    {
      apiInvariant( () -> Replicant.areZonesEnabled() || null == context,
                    () -> "Replicant-0180: Node passed a context but Replicant.areZonesEnabled() is false" );
    }
    _context = Replicant.areZonesEnabled() ? Objects.requireNonNull( context ) : null;
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
    return RepositoryUtil.asList( entities() );
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
    return super.findByArezId( address );
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
  AreaOfInterest findOrCreateAreaOfInterest( @Nonnull final ChannelAddress address, @Nullable final Object filter )
  {
    final AreaOfInterest areaOfInterest = findAreaOfInterestByAddress( address );
    if ( null != areaOfInterest )
    {
      if ( !FilterUtil.filtersEqual( areaOfInterest.getChannel().getFilter(), filter ) )
      {
        areaOfInterest.getChannel().setFilter( filter );
      }
      return areaOfInterest;
    }
    else
    {
      final Channel channel = Channel.create( address, filter );
      final AreaOfInterest newAreaOfInterest = AreaOfInterest.create( channel );
      registerEntity( newAreaOfInterest );
      if ( Replicant.areSpiesEnabled() && getReplicantContext().getSpy().willPropagateSpyEvents() )
      {
        getReplicantContext().getSpy().reportSpyEvent( new AreaOfInterestCreatedEvent( newAreaOfInterest ) );
      }
      return newAreaOfInterest;
    }
  }

  @Nonnull
  private ReplicantContext getReplicantContext()
  {
    return Replicant.areZonesEnabled() ? Objects.requireNonNull( _context ) : Replicant.context();
  }
}
