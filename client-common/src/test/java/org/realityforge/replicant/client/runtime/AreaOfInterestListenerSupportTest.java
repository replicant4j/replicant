package org.realityforge.replicant.client.runtime;

import org.realityforge.guiceyloops.shared.ValueUtil;
import org.realityforge.replicant.client.ChannelAddress;
import org.testng.annotations.Test;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class AreaOfInterestListenerSupportTest
{
  enum TestGraph
  {
    A
  }

  @Test
  public void basicOperation()
  {
    final AreaOfInterestListenerSupport support = new AreaOfInterestListenerSupport();

    final AreaOfInterestService service = mock( AreaOfInterestService.class );
    final Subscription subscription =
      new Subscription( service, new ChannelAddress( TestGraph.A, ValueUtil.randomString() ) );

    final AreaOfInterestListener listener = mock( AreaOfInterestListener.class );

    assertTrue( support.addListener( listener ) );
    assertFalse( support.addListener( listener ), "Can not add duplicate" );

    assertTrue( support.getListeners().contains( listener ) );

    support.subscriptionCreated( subscription );
    verify( listener ).subscriptionCreated( subscription );

    support.subscriptionUpdated( subscription );
    verify( listener ).subscriptionUpdated( subscription );

    support.subscriptionDeleted( subscription );
    verify( listener ).subscriptionDeleted( subscription );

    assertTrue( support.removeListener( listener ) );
    assertFalse( support.removeListener( listener ), "Can not remove duplicate" );

    assertFalse( support.getListeners().contains( listener ) );

    reset( listener );
    support.subscriptionDeleted( subscription );
    verify( listener, never() ).subscriptionDeleted( subscription );
  }
}
