package org.realityforge.replicant.client.ee;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class ChangeDTOTest
{
  @Test
  public void simpleChange()
    throws Exception
  {
    final String content =
      "{\n" +
      "  \"id\": 2,\n" +
      "  \"type\": 11,\n" +
      "  \"channels\": [\n" +
      "    {\n" +
      "      \"cid\": 1\n" +
      "    },\n" +
      "    {\n" +
      "      \"cid\": 2,\n" +
      "      \"scid\": 53\n" +
      "    },\n" +
      "    {\n" +
      "      \"cid\": 3,\n" +
      "      \"scid\": \"X\"\n" +
      "    }\n" +
      "  ],\n" +
      "  \"data\": {\n" +
      "    \"ID\": 1,\n" +
      "    \"CalendarTypeCode\": \"PERSONAL\",\n" +
      "    \"TargetLocation\": null\n" +
      "  }\n" +
      "}\n";
    final ChangeDTO change = toChange( content );
    assertEquals( change.getDesignatorAsInt(), 2 );
    assertEquals( change.getTypeID(), 11 );
    assertEquals( change.getChannelCount(), 3 );
    assertEquals( change.getChannelID( 0 ), 1 );
    assertEquals( change.getSubChannelID( 0 ), null );
    assertEquals( change.getChannelID( 1 ), 2 );
    assertEquals( change.getSubChannelID( 1 ), 53 );
    assertEquals( change.getChannelID( 2 ), 3 );
    assertEquals( change.getSubChannelID( 2 ), "X" );
    assertEquals( change.isUpdate(), true );
    assertEquals( change.getStringValue( "CalendarTypeCode" ), "PERSONAL" );
    assertEquals( change.getIntegerValue( "ID" ), 1 );
    assertEquals( change.isNull( "CalendarTypeCode" ), false );
    assertEquals( change.isNull( "TargetLocation" ), true );
  }

  @Test
  public void deleteChange()
    throws Exception
  {
    final String content =
      "{\n" +
      "  \"id\": 22,\n" +
      "  \"type\": 13,\n" +
      "  \"channels\": [\n" +
      "    {\n" +
      "      \"cid\": 1\n" +
      "    }\n" +
      "  ]\n" +
      "}\n";
    final ChangeDTO change = toChange( content );
    assertEquals( change.getDesignatorAsInt(), 22 );
    assertEquals( change.getTypeID(), 13 );
    assertEquals( change.getChannelCount(), 1 );
    assertEquals( change.getChannelID( 0 ), 1 );
    assertEquals( change.getSubChannelID( 0 ), null );
    assertEquals( change.isUpdate(), false );
  }

  @Test
  public void changeWithStringDesignator()
    throws Exception
  {
    final String content =
      "{\n" +
      "  \"id\": \"MyID\",\n" +
      "  \"type\": 12,\n" +
      "  \"channels\": [\n" +
      "    {\n" +
      "      \"cid\": 1\n" +
      "    }\n" +
      "  ],\n" +
      "  \"data\": {\n" +
      "    \"ID\": \"MyID\"\n" +
      "  }\n" +
      "}\n";
    final ChangeDTO change = toChange( content );
    assertEquals( change.getDesignatorAsString(), "MyID" );
    assertEquals( change.getTypeID(), 12 );
    assertEquals( change.getChannelCount(), 1 );
    assertEquals( change.getChannelID( 0 ), 1 );
    assertEquals( change.getSubChannelID( 0 ), null );
    assertEquals( change.isUpdate(), true );
    assertEquals( change.getStringValue( "ID" ), "MyID" );
  }

  private ChangeDTO toChange( final String content )
  {
    return new ChangeDTO( JsonTestUtil.toJsonObject( content ) );
  }
}
