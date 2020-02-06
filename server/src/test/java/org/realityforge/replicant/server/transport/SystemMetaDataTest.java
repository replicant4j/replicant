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
      new ChannelMetaData( 0, ValueUtil.randomString(), 2, ChannelMetaData.FilterType.NONE, null, false, false, false );
    final ChannelMetaData ch1 =
      new ChannelMetaData( 1,
                           ValueUtil.randomString(),
                           null,
                           ChannelMetaData.FilterType.NONE,
                           null,
                           false,
                           false,
                           false );
    final ChannelMetaData ch2 =
      new ChannelMetaData( 2, ValueUtil.randomString(), 54, ChannelMetaData.FilterType.NONE, null, false, true, false );
    final String name = ValueUtil.randomString();

    final SystemMetaData systemMetaData = new SystemMetaData( name, ch0, ch1, ch2 );

    assertEquals( systemMetaData.getName(), name );
    assertEquals( systemMetaData.getChannelMetaData( 0 ), ch0 );
    assertEquals( systemMetaData.getChannelMetaData( 1 ), ch1 );
    assertEquals( systemMetaData.getChannelMetaData( 2 ), ch2 );
    assertEquals( systemMetaData.getChannelCount(), 3 );
    assertEquals( systemMetaData.getInstanceChannelCount(), 2 );
    assertEquals( systemMetaData.getInstanceChannelCount(), 2 );
    assertEquals( systemMetaData.getInstanceChannelByIndex( 0 ), ch0 );
    assertEquals( systemMetaData.getInstanceChannelByIndex( 1 ), ch2 );
  }
}
