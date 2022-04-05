package org.realityforge.replicant.server;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class EntityMessageSorterTest
{
  private int _nextID;

  @BeforeMethod
  public void resetNextID()
  {
    _nextID = 0;
  }

  @Test
  public void sort()
  {
    final EntityMessage m1 = newEntityMessage( 1, 1, 10L, false );
    final EntityMessage m2 = newEntityMessage( 2, 2, 100L, false );
    final EntityMessage m3 = newEntityMessage( 3, 2, 10L, false );
    final EntityMessage m4 = newEntityMessage( 4, 3, 10L, true );
    final EntityMessage m5 = newEntityMessage( 5, 3, 100L, true );

    {
      final List<EntityMessage> l1 = Arrays.asList( m1, m2, m3, m4, m5 );
      final List<EntityMessage> r1 = EntityMessageSorter.sort( l1 );
      assertIndex( r1, 0, 5 );
      assertIndex( r1, 1, 4 );
      assertIndex( r1, 2, 1 );
      assertIndex( r1, 3, 3 );
      assertIndex( r1, 4, 2 );
    }

    {
      final List<EntityMessage> l2 = Arrays.asList( m5, m4, m3, m2, m1 );
      final List<EntityMessage> r2 = EntityMessageSorter.sort( l2 );
      assertIndex( r2, 0, 5 );
      assertIndex( r2, 1, 4 );
      assertIndex( r2, 2, 1 );
      assertIndex( r2, 3, 3 );
      assertIndex( r2, 4, 2 );
    }

    {
      final List<EntityMessage> l2 = Arrays.asList( m1, m1, m2, m2, m1 );
      final List<EntityMessage> r2 = EntityMessageSorter.sort( l2 );
      assertIndex( r2, 0, 1 );
      assertIndex( r2, 1, 1 );
      assertIndex( r2, 2, 1 );
      assertIndex( r2, 3, 2 );
      assertIndex( r2, 4, 2 );
    }

    {
      final List<EntityMessage> l2 = Arrays.asList( m4, m4, m5, m5, m4 );
      final List<EntityMessage> r2 = EntityMessageSorter.sort( l2 );
      assertIndex( r2, 0, 5 );
      assertIndex( r2, 1, 5 );
      assertIndex( r2, 2, 4 );
      assertIndex( r2, 3, 4 );
      assertIndex( r2, 4, 4 );
    }
  }

  private void assertIndex( final List<EntityMessage> l1, final int index, final int value )
  {
    assertEquals( l1.get( index ).getId(), value );
  }

  private EntityMessage newEntityMessage( final int id, final int typeID, final long timestamp, final boolean isDelete )
  {
    return new EntityMessage( id, typeID, timestamp, new HashMap<>(), isDelete ? null : new HashMap<>(), null );
  }

  @Test
  public void deletionsShouldOrderBeforeChanges()
  {
    final EntityMessage[] messages = { createDeletionMessage( 1 ),
                                       createUpdateMessage( 1 ),
                                       createDeletionMessage( 1 ) };

    final List<EntityMessage> sortedMessages = EntityMessageSorter.sort( Arrays.asList( messages ) );

    assertDeletion( sortedMessages.get( 0 ) );
    assertDeletion( sortedMessages.get( 1 ) );
    assertUpdate( sortedMessages.get( 2 ) );
  }

  @Test
  public void typesShouldOrderDescendingWithinDeletions()
  {
    final EntityMessage[] messages = { createUpdateMessage( 1 ),
                                       createDeletionMessage( 1 ),
                                       createDeletionMessage( 3 ),
                                       createDeletionMessage( 2 ),
                                       createDeletionMessage( 4 ) };

    final List<EntityMessage> sortedMessages = EntityMessageSorter.sort( Arrays.asList( messages ) );

    assertEquals( sortedMessages.get( 0 ).getTypeId(), 4 );
    assertEquals( sortedMessages.get( 1 ).getTypeId(), 3 );
    assertEquals( sortedMessages.get( 2 ).getTypeId(), 2 );
    assertEquals( sortedMessages.get( 3 ).getTypeId(), 1 );
    assertUpdate( sortedMessages.get( 4 ) );
  }

  @Test
  public void typesShouldOrderAscendingWithinUpdates()
  {
    final EntityMessage[] messages = { createUpdateMessage( 1 ),
                                       createUpdateMessage( 3 ),
                                       createUpdateMessage( 2 ),
                                       createUpdateMessage( 4 ),
                                       createDeletionMessage( 2 ) };

    final List<EntityMessage> sortedMessages = EntityMessageSorter.sort( Arrays.asList( messages ) );

    assertDeletion( sortedMessages.get( 0 ) );
    assertEquals( sortedMessages.get( 1 ).getTypeId(), 1 );
    assertEquals( sortedMessages.get( 2 ).getTypeId(), 2 );
    assertEquals( sortedMessages.get( 3 ).getTypeId(), 3 );
    assertEquals( sortedMessages.get( 4 ).getTypeId(), 4 );
  }

  @Test
  public void deletionForSameTypeShouldOrderByReverseTime()
  {
    final EntityMessage[] messages = { createDeletionMessage( 2, 10 ),
                                       createDeletionMessage( 1, 15 ),
                                       createDeletionMessage( 2, 20 ),
                                       createDeletionMessage( 2, 15 ) };

    final List<EntityMessage> sortedMessages = EntityMessageSorter.sort( Arrays.asList( messages ) );

    assertEquals( sortedMessages.get( 0 ).getTimestamp(), 20 );
    assertEquals( sortedMessages.get( 1 ).getTimestamp(), 15 );
    assertEquals( sortedMessages.get( 2 ).getTimestamp(), 10 );
    assertEquals( sortedMessages.get( 3 ).getTypeId(), 1 );
  }

  @Test
  public void updateForSameTypeShouldOrderByTime()
  {
    final EntityMessage[] messages = { createUpdateMessage( 2, 10 ),
                                       createUpdateMessage( 1, 15 ),
                                       createUpdateMessage( 2, 20 ),
                                       createUpdateMessage( 2, 15 ) };

    final List<EntityMessage> sortedMessages = EntityMessageSorter.sort( Arrays.asList( messages ) );

    assertEquals( sortedMessages.get( 0 ).getTypeId(), 1 );
    assertEquals( sortedMessages.get( 1 ).getTimestamp(), 10 );
    assertEquals( sortedMessages.get( 2 ).getTimestamp(), 15 );
    assertEquals( sortedMessages.get( 3 ).getTimestamp(), 20 );
  }

  private EntityMessage createUpdateMessage( final int typeID )
  {
    return createUpdateMessage( typeID, 0 );
  }

  private EntityMessage createUpdateMessage( final int typeID, final long time )
  {
    return new EntityMessage( _nextID++, typeID, time, new HashMap<>(), new HashMap<>(), null );
  }

  private EntityMessage createDeletionMessage( final int typeID )
  {
    return createDeletionMessage( typeID, 0 );
  }

  private EntityMessage createDeletionMessage( final int typeID, final long time )
  {
    return new EntityMessage( _nextID++, typeID, time, new HashMap<>(), null, null );
  }

  private void assertDeletion( final EntityMessage message )
  {
    assertTrue( message.isDelete(), "Expected " + message + " to be a deletion" );
  }

  private void assertUpdate( final EntityMessage message )
  {
    assertTrue( message.isUpdate(), "Expected " + message + " to be an update" );
  }
}
