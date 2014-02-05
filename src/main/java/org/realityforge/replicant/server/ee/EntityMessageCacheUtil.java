package org.realityforge.replicant.server.ee;


import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.naming.InitialContext;
import javax.naming.NamingException;
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
    return getEntityMessageSet( lookupTransactionSynchronizationRegistry(), KEY );
  }

  @Nonnull
  public static EntityMessageSet getEntityMessageSet( @Nonnull final TransactionSynchronizationRegistry r )
  {
    return getEntityMessageSet( r, KEY );
  }

  @Nullable
  public static EntityMessageSet lookupEntityMessageSet()
  {
    return lookupEntityMessageSet( lookupTransactionSynchronizationRegistry(), KEY );
  }

  @Nullable
  public static EntityMessageSet lookupEntityMessageSet( @Nonnull final TransactionSynchronizationRegistry r )
  {
    return lookupEntityMessageSet( r, KEY );
  }

  @Nullable
  public static EntityMessageSet removeEntityMessageSet()
  {
    return removeEntityMessageSet( lookupTransactionSynchronizationRegistry(), KEY );
  }

  @Nullable
  public static EntityMessageSet removeEntityMessageSet( @Nonnull final TransactionSynchronizationRegistry r )
  {
    return removeEntityMessageSet( r, KEY );
  }

  @Nonnull
  public static EntityMessageSet getSessionEntityMessageSet()
  {
    return getEntityMessageSet( lookupTransactionSynchronizationRegistry(), SESSION_KEY );
  }

  @Nonnull
  public static EntityMessageSet getSessionEntityMessageSet( @Nonnull final TransactionSynchronizationRegistry r )
  {
    return getEntityMessageSet( r, SESSION_KEY );
  }

  @Nullable
  public static EntityMessageSet lookupSessionEntityMessageSet( @Nonnull final TransactionSynchronizationRegistry r )
  {
    return lookupEntityMessageSet( r, SESSION_KEY );
  }

  @Nullable
  public static EntityMessageSet lookupSessionEntityMessageSet()
  {
    return lookupEntityMessageSet( lookupTransactionSynchronizationRegistry(), SESSION_KEY );
  }

  @Nullable
  public static EntityMessageSet removeSessionEntityMessageSet()
  {
    return removeEntityMessageSet( lookupTransactionSynchronizationRegistry(), SESSION_KEY );
  }

  @Nullable
  public static EntityMessageSet removeSessionEntityMessageSet( @Nonnull final TransactionSynchronizationRegistry r )
  {
    return removeEntityMessageSet( r, SESSION_KEY );
  }

  private static EntityMessageSet removeEntityMessageSet( final TransactionSynchronizationRegistry r, final String key )
  {
    final EntityMessageSet messageSet = lookupEntityMessageSet( r, key );
    if ( null != messageSet )
    {
      r.putResource( key, null );
    }
    return messageSet;
  }

  private static EntityMessageSet getEntityMessageSet( final TransactionSynchronizationRegistry r, final String key )
  {
    EntityMessageSet messageSet = lookupEntityMessageSet( r, key );
    if ( null == messageSet )
    {
      messageSet = new EntityMessageSet();
      r.putResource( key, messageSet );
    }
    return messageSet;
  }

  private static EntityMessageSet lookupEntityMessageSet( final TransactionSynchronizationRegistry r, final String key )
  {
    return (EntityMessageSet) r.getResource( key );
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
