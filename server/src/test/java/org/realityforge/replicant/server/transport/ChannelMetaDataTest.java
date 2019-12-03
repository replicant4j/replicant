package org.realityforge.replicant.server.transport;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class ChannelMetaDataTest
{
  @Test
  public void typeGraph()
  {
    final ChannelMetaData metaData =
      new ChannelMetaData( 1, "MetaData", true, ChannelMetaData.FilterType.NONE, null, false, true );
    assertEquals( metaData.getChannelId(), 1 );
    assertEquals( metaData.getName(), "MetaData" );
    assertTrue( metaData.isTypeGraph() );
    assertFalse( metaData.isInstanceGraph() );
    assertEquals( metaData.getFilterType(), ChannelMetaData.FilterType.NONE );
    assertFalse( metaData.isCacheable() );

    assertThrows( metaData::getFilterParameterType );
  }

  @Test
  public void instanceGraph()
  {
    final ChannelMetaData metaData =
      new ChannelMetaData( 1, "MetaData", false, ChannelMetaData.FilterType.NONE, null, false, true );
    assertEquals( metaData.getChannelId(), 1 );
    assertEquals( metaData.getName(), "MetaData" );
    assertFalse( metaData.isTypeGraph() );
    assertTrue( metaData.isInstanceGraph() );
    assertEquals( metaData.getFilterType(), ChannelMetaData.FilterType.NONE );
    assertFalse( metaData.isCacheable() );
  }

  @Test
  public void filteredGraph()
  {
    final ChannelMetaData metaData =
      new ChannelMetaData( 1, "MetaData", false, ChannelMetaData.FilterType.STATIC, String.class, false, true );
    assertEquals( metaData.getChannelId(), 1 );
    assertEquals( metaData.getName(), "MetaData" );
    assertFalse( metaData.isTypeGraph() );
    assertTrue( metaData.isInstanceGraph() );
    assertEquals( metaData.getFilterType(), ChannelMetaData.FilterType.STATIC );
    assertEquals( metaData.getFilterParameterType(), String.class );
    assertFalse( metaData.isCacheable() );
  }

  @Test
  public void badFilteredConfig()
  {
    assertThrows( () -> new ChannelMetaData( 1, "X", true, ChannelMetaData.FilterType.STATIC, null, false, true ) );
    assertThrows( () -> new ChannelMetaData( 1, "X", true, ChannelMetaData.FilterType.DYNAMIC, null, false, true ) );
    assertThrows( () -> new ChannelMetaData( 1, "X", true, ChannelMetaData.FilterType.NONE, String.class, false, true ) );
  }
}
