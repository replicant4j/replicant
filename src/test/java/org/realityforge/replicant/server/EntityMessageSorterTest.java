package org.realityforge.replicant.server;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class EntityMessageSorterTest
{
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

  private void assertIndex( final List<EntityMessage> l1, final int index, final Integer value )
  {
    assertEquals( l1.get( index ).getID(), value );
  }

  private EntityMessage newEntityMessage( final int id,
                                          final int typeID,
                                          final long timestamp,
                                          final boolean isDelete )
  {
    return new EntityMessage( id,
                              typeID,
                              timestamp,
                              new HashMap<String, Serializable>(),
                              isDelete ? null : new HashMap<String, Serializable>() );
  }
}
