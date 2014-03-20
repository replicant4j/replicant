package org.realityforge.replicant.server;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import org.realityforge.replicant.server.ChannelAction.Action;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class ChangeSetTest
{
  @Test
  public void basicOperation()
  {
    final String id = "myID";
    final int typeID = 42;

    final EntityMessage message1 = MessageTestUtil.createMessage( id, typeID, 0, "r1", "r2", "a1", "a2" );
    final EntityMessage message2 = MessageTestUtil.createMessage( id, typeID, 0, "r1", "r3", "aZ", "a2" );
    final EntityMessage message3 = MessageTestUtil.createMessage( "X", 42, 0, "X", "X", "X", "X" );

    final Change change1 = new Change( message1 );
    final Change change2 = new Change( message2 );
    final Change change3 = new Change( message3 );

    change1.getChannels().put( 1, 1 );
    change2.getChannels().put( 2, 3 );
    change3.getChannels().put( 3, "S" );

    final ChangeSet changeSet = new ChangeSet();

    assertEquals( changeSet.getChanges().size(), 0 );

    changeSet.mergeAll( Arrays.asList( change1 ) );

    assertEquals( changeSet.getChanges().size(), 1 );
    assertEquals( change1.getChannels().size(), 1 );

    changeSet.merge( change2 );

    assertEquals( changeSet.getChanges().size(), 1 );
    assertEquals( change1.getChannels().size(), 2 );

    //Re-merge same
    changeSet.merge( change2 );

    assertEquals( changeSet.getChanges().size(), 1 );
    assertEquals( change1.getChannels().size(), 2 );

    changeSet.merge( change3 );

    assertEquals( changeSet.getChanges().size(), 2 );
  }

  @Test
  public void actions()
  {
    final ChangeSet changeSet = new ChangeSet();

    assertEquals( changeSet.getChannelActions().size(), 0 );

    changeSet.addAction( new ChannelAction( 1, 2, Action.ADD ) );

    assertEquals( changeSet.getChannelActions().size(), 1 );

    final ChannelAction action = changeSet.getChannelActions().get( 0 );
    assertEquals( action.getID(), 1 );
    final Serializable subChannelID = 1;
    assertEquals( action.getSubChannelID(), subChannelID );
    assertEquals( action.getAction(), Action.ADD );
  }

  @Test
  public void merge_with_Copy()
  {
    final String id = "myID";
    final int typeID = 42;

    final EntityMessage message1 = MessageTestUtil.createMessage( id, typeID, 0, "r1", "r2", "a1", "a2" );
    final Change change1 = new Change( message1 );

    final ChangeSet changeSet = new ChangeSet();

    assertEquals( changeSet.getChanges().size(), 0 );

    changeSet.merge( change1, true );

    final Collection<Change> changes = changeSet.getChanges();
    assertEquals( changes.size(), 1 );
    final Change change = changes.iterator().next();
    assertEquals( change.getEntityMessage().getID(), id );
    assertNotSame( change, change1 );
  }
}
