package org.realityforge.replicant.client.aoi;

import arez.Disposable;
import arez.annotations.Action;
import arez.annotations.ArezComponent;
import arez.component.AbstractContainer;
import arez.component.RepositoryUtil;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Singleton;
import org.realityforge.replicant.client.Channel;
import org.realityforge.replicant.client.ChannelAddress;
import org.realityforge.replicant.client.FilterUtil;

/**
 * The AreaOfInterestService is responsible for managing the expected subscriptions
 * that the client is interested in. The subscriptions may cross different replicant
 * systems, and may exist before the data sources have been connected. The AreaOfInterestService
 * intends to represent the desired state that the DataSources converge towards.
 */
@Singleton
@ArezComponent
public abstract class AreaOfInterestService
  extends AbstractContainer<ChannelAddress, AreaOfInterest>
{
  @Nonnull
  public static AreaOfInterestService create()
  {
    return new Arez_AreaOfInterestService();
  }

  @Nonnull
  public List<AreaOfInterest> getAreasOfInterest()
  {
    return RepositoryUtil.asList( entities() );
  }

  @Nullable
  public AreaOfInterest findAreaOfInterestByAddress( @Nonnull final ChannelAddress address )
  {
    return super.findByArezId( address );
  }

  public void updateAreaOfInterest( @Nonnull final AreaOfInterest areaOfInterest, @Nullable final Object filter )
  {
    assert !Disposable.isDisposed( areaOfInterest );
    areaOfInterest.getChannel().setFilter( filter );
  }

  @Nonnull
  public AreaOfInterest findOrCreateAreaOfInterest( @Nonnull final ChannelAddress address,
                                                    @Nullable final Object filter )
  {
    final AreaOfInterest areaOfInterest = findAreaOfInterestByAddress( address );
    if ( null != areaOfInterest )
    {
      if ( !FilterUtil.filtersEqual( areaOfInterest.getChannel().getFilter(), filter ) )
      {
        updateAreaOfInterest( areaOfInterest, filter );
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

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean contains( @Nonnull final AreaOfInterest areaOfInterest )
  {
    return super.contains( areaOfInterest );
  }

  /**
   * {@inheritDoc}
   */
  @Action
  @Override
  public void destroy( @Nonnull final AreaOfInterest areaOfInterest )
  {
    super.destroy( areaOfInterest );
  }
}
