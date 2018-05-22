package replicant;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class EntityChannelTest
  extends AbstractReplicantTest
{
  @Test
  public void basicLifecycle_NoSubChannel()
  {
    final EntityChannel entityChannel = EntityChannel.create( 42 );

    assertEquals( entityChannel.getId(), 42 );
    assertEquals( entityChannel.hasSubChannelId(), false );
    assertEquals( entityChannel.toAddress( 1 ), new ChannelAddress( 1, 42 ) );
  }

  @Test
  public void basicLifecycle_HasSubChannel()
  {
    final EntityChannel entityChannel = EntityChannel.create( 42, 31 );

    assertEquals( entityChannel.getId(), 42 );
    assertEquals( entityChannel.hasSubChannelId(), true );
    assertEquals( entityChannel.getSubChannelId(), 31 );
    assertEquals( entityChannel.toAddress( 1 ), new ChannelAddress( 1, 42, 31 ) );
  }
}
