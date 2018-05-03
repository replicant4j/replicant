package replicant;

import arez.annotations.ArezComponent;
import arez.component.AbstractContainer;
import arez.component.RepositoryUtil;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Singleton;

/**
 * The AreaOfInterestService is responsible for managing AreaOfInterest instance.
 * An {@link AreaOfInterest} represents a delcaration of a desire for a
 * {@link Subscription}. The intention
 * is that user code defines the desired state as instances of {@link AreaOfInterest}
 * and the {@link org.realityforge.replicant.client.converger.ContextConverger} converges
 * the actual state towards the desired state.
 */
@Singleton
@ArezComponent
public abstract class AreaOfInterestService
  extends AbstractContainer<ChannelAddress, AreaOfInterest>
{
  /**
   * Create an instance of the AreaOfInterestService.
   *
   * @return an instance of the AreaOfInterestService.
   */
  @Nonnull
  public static AreaOfInterestService create()
  {
    return new Arez_AreaOfInterestService();
  }

  /**
   * Return the collection of AreaOfInterest that have been declared.
   *
   * @return the collection of AreaOfInterest that have been declared.
   */
  @Nonnull
  public List<AreaOfInterest> getAreasOfInterest()
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
  public AreaOfInterest findAreaOfInterestByAddress( @Nonnull final ChannelAddress address )
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
  public AreaOfInterest findOrCreateAreaOfInterest( @Nonnull final ChannelAddress address,
                                                    @Nullable final Object filter )
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
      return newAreaOfInterest;
    }
  }
}
