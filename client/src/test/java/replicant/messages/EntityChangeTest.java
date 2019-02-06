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
    final int id = 1;
    final int type = 2;
    final EntityChannel[] channels = { EntityChannel.create( 0 ), EntityChannel.create( 3, 4 ) };
    final EntityChange change = EntityChange.create( id, type, channels );

    assertEquals( change.getId(), 1 );
    assertEquals( change.getTypeId(), 2 );
    assertEquals( change.getChannels(), channels );
    assertTrue( change.isRemove() );
    assertFalse( change.isUpdate() );
    assertThrows( change::getData );
  }

  @Test
  public void construct_updateMessage()
  {
    final int id = 1;
    final int type = 2;
    final EntityChannel[] channels = { EntityChannel.create( 0 ), EntityChannel.create( 3, 4 ) };
    final EntityChangeData data = mock( EntityChangeData.class );
    final EntityChange change = EntityChange.create( id, type, channels, data );

    assertEquals( change.getId(), 1 );
    assertEquals( change.getTypeId(), 2 );
    assertEquals( change.getChannels(), channels );
    assertFalse( change.isRemove() );
    assertTrue( change.isUpdate() );
    assertEquals( change.getData(), data );
  }
}
