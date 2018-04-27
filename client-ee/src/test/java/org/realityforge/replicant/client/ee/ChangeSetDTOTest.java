package org.realityforge.replicant.client.ee;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class ChangeSetDTOTest
{
  @Test
  public void simpleChangeSet()
    throws Exception
  {
    final String content =
      "{\n" +
      "  \"last_id\": 1,\n" +
      "  \"request_id\": \"2\",\n" +
      "  \"etag\": null,\n" +
      "  \"channel_actions\": [\n" +
      "    {\n" +
      "      \"cid\": 33,\n" +
      "      \"action\": \"add\",\n" +
      "      \"filter\": {}\n" +
      "    }\n" +
      "  ],\n" +
      "  \"changes\": [\n" +
      "    {\n" +
      "      \"id\": 2,\n" +
      "      \"type\": 11,\n" +
      "      \"channels\": [\n" +
      "        {\n" +
      "          \"cid\": 1\n" +
      "        }\n" +
      "      ],\n" +
      "      \"data\": {\n" +
      "        \"SecurityGroup\": null,\n" +
      "        \"Target\": 0,\n" +
      "        \"ID\": 2,\n" +
      "        \"ActivityTypeCode\": null,\n" +
      "        \"CalendarTypeCode\": \"WORKING\",\n" +
      "        \"TargetLocation\": null\n" +
      "      }\n" +
      "    }\n" +
      "  ]\n" +
      "}\n";
    final ChangeSetDTO change = toChangeSet( content );
    assertEquals( change.getSequence(), 1 );
    assertEquals( change.getRequestID(), "2" );
    assertEquals( change.getETag(), null );
    assertEquals( change.getChannelActionCount(), 1 );
    assertEquals( change.getChannelAction( 0 ).getChannelId(), 33 );
    assertEquals( change.getChangeCount(), 1 );
    assertEquals( change.getChange( 0 ).getDesignatorAsInt(), 2 );
  }

  @Test
  public void changeSetWithETag()
    throws Exception
  {
    final String content =
      "{\n" +
      "  \"last_id\": 1,\n" +
      "  \"request_id\": \"2\",\n" +
      "  \"etag\": \"X\",\n" +
      "  \"channel_actions\": [],\n" +
      "  \"changes\": []\n" +
      "}\n";
    final ChangeSetDTO change = toChangeSet( content );
    assertEquals( change.getSequence(), 1 );
    assertEquals( change.getRequestID(), "2" );
    assertEquals( change.getETag(), "X" );
    assertEquals( change.getChannelActionCount(), 0 );
    assertEquals( change.getChangeCount(), 0 );
  }

  @Test
  public void changeSetWithNoRequestID()
    throws Exception
  {
    final String content =
      "{\n" +
      "  \"last_id\": 1,\n" +
      "  \"request_id\": null,\n" +
      "  \"etag\": null,\n" +
      "  \"channel_actions\": [],\n" +
      "  \"changes\": []\n" +
      "}\n";
    final ChangeSetDTO change = toChangeSet( content );
    assertEquals( change.getSequence(), 1 );
    assertEquals( change.getRequestID(), null );
    assertEquals( change.getETag(), null );
    assertEquals( change.getChannelActionCount(), 0 );
    assertEquals( change.getChangeCount(), 0 );
  }

  private ChangeSetDTO toChangeSet( final String content )
  {
    return new ChangeSetDTO( JsonTestUtil.toJsonObject( content ) );
  }
}
