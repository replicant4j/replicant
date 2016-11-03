package org.realityforge.replicant.client.ee;

import javax.json.JsonObject;
import org.realityforge.replicant.client.ChannelAction;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class ChannelActionDTOTest
{
  @Test
  public void simpleChange()
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
    assertEquals( action.getAction(), ChannelAction.Action.ADD );
    assertTrue( action.getChannelFilter() instanceof JsonObject );
  }

  private ChannelActionDTO toChannelAction( final String content )
  {
    return new ChannelActionDTO( JsonUtil.toJsonObject( content ) );
  }
}
