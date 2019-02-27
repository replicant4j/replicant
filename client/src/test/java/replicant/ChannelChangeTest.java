package replicant;

import org.realityforge.guiceyloops.shared.ValueUtil;
import org.testng.annotations.Test;
import replicant.messages.ChannelChange;
import static org.testng.Assert.*;

public class ChannelChangeTest
  extends AbstractReplicantTest
{
  @Test
  public void construct()
  {
    final ChannelChange action = ChannelChange.create( "+1.2", null );

    assertEquals( action.getChannel(), "+1.2" );
    assertNull( action.getFilter() );
  }

  @Test
  public void construct_withoutSubchannelId()
  {
    final Object filter = ValueUtil.randomString();
    final ChannelChange action = ChannelChange.create( "-1", filter );

    assertEquals( action.getChannel(), "-1" );
    assertEquals( action.getFilter(), filter );
  }
}
