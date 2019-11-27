package org.realityforge.replicant.client.runtime;

import org.realityforge.guiceyloops.shared.ValueUtil;
import org.realityforge.replicant.client.ChannelDescriptor;
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
    final Scope scope = new Scope( service, ValueUtil.randomString() );
    final Subscription subscription =
      new Subscription( service, new ChannelDescriptor( TestGraph.A, ValueUtil.randomString() ) );

    final AreaOfInterestListener listener = mock( AreaOfInterestListener.class );

    assertTrue( support.addListener( listener ) );
    assertFalse( support.addListener( listener ), "Can not add duplicate" );

    assertTrue( support.getListeners().contains( listener ) );

    support.scopeCreated( scope );
    verify( listener ).scopeCreated( scope );

    support.scopeDeleted( scope );
    verify( listener ).scopeDeleted( scope );

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
