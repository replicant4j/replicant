package replicant;

import org.realityforge.guiceyloops.shared.ValueUtil;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class ChannelChangeTest
  extends AbstractReplicantTest
{
  @Test
  public void construct()
  {
    final ChannelChange action = ChannelChange.create( 1, 2, ChannelChange.Action.ADD, null );

    assertEquals( action.getChannelId(), 1 );
    assertEquals( action.hasSubChannelId(), true );
    assertEquals( action.getSubChannelId(), 2 );
    assertEquals( action.getAction(), ChannelChange.Action.ADD );
    assertEquals( action.getChannelFilter(), null );
  }

  @SuppressWarnings( "ResultOfMethodCallIgnored" )
  @Test
  public void construct_withoutSubchannelId()
  {
    final Object filter = ValueUtil.randomString();
    final ChannelChange action = ChannelChange.create( 1, ChannelChange.Action.REMOVE, filter );

    assertEquals( action.getChannelId(), 1 );
    assertEquals( action.hasSubChannelId(), false );
    assertThrows( action::getSubChannelId );
    assertEquals( action.getAction(), ChannelChange.Action.REMOVE );
    assertEquals( action.getChannelFilter(), filter );
  }
}
