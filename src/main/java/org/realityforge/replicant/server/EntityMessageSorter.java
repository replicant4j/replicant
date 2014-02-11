package org.realityforge.replicant.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Provides ordering of EntityMessages so as to ensure their correct processing when retrieved from the database
 * Order changes as they should be processed, so a to make subsequent retrieval and application easier.
 * Ordering is:
 * - Deletions, then updates
 * - Within deletions, order by type, desc, so as to delete in the reverse order from the architecture.rb.
 * - Within updates, order by type, so as to create in the order from the architecture.rb.
 * - Within identical types, order by change time, desc on delete, asc on update.
 */
public final class EntityMessageSorter
  implements Comparator<EntityMessage>
{
  private EntityMessageSorter()
  {
  }

  public static List<EntityMessage> sort( final Collection<EntityMessage> messages )
  {
    final ArrayList<EntityMessage> sortedMessages = new ArrayList<EntityMessage>( messages );
    Collections.sort( sortedMessages, COMPARATOR );
    return sortedMessages;
  }

  public static final EntityMessageSorter COMPARATOR = new EntityMessageSorter();

  @Override
  public int compare( final EntityMessage o1, final EntityMessage o2 )
  {
    if ( o1.isDelete() )
    {
      if ( o2.isUpdate() )
      {
        return -1;
      }
      final int typeComparison = o2.getTypeID() - o1.getTypeID();
      if ( 0 != typeComparison )
      {
        return typeComparison;
      }
      if ( o2.getTimestamp() < o1.getTimestamp() )
      {
        return -1;
      }
      if ( o2.getTimestamp() > o1.getTimestamp() )
      {
        return 1;
      }
      return 0;
    }
    if ( o2.isDelete() )
    {
      return 1;
    }
    final int typeComparison = o1.getTypeID() - o2.getTypeID();
    if ( 0 != typeComparison )
    {
      return typeComparison;
    }
    if ( o1.getTimestamp() < o2.getTimestamp() )
    {
      return -1;
    }
    if ( o1.getTimestamp() > o2.getTimestamp() )
    {
      return 1;
    }
    return 0;
  }
}
