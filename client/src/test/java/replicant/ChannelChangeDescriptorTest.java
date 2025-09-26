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
    assertEquals( descriptor.address().schemaId(), schemaId );
    assertEquals( descriptor.address().getName(), "0.23" );
    assertEquals( descriptor.type(), ChannelChangeDescriptor.Type.ADD );
    assertNull( descriptor.filter() );
  }

  @Test
  public void addInstanceGraph()
  {
    final int schemaId = 0;
    final ChannelChangeDescriptor descriptor = ChannelChangeDescriptor.from( schemaId, "+23.2" );
    assertEquals( descriptor.address().schemaId(), schemaId );
    assertEquals( descriptor.address().getName(), "0.23.2" );
    assertEquals( descriptor.type(), ChannelChangeDescriptor.Type.ADD );
    assertNull( descriptor.filter() );
  }

  @Test
  public void removeTypeGraph()
  {
    final int schemaId = 0;
    final ChannelChangeDescriptor descriptor = ChannelChangeDescriptor.from( schemaId, "-23" );
    assertEquals( descriptor.address().schemaId(), schemaId );
    assertEquals( descriptor.address().getName(), "0.23" );
    assertEquals( descriptor.type(), ChannelChangeDescriptor.Type.REMOVE );
    assertNull( descriptor.filter() );
  }

  @Test
  public void removeInstanceGraph()
  {
    final int schemaId = 0;
    final ChannelChangeDescriptor descriptor = ChannelChangeDescriptor.from( schemaId, "-23.2" );
    assertEquals( descriptor.address().schemaId(), schemaId );
    assertEquals( descriptor.address().getName(), "0.23.2" );
    assertEquals( descriptor.type(), ChannelChangeDescriptor.Type.REMOVE );
    assertNull( descriptor.filter() );
  }

  @Test
  public void addFilteredTypeGraph()
  {
    final int schemaId = 0;
    final String filter = ValueUtil.randomString();
    final ChannelChangeDescriptor descriptor =
      ChannelChangeDescriptor.from( schemaId, ChannelChange.create( "+23", filter ) );
    assertEquals( descriptor.address().schemaId(), schemaId );
    assertEquals( descriptor.address().getName(), "0.23" );
    assertEquals( descriptor.type(), ChannelChangeDescriptor.Type.ADD );
    assertEquals( descriptor.filter(), filter );
  }

  @Test
  public void addFilteredInstanceGraph()
  {
    final int schemaId = 0;
    final String filter = ValueUtil.randomString();
    final ChannelChangeDescriptor descriptor =
      ChannelChangeDescriptor.from( schemaId, ChannelChange.create( "+23.2", filter ) );
    assertEquals( descriptor.address().schemaId(), schemaId );
    assertEquals( descriptor.address().getName(), "0.23.2" );
    assertEquals( descriptor.type(), ChannelChangeDescriptor.Type.ADD );
    assertEquals( descriptor.filter(), filter );
  }

  @Test
  public void removeFilteredTypeGraph()
  {
    final int schemaId = 0;
    final String filter = ValueUtil.randomString();
    final ChannelChangeDescriptor descriptor =
      ChannelChangeDescriptor.from( schemaId, ChannelChange.create( "-23", filter ) );
    assertEquals( descriptor.address().schemaId(), schemaId );
    assertEquals( descriptor.address().getName(), "0.23" );
    assertEquals( descriptor.type(), ChannelChangeDescriptor.Type.REMOVE );
    assertEquals( descriptor.filter(), filter );
  }

  @Test
  public void removeFilteredInstanceGraph()
  {
    final int schemaId = 0;
    final String filter = ValueUtil.randomString();
    final ChannelChangeDescriptor descriptor =
      ChannelChangeDescriptor.from( schemaId, ChannelChange.create( "-23.2", filter ) );
    assertEquals( descriptor.address().schemaId(), schemaId );
    assertEquals( descriptor.address().getName(), "0.23.2" );
    assertEquals( descriptor.type(), ChannelChangeDescriptor.Type.REMOVE );
    assertEquals( descriptor.filter(), filter );
  }

  @Test
  public void updateFilteredTypeGraph()
  {
    final int schemaId = 0;
    final String filter = ValueUtil.randomString();
    final ChannelChangeDescriptor descriptor =
      ChannelChangeDescriptor.from( schemaId, ChannelChange.create( "=23", filter ) );
    assertEquals( descriptor.address().schemaId(), schemaId );
    assertEquals( descriptor.address().getName(), "0.23" );
    assertEquals( descriptor.type(), ChannelChangeDescriptor.Type.UPDATE );
    assertEquals( descriptor.filter(), filter );
  }

  @Test
  public void updateFilteredInstanceGraph()
  {
    final int schemaId = 0;
    final String filter = ValueUtil.randomString();
    final ChannelChangeDescriptor descriptor =
      ChannelChangeDescriptor.from( schemaId, ChannelChange.create( "=23.2", filter ) );
    assertEquals( descriptor.address().schemaId(), schemaId );
    assertEquals( descriptor.address().getName(), "0.23.2" );
    assertEquals( descriptor.type(), ChannelChangeDescriptor.Type.UPDATE );
    assertEquals( descriptor.filter(), filter );
  }

  @Test
  public void deleteInstanceGraph()
  {
    final int schemaId = 0;
    final ChannelChangeDescriptor descriptor = ChannelChangeDescriptor.from( schemaId, "!23.2" );
    assertEquals( descriptor.address().schemaId(), schemaId );
    assertEquals( descriptor.address().getName(), "0.23.2" );
    assertEquals( descriptor.type(), ChannelChangeDescriptor.Type.DELETE );
    assertNull( descriptor.filter() );
  }
}
