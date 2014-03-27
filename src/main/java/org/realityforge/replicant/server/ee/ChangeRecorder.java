package org.realityforge.replicant.server.ee;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.Resource;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.PostPersist;
import javax.persistence.PostUpdate;
import javax.persistence.PreRemove;
import javax.transaction.TransactionSynchronizationRegistry;
import org.realityforge.replicant.server.EntityMessage;
import org.realityforge.replicant.server.EntityMessageGenerator;
import org.realityforge.replicant.server.EntityMessageSet;

/**
 * Abstract class to extend to collect changes to entities so that they can be replicated by the library.
 * Library users should implement the {@link #getEntityMessageGenerator()}} method to provide message
 * generator specific to the particular application.
 */
public abstract class ChangeRecorder
{
  /**
   * Standard JNDI key for TransactionSynchronizationRegistry.
   */
  private static final String REGISTRY_KEY = "java:comp/TransactionSynchronizationRegistry";

  /**
   * The registry is actually accessed via JNDI. The @Resource annotation is ignored in production as JPA 2.0 does not
   * support it. However our Guice based test infrastructure uses it to populate and avoid instantiation of JNDI
   * resources.
   */
  @Resource
  private TransactionSynchronizationRegistry _registry;

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

  @PostUpdate
  @PostPersist
  public void postUpdate( final Object object )
  {
    if ( !inRollback() )
    {
      recordEntityMessageForEntity( object, true );
    }
  }

  /**
   * Collect messages before they are committed to the database with the
   * assumption that the remove will not fail. This allows us to traverse
   * the object graph before it is deleted. Note: This is a different strategy
   * from postUpdate() but PostUpdate may be changed in the future to match
   * remove hook.
   *
   * @param object the entity removed.
   */
  @PreRemove
  public void preRemove( final Object object )
  {
    if ( !inRollback() )
    {
      recordEntityMessageForEntity( object, false );
    }
  }

  /**
   * @return true if transaction is in rollback.
   */
  protected final boolean inRollback()
  {
    return getRegistry().getRollbackOnly();
  }

  /**
   * Record the EntityMessage for specified entity in the transactions EntityMessageSet.
   *
   * @param entity   the entity to record.
   * @param isUpdate true if change is an update, false if it is a delete.
   * @return the EntityMessage for specified entity if any was recorded
   */
  @Nullable
  protected final EntityMessage recordEntityMessageForEntity( @Nonnull final Object entity, final boolean isUpdate )
  {
    final EntityMessage entityMessage = getEntityMessageGenerator().convertToEntityMessage( entity, isUpdate );
    if ( null != entityMessage )
    {
      getEntityMessageSet().merge( entityMessage );
    }
    return entityMessage;
  }

  /**
   * Return the EntityMessageSet used to collect messages for the current transaction.
   *
   * @return the EntityMessageSet used to collect messages for the current transaction.
   */
  protected final EntityMessageSet getEntityMessageSet()
  {
    return EntityMessageCacheUtil.getEntityMessageSet( getRegistry() );
  }

  /**
   * @return the message generator for the specific application.
   */
  protected abstract EntityMessageGenerator getEntityMessageGenerator();

  private TransactionSynchronizationRegistry getRegistry()
  {
    if ( null == _registry )
    {
      _registry = lookupTransactionSynchronizationRegistry();
    }
    return _registry;
  }
}

