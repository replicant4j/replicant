package org.realityforge.replicant.server.ee;

import javax.annotation.Nonnull;
import javax.annotation.Resource;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.PostPersist;
import javax.persistence.PostUpdate;
import javax.persistence.PreRemove;
import javax.transaction.TransactionSynchronizationRegistry;
import org.realityforge.replicant.server.EntityMessage;
import org.realityforge.replicant.server.EntityMessageGenerator;

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

  @PostUpdate
  @PostPersist
  public void postUpdate( final Object object )
  {
    queueEntityMessageForObject( object, true );
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
  public void preRemove(final Object object)
  {
    queueEntityMessageForObject( object, false );
  }

  private void queueEntityMessageForObject( @Nonnull final Object object, final boolean update )
  {
    if ( !getRegistry().getRollbackOnly() )
    {
      final EntityMessage entityMessage = getEntityMessageGenerator().convertToEntityMessage( object, update );
      if ( null != entityMessage )
      {
        EntityMessageCacheUtil.getEntityMessageSet( getRegistry() ).merge( entityMessage );
      }
    }
  }

  /**
   * @return the message generator for the specific application.
   */
  protected abstract EntityMessageGenerator getEntityMessageGenerator();

  private TransactionSynchronizationRegistry getRegistry()
  {
    if( null == _registry )
    {
      _registry = lookupTransactionSynchronizationRegistry();
    }
    return _registry;
  }

  private static TransactionSynchronizationRegistry lookupTransactionSynchronizationRegistry()
  {
    try
    {
      return (TransactionSynchronizationRegistry) new InitialContext().lookup( REGISTRY_KEY );
    }
    catch( final NamingException ne )
    {
      final String message =
          "Unable to locate TransactionSynchronizationRegistry at " + REGISTRY_KEY + " due to " + ne;
      throw new IllegalStateException( message, ne );
    }
  }
}
