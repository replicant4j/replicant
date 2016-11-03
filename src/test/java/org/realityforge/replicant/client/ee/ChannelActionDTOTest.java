package org.realityforge.replicant.client.ee;

import javax.json.JsonObject;
import org.realityforge.replicant.client.ChannelAction;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class ChannelActionDTOTest
{
  @Test
  public void simpleAction()
    throws Exception
  {
    final String content =
      "{\n" +
      "  \"cid\": 1,\n" +
      "  \"action\": \"add\",\n" +
      "  \"filter\": {}\n" +
      "}\n";
    final ChannelAction action = toChannelAction( content );
    assertEquals( action.getChannelID(), 1 );
    assertEquals( action.getSubChannelID(), null );
    assertEquals( action.getAction(), ChannelAction.Action.ADD );
    assertTrue( action.getChannelFilter() instanceof JsonObject );
  }

  @Test
  public void actionWithSubChannel()
    throws Exception
  {
    final String content =
      "{\n" +
      "  \"cid\": 1,\n" +
      "  \"scid\": 2,\n" +
      "  \"action\": \"remove\",\n" +
      "  \"filter\": null\n" +
      "}\n";
    final ChannelAction action = toChannelAction( content );
    assertEquals( action.getChannelID(), 1 );
    assertEquals( action.getSubChannelID(), 2 );
    assertEquals( action.getAction(), ChannelAction.Action.REMOVE );
    assertNull( action.getChannelFilter() );
  }


  @Test
  public void actionWithSubChannelAsString()
    throws Exception
  {
    final String content =
      "{\n" +
      "  \"cid\": 1,\n" +
      "  \"scid\": \"X\",\n" +
      "  \"action\": \"update\",\n" +
      "  \"filter\": 22\n" +
      "}\n";
    final ChannelAction action = toChannelAction( content );
    assertEquals( action.getChannelID(), 1 );
    assertEquals( action.getSubChannelID(), "X" );
    assertEquals( action.getAction(), ChannelAction.Action.UPDATE );
    assertEquals( action.getChannelFilter(), 22.0D );
  }

  private ChannelActionDTO toChannelAction( final String content )
  {
    return new ChannelActionDTO( JsonUtil.toJsonObject( content ) );
  }
}
