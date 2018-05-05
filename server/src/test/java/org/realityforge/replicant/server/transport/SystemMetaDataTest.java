package org.realityforge.replicant.server.transport;

import org.realityforge.guiceyloops.shared.ValueUtil;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class SystemMetaDataTest
{
  @Test
  public void basicOperation()
  {
    final ChannelMetaData ch0 =
      new ChannelMetaData( 0, ValueUtil.randomString(), true, ChannelMetaData.FilterType.NONE, null, false, false );
    final ChannelMetaData ch1 =
      new ChannelMetaData( 1, ValueUtil.randomString(), true, ChannelMetaData.FilterType.NONE, null, false, false );
    final ChannelMetaData ch2 =
      new ChannelMetaData( 2, ValueUtil.randomString(), true, ChannelMetaData.FilterType.NONE, null, false, false );
    final String name = ValueUtil.randomString();

    final SystemMetaData systemMetaData = new SystemMetaData( name, ch0, ch1, ch2 );

    assertEquals( systemMetaData.getName(), name );
    assertEquals( systemMetaData.getChannelMetaData( 0 ), ch0 );
    assertEquals( systemMetaData.getChannelMetaData( 1 ), ch1 );
    assertEquals( systemMetaData.getChannelMetaData( 2 ), ch2 );
    assertEquals( systemMetaData.getChannels().size(), 3 );
    assertEquals( systemMetaData.getChannels().get( 0 ), ch0 );
    assertEquals( systemMetaData.getChannels().get( 1 ), ch1 );
    assertEquals( systemMetaData.getChannels().get( 2 ), ch2 );

    assertThrows( () -> systemMetaData.getChannels().add( ch0 ) );
  }
}
