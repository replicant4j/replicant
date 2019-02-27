package replicant;

import org.realityforge.guiceyloops.shared.ValueUtil;
import org.testng.annotations.Test;
import replicant.messages.ChannelChange;
import static org.testng.Assert.*;

public class ChannelChangeDescriptorTest
  extends AbstractReplicantTest
{
  @Test
  public void addTypeGraph()
  {
    final int schemaId = 0;
    final ChannelChangeDescriptor descriptor = ChannelChangeDescriptor.from( schemaId, "+23" );
    assertEquals( descriptor.getAddress().getSystemId(), schemaId );
    assertEquals( descriptor.getAddress().getName(), "0.23" );
    assertEquals( descriptor.getType(), ChannelChangeDescriptor.Type.ADD );
    assertNull( descriptor.getFilter() );
  }

  @Test
  public void addInstanceGraph()
  {
    final int schemaId = 0;
    final ChannelChangeDescriptor descriptor = ChannelChangeDescriptor.from( schemaId, "+23.2" );
    assertEquals( descriptor.getAddress().getSystemId(), schemaId );
    assertEquals( descriptor.getAddress().getName(), "0.23.2" );
    assertEquals( descriptor.getType(), ChannelChangeDescriptor.Type.ADD );
    assertNull( descriptor.getFilter() );
  }

  @Test
  public void removeTypeGraph()
  {
    final int schemaId = 0;
    final ChannelChangeDescriptor descriptor = ChannelChangeDescriptor.from( schemaId, "-23" );
    assertEquals( descriptor.getAddress().getSystemId(), schemaId );
    assertEquals( descriptor.getAddress().getName(), "0.23" );
    assertEquals( descriptor.getType(), ChannelChangeDescriptor.Type.REMOVE );
    assertNull( descriptor.getFilter() );
  }

  @Test
  public void removeInstanceGraph()
  {
    final int schemaId = 0;
    final ChannelChangeDescriptor descriptor = ChannelChangeDescriptor.from( schemaId, "-23.2" );
    assertEquals( descriptor.getAddress().getSystemId(), schemaId );
    assertEquals( descriptor.getAddress().getName(), "0.23.2" );
    assertEquals( descriptor.getType(), ChannelChangeDescriptor.Type.REMOVE );
    assertNull( descriptor.getFilter() );
  }

  @Test
  public void addFilteredTypeGraph()
  {
    final int schemaId = 0;
    final String filter = ValueUtil.randomString();
    final ChannelChangeDescriptor descriptor =
      ChannelChangeDescriptor.from( schemaId, ChannelChange.create( "+23", filter ) );
    assertEquals( descriptor.getAddress().getSystemId(), schemaId );
    assertEquals( descriptor.getAddress().getName(), "0.23" );
    assertEquals( descriptor.getType(), ChannelChangeDescriptor.Type.ADD );
    assertEquals( descriptor.getFilter(), filter );
  }

  @Test
  public void addFilteredInstanceGraph()
  {
    final int schemaId = 0;
    final String filter = ValueUtil.randomString();
    final ChannelChangeDescriptor descriptor =
      ChannelChangeDescriptor.from( schemaId, ChannelChange.create( "+23.2", filter ) );
    assertEquals( descriptor.getAddress().getSystemId(), schemaId );
    assertEquals( descriptor.getAddress().getName(), "0.23.2" );
    assertEquals( descriptor.getType(), ChannelChangeDescriptor.Type.ADD );
    assertEquals( descriptor.getFilter(), filter );
  }

  @Test
  public void removeFilteredTypeGraph()
  {
    final int schemaId = 0;
    final String filter = ValueUtil.randomString();
    final ChannelChangeDescriptor descriptor =
      ChannelChangeDescriptor.from( schemaId, ChannelChange.create( "-23", filter ) );
    assertEquals( descriptor.getAddress().getSystemId(), schemaId );
    assertEquals( descriptor.getAddress().getName(), "0.23" );
    assertEquals( descriptor.getType(), ChannelChangeDescriptor.Type.REMOVE );
    assertEquals( descriptor.getFilter(), filter );
  }

  @Test
  public void removeFilteredInstanceGraph()
  {
    final int schemaId = 0;
    final String filter = ValueUtil.randomString();
    final ChannelChangeDescriptor descriptor =
      ChannelChangeDescriptor.from( schemaId, ChannelChange.create( "-23.2", filter ) );
    assertEquals( descriptor.getAddress().getSystemId(), schemaId );
    assertEquals( descriptor.getAddress().getName(), "0.23.2" );
    assertEquals( descriptor.getType(), ChannelChangeDescriptor.Type.REMOVE );
    assertEquals( descriptor.getFilter(), filter );
  }

  @Test
  public void updateFilteredTypeGraph()
  {
    final int schemaId = 0;
    final String filter = ValueUtil.randomString();
    final ChannelChangeDescriptor descriptor =
      ChannelChangeDescriptor.from( schemaId, ChannelChange.create( "=23", filter ) );
    assertEquals( descriptor.getAddress().getSystemId(), schemaId );
    assertEquals( descriptor.getAddress().getName(), "0.23" );
    assertEquals( descriptor.getType(), ChannelChangeDescriptor.Type.UPDATE );
    assertEquals( descriptor.getFilter(), filter );
  }

  @Test
  public void updateFilteredInstanceGraph()
  {
    final int schemaId = 0;
    final String filter = ValueUtil.randomString();
    final ChannelChangeDescriptor descriptor =
      ChannelChangeDescriptor.from( schemaId, ChannelChange.create( "=23.2", filter ) );
    assertEquals( descriptor.getAddress().getSystemId(), schemaId );
    assertEquals( descriptor.getAddress().getName(), "0.23.2" );
    assertEquals( descriptor.getType(), ChannelChangeDescriptor.Type.UPDATE );
    assertEquals( descriptor.getFilter(), filter );
  }
}
