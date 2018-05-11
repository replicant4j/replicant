package replicant;

import org.realityforge.guiceyloops.shared.ValueUtil;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class ChannelActionTest
  extends AbstractReplicantTest
{
  @Test
  public void construct()
  {
    final ChannelAction action = ChannelAction.create( 1, 2, ChannelAction.Action.ADD, null );

    assertEquals( action.getChannelId(), 1 );
    assertEquals( action.hasSubChannelId(), true );
    assertEquals( action.getSubChannelId(), 2 );
    assertEquals( action.getAction(), ChannelAction.Action.ADD );
    assertEquals( action.getChannelFilter(), null );
  }

  @SuppressWarnings( "ResultOfMethodCallIgnored" )
  @Test
  public void construct_withoutSubchannelId()
  {
    final Object filter = ValueUtil.randomString();
    final ChannelAction action = ChannelAction.create( 1, ChannelAction.Action.REMOVE, filter );

    assertEquals( action.getChannelId(), 1 );
    assertEquals( action.hasSubChannelId(), false );
    assertThrows( action::getSubChannelId );
    assertEquals( action.getAction(), ChannelAction.Action.REMOVE );
    assertEquals( action.getChannelFilter(), filter );
  }
}
