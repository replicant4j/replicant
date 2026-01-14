package replicant.server.transport;

import java.util.function.Function;
import javax.json.JsonObject;
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
                           false );
    assertEquals( metaData.getChannelId(), 1 );
    assertEquals( metaData.getName(), "MetaData" );
    assertTrue( metaData.isTypeGraph() );
    assertFalse( metaData.isInstanceGraph() );
    assertEquals( metaData.getFilterType(), ChannelMetaData.FilterType.NONE );
    assertFalse( metaData.isCacheable() );
    assertFalse( metaData.hasFilterParameter() );
    assertFalse( metaData.isExternal() );

    assertThrows( metaData::getInstanceRootEntityTypeId );
    assertThrows( metaData::getFilterParameterFactory );
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
    final Function<JsonObject, Object> filterParameterFactory = e -> null;
    final ChannelMetaData metaData =
      new ChannelMetaData( 1,
                           "MetaData",
                           22,
                           ChannelMetaData.FilterType.STATIC,
                           filterParameterFactory,
                           ChannelMetaData.CacheType.NONE,
                           true );
    assertEquals( metaData.getChannelId(), 1 );
    assertEquals( metaData.getName(), "MetaData" );
    assertFalse( metaData.isTypeGraph() );
    assertTrue( metaData.isInstanceGraph() );
    assertEquals( metaData.getFilterType(), ChannelMetaData.FilterType.STATIC );
    assertEquals( metaData.getFilterParameterFactory(), filterParameterFactory );
    assertFalse( metaData.isCacheable() );
    assertTrue( metaData.hasFilterParameter() );
    assertTrue( metaData.isExternal() );
  }

  @Test
  public void staticInstancedFilteredGraph()
  {
    final Function<JsonObject, Object> filterParameterFactory = e -> null;
    final ChannelMetaData metaData =
      new ChannelMetaData( 2,
                           "MetaData",
                           22,
                           ChannelMetaData.FilterType.STATIC_INSTANCED,
                           filterParameterFactory,
                           ChannelMetaData.CacheType.NONE,
                           true );
    assertEquals( metaData.getChannelId(), 2 );
    assertEquals( metaData.getName(), "MetaData" );
    assertFalse( metaData.isTypeGraph() );
    assertTrue( metaData.isInstanceGraph() );
    assertEquals( metaData.getFilterType(), ChannelMetaData.FilterType.STATIC_INSTANCED );
    assertEquals( metaData.getFilterParameterFactory(), filterParameterFactory );
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
                                             true ) );
    assertThrows( () -> new ChannelMetaData( 1,
                                             "X",
                                             null,
                                             ChannelMetaData.FilterType.STATIC_INSTANCED,
                                             null,
                                             ChannelMetaData.CacheType.NONE,
                                             true ) );
    assertThrows( () -> new ChannelMetaData( 1,
                                             "X",
                                             null,
                                             ChannelMetaData.FilterType.DYNAMIC,
                                             null,
                                             ChannelMetaData.CacheType.NONE,
                                             true ) );
    assertThrows( () -> new ChannelMetaData( 1,
                                             "X",
                                             null,
                                             ChannelMetaData.FilterType.NONE,
                                             j -> null,
                                             ChannelMetaData.CacheType.NONE,
                                             true ) );
  }
}
