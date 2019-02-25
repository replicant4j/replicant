package replicant.messages;

import org.testng.annotations.Test;
import replicant.AbstractReplicantTest;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class EntityChangeTest
  extends AbstractReplicantTest
{
  @SuppressWarnings( "ResultOfMethodCallIgnored" )
  @Test
  public void construct_removeMessage()
  {
    final String[] channels = { "0", "3.4" };
    final EntityChange change = EntityChange.create( 2, 1, channels );

    assertEquals( change.getId(), "2.1" );
    assertEquals( change.getChannels(), channels );
    assertTrue( change.isRemove() );
    assertFalse( change.isUpdate() );
    assertThrows( change::getData );
  }

  @Test
  public void construct_updateMessage()
  {
    final String[] channels = { "0", "3.4" };
    final EntityChangeData data = mock( EntityChangeData.class );
    final EntityChange change = EntityChange.create( 2, 1, channels, data );

    assertEquals( change.getId(), "2.1" );
    assertEquals( change.getChannels(), channels );
    assertFalse( change.isRemove() );
    assertTrue( change.isUpdate() );
    assertEquals( change.getData(), data );
  }
}
