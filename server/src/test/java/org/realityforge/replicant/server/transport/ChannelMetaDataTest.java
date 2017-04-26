package org.realityforge.replicant.server.transport;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class ChannelMetaDataTest
{
  @Test
  public void typeGraph()
  {
    final ChannelMetaData metaData = new ChannelMetaData( 1, "MetaData", null, ChannelMetaData.FilterType.NONE, false );
    assertEquals( metaData.getChannelID(), 1 );
    assertEquals( metaData.getName(), "MetaData" );
    assertEquals( metaData.isTypeGraph(), true );
    assertEquals( metaData.isInstanceGraph(), false );
    assertEquals( metaData.getFilterType(), ChannelMetaData.FilterType.NONE );
    assertEquals( metaData.isCacheable(), false );

    assertThrows( metaData::getSubChannelType );
  }

  @Test
  public void instanceGraph()
  {
    final ChannelMetaData metaData =
      new ChannelMetaData( 1, "MetaData", Integer.class, ChannelMetaData.FilterType.NONE, false );
    assertEquals( metaData.getChannelID(), 1 );
    assertEquals( metaData.getName(), "MetaData" );
    assertEquals( metaData.isTypeGraph(), false );
    assertEquals( metaData.isInstanceGraph(), true );
    assertEquals( metaData.getFilterType(), ChannelMetaData.FilterType.NONE );
    assertEquals( metaData.isCacheable(), false );
    assertEquals( metaData.getSubChannelType(), Integer.class );
  }
}
