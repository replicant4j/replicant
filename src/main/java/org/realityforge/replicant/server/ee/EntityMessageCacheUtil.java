package org.realityforge.replicant.server.ee;


import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.TransactionSynchronizationRegistry;
import org.realityforge.replicant.server.EntityMessageSet;
import org.realityforge.replicant.server.transport.ChangeSet;

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

  /**
   * Standard JNDI key for TransactionSynchronizationRegistry.
   */
  private static final String REGISTRY_KEY = "java:comp/TransactionSynchronizationRegistry";


  private EntityMessageCacheUtil()
  {
  }

  @Nonnull
  public static EntityMessageSet getEntityMessageSet()
  {
    return getEntityMessageSet( lookupTransactionSynchronizationRegistry() );
  }

  @Nonnull
  public static EntityMessageSet getEntityMessageSet( @Nonnull final TransactionSynchronizationRegistry r )
  {
    return getEntityMessageSet( r, KEY );
  }

  @Nullable
  public static EntityMessageSet lookupEntityMessageSet()
  {
    return lookupEntityMessageSet( lookupTransactionSynchronizationRegistry() );
  }

  @Nullable
  public static EntityMessageSet lookupEntityMessageSet( @Nonnull final TransactionSynchronizationRegistry r )
  {
    return lookup( r, KEY );
  }

  @Nullable
  public static EntityMessageSet removeEntityMessageSet()
  {
    return removeEntityMessageSet( lookupTransactionSynchronizationRegistry() );
  }

  @Nullable
  public static EntityMessageSet removeEntityMessageSet( @Nonnull final TransactionSynchronizationRegistry r )
  {
    return remove( r, KEY );
  }

  @Nonnull
  public static ChangeSet getSessionChanges()
  {
    return getSessionChanges( lookupTransactionSynchronizationRegistry() );
  }

  @Nonnull
  public static ChangeSet getSessionChanges( @Nonnull final TransactionSynchronizationRegistry r )
  {
    return getChanges( r, SESSION_KEY );
  }

  @Nullable
  public static ChangeSet lookupSessionChanges()
  {
    return lookupSessionChanges( lookupTransactionSynchronizationRegistry() );
  }

  @Nullable
  public static ChangeSet lookupSessionChanges( @Nonnull final TransactionSynchronizationRegistry r )
  {
    return lookup( r, SESSION_KEY );
  }

  @Nullable
  public static ChangeSet removeSessionChanges()
  {
    return removeSessionChanges( lookupTransactionSynchronizationRegistry() );
  }

  @Nullable
  public static ChangeSet removeSessionChanges( @Nonnull final TransactionSynchronizationRegistry r )
  {
    return remove( r, SESSION_KEY );
  }

  private static ChangeSet getChanges( final TransactionSynchronizationRegistry r, final String key )
  {
    ChangeSet changes = lookup( r, key );
    if ( null == changes )
    {
      changes = new ChangeSet();
      r.putResource( key, changes );
    }
    return changes;
  }

  private static EntityMessageSet getEntityMessageSet( final TransactionSynchronizationRegistry r, final String key )
  {
    EntityMessageSet messageSet = lookup( r, key );
    if ( null == messageSet )
    {
      messageSet = new EntityMessageSet();
      r.putResource( key, messageSet );
    }
    return messageSet;
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
    return (T) r.getResource( key );
  }

  @Nonnull
  private static TransactionSynchronizationRegistry lookupTransactionSynchronizationRegistry()
  {
    try
    {
      return (TransactionSynchronizationRegistry) new InitialContext().lookup( REGISTRY_KEY );
    }
    catch ( final NamingException ne )
    {
      final String message =
        "Unable to locate TransactionSynchronizationRegistry at " + REGISTRY_KEY + " due to " + ne;
      throw new IllegalStateException( message, ne );
    }
  }
}
