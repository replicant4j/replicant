package replicant;

import arez.Arez;
import arez.Disposable;
import java.util.Collection;
import org.realityforge.guiceyloops.shared.ValueUtil;
import org.realityforge.replicant.client.AbstractReplicantTest;
import org.testng.annotations.Test;
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

    final ChannelAddress address1 = new ChannelAddress( TestSystem.A, null );
    final ChannelAddress address2 = new ChannelAddress( TestSystem.B, 1 );
    final ChannelAddress address3 = new ChannelAddress( TestSystem.B, 2 );

    Arez.context().safeAction( () -> {
      final AreaOfInterest areaOfInterest1 = service.findOrCreateAreaOfInterest( address1, null );
      assertNotNull( areaOfInterest1 );

      assertEquals( areaOfInterest1.getAddress(), address1 );
      assertEquals( !Disposable.isDisposed( areaOfInterest1 ), true );

      final Collection<AreaOfInterest> subscriptions = service.getAreasOfInterest();
      assertEquals( subscriptions.size(), 1 );
      assertEquals( subscriptions.stream().anyMatch( n -> n.getChannel().getAddress().equals( address1 ) ), true );
      assertEquals( subscriptions.stream().anyMatch( n -> n.getChannel().getAddress().equals( address2 ) ), false );
      assertEquals( subscriptions.stream().anyMatch( n -> n.getChannel().getAddress().equals( address3 ) ), false );

      final Object newFilter = new Object();
      areaOfInterest1.getChannel().setFilter( newFilter );

      assertEquals( areaOfInterest1.getChannel().getFilter(), newFilter );

      Disposable.dispose( areaOfInterest1 );
      assertEquals( Disposable.isDisposed( areaOfInterest1 ), true );

      assertEquals( service.getAreasOfInterest().size(), 0 );
    } );
  }

  @Test
  public void createSubscription()
  {
    Arez.context().safeAction( () -> {
      final AreaOfInterestService service = AreaOfInterestService.create();

      final ChannelAddress address1 = new ChannelAddress( TestSystem.A );
      final ChannelAddress address2 = new ChannelAddress( TestSystem.B );

      final String filer1 = "Filer1";
      final String filer2 = null;

      final AreaOfInterest areaOfInterest1 = service.findOrCreateAreaOfInterest( address1, filer1 );

      assertEquals( areaOfInterest1.getAddress(), address1 );
      assertEquals( areaOfInterest1.getChannel().getFilter(), filer1 );

      final AreaOfInterest areaOfInterest2 = service.findOrCreateAreaOfInterest( address2, filer2 );

      assertEquals( areaOfInterest2.getAddress(), address2 );
      assertEquals( areaOfInterest2.getChannel().getFilter(), filer2 );
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

      // No existing subscription
      final AreaOfInterest areaOfInterest1 = service.findOrCreateAreaOfInterest( channel, filter1 );
      assertEquals( areaOfInterest1.getAddress(), channel );
      assertEquals( areaOfInterest1.getChannel().getFilter(), filter1 );
      assertEquals( service.findAreaOfInterestByAddress( channel ), areaOfInterest1 );
      assertEquals( service.getAreasOfInterest().size(), 1 );

      //Existing subscription, same filter
      final AreaOfInterest areaOfInterest2 = service.findOrCreateAreaOfInterest( channel, filter1 );
      assertEquals( areaOfInterest2.getAddress(), channel );
      assertEquals( areaOfInterest2.getChannel().getFilter(), filter1 );
      assertEquals( areaOfInterest1, areaOfInterest2 );
      assertEquals( service.findAreaOfInterestByAddress( channel ), areaOfInterest2 );
      assertEquals( service.getAreasOfInterest().size(), 1 );

      //Existing subscription, different filter
      final AreaOfInterest subscription3 = service.findOrCreateAreaOfInterest( channel, filter2 );
      assertEquals( subscription3.getAddress(), channel );
      assertEquals( subscription3.getChannel().getFilter(), filter2 );
      assertEquals( areaOfInterest1, subscription3 );
      assertEquals( service.findAreaOfInterestByAddress( channel ), subscription3 );
      assertEquals( service.getAreasOfInterest().size(), 1 );
    } );
  }
}
