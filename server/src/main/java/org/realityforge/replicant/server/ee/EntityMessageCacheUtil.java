package org.realityforge.replicant.server.ee;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.transaction.TransactionSynchronizationRegistry;
import org.realityforge.replicant.server.ChangeSet;
import org.realityforge.replicant.server.EntityMessageSet;
import org.realityforge.replicant.shared.SharedConstants;

/**
 * Some utility methods for interacting with the TransactionSynchronizationRegistry to access an EntityMessageSet.
 */
public final class EntityMessageCacheUtil
{
  /**
   * Key used to reference the set of changes in the TransactionSynchronizationRegistry.
   */
  private static final String KEY = EntityMessageSet.class.getName();
  /**
   * Key used to lookup client specific changes. Should not be routed.
   */
  private static final String SESSION_KEY = KEY + "/Session";

  private EntityMessageCacheUtil()
  {
  }

  @Nonnull
  public static EntityMessageSet getEntityMessageSet( @Nonnull final TransactionSynchronizationRegistry r )
  {
    EntityMessageSet messageSet = lookup( r, KEY );
    if ( null == messageSet )
    {
      messageSet = new EntityMessageSet();
      r.putResource( KEY, messageSet );
    }
    return messageSet;
  }

  @Nullable
  public static EntityMessageSet lookupEntityMessageSet()
  {
    return lookup( TransactionSynchronizationRegistryUtil.lookup(), KEY );
  }

  @Nullable
  public static EntityMessageSet removeEntityMessageSet()
  {
    return removeEntityMessageSet( TransactionSynchronizationRegistryUtil.lookup() );
  }

  @Nullable
  public static EntityMessageSet removeEntityMessageSet( @Nonnull final TransactionSynchronizationRegistry r )
  {
    return remove( r, KEY );
  }

  @Nonnull
  public static ChangeSet getSessionChanges()
  {
    return getSessionChanges( TransactionSynchronizationRegistryUtil.lookup() );
  }

  @Nonnull
  public static ChangeSet getSessionChanges( @Nonnull final TransactionSynchronizationRegistry r )
  {
    ChangeSet changes = lookup( r, SESSION_KEY );
    if ( null == changes )
    {
      changes = new ChangeSet();
      r.putResource( SESSION_KEY, changes );
    }
    return changes;
  }

  @Nullable
  public static ChangeSet lookupSessionChanges()
  {
    return lookupSessionChanges( TransactionSynchronizationRegistryUtil.lookup() );
  }

  @Nullable
  public static ChangeSet lookupSessionChanges( @Nonnull final TransactionSynchronizationRegistry r )
  {
    return lookup( r, SESSION_KEY );
  }

  @Nullable
  public static ChangeSet removeSessionChanges()
  {
    return removeSessionChanges( TransactionSynchronizationRegistryUtil.lookup() );
  }

  @Nullable
  public static ChangeSet removeSessionChanges( @Nonnull final TransactionSynchronizationRegistry r )
  {
    return remove( r, SESSION_KEY );
  }

  private static <T> T remove( final TransactionSynchronizationRegistry r, final String key )
  {
    final T messageSet = lookup( r, key );
    if ( null != messageSet )
    {
      r.putResource( key, null );
    }
    return messageSet;
  }

  @SuppressWarnings( "unchecked" )
  private static <T> T lookup( final TransactionSynchronizationRegistry r, final String key )
  {
    final Object invocationContext = r.getResource( SharedConstants.REPLICATION_INVOCATION_KEY );
    if ( null == invocationContext )
    {
      final String message =
        "Attempting to look up replication resource '" + key + "' but there is no active replication context. " +
        "This probably means you are attempting to update replicated entities outside of a valid replication context. " +
        "Make sure the entity is modified in a service surrounded by a replication interceptor.";
      throw new IllegalStateException( message );
    }
    return (T) r.getResource( key );
  }
}
