package replicant.messages;

import org.testng.Assert;
import org.testng.annotations.Test;
import replicant.AbstractReplicantTest;
import replicant.ChannelAddress;
import static org.testng.Assert.*;

public class EntityChannelTest
  extends AbstractReplicantTest
{
  @Test
  public void basicLifecycle_NoSubChannel()
  {
    final EntityChannel entityChannel = EntityChannel.create( 42 );

    assertEquals( entityChannel.getId(), 42 );
    assertFalse( entityChannel.hasSubChannelId() );
    Assert.assertEquals( entityChannel.toAddress( 1 ), new ChannelAddress( 1, 42 ) );
  }

  @Test
  public void basicLifecycle_HasSubChannel()
  {
    final EntityChannel entityChannel = EntityChannel.create( 42, 31 );

    assertEquals( entityChannel.getId(), 42 );
    assertTrue( entityChannel.hasSubChannelId() );
    assertEquals( entityChannel.getSubChannelId(), 31 );
    assertEquals( entityChannel.toAddress( 1 ), new ChannelAddress( 1, 42, 31 ) );
  }
}
