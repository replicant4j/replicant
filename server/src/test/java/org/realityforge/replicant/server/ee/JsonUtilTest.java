package org.realityforge.replicant.server.ee;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonTypeName;
import javax.json.JsonObject;
import javax.json.stream.JsonParsingException;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class JsonUtilTest
{
  @Test
  public void toJsonObject_emptyObject()
  {
    final JsonObject jsonObject = JsonUtil.toJsonObject( "{}" );
    assertEquals( jsonObject.size(), 0 );
  }

  @Test
  public void toJsonObject_simpleObject()
  {
    final JsonObject jsonObject = JsonUtil.toJsonObject( "{\"a\":42}" );
    assertEquals( jsonObject.size(), 1 );
    assertEquals( jsonObject.getInt( "a" ), 42 );
  }

  @Test( expectedExceptions = { JsonParsingException.class } )
  public void toJsonObject_badObject()
  {
    final JsonObject jsonObject = JsonUtil.toJsonObject( "" );
    assertEquals( jsonObject.size(), 0 );
  }

  @JsonAutoDetect( fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE, setterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE, creatorVisibility = JsonAutoDetect.Visibility.NONE )
  @JsonTypeName( "foo" )
  public static class Foo
  {
    private int myField;

    public Foo( final int myField )
    {
      this.myField = myField;
    }
  }

  @Test
  public void toJsonObject_structuredObject()
  {
    final JsonObject jsonObject = JsonUtil.toJsonObject( new Foo( 45 ) );
    assertEquals( jsonObject.size(), 1 );
    assertEquals( jsonObject.getInt( "myField" ), 45 );
  }
}
