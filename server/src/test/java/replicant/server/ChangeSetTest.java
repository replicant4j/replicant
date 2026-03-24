package replicant.server;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;
import javax.json.Json;
import javax.json.JsonObject;
import org.testng.annotations.Test;
import replicant.server.ChannelAction.Action;
import replicant.server.transport.TestFilter;
import static org.testng.Assert.*;

public class ChangeSetTest
{
  @Test
  public void basicOperation()
  {
    final var id = 17;
    final var typeID = 42;

    final var message1 = MessageTestUtil.createMessage( id, typeID, 0, "r1", "r2", "a1", "a2" );
    final var message2 = MessageTestUtil.createMessage( id, typeID, 0, "r1", "r3", "aZ", "a2" );
    final var message3 = MessageTestUtil.createMessage( 18, 42, 0, "X", "X", "X", "X" );

    final var change1 = new Change( message1 );
    final var change2 = new Change( message2 );
    final var change3 = new Change( message3 );

    change1.getChannels().add( new ChannelAddress( 1, 1 ) );
    change2.getChannels().add( new ChannelAddress( 2, 3 ) );
    change3.getChannels().add( new ChannelAddress( 3, 42 ) );

    final var changeSet = new ChangeSet();

    assertEquals( changeSet.getChanges().size(), 0 );

    changeSet.merge( Collections.singletonList( change1 ) );

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
    final var changeSet = new ChangeSet();

    assertEquals( changeSet.getChannelActions().size(), 0 );

    final var filter = Json.createBuilderFactory( null ).createObjectBuilder().build();
    changeSet.mergeAction( new ChannelAction( new ChannelAddress( 1, 2 ), Action.ADD, filter ) );

    assertEquals( changeSet.getChannelActions().size(), 1 );

    final var action = changeSet.getChannelActions().get( 0 );
    assertEquals( action.address().channelId(), 1 );
    assertEquals( action.address().rootId(), (Integer) 2 );
    assertEquals( action.action(), Action.ADD );
    assertEquals( action.filter(), filter );
  }

  @Test
  public void addAction_basic()
  {
    final var changeSet = new ChangeSet();

    assertEquals( changeSet.getChannelActions().size(), 0 );

    final var myFilter = new TestFilter( 23 );
    changeSet.mergeAction( new ChannelAddress( 1, 2 ), Action.ADD, myFilter );

    assertEquals( changeSet.getChannelActions().size(), 1 );

    final var action = changeSet.getChannelActions().get( 0 );
    assertEquals( action.address().channelId(), 1 );
    assertEquals( action.address().rootId(), (Integer) 2 );
    assertEquals( action.action(), Action.ADD );

    final var filter =
      Json.createBuilderFactory( null ).createObjectBuilder().add( "myField", 23 ).build();
    assertEquals( action.filter(), filter );
  }

  @Test
  public void merge_with_Copy()
  {
    final var id = 17;
    final var typeID = 42;

    final var message1 = MessageTestUtil.createMessage( id, typeID, 0, "r1", "r2", "a1", "a2" );
    final var change1 = new Change( message1 );

    final var changeSet = new ChangeSet();

    assertEquals( changeSet.getChanges().size(), 0 );

    changeSet.merge( change1, true );

    final var changes = changeSet.getChanges();
    assertEquals( changes.size(), 1 );
    final var change = changes.iterator().next();
    assertEquals( change.getEntityMessage().getId(), id );
    assertNotSame( change, change1 );
  }

  @Test
  public void fullMerge()
  {
    final var changeSet = new ChangeSet();

    final var id = 17;
    final var typeID = 42;

    final var message1 = MessageTestUtil.createMessage( id, typeID, 0, "r1", "r2", "a1", "a2" );
    final var change1 = new Change( message1 );
    changeSet.merge( change1 );

    final var filter = Json.createBuilderFactory( null ).createObjectBuilder().build();
    changeSet.mergeAction( new ChannelAction( new ChannelAddress( 1, 2 ), Action.ADD, filter ) );

    final var changeSet2 = new ChangeSet();
    changeSet2.merge( changeSet, true );

    final var changes = changeSet2.getChanges();
    assertEquals( changes.size(), 1 );
    final var change = changes.iterator().next();
    assertEquals( change.getEntityMessage().getId(), id );
    assertNotSame( change, change1 );

    final var actions = changeSet2.getChannelActions();
    assertEquals( actions.size(), 1 );

    final var action = actions.get( 0 );
    assertEquals( action.address().channelId(), 1 );
    assertEquals( action.address().rootId(), (Integer) 2 );
    assertEquals( action.action(), Action.ADD );
    assertEquals( action.filter(), filter );
  }

  @Test
  public void mergeEntityMessageSet()
  {
    final var changeSet = new ChangeSet();

    final var id = 17;
    final var typeID = 42;

    final var message1 = MessageTestUtil.createMessage( id, typeID, 0, "r1", "r2", "a1", "a2" );
    final var messageSet = new EntityMessageSet();
    messageSet.merge( message1 );

    changeSet.merge( new ChannelAddress( 1 ), messageSet );

    final var changes = changeSet.getChanges();
    assertEquals( changes.size(), 1 );
    final var change = changes.iterator().next();
    assertEquals( change.getEntityMessage().getId(), id );
    assertEquals( change.getChannels().size(), 1 );
    assertTrue( change.getChannels().contains( new ChannelAddress( 1 ) ) );
  }

  @Test
  public void mergeActionDelete()
  {
    final var changeSet = new ChangeSet();

    assertEquals( changeSet.getChannelActions().size(), 0 );

    final var address1 = new ChannelAddress( 1, 2 );
    final var address2 = new ChannelAddress( 1, 3 );
    final var address3 = new ChannelAddress( 1, 4 );

    final var filter = new TestFilter( 23 );

    changeSet.mergeAction( address1, Action.ADD, filter );
    changeSet.mergeAction( address2, Action.REMOVE, null );
    changeSet.mergeAction( address3, Action.UPDATE, filter );

    assertEquals( changeSet.getChannelActions().size(), 3 );

    assertAction( changeSet, Action.ADD, address1 );
    assertAction( changeSet, Action.REMOVE, address2 );
    assertAction( changeSet, Action.UPDATE, address3 );

    changeSet.mergeAction( address3, Action.DELETE, null );

    assertEquals( changeSet.getChannelActions().size(), 3 );

    assertAction( changeSet, Action.ADD, address1 );
    assertAction( changeSet, Action.REMOVE, address2 );
    assertAction( changeSet, Action.DELETE, address3 );

    changeSet.mergeAction( address2, Action.DELETE, null );

    assertEquals( changeSet.getChannelActions().size(), 3 );

    assertAction( changeSet, Action.ADD, address1 );
    assertAction( changeSet, Action.DELETE, address2 );
    assertAction( changeSet, Action.DELETE, address3 );

    changeSet.mergeAction( address1, Action.DELETE, null );

    assertEquals( changeSet.getChannelActions().size(), 2 );

    assertAction( changeSet, Action.DELETE, address2 );
    assertAction( changeSet, Action.DELETE, address3 );
  }

  private void assertAction( @Nonnull final ChangeSet changeSet,
                             @Nonnull final Action action,
                             @Nonnull final ChannelAddress address )
  {
    assertTrue( changeSet.getChannelActions()
                  .stream()
                  .anyMatch( a -> a.address().equals( address ) && a.action() == action ) );
  }
}
