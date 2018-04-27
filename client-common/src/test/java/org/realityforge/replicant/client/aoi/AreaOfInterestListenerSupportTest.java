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

    final Channel subscription =
      Channel.create( new ChannelAddress( TestSystem.A, ValueUtil.randomString() ), null );

    final AreaOfInterestListener listener = mock( AreaOfInterestListener.class );

    assertTrue( support.addListener( listener ) );
    assertFalse( support.addListener( listener ), "Can not add duplicate" );

    assertTrue( support.getListeners().contains( listener ) );

    support.channelCreated( subscription );
    verify( listener ).channelCreated( subscription );

    support.channelUpdated( subscription );
    verify( listener ).channelUpdated( subscription );

    support.channelDeleted( subscription );
    verify( listener ).channelDeleted( subscription );

    assertTrue( support.removeListener( listener ) );
    assertFalse( support.removeListener( listener ), "Can not remove duplicate" );

    assertFalse( support.getListeners().contains( listener ) );

    reset( listener );
    support.channelDeleted( subscription );
    verify( listener, never() ).channelDeleted( subscription );
  }
}
