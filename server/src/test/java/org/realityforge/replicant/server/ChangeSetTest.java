package org.realityforge.replicant.server;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import javax.json.Json;
import javax.json.JsonObject;
import org.realityforge.replicant.server.ChannelAction.Action;
import org.realityforge.replicant.server.transport.TestFilter;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class ChangeSetTest
{
  @Test
  public void basicOperation()
  {
    final int id = 17;
    final int typeID = 42;

    final EntityMessage message1 = MessageTestUtil.createMessage( id, typeID, 0, "r1", "r2", "a1", "a2" );
    final EntityMessage message2 = MessageTestUtil.createMessage( id, typeID, 0, "r1", "r3", "aZ", "a2" );
    final EntityMessage message3 = MessageTestUtil.createMessage( 18, 42, 0, "X", "X", "X", "X" );

    final Change change1 = new Change( message1 );
    final Change change2 = new Change( message2 );
    final Change change3 = new Change( message3 );

    change1.getChannels().put( 1, 1 );
    change2.getChannels().put( 2, 3 );
    change3.getChannels().put( 3, 42 );

    final ChangeSet changeSet = new ChangeSet();

    assertEquals( changeSet.getChanges().size(), 0 );

    changeSet.mergeAll( Collections.singletonList( change1 ) );

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

    final JsonObject filter = Json.createBuilderFactory( null ).createObjectBuilder().build();
    changeSet.mergeAction( new ChannelAction( new ChannelAddress( 1, 2 ), Action.ADD, filter ) );

    assertEquals( changeSet.getChannelActions().size(), 1 );

    final ChannelAction action = changeSet.getChannelActions().get( 0 );
    assertEquals( action.getAddress().getChannelId(), 1 );
    assertEquals( action.getAddress().getSubChannelId(), (Integer) 2 );
    assertEquals( action.getAction(), Action.ADD );
    assertEquals( action.getFilter(), filter );
  }

  @Test
  public void addAction_basic()
  {
    final ChangeSet changeSet = new ChangeSet();

    assertEquals( changeSet.getChannelActions().size(), 0 );

    final TestFilter myFilter = new TestFilter( 23 );
    changeSet.mergeAction( new ChannelAddress( 1, 2 ), Action.ADD, myFilter );

    assertEquals( changeSet.getChannelActions().size(), 1 );

    final ChannelAction action = changeSet.getChannelActions().get( 0 );
    assertEquals( action.getAddress().getChannelId(), 1 );
    assertEquals( action.getAddress().getSubChannelId(), (Integer) 2 );
    assertEquals( action.getAction(), Action.ADD );

    final JsonObject filter =
      Json.createBuilderFactory( null ).createObjectBuilder().add( "myField", 23 ).build();
    assertEquals( action.getFilter(), filter );
  }

  @Test
  public void merge_with_Copy()
  {
    final int id = 17;
    final int typeID = 42;

    final EntityMessage message1 = MessageTestUtil.createMessage( id, typeID, 0, "r1", "r2", "a1", "a2" );
    final Change change1 = new Change( message1 );

    final ChangeSet changeSet = new ChangeSet();

    assertEquals( changeSet.getChanges().size(), 0 );

    changeSet.merge( change1, true );

    final Collection<Change> changes = changeSet.getChanges();
    assertEquals( changes.size(), 1 );
    final Change change = changes.iterator().next();
    assertEquals( change.getEntityMessage().getId(), id );
    assertNotSame( change, change1 );
  }

  @Test
  public void fullMerge()
  {
    final ChangeSet changeSet = new ChangeSet();

    final int id = 17;
    final int typeID = 42;

    final EntityMessage message1 = MessageTestUtil.createMessage( id, typeID, 0, "r1", "r2", "a1", "a2" );
    final Change change1 = new Change( message1 );
    changeSet.merge( change1 );

    final JsonObject filter = Json.createBuilderFactory( null ).createObjectBuilder().build();
    changeSet.mergeAction( new ChannelAction( new ChannelAddress( 1, 2 ), Action.ADD, filter ) );

    final ChangeSet changeSet2 = new ChangeSet();
    changeSet2.merge( changeSet, true );

    final Collection<Change> changes = changeSet2.getChanges();
    assertEquals( changes.size(), 1 );
    final Change change = changes.iterator().next();
    assertEquals( change.getEntityMessage().getId(), id );
    assertNotSame( change, change1 );

    final LinkedList<ChannelAction> actions = changeSet2.getChannelActions();
    assertEquals( actions.size(), 1 );

    final ChannelAction action = actions.get( 0 );
    assertEquals( action.getAddress().getChannelId(), 1 );
    assertEquals( action.getAddress().getSubChannelId(), (Integer) 2 );
    assertEquals( action.getAction(), Action.ADD );
    assertEquals( action.getFilter(), filter );
  }

  @Test
  public void mergeEntityMessageSet()
  {
    final ChangeSet changeSet = new ChangeSet();

    final int id = 17;
    final int typeID = 42;

    final EntityMessage message1 = MessageTestUtil.createMessage( id, typeID, 0, "r1", "r2", "a1", "a2" );
    final EntityMessageSet messageSet = new EntityMessageSet();
    messageSet.merge( message1 );

    changeSet.merge( new ChannelAddress( 1, null ), messageSet );

    final Collection<Change> changes = changeSet.getChanges();
    assertEquals( changes.size(), 1 );
    final Change change = changes.iterator().next();
    assertEquals( change.getEntityMessage().getId(), id );
    assertEquals( change.getChannels().size(), 1 );
    assertNull( change.getChannels().get( 1 ) );
  }
}
