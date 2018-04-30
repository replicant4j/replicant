package org.realityforge.replicant.client.aoi;

import org.realityforge.guiceyloops.shared.ValueUtil;
import org.realityforge.replicant.client.Channel;
import org.realityforge.replicant.client.ChannelAddress;
import org.testng.annotations.Test;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class AreaOfInterestListenerSupportTest
{
  enum TestSystem
  {
    A
  }

  @Test
  public void basicOperation()
  {
    final AreaOfInterestListenerSupport support = new AreaOfInterestListenerSupport();

    final Channel channel = Channel.create( new ChannelAddress( TestSystem.A, ValueUtil.randomString() ), null );
    final AreaOfInterest areaOfInterest = AreaOfInterest.create( channel );

    final AreaOfInterestListener listener = mock( AreaOfInterestListener.class );

    assertTrue( support.addListener( listener ) );
    assertFalse( support.addListener( listener ), "Can not add duplicate" );

    assertTrue( support.getListeners().contains( listener ) );

    support.areaOfInterestCreated( areaOfInterest );
    verify( listener ).areaOfInterestCreated( areaOfInterest );

    support.areaOfInterestUpdated( areaOfInterest );
    verify( listener ).areaOfInterestUpdated( areaOfInterest );

    support.areaOfInterestDeleted( areaOfInterest );
    verify( listener ).areaOfInterestDeleted( areaOfInterest );

    assertTrue( support.removeListener( listener ) );
    assertFalse( support.removeListener( listener ), "Can not remove duplicate" );

    assertFalse( support.getListeners().contains( listener ) );

    reset( listener );
    support.areaOfInterestDeleted( areaOfInterest );
    verify( listener, never() ).areaOfInterestDeleted( areaOfInterest );
  }
}
