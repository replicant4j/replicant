package replicant;

import arez.Arez;
import arez.Disposable;
import org.realityforge.guiceyloops.shared.ValueUtil;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class ReplicantContextTest
  extends AbstractReplicantTest
{
  @Test
  public void areasOfInterest()
  {
    final ReplicantContext context = Replicant.context();
    final ChannelAddress address = new ChannelAddress( G.G1 );
    final String filter = ValueUtil.randomString();
    final String filter2 = ValueUtil.randomString();

    Arez.context().safeAction( () -> {
      assertEquals( context.getAreasOfInterest().size(), 0 );
      assertEquals( context.findAreaOfInterestByAddress( address ), null );

      final AreaOfInterest areaOfInterest = context.findOrCreateAreaOfInterest( address, filter );

      assertEquals( areaOfInterest.getChannel().getFilter(), filter );
      assertEquals( context.getAreasOfInterest().size(), 1 );
      assertEquals( context.findAreaOfInterestByAddress( address ), areaOfInterest );

      final AreaOfInterest areaOfInterest2 = context.findOrCreateAreaOfInterest( address, filter2 );

      assertEquals( areaOfInterest2, areaOfInterest );
      assertEquals( areaOfInterest2.getChannel().getFilter(), filter2 );
      assertEquals( context.getAreasOfInterest().size(), 1 );
      assertEquals( context.findAreaOfInterestByAddress( address ), areaOfInterest2 );
      assertEquals( context.findAreaOfInterestByAddress( address ), areaOfInterest2 );
    } );
  }

  @Test
  public void entities()
  {
    final ReplicantContext context = Replicant.context();

    Arez.context().safeAction( () -> {
      assertEquals( context.findAllEntityTypes().size(), 0 );
      assertEquals( context.findAllEntitiesByType( A.class ).size(), 0 );
      assertEquals( context.findAllEntitiesByType( B.class ).size(), 0 );
      assertEquals( context.findEntityByTypeAndId( A.class, 1 ), null );
      assertEquals( context.findEntityByTypeAndId( A.class, 2 ), null );
      assertEquals( context.findEntityByTypeAndId( B.class, "A" ), null );

      final Entity entity1 = context.findOrCreateEntity( A.class, 1 );

      assertEquals( context.findAllEntityTypes().size(), 1 );
      assertEquals( context.findAllEntitiesByType( A.class ).size(), 1 );
      assertEquals( context.findAllEntitiesByType( B.class ).size(), 0 );
      assertEquals( context.findEntityByTypeAndId( A.class, 1 ), entity1 );
      assertEquals( context.findEntityByTypeAndId( A.class, 2 ), null );
      assertEquals( context.findEntityByTypeAndId( B.class, "A" ), null );

      final Entity entity2 = context.findOrCreateEntity( A.class, 2 );

      assertEquals( context.findAllEntityTypes().size(), 1 );
      assertEquals( context.findAllEntitiesByType( A.class ).size(), 2 );
      assertEquals( context.findAllEntitiesByType( B.class ).size(), 0 );
      assertEquals( context.findEntityByTypeAndId( A.class, 1 ), entity1 );
      assertEquals( context.findEntityByTypeAndId( A.class, 2 ), entity2 );
      assertEquals( context.findEntityByTypeAndId( B.class, "A" ), null );

      final Entity entity3 = context.findOrCreateEntity( B.class, "A" );

      assertEquals( context.findAllEntityTypes().size(), 2 );
      assertEquals( context.findAllEntitiesByType( A.class ).size(), 2 );
      assertEquals( context.findAllEntitiesByType( B.class ).size(), 1 );
      assertEquals( context.findEntityByTypeAndId( A.class, 1 ), entity1 );
      assertEquals( context.findEntityByTypeAndId( A.class, 2 ), entity2 );
      assertEquals( context.findEntityByTypeAndId( B.class, "A" ), entity3 );

      Disposable.dispose( entity1 );

      assertEquals( context.findAllEntityTypes().size(), 2 );
      assertEquals( context.findAllEntitiesByType( A.class ).size(), 1 );
      assertEquals( context.findAllEntitiesByType( B.class ).size(), 1 );
      assertEquals( context.findEntityByTypeAndId( A.class, 1 ), null );
      assertEquals( context.findEntityByTypeAndId( A.class, 2 ), entity2 );
      assertEquals( context.findEntityByTypeAndId( B.class, "A" ), entity3 );

      Disposable.dispose( entity2 );

      assertEquals( context.findAllEntityTypes().size(), 1 );
      assertEquals( context.findAllEntitiesByType( A.class ).size(), 0 );
      assertEquals( context.findAllEntitiesByType( B.class ).size(), 1 );
      assertEquals( context.findEntityByTypeAndId( A.class, 1 ), null );
      assertEquals( context.findEntityByTypeAndId( A.class, 2 ), null );
      assertEquals( context.findEntityByTypeAndId( B.class, "A" ), entity3 );

      Disposable.dispose( entity3 );

      assertEquals( context.findAllEntityTypes().size(), 0 );
      assertEquals( context.findAllEntitiesByType( A.class ).size(), 0 );
      assertEquals( context.findAllEntitiesByType( B.class ).size(), 0 );
      assertEquals( context.findEntityByTypeAndId( A.class, 1 ), null );
      assertEquals( context.findEntityByTypeAndId( A.class, 2 ), null );
      assertEquals( context.findEntityByTypeAndId( B.class, "A" ), null );
    } );
  }

  @Test
  public void subscriptions()
  {
    final ReplicantContext context = Replicant.context();
    final ChannelAddress address1 = new ChannelAddress( G.G1 );
    final ChannelAddress address2 = new ChannelAddress( G.G2, 1 );
    final ChannelAddress address3 = new ChannelAddress( G.G2, 2 );
    final String filter1 = null;
    final String filter2 = ValueUtil.randomString();
    final String filter3 = ValueUtil.randomString();
    final boolean explicitSubscription1 = true;
    final boolean explicitSubscription2 = true;
    final boolean explicitSubscription3 = false;

    Arez.context().safeAction( () -> {
      assertEquals( context.getTypeSubscriptions().size(), 0 );
      assertEquals( context.getInstanceSubscriptions().size(), 0 );
      assertEquals( context.getInstanceSubscriptionIds( G.G2 ).size(), 0 );
      assertEquals( context.findSubscription( address1 ), null );
      assertEquals( context.findSubscription( address2 ), null );
      assertEquals( context.findSubscription( address3 ), null );

      final Subscription subscription1 = context.createSubscription( address1, filter1, explicitSubscription1 );

      assertEquals( subscription1.getChannel().getAddress(), address1 );
      assertEquals( subscription1.getChannel().getFilter(), filter1 );
      assertEquals( subscription1.isExplicitSubscription(), explicitSubscription1 );

      assertEquals( context.getTypeSubscriptions().size(), 1 );
      assertEquals( context.getInstanceSubscriptions().size(), 0 );
      assertEquals( context.getInstanceSubscriptionIds( G.G2 ).size(), 0 );
      assertEquals( context.findSubscription( address1 ), subscription1 );
      assertEquals( context.findSubscription( address2 ), null );
      assertEquals( context.findSubscription( address3 ), null );

      final Subscription subscription2 = context.createSubscription( address2, filter2, explicitSubscription2 );

      assertEquals( subscription2.getChannel().getAddress(), address2 );
      assertEquals( subscription2.getChannel().getFilter(), filter2 );
      assertEquals( subscription2.isExplicitSubscription(), explicitSubscription2 );

      assertEquals( context.getTypeSubscriptions().size(), 1 );
      assertEquals( context.getInstanceSubscriptions().size(), 1 );
      assertEquals( context.getInstanceSubscriptionIds( G.G2 ).size(), 1 );
      assertEquals( context.findSubscription( address1 ), subscription1 );
      assertEquals( context.findSubscription( address2 ), subscription2 );
      assertEquals( context.findSubscription( address3 ), null );

      final Subscription subscription3 = context.createSubscription( address3, filter3, explicitSubscription3 );

      assertEquals( subscription3.getChannel().getAddress(), address3 );
      assertEquals( subscription3.getChannel().getFilter(), filter3 );
      assertEquals( subscription3.isExplicitSubscription(), explicitSubscription3 );

      assertEquals( context.getTypeSubscriptions().size(), 1 );
      assertEquals( context.getInstanceSubscriptions().size(), 2 );
      assertEquals( context.getInstanceSubscriptionIds( G.G2 ).size(), 2 );
      assertEquals( context.findSubscription( address1 ), subscription1 );
      assertEquals( context.findSubscription( address2 ), subscription2 );
      assertEquals( context.findSubscription( address3 ), subscription3 );

      Disposable.dispose( subscription2 );
      Disposable.dispose( subscription3 );

      assertEquals( context.getTypeSubscriptions().size(), 1 );
      assertEquals( context.getInstanceSubscriptions().size(), 0 );
      assertEquals( context.getInstanceSubscriptionIds( G.G2 ).size(), 0 );
      assertEquals( context.findSubscription( address1 ), subscription1 );
      assertEquals( context.findSubscription( address2 ), null );
      assertEquals( context.findSubscription( address3 ), null );
    } );
  }

  enum G
  {
    G1, G2
  }

  static class A
  {
  }

  static class B
  {
  }
}
