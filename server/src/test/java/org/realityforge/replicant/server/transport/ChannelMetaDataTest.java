package org.realityforge.replicant.server.transport;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class ChannelMetaDataTest
{
  @SuppressWarnings( "ResultOfMethodCallIgnored" )
  @Test
  public void typeGraph()
  {
    final ChannelMetaData metaData =
      new ChannelMetaData( 1,
                           "MetaData",
                           null,
                           ChannelMetaData.FilterType.NONE,
                           null,
                           ChannelMetaData.CacheType.NONE,
                           false,
                           false );
    assertEquals( metaData.getChannelId(), 1 );
    assertEquals( metaData.getName(), "MetaData" );
    assertTrue( metaData.isTypeGraph() );
    assertFalse( metaData.isInstanceGraph() );
    assertEquals( metaData.getFilterType(), ChannelMetaData.FilterType.NONE );
    assertFalse( metaData.isCacheable() );
    assertFalse( metaData.hasFilterParameter() );
    assertFalse( metaData.isExternal() );
    assertFalse( metaData.areBulkLoadsSupported() );

    assertThrows( metaData::getInstanceRootEntityTypeId );
    assertThrows( metaData::getFilterParameterType );
  }

  @Test
  public void instanceGraph()
  {
    final ChannelMetaData metaData =
      new ChannelMetaData( 1,
                           "MetaData",
                           23,
                           ChannelMetaData.FilterType.NONE,
                           null,
                           ChannelMetaData.CacheType.NONE,
                           false,
                           true );
    assertEquals( metaData.getChannelId(), 1 );
    assertEquals( metaData.getName(), "MetaData" );
    assertFalse( metaData.isTypeGraph() );
    assertTrue( metaData.isInstanceGraph() );
    assertEquals( metaData.getInstanceRootEntityTypeId(), (Integer) 23 );
    assertEquals( metaData.getFilterType(), ChannelMetaData.FilterType.NONE );
    assertFalse( metaData.isCacheable() );
    assertFalse( metaData.hasFilterParameter() );
    assertTrue( metaData.isExternal() );
  }

  @Test
  public void filteredGraph()
  {
    final ChannelMetaData metaData =
      new ChannelMetaData( 1,
                           "MetaData",
                           22,
                           ChannelMetaData.FilterType.STATIC,
                           String.class,
                           ChannelMetaData.CacheType.NONE,
                           false,
                           true );
    assertEquals( metaData.getChannelId(), 1 );
    assertEquals( metaData.getName(), "MetaData" );
    assertFalse( metaData.isTypeGraph() );
    assertTrue( metaData.isInstanceGraph() );
    assertEquals( metaData.getFilterType(), ChannelMetaData.FilterType.STATIC );
    assertEquals( metaData.getFilterParameterType(), String.class );
    assertFalse( metaData.isCacheable() );
    assertTrue( metaData.hasFilterParameter() );
    assertTrue( metaData.isExternal() );
  }

  @Test
  public void badFilteredConfig()
  {
    assertThrows( () -> new ChannelMetaData( 1,
                                             "X",
                                             null,
                                             ChannelMetaData.FilterType.STATIC,
                                             null,
                                             ChannelMetaData.CacheType.NONE,
                                             false,
                                             true ) );
    assertThrows( () -> new ChannelMetaData( 1,
                                             "X",
                                             null,
                                             ChannelMetaData.FilterType.DYNAMIC,
                                             null,
                                             ChannelMetaData.CacheType.NONE,
                                             false,
                                             true ) );
    assertThrows( () -> new ChannelMetaData( 1,
                                             "X",
                                             null,
                                             ChannelMetaData.FilterType.NONE,
                                             String.class,
                                             ChannelMetaData.CacheType.NONE,
                                             false,
                                             true ) );
  }
}
