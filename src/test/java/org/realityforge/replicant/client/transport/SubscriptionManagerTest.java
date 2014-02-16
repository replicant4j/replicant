package org.realityforge.replicant.client.transport;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class SubscriptionManagerTest
{
  @Test
  public void typeGraph()
  {
    final TestSubscriptionManager sm = new TestSubscriptionManager();

    assertNull( sm.findTypeGraphSubscription( TestGraph.A ) );
    final SubscriptionEntry<TestGraph> e1 = sm.subscribeToTypeGraph( TestGraph.A );
    assertNotNull( e1 );
    assertEquals( sm.findTypeGraphSubscription( TestGraph.A ), e1 );

    assertEquals( e1.getGraph(), TestGraph.A );
    assertEquals( e1.getId(), null );
    assertEquals( e1.isDeregisterInProgress(), false );
    assertEquals( e1.isPresent(), false );
    assertEquals( e1.isRegistered(), true );

    assertNotNull( sm.getTypeSubscriptions().get( TestGraph.A ) );
    assertNull( sm.getTypeSubscriptions().get( TestGraph.B ) );

    // Already subscribed so don't subscribe
    final SubscriptionEntry<TestGraph> e2 = sm.subscribeToTypeGraph( TestGraph.A );
    assertNull( e2 );

    final SubscriptionEntry<TestGraph> e3 = sm.unsubscribeFromTypeGraph( TestGraph.A );
    assertNotNull( e3 );
    assertEquals( e3, e1 );
    assertEquals( e3.isDeregisterInProgress(), true );

    final SubscriptionEntry<TestGraph> e4 = sm.unsubscribeFromTypeGraph( TestGraph.A );
    assertNull( e4 );
  }

  @Test
  public void instanceGraph()
  {
    final TestSubscriptionManager sm = new TestSubscriptionManager();

    assertNull( sm.findInstanceGraphSubscription( TestGraph.A, 1 ) );
    final SubscriptionEntry<TestGraph> e1 = sm.subscribeToInstanceGraph( TestGraph.A, 1 );
    assertNotNull( e1 );
    assertEquals( sm.findInstanceGraphSubscription( TestGraph.A, 1 ), e1 );

    assertEquals( e1.getGraph(), TestGraph.A );
    assertEquals( e1.getId(), 1 );
    assertEquals( e1.isDeregisterInProgress(), false );
    assertEquals( e1.isPresent(), false );
    assertEquals( e1.isRegistered(), true );

    assertNotNull( sm.getInstanceSubscriptions().get( TestGraph.A ).get( 1 ) );
    assertNull( sm.getInstanceSubscriptions().get( TestGraph.A ).get( 2 ) );

    // Already subscribed so don't subscribe
    final SubscriptionEntry<TestGraph> e2 = sm.subscribeToInstanceGraph( TestGraph.A, 1 );
    assertNull( e2 );

    final SubscriptionEntry<TestGraph> e3 = sm.unsubscribeFromInstanceGraph( TestGraph.A, 1 );
    assertNotNull( e3 );
    assertEquals( e3, e1 );
    assertEquals( e3.isDeregisterInProgress(), true );

    final SubscriptionEntry<TestGraph> e4 = sm.unsubscribeFromInstanceGraph( TestGraph.A, 1 );
    assertNull( e4 );
  }

  static enum TestGraph
  {
    A, B
  }

  static class TestSubscriptionManager
    extends AbstractSubscriptionManager<TestGraph>
  {

  }
}
