package org.realityforge.replicant.client;

import arez.ArezTestUtil;
import arez.Disposable;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class ChannelSubscriptionTest
{
  enum TestGraph
  {
    A
  }

  @Test
  public void basicSubscriptionOperation()
  {
    final ChannelAddress address = new ChannelAddress( TestGraph.A, null );

    final ChannelSubscription subscription = ChannelSubscription.create( address, null );

    assertEquals( subscription.getAddress(), address );
    assertEquals( Disposable.isDisposed( subscription ), false );
    assertEquals( subscription.getFilter(), null );

    // Update subscription works ....

    final Object filter = new Object();
    subscription.setFilter( filter );
    assertEquals( subscription.getFilter(), filter );

    Disposable.dispose( subscription );

    assertEquals( Disposable.isDisposed( subscription ), true );
  }

  @Test
  public void test_toString()
  {
    final ChannelAddress channel = new ChannelAddress( TestGraph.A, null );

    final ChannelSubscription subscription = ChannelSubscription.create( channel, null );

    assertEquals( subscription.toString(), "ChannelSubscription[" + channel + " :: Filter=null]" );

    ArezTestUtil.disableNames();

    assertEquals( subscription.toString(), "ChannelSubscription@1" );
  }
}
