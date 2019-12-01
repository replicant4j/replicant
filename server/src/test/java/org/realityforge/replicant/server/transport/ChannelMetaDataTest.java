package org.realityforge.replicant.server.transport;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class ChannelMetaDataTest
{
  @Test
  public void typeGraph()
  {
    final ChannelMetaData metaData =
      new ChannelMetaData( 1, "MetaData", null, ChannelMetaData.FilterType.NONE, null, false );
    assertEquals( metaData.getChannelId(), 1 );
    assertEquals( metaData.getName(), "MetaData" );
    assertEquals( metaData.isTypeGraph(), true );
    assertEquals( metaData.isInstanceGraph(), false );
    assertEquals( metaData.getFilterType(), ChannelMetaData.FilterType.NONE );
    assertEquals( metaData.isCacheable(), false );

    assertThrows( metaData::getSubChannelType );
    assertThrows( metaData::getFilterParameterType );
  }

  @Test
  public void instanceGraph()
  {
    final ChannelMetaData metaData =
      new ChannelMetaData( 1, "MetaData", Integer.class, ChannelMetaData.FilterType.NONE, null, false );
    assertEquals( metaData.getChannelId(), 1 );
    assertEquals( metaData.getName(), "MetaData" );
    assertEquals( metaData.isTypeGraph(), false );
    assertEquals( metaData.isInstanceGraph(), true );
    assertEquals( metaData.getFilterType(), ChannelMetaData.FilterType.NONE );
    assertEquals( metaData.isCacheable(), false );
    assertEquals( metaData.getSubChannelType(), Integer.class );
  }

  @Test
  public void filteredGraph()
  {
    final ChannelMetaData metaData =
      new ChannelMetaData( 1, "MetaData", Integer.class, ChannelMetaData.FilterType.STATIC, String.class, false );
    assertEquals( metaData.getChannelId(), 1 );
    assertEquals( metaData.getName(), "MetaData" );
    assertEquals( metaData.isTypeGraph(), false );
    assertEquals( metaData.isInstanceGraph(), true );
    assertEquals( metaData.getFilterType(), ChannelMetaData.FilterType.STATIC );
    assertEquals( metaData.getFilterParameterType(), String.class );
    assertEquals( metaData.isCacheable(), false );
    assertEquals( metaData.getSubChannelType(), Integer.class );
  }

  @Test
  public void badFilteredConfig()
  {
    assertThrows( () -> new ChannelMetaData( 1, "X", null, ChannelMetaData.FilterType.STATIC, null, false ) );
    assertThrows( () -> new ChannelMetaData( 1, "X", null, ChannelMetaData.FilterType.DYNAMIC, null, false ) );
    assertThrows( () -> new ChannelMetaData( 1, "X", null, ChannelMetaData.FilterType.NONE, String.class, false ) );
  }
}
