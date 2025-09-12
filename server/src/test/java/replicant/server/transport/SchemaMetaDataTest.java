package replicant.server.transport;

import org.realityforge.guiceyloops.shared.ValueUtil;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class SchemaMetaDataTest
{
  @Test
  public void basicOperation()
  {
    final ChannelMetaData ch0 =
      new ChannelMetaData( 0,
                           ValueUtil.randomString(),
                           2,
                           ChannelMetaData.FilterType.NONE,
                           null,
                           ChannelMetaData.CacheType.NONE,
                           false,
                           false );
    final ChannelMetaData ch1 =
      new ChannelMetaData( 1,
                           ValueUtil.randomString(),
                           null,
                           ChannelMetaData.FilterType.NONE,
                           null,
                           ChannelMetaData.CacheType.NONE,
                           false,
                           false );
    final ChannelMetaData ch2 =
      new ChannelMetaData( 2,
                           ValueUtil.randomString(),
                           54,
                           ChannelMetaData.FilterType.NONE,
                           null,
                           ChannelMetaData.CacheType.NONE,
                           true,
                           false );
    final String name = ValueUtil.randomString();

    final SchemaMetaData schemaMetaData = new SchemaMetaData( name, ch0, ch1, ch2 );

    assertEquals( schemaMetaData.getName(), name );
    assertEquals( schemaMetaData.getChannelMetaData( 0 ), ch0 );
    assertEquals( schemaMetaData.getChannelMetaData( 1 ), ch1 );
    assertEquals( schemaMetaData.getChannelMetaData( 2 ), ch2 );
    assertEquals( schemaMetaData.getChannelCount(), 3 );
    assertEquals( schemaMetaData.getInstanceChannelCount(), 2 );
    assertEquals( schemaMetaData.getInstanceChannelCount(), 2 );
    assertEquals( schemaMetaData.getInstanceChannelByIndex( 0 ), ch0 );
    assertEquals( schemaMetaData.getInstanceChannelByIndex( 1 ), ch2 );
  }
}
