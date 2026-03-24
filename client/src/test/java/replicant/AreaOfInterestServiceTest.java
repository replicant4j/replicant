package replicant;

import arez.Disposable;
import java.util.Collection;
import org.testng.annotations.Test;
import replicant.spy.AreaOfInterestCreatedEvent;
import replicant.spy.AreaOfInterestDisposedEvent;
import replicant.spy.AreaOfInterestFilterUpdatedEvent;
import static org.testng.Assert.*;

public class AreaOfInterestServiceTest
  extends AbstractReplicantTest
{
  @Test
  public void constructPassingContextWhenZonesDisabled()
  {
    final var exception =
      expectThrows( IllegalStateException.class, () -> AreaOfInterestService.create( Replicant.context() ) );

    assertEquals( exception.getMessage(),
                  "Replicant-0037: ReplicantService passed a context but Replicant.areZonesEnabled() is false" );
  }

  @Test
  public void basicSubscriptionManagement()
  {
    final var service = AreaOfInterestService.create( null );

    final var address1 = new ChannelAddress( 1, 0, null );
    final var address2 = new ChannelAddress( 1, 1, 1 );
    final var address3 = new ChannelAddress( 1, 1, 2 );

    safeAction( () -> {
      final var areaOfInterest1 = service.createOrUpdateAreaOfInterest( address1, null );
      assertNotNull( areaOfInterest1 );

      assertEquals( areaOfInterest1.getAddress(), address1 );
      assertTrue( Disposable.isNotDisposed( areaOfInterest1 ) );

      final var subscriptions = service.getAreasOfInterest();
      assertEquals( subscriptions.size(), 1 );
      assertTrue( subscriptions.stream().anyMatch( n -> n.getAddress().equals( address1 ) ) );
      assertFalse( subscriptions.stream().anyMatch( n -> n.getAddress().equals( address2 ) ) );
      assertFalse( subscriptions.stream().anyMatch( n -> n.getAddress().equals( address3 ) ) );

      final var newFilter = new Object();
      areaOfInterest1.setFilter( newFilter );

      assertEquals( areaOfInterest1.getFilter(), newFilter );

      Disposable.dispose( areaOfInterest1 );
      assertTrue( Disposable.isDisposed( areaOfInterest1 ) );

      assertEquals( service.getAreasOfInterest().size(), 0 );
    } );
  }

  @Test
  public void createAreaOfInterestGeneratesSpyEvent()
  {
    final var service = AreaOfInterestService.create( null );
    final var address1 = new ChannelAddress( 1, 0, null );

    safeAction( () -> {
      final var handler = registerTestSpyEventHandler();

      final var areaOfInterest = service.createOrUpdateAreaOfInterest( address1, null );
      assertNotNull( areaOfInterest );

      handler.assertEventCount( 1 );
      handler.assertNextEvent( AreaOfInterestCreatedEvent.class,
                               e -> assertEquals( e.getAreaOfInterest(), areaOfInterest ) );
    } );
  }

  @Test
  public void updateAreaOfInterestGeneratesSpyEvent()
  {
    final var service = AreaOfInterestService.create( null );
    final var address1 = new ChannelAddress( 1, 0, null );

    safeAction( () -> {
      service.createOrUpdateAreaOfInterest( address1, "Filter1" );

      final var handler = registerTestSpyEventHandler();

      final var areaOfInterest = service.createOrUpdateAreaOfInterest( address1, "Filter2" );

      handler.assertEventCount( 1 );

      handler.assertNextEvent( AreaOfInterestFilterUpdatedEvent.class,
                               e -> assertEquals( e.getAreaOfInterest(), areaOfInterest ) );
    } );
  }

  @Test
  public void disposeAreaOfInterestGeneratesSpyEvent()
  {
    final var service = AreaOfInterestService.create( null );
    final var address1 = new ChannelAddress( 1, 0, null );

    final var areaOfInterest =
      safeAction( () -> service.createOrUpdateAreaOfInterest( address1, "Filter1" ) );

    final var handler = registerTestSpyEventHandler();

    Disposable.dispose( areaOfInterest );
    handler.assertEventCount( 1 );

    handler.assertNextEvent( AreaOfInterestDisposedEvent.class,
                             e -> assertEquals( e.getAreaOfInterest(), areaOfInterest ) );
  }

  @Test
  public void createSubscription()
  {
    safeAction( () -> {
      final var service = AreaOfInterestService.create( null );

      final var address1 = new ChannelAddress( 1, 0 );
      final var address2 = new ChannelAddress( 1, 1 );

      final var filer1 = "Filer1";
      final var filer2 = (String) null;

      final var areaOfInterest1 = service.createOrUpdateAreaOfInterest( address1, filer1 );

      assertEquals( areaOfInterest1.getAddress(), address1 );
      assertEquals( areaOfInterest1.getFilter(), filer1 );

      final var areaOfInterest2 = service.createOrUpdateAreaOfInterest( address2, filer2 );

      assertEquals( areaOfInterest2.getAddress(), address2 );
      assertEquals( areaOfInterest2.getFilter(), filer2 );
    } );
  }

  @Test
  public void createOrUpdateAreaOfInterest()
  {
    safeAction( () -> {
      final var channel = new ChannelAddress( 1, 0 );
      final var filter1 = ValueUtil.randomString();
      final var filter2 = ValueUtil.randomString();

      final var service = AreaOfInterestService.create( null );

      // No existing subscription
      final var areaOfInterest1 = service.createOrUpdateAreaOfInterest( channel, filter1 );
      assertEquals( areaOfInterest1.getAddress(), channel );
      assertEquals( areaOfInterest1.getFilter(), filter1 );
      assertEquals( service.findAreaOfInterestByAddress( channel ), areaOfInterest1 );
      assertEquals( service.getAreasOfInterest().size(), 1 );

      //Existing subscription, same filter
      final var areaOfInterest2 = service.createOrUpdateAreaOfInterest( channel, filter1 );
      assertEquals( areaOfInterest2.getAddress(), channel );
      assertEquals( areaOfInterest2.getFilter(), filter1 );
      assertEquals( areaOfInterest1, areaOfInterest2 );
      assertEquals( service.findAreaOfInterestByAddress( channel ), areaOfInterest2 );
      assertEquals( service.getAreasOfInterest().size(), 1 );

      //Existing subscription, different filter
      final var subscription3 = service.createOrUpdateAreaOfInterest( channel, filter2 );
      assertEquals( subscription3.getAddress(), channel );
      assertEquals( subscription3.getFilter(), filter2 );
      assertEquals( areaOfInterest1, subscription3 );
      assertEquals( service.findAreaOfInterestByAddress( channel ), subscription3 );
      assertEquals( service.getAreasOfInterest().size(), 1 );
    } );
  }
}
