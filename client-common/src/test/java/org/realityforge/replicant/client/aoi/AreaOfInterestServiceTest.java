package org.realityforge.replicant.client.aoi;

import arez.Arez;
import arez.Disposable;
import java.util.Collection;
import org.realityforge.guiceyloops.shared.ValueUtil;
import org.realityforge.replicant.client.AbstractReplicantTest;
import org.realityforge.replicant.client.ChannelAddress;
import org.testng.annotations.Test;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

@SuppressWarnings( "Duplicates" )
public class AreaOfInterestServiceTest
  extends AbstractReplicantTest
{
  enum TestSystem
  {
    A, B
  }

  @Test
  public void basicSubscriptionManagement()
  {
    final AreaOfInterestService service = AreaOfInterestService.create();

    final AreaOfInterestListener listener = mock( AreaOfInterestListener.class );
    service.addAreaOfInterestListener( listener );

    final ChannelAddress descriptor1 = new ChannelAddress( TestSystem.A, null );
    final ChannelAddress descriptor2 = new ChannelAddress( TestSystem.B, 1 );
    final ChannelAddress descriptor3 = new ChannelAddress( TestSystem.B, 2 );

    Arez.context().safeAction( () -> {
      final AreaOfInterest subscription1 = service.findOrCreateSubscription( descriptor1, null );
      assertNotNull( subscription1 );

      assertEquals( subscription1.getAddress(), descriptor1 );
      assertEquals( !Disposable.isDisposed( subscription1 ), true );

      verify( listener ).channelCreated( subscription1.getChannel() );

      final Collection<AreaOfInterest> subscriptions = service.getAreasOfInterest();
      assertEquals( subscriptions.size(), 1 );
      assertEquals( subscriptions.stream().anyMatch( n -> n.getChannel().getAddress().equals( descriptor1 ) ), true );
      assertEquals( subscriptions.stream().anyMatch( n -> n.getChannel().getAddress().equals( descriptor2 ) ), false );
      assertEquals( subscriptions.stream().anyMatch( n -> n.getChannel().getAddress().equals( descriptor3 ) ), false );

      final Object newFilter = new Object();
      service.updateAreaOfInterest( subscription1, newFilter );

      assertEquals( subscription1.getChannel().getFilter(), newFilter );

      verify( listener ).channelUpdated( subscription1.getChannel() );

      Disposable.dispose( subscription1 );
      assertEquals( Disposable.isDisposed( subscription1 ), true );

      assertEquals( service.getAreasOfInterest().size(), 0 );
    } );

    verify( listener ).channelDeleted( any() );
  }

  @Test
  public void createSubscription()
  {
    Arez.context().safeAction( () -> {
      final AreaOfInterestService service = AreaOfInterestService.create();

      final AreaOfInterestListener listener = mock( AreaOfInterestListener.class );
      service.addAreaOfInterestListener( listener );

      final ChannelAddress descriptor1 = new ChannelAddress( TestSystem.A );
      final ChannelAddress descriptor2 = new ChannelAddress( TestSystem.B );

      final String filer1 = "Filer1";
      final String filer2 = null;

      final AreaOfInterest subscription1 = service.findOrCreateSubscription( descriptor1, filer1 );

      assertEquals( subscription1.getAddress(), descriptor1 );
      assertEquals( subscription1.getChannel().getFilter(), filer1 );

      verify( listener ).channelCreated( subscription1.getChannel() );

      final AreaOfInterest subscription2 = service.findOrCreateSubscription( descriptor2, filer2 );

      assertEquals( subscription2.getAddress(), descriptor2 );
      assertEquals( subscription2.getChannel().getFilter(), filer2 );

      verify( listener ).channelCreated( subscription2.getChannel() );
    } );
  }

  @Test
  public void findOrCreateSubscription()
  {
    Arez.context().safeAction( () -> {
      final ChannelAddress channel = new ChannelAddress( TestSystem.A );
      final String filter1 = ValueUtil.randomString();
      final String filter2 = ValueUtil.randomString();

      final AreaOfInterestService service = AreaOfInterestService.create();

      final AreaOfInterestListener listener = mock( AreaOfInterestListener.class );
      service.addAreaOfInterestListener( listener );

      // No existing subscription
      final AreaOfInterest subscription1 = service.findOrCreateSubscription( channel, filter1 );
      assertEquals( subscription1.getAddress(), channel );
      assertEquals( subscription1.getChannel().getFilter(), filter1 );
      assertEquals( service.findAreaOfInterest( channel ), subscription1 );
      assertEquals( service.getAreasOfInterest().size(), 1 );

      verify( listener ).channelCreated( subscription1.getChannel() );
      verify( listener, never() ).channelUpdated( subscription1.getChannel() );

      reset( listener );

      //Existing subscription, same filter
      final AreaOfInterest subscription2 = service.findOrCreateSubscription( channel, filter1 );
      assertEquals( subscription2.getAddress(), channel );
      assertEquals( subscription2.getChannel().getFilter(), filter1 );
      assertEquals( subscription1, subscription2 );
      assertEquals( service.findAreaOfInterest( channel ), subscription2 );
      assertEquals( service.getAreasOfInterest().size(), 1 );

      verify( listener, never() ).channelCreated( subscription1.getChannel() );
      verify( listener, never() ).channelUpdated( subscription1.getChannel() );

      reset( listener );

      //Existing subscription, different filter
      final AreaOfInterest subscription3 = service.findOrCreateSubscription( channel, filter2 );
      assertEquals( subscription3.getAddress(), channel );
      assertEquals( subscription3.getChannel().getFilter(), filter2 );
      assertEquals( subscription1, subscription3 );
      assertEquals( service.findAreaOfInterest( channel ), subscription3 );
      assertEquals( service.getAreasOfInterest().size(), 1 );

      verify( listener, never() ).channelCreated( subscription1.getChannel() );
      verify( listener ).channelUpdated( subscription1.getChannel() );
    } );
  }
}
