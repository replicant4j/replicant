package replicant;

import org.realityforge.guiceyloops.shared.ValueUtil;
import org.testng.annotations.Test;
import replicant.messages.ChannelChange;
import static org.testng.Assert.*;

public final class ChannelChangeDescriptorTest
  extends AbstractReplicantTest
{
  @Test
  void addTypeGraph()
  {
    final int schemaId = 0;
    final ChannelChangeDescriptor descriptor = ChannelChangeDescriptor.from( schemaId, "+23" );
    assertEquals( descriptor.getAddress().schemaId(), schemaId );
    assertEquals( descriptor.getAddress().getName(), "0.23" );
    assertEquals( descriptor.getType(), ChannelChangeDescriptor.Type.ADD );
    assertNull( descriptor.getFilter() );
  }

  @Test
  void addInstanceGraph()
  {
    final int schemaId = 0;
    final ChannelChangeDescriptor descriptor = ChannelChangeDescriptor.from( schemaId, "+23.2" );
    assertEquals( descriptor.getAddress().schemaId(), schemaId );
    assertEquals( descriptor.getAddress().getName(), "0.23.2" );
    assertEquals( descriptor.getType(), ChannelChangeDescriptor.Type.ADD );
    assertNull( descriptor.getFilter() );
  }

  @Test
  void removeTypeGraph()
  {
    final int schemaId = 0;
    final ChannelChangeDescriptor descriptor = ChannelChangeDescriptor.from( schemaId, "-23" );
    assertEquals( descriptor.getAddress().schemaId(), schemaId );
    assertEquals( descriptor.getAddress().getName(), "0.23" );
    assertEquals( descriptor.getType(), ChannelChangeDescriptor.Type.REMOVE );
    assertNull( descriptor.getFilter() );
  }

  @Test
  void removeInstanceGraph()
  {
    final int schemaId = 0;
    final ChannelChangeDescriptor descriptor = ChannelChangeDescriptor.from( schemaId, "-23.2" );
    assertEquals( descriptor.getAddress().schemaId(), schemaId );
    assertEquals( descriptor.getAddress().getName(), "0.23.2" );
    assertEquals( descriptor.getType(), ChannelChangeDescriptor.Type.REMOVE );
    assertNull( descriptor.getFilter() );
  }

  @Test
  void addFilteredTypeGraph()
  {
    final int schemaId = 0;
    final String filter = ValueUtil.randomString();
    final ChannelChangeDescriptor descriptor =
      ChannelChangeDescriptor.from( schemaId, ChannelChange.create( "+23", filter ) );
    assertEquals( descriptor.getAddress().schemaId(), schemaId );
    assertEquals( descriptor.getAddress().getName(), "0.23" );
    assertEquals( descriptor.getType(), ChannelChangeDescriptor.Type.ADD );
    assertEquals( descriptor.getFilter(), filter );
  }

  @Test
  void addFilteredInstanceGraph()
  {
    final int schemaId = 0;
    final String filter = ValueUtil.randomString();
    final ChannelChangeDescriptor descriptor =
      ChannelChangeDescriptor.from( schemaId, ChannelChange.create( "+23.2", filter ) );
    assertEquals( descriptor.getAddress().schemaId(), schemaId );
    assertEquals( descriptor.getAddress().getName(), "0.23.2" );
    assertEquals( descriptor.getType(), ChannelChangeDescriptor.Type.ADD );
    assertEquals( descriptor.getFilter(), filter );
  }

  @Test
  void removeFilteredTypeGraph()
  {
    final int schemaId = 0;
    final String filter = ValueUtil.randomString();
    final ChannelChangeDescriptor descriptor =
      ChannelChangeDescriptor.from( schemaId, ChannelChange.create( "-23", filter ) );
    assertEquals( descriptor.getAddress().schemaId(), schemaId );
    assertEquals( descriptor.getAddress().getName(), "0.23" );
    assertEquals( descriptor.getType(), ChannelChangeDescriptor.Type.REMOVE );
    assertEquals( descriptor.getFilter(), filter );
  }

  @Test
  void removeFilteredInstanceGraph()
  {
    final int schemaId = 0;
    final String filter = ValueUtil.randomString();
    final ChannelChangeDescriptor descriptor =
      ChannelChangeDescriptor.from( schemaId, ChannelChange.create( "-23.2", filter ) );
    assertEquals( descriptor.getAddress().schemaId(), schemaId );
    assertEquals( descriptor.getAddress().getName(), "0.23.2" );
    assertEquals( descriptor.getType(), ChannelChangeDescriptor.Type.REMOVE );
    assertEquals( descriptor.getFilter(), filter );
  }

  @Test
  void updateFilteredTypeGraph()
  {
    final int schemaId = 0;
    final String filter = ValueUtil.randomString();
    final ChannelChangeDescriptor descriptor =
      ChannelChangeDescriptor.from( schemaId, ChannelChange.create( "=23", filter ) );
    assertEquals( descriptor.getAddress().schemaId(), schemaId );
    assertEquals( descriptor.getAddress().getName(), "0.23" );
    assertEquals( descriptor.getType(), ChannelChangeDescriptor.Type.UPDATE );
    assertEquals( descriptor.getFilter(), filter );
  }

  @Test
  void updateFilteredInstanceGraph()
  {
    final int schemaId = 0;
    final String filter = ValueUtil.randomString();
    final ChannelChangeDescriptor descriptor =
      ChannelChangeDescriptor.from( schemaId, ChannelChange.create( "=23.2", filter ) );
    assertEquals( descriptor.getAddress().schemaId(), schemaId );
    assertEquals( descriptor.getAddress().getName(), "0.23.2" );
    assertEquals( descriptor.getType(), ChannelChangeDescriptor.Type.UPDATE );
    assertEquals( descriptor.getFilter(), filter );
  }

  @Test
  void deleteInstanceGraph()
  {
    final int schemaId = 0;
    final ChannelChangeDescriptor descriptor = ChannelChangeDescriptor.from( schemaId, "!23.2" );
    assertEquals( descriptor.getAddress().schemaId(), schemaId );
    assertEquals( descriptor.getAddress().getName(), "0.23.2" );
    assertEquals( descriptor.getType(), ChannelChangeDescriptor.Type.DELETE );
    assertNull( descriptor.getFilter() );
  }

  @Test
  void badAction()
  {
    final IllegalStateException exception =
      expectThrows( IllegalStateException.class, () -> ChannelChangeDescriptor.from( 0, "*1" ) );
    assertEquals( exception.getMessage(), "Failed to parse channel action '*1'" );
  }

  @Test
  void badAddress()
  {
    final IllegalStateException exception =
      expectThrows( IllegalStateException.class, () -> ChannelChangeDescriptor.from( 0, "+X" ) );
    assertEquals( exception.getMessage(), "Failed to parse channel action '+X'" );
  }
}
