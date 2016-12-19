package org.realityforge.replicant.server.transport;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class ChannelMetaDataTest
{
  @Test
  public void basicOperation()
  {
    final ChannelMetaData metaData = new ChannelMetaData( 1, "MetaData", true, ChannelMetaData.FilterType.NONE, false );
    assertEquals( metaData.getChannelID(), 1 );
    assertEquals( metaData.getName(), "MetaData" );
    assertEquals( metaData.isTypeGraph(), true );
    assertEquals( metaData.isInstanceGraph(), false );
    assertEquals( metaData.getFilterType(), ChannelMetaData.FilterType.NONE );
    assertEquals( metaData.isCacheable(), false );
  }
}
