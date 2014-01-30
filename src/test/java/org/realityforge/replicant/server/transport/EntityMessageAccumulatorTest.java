package org.realityforge.replicant.server.transport;

import javax.annotation.Nonnull;
import org.realityforge.replicant.server.EntityMessage;
import org.realityforge.replicant.server.MessageTestUtil;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class EntityMessageAccumulatorTest
{
  static class TestSession
    extends ReplicantSession
  {
    TestSession( @Nonnull final String sessionID )
    {
      super( sessionID );
    }
  }

  @Test
  public void basicOperation()
  {
    final TestSession c = new TestSession( "s1" );
    final EntityMessageAccumulator accumulator = new EntityMessageAccumulator();

    final String id = "myID";
    final int typeID = 42;

    final EntityMessage message = MessageTestUtil.createMessage( id, typeID, 0, "r1", "r2", "a1", "a2" );

    accumulator.addEntityMessage( c, message );
    accumulator.complete( "s1", "j1" );

    assertEquals( c.getQueue().size(), 1 );
    final Packet packet = c.getQueue().nextPacketToProcess();
    assertEquals( packet.getChanges().get( 0 ).getID(), id );
    assertEquals( packet.getJobID(), "j1" );

    accumulator.complete( null, null );
    assertEquals( c.getQueue().size(), 1 );
  }

  @Test
  public void basicOperation_whereJobIDDifferent()
  {
    final TestSession c = new TestSession( "s1" );
    final EntityMessageAccumulator accumulator = new EntityMessageAccumulator();

    final EntityMessage message = MessageTestUtil.createMessage( "myID", 42, 0, "r1", "r2", "a1", "a2" );

    accumulator.addEntityMessage( c, message );
    accumulator.complete( "s2", "j1" );

    assertEquals( c.getQueue().size(), 1 );
    assertNull( c.getQueue().nextPacketToProcess().getJobID() );
  }
}
