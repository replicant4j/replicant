package replicant.server.ee;

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
}
