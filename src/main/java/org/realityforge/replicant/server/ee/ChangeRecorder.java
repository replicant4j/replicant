package org.realityforge.replicant.server.ee;

import javax.annotation.Nonnull;
import javax.annotation.Resource;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.PostPersist;
import javax.persistence.PostRemove;
import javax.persistence.PostUpdate;
import javax.transaction.TransactionSynchronizationRegistry;
import org.realityforge.replicant.server.EntityMessage;

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

  @PostRemove
  public void postRemove( final Object object )
  {
    queueEntityMessageForObject( object, false );
  }

  private void queueEntityMessageForObject( @Nonnull final Object object, final boolean update )
  {
    final EntityMessage entityMessage = toEntityMessage( object, update );
    if( null != entityMessage )
    {
      EntityMessageCacheUtil.getEntityMessageSet( getRegistry() ).merge( entityMessage );
    }
  }

  protected abstract EntityMessage toEntityMessage( @Nonnull Object object, boolean update );

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
