package org.realityforge.replicant.server;

import java.util.Arrays;
import java.util.List;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class ChangeUtilTest
{
  @Test
  public void basicOperation()
  {
    final String id = "myID";
    final int typeID = 42;

    final EntityMessage message1 = MessageTestUtil.createMessage( id, typeID, 0, "r1", "r2", "a1", "a2" );
    final EntityMessage message2 = MessageTestUtil.createMessage( id, typeID, 0, "r1", "r3", "aZ", "a2" );

    final List<Change> changes = ChangeUtil.toChanges( Arrays.asList( message1, message2 ), 1, "B" );

    assertEquals( changes.size(), 2 );

    assertEquals( changes.get( 0 ).getEntityMessage(), message1 );
    assertEquals( changes.get( 0 ).getChannels().get( 1 ), "B" );
    assertEquals( changes.get( 1 ).getEntityMessage(), message2 );
    assertEquals( changes.get( 1 ).getChannels().get( 1 ), "B" );
  }
}
