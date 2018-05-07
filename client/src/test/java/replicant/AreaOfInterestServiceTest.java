package replicant;

import arez.Arez;
import arez.Disposable;
import java.util.Collection;
import org.realityforge.guiceyloops.shared.ValueUtil;
import org.testng.annotations.Test;
import replicant.spy.AreaOfInterestCreatedEvent;
import replicant.spy.AreaOfInterestDisposedEvent;
import replicant.spy.AreaOfInterestUpdatedEvent;
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
  public void constructPassingContextWhenZonesDisabled()
  {
    final IllegalStateException exception =
      expectThrows( IllegalStateException.class, () -> AreaOfInterestService.create( Replicant.context() ) );

    assertEquals( exception.getMessage(), "Replicant-0180: AreaOfInterestService passed a context but Replicant.areZonesEnabled() is false" );
  }

  @Test
  public void basicSubscriptionManagement()
  {
    final AreaOfInterestService service = AreaOfInterestService.create( null );

    final ChannelAddress address1 = new ChannelAddress( TestSystem.A, null );
    final ChannelAddress address2 = new ChannelAddress( TestSystem.B, 1 );
    final ChannelAddress address3 = new ChannelAddress( TestSystem.B, 2 );

    Arez.context().safeAction( () -> {
      final AreaOfInterest areaOfInterest1 = service.createOrUpdateAreaOfInterest( address1, null );
      assertNotNull( areaOfInterest1 );

      assertEquals( areaOfInterest1.getAreaOfInterestService(), service );
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
  public void createAreaOfInterestGeneratesSpyEvent()
  {
    final AreaOfInterestService service = AreaOfInterestService.create( null );
    final ChannelAddress address1 = new ChannelAddress( TestSystem.A, null );

    Arez.context().safeAction( () -> {
      final TestSpyEventHandler handler = new TestSpyEventHandler();
      Replicant.context().getSpy().addSpyEventHandler( handler );

      final AreaOfInterest areaOfInterest = service.createOrUpdateAreaOfInterest( address1, null );
      assertNotNull( areaOfInterest );

      assertEquals( areaOfInterest.getAreaOfInterestService(), service );

      handler.assertEventCount( 1 );

      final AreaOfInterestCreatedEvent event = handler.assertNextEvent( AreaOfInterestCreatedEvent.class );
      assertEquals( event.getAreaOfInterest(), areaOfInterest );
    } );
  }

  @Test
  public void updateAreaOfInterestGeneratesSpyEvent()
  {
    final AreaOfInterestService service = AreaOfInterestService.create( null );
    final ChannelAddress address1 = new ChannelAddress( TestSystem.A, null );

    Arez.context().safeAction( () -> {
      final TestSpyEventHandler handler = new TestSpyEventHandler();

      service.createOrUpdateAreaOfInterest( address1, "Filter1" );

      Replicant.context().getSpy().addSpyEventHandler( handler );

      final AreaOfInterest areaOfInterest = service.createOrUpdateAreaOfInterest( address1, "Filter2" );

      handler.assertEventCount( 1 );

      final AreaOfInterestUpdatedEvent event = handler.assertNextEvent( AreaOfInterestUpdatedEvent.class );
      assertEquals( event.getAreaOfInterest(), areaOfInterest );
    } );
  }

  @Test
  public void disposeAreaOfInterestGeneratesSpyEvent()
  {
    final AreaOfInterestService service = AreaOfInterestService.create( null );
    final ChannelAddress address1 = new ChannelAddress( TestSystem.A, null );

    final TestSpyEventHandler handler = new TestSpyEventHandler();

    final AreaOfInterest areaOfInterest =
      Arez.context().safeAction( () -> service.createOrUpdateAreaOfInterest( address1, "Filter1" ) );

    Replicant.context().getSpy().addSpyEventHandler( handler );

    Arez.context().safeAction( () -> Disposable.dispose( areaOfInterest ) );
    handler.assertEventCount( 1 );

    final AreaOfInterestDisposedEvent event = handler.assertNextEvent( AreaOfInterestDisposedEvent.class );
    assertEquals( event.getAreaOfInterest(), areaOfInterest );
  }

  @Test
  public void createSubscription()
  {
    Arez.context().safeAction( () -> {
      final AreaOfInterestService service = AreaOfInterestService.create( null );

      final ChannelAddress address1 = new ChannelAddress( TestSystem.A );
      final ChannelAddress address2 = new ChannelAddress( TestSystem.B );

      final String filer1 = "Filer1";
      final String filer2 = null;

      final AreaOfInterest areaOfInterest1 = service.createOrUpdateAreaOfInterest( address1, filer1 );

      assertEquals( areaOfInterest1.getAddress(), address1 );
      assertEquals( areaOfInterest1.getChannel().getFilter(), filer1 );

      final AreaOfInterest areaOfInterest2 = service.createOrUpdateAreaOfInterest( address2, filer2 );

      assertEquals( areaOfInterest2.getAddress(), address2 );
      assertEquals( areaOfInterest2.getChannel().getFilter(), filer2 );
    } );
  }

  @Test
  public void createOrUpdateAreaOfInterest()
  {
    Arez.context().safeAction( () -> {
      final ChannelAddress channel = new ChannelAddress( TestSystem.A );
      final String filter1 = ValueUtil.randomString();
      final String filter2 = ValueUtil.randomString();

      final AreaOfInterestService service = AreaOfInterestService.create( null );

      // No existing subscription
      final AreaOfInterest areaOfInterest1 = service.createOrUpdateAreaOfInterest( channel, filter1 );
      assertEquals( areaOfInterest1.getAddress(), channel );
      assertEquals( areaOfInterest1.getChannel().getFilter(), filter1 );
      assertEquals( service.findAreaOfInterestByAddress( channel ), areaOfInterest1 );
      assertEquals( service.getAreasOfInterest().size(), 1 );

      //Existing subscription, same filter
      final AreaOfInterest areaOfInterest2 = service.createOrUpdateAreaOfInterest( channel, filter1 );
      assertEquals( areaOfInterest2.getAddress(), channel );
      assertEquals( areaOfInterest2.getChannel().getFilter(), filter1 );
      assertEquals( areaOfInterest1, areaOfInterest2 );
      assertEquals( service.findAreaOfInterestByAddress( channel ), areaOfInterest2 );
      assertEquals( service.getAreasOfInterest().size(), 1 );

      //Existing subscription, different filter
      final AreaOfInterest subscription3 = service.createOrUpdateAreaOfInterest( channel, filter2 );
      assertEquals( subscription3.getAddress(), channel );
      assertEquals( subscription3.getChannel().getFilter(), filter2 );
      assertEquals( areaOfInterest1, subscription3 );
      assertEquals( service.findAreaOfInterestByAddress( channel ), subscription3 );
      assertEquals( service.getAreasOfInterest().size(), 1 );
    } );
  }
}
