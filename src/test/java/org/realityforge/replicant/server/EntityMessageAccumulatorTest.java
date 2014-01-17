package org.realityforge.replicant.server;

import java.util.List;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class EntityMessageAccumulatorTest
{
  static class TesReplicantClient
    implements ReplicantClient
  {
    List<EntityMessage> _changeSet;

    @Override
    public void addChangeSet( final List<EntityMessage> changeSet )
    {
      _changeSet = changeSet;
    }
  }

  @Test
  public void basicOperation()
  {
    final TesReplicantClient c = new TesReplicantClient();
    final EntityMessageAccumulator<TesReplicantClient> accumulator = new EntityMessageAccumulator<>();

    final String id = "myID";
    final int typeID = 42;

    final EntityMessage message = MessageTestUtil.createMessage( id, typeID, 0, "r1", "r2", "a1", "a2" );

    accumulator.addEntityMessage( c, message );
    accumulator.complete();

    assertNotNull( c._changeSet );
    assertEquals( c._changeSet.size(), 1 );
    assertEquals( c._changeSet.get( 0 ).getID(), id );

    c._changeSet = null;
    accumulator.complete();
    assertNull( c._changeSet );
  }
}
