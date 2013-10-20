package org.realityforge.replicant.server.ee;


import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.transaction.TransactionSynchronizationRegistry;
import org.realityforge.replicant.server.EntityMessageSet;

/**
 * Some utility methods for interacting with the TransactionSynchronizationRegistry to access an EntityMessageSet.
 */
public final class EntityMessageCacheUtil
{
  /**
   * Key used to reference the set of changes in the TransactionSynchronizationRegistry.
   */
  private static final String KEY = EntityMessageSet.class.getName();

  private EntityMessageCacheUtil()
  {
  }

  @Nonnull
  public static EntityMessageSet getEntityMessageSet( @Nonnull final TransactionSynchronizationRegistry r )
  {
    EntityMessageSet messageSet = lookupEntityMessageSet( r );
    if( null == messageSet )
    {
      messageSet = new EntityMessageSet();
      r.putResource( KEY, messageSet );
    }
    return messageSet;
  }

  @Nullable
  public static EntityMessageSet lookupEntityMessageSet( @Nonnull final TransactionSynchronizationRegistry r )
  {
    return (EntityMessageSet) r.getResource( KEY );
  }

  @Nullable
  public static EntityMessageSet removeEntityMessageSet( @Nonnull final TransactionSynchronizationRegistry r )
  {
    final EntityMessageSet messageSet = (EntityMessageSet) r.getResource( KEY );
    if( null != messageSet )
    {
      r.putResource( KEY, null );
    }
    return messageSet;
  }
}
