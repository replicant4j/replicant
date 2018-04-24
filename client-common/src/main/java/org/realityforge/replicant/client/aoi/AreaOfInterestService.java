package org.realityforge.replicant.client.aoi;

import arez.Disposable;
import arez.annotations.Action;
import arez.annotations.ArezComponent;
import arez.component.AbstractContainer;
import arez.component.RepositoryUtil;
import java.util.List;
import java.util.Objects;
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
@ArezComponent( allowEmpty = true )
public abstract class AreaOfInterestService
  extends AbstractContainer<ChannelAddress, AreaOfInterest>
{
  private final AreaOfInterestListenerSupport _listeners = new AreaOfInterestListenerSupport();

  @Nonnull
  public static AreaOfInterestService create()
  {
    return new Arez_AreaOfInterestService();
  }

  @SuppressWarnings( "UnusedReturnValue" )
  public boolean addAreaOfInterestListener( @Nonnull final AreaOfInterestListener listener )
  {
    return _listeners.addListener( Objects.requireNonNull( listener ) );
  }

  @SuppressWarnings( "UnusedReturnValue" )
  public boolean removeAreaOfInterestListener( @Nonnull final AreaOfInterestListener listener )
  {
    return _listeners.removeListener( Objects.requireNonNull( listener ) );
  }

  @Nonnull
  public List<AreaOfInterest> getAreasOfInterest()
  {
    return RepositoryUtil.asList( entities() );
  }

  @Nullable
  public AreaOfInterest findAreaOfInterest( @Nonnull final ChannelAddress address )
  {
    return super.findByArezId( address );
  }

  public void updateAreaOfInterest( @Nonnull final AreaOfInterest subscription, @Nullable final Object filter )
  {
    assert !Disposable.isDisposed( subscription );
    final Channel channel = subscription.getChannel();
    channel.setFilter( filter );
    _listeners.channelUpdated( channel );
  }

  @Nonnull
  public AreaOfInterest findOrCreateSubscription( @Nonnull final ChannelAddress address,
                                                  @Nullable final Object filter )
  {
    final AreaOfInterest subscription = findAreaOfInterest( address );
    if ( null != subscription )
    {
      if ( !FilterUtil.filtersEqual( subscription.getChannel().getFilter(), filter ) )
      {
        updateAreaOfInterest( subscription, filter );
      }
      return subscription;
    }
    else
    {
      final Channel channel = Channel.create( address, filter );
      final AreaOfInterest newSubscription = AreaOfInterest.create( channel );
      registerEntity( newSubscription );
      _listeners.channelCreated( channel );
      return newSubscription;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean contains( @Nonnull final AreaOfInterest entity )
  {
    return super.contains( entity );
  }

  /**
   * {@inheritDoc}
   */
  @Action
  @Override
  public void destroy( @Nonnull final AreaOfInterest entity )
  {
    super.destroy( entity );
    _listeners.channelDeleted( entity.getChannel() );
  }
}
