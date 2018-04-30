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
      final AreaOfInterest areaOfInterest1 = service.findOrCreateAreaOfInterest( descriptor1, null );
      assertNotNull( areaOfInterest1 );

      assertEquals( areaOfInterest1.getAddress(), descriptor1 );
      assertEquals( !Disposable.isDisposed( areaOfInterest1 ), true );

      verify( listener ).areaOfInterestCreated( areaOfInterest1 );

      final Collection<AreaOfInterest> subscriptions = service.getAreasOfInterest();
      assertEquals( subscriptions.size(), 1 );
      assertEquals( subscriptions.stream().anyMatch( n -> n.getChannel().getAddress().equals( descriptor1 ) ), true );
      assertEquals( subscriptions.stream().anyMatch( n -> n.getChannel().getAddress().equals( descriptor2 ) ), false );
      assertEquals( subscriptions.stream().anyMatch( n -> n.getChannel().getAddress().equals( descriptor3 ) ), false );

      final Object newFilter = new Object();
      service.updateAreaOfInterest( areaOfInterest1, newFilter );

      assertEquals( areaOfInterest1.getChannel().getFilter(), newFilter );

      verify( listener ).areaOfInterestUpdated( areaOfInterest1 );

      Disposable.dispose( areaOfInterest1 );
      assertEquals( Disposable.isDisposed( areaOfInterest1 ), true );

      assertEquals( service.getAreasOfInterest().size(), 0 );
    } );

    verify( listener ).areaOfInterestDeleted( any() );
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

      final AreaOfInterest areaOfInterest1 = service.findOrCreateAreaOfInterest( descriptor1, filer1 );

      assertEquals( areaOfInterest1.getAddress(), descriptor1 );
      assertEquals( areaOfInterest1.getChannel().getFilter(), filer1 );

      verify( listener ).areaOfInterestCreated( areaOfInterest1 );

      final AreaOfInterest areaOfInterest2 = service.findOrCreateAreaOfInterest( descriptor2, filer2 );

      assertEquals( areaOfInterest2.getAddress(), descriptor2 );
      assertEquals( areaOfInterest2.getChannel().getFilter(), filer2 );

      verify( listener ).areaOfInterestCreated( areaOfInterest2 );
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
      final AreaOfInterest areaOfInterest1 = service.findOrCreateAreaOfInterest( channel, filter1 );
      assertEquals( areaOfInterest1.getAddress(), channel );
      assertEquals( areaOfInterest1.getChannel().getFilter(), filter1 );
      assertEquals( service.findAreaOfInterestByAddress( channel ), areaOfInterest1 );
      assertEquals( service.getAreasOfInterest().size(), 1 );

      verify( listener ).areaOfInterestCreated( areaOfInterest1 );
      verify( listener, never() ).areaOfInterestUpdated( areaOfInterest1 );

      reset( listener );

      //Existing subscription, same filter
      final AreaOfInterest areaOfInterest2 = service.findOrCreateAreaOfInterest( channel, filter1 );
      assertEquals( areaOfInterest2.getAddress(), channel );
      assertEquals( areaOfInterest2.getChannel().getFilter(), filter1 );
      assertEquals( areaOfInterest1, areaOfInterest2 );
      assertEquals( service.findAreaOfInterestByAddress( channel ), areaOfInterest2 );
      assertEquals( service.getAreasOfInterest().size(), 1 );

      verify( listener, never() ).areaOfInterestCreated( areaOfInterest1 );
      verify( listener, never() ).areaOfInterestUpdated( areaOfInterest1 );

      reset( listener );

      //Existing subscription, different filter
      final AreaOfInterest subscription3 = service.findOrCreateAreaOfInterest( channel, filter2 );
      assertEquals( subscription3.getAddress(), channel );
      assertEquals( subscription3.getChannel().getFilter(), filter2 );
      assertEquals( areaOfInterest1, subscription3 );
      assertEquals( service.findAreaOfInterestByAddress( channel ), subscription3 );
      assertEquals( service.getAreasOfInterest().size(), 1 );

      verify( listener, never() ).areaOfInterestCreated( areaOfInterest1 );
      verify( listener ).areaOfInterestUpdated( areaOfInterest1 );
    } );
  }
}
