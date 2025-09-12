package org.realityforge.replicant.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import javax.annotation.Nonnull;

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

  @Nonnull
  public static List<EntityMessage> sort( @Nonnull final Collection<EntityMessage> messages )
  {
    final ArrayList<EntityMessage> sortedMessages = new ArrayList<>( messages );
    sortedMessages.sort( COMPARATOR );
    return sortedMessages;
  }

  public static final EntityMessageSorter COMPARATOR = new EntityMessageSorter();

  @Override
  public int compare( @Nonnull final EntityMessage o1, @Nonnull final EntityMessage o2 )
  {
    if ( o1.isDelete() )
    {
      if ( o2.isUpdate() )
      {
        return -1;
      }
      else
      {
        final int typeComparison = o2.getTypeId() - o1.getTypeId();
        return 0 != typeComparison ? typeComparison : Long.compare( o2.getTimestamp(), o1.getTimestamp() );
      }
    }
    else if ( o2.isDelete() )
    {
      return 1;
    }
    else
    {
      final int typeComparison = o1.getTypeId() - o2.getTypeId();
      return 0 != typeComparison ? typeComparison : Long.compare( o1.getTimestamp(), o2.getTimestamp() );
    }
  }
}
