package replicant.server.ee;

import org.jspecify.annotations.NonNull;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.PostPersist;
import javax.persistence.PostUpdate;
import javax.persistence.PreRemove;
import javax.transaction.TransactionSynchronizationRegistry;
import replicant.server.runtime.EntityMessageCacheUtil;
import replicant.server.runtime.ReplicantSystem;
import replicant.server.transport.ReplicantChangeRecorder;

@SuppressWarnings( "BanJNDI" )
public class ReplicantEntityChangeListener
{
  /*
   * The registry and recorder are actually accessed via JNDI. The @Inject annotations are ignored in production as JPA 2.0 does not
   * support it. However our Guice based test infrastructure uses it to populate and avoid instantiation of JNDI
   * resources.
   */
  @Inject
  @ReplicantSystem
  private TransactionSynchronizationRegistry _registry;
  @Inject
  private ReplicantChangeRecorder _recorder;

  @PostUpdate
  @PostPersist
  public void postUpdate( final Object object )
  {
    final var registry = getRegistry();
    if ( !registry.getRollbackOnly() )
    {
      final var entityMessage = getRecorder().convertToEntityMessage( object, true );
      if ( null != entityMessage )
      {
        EntityMessageCacheUtil.getEntityMessageSet( registry ).merge( entityMessage );
      }
    }
  }

  /**
   * Collect messages before they are committed to the database with the
   * assumption that the remove will not fail. This allows us to traverse
   * the object graph before it is deleted. Note: This is a different strategy
   * from postUpdate() but PostUpdate may be changed in the future to match
   * remove hook. (Compare Pre versus Post hooks)
   *
   * @param object the entity removed.
   */
  @PreRemove
  public void preRemove( final Object object )
  {
    final var registry = getRegistry();
    if ( !registry.getRollbackOnly() )
    {
      final var entityMessage = getRecorder().convertToEntityMessage( object, false );
      if ( null != entityMessage )
      {
        EntityMessageCacheUtil.getEntityMessageSet( registry ).merge( entityMessage );
      }
    }
  }

  @SuppressWarnings( "unchecked" )
  @NonNull
  private static <T> T lookup( final String key )
  {
    try
    {
      return (T) new InitialContext().lookup( key );
    }
    catch ( final NamingException ne )
    {
      final var message = "Unable to locate element at " + key + " due to " + ne;
      throw new IllegalStateException( message, ne );
    }
  }

  @NonNull
  private TransactionSynchronizationRegistry getRegistry()
  {
    if ( null == _registry )
    {
      _registry = lookup( "java:comp/TransactionSynchronizationRegistry" );
    }
    return _registry;
  }

  @NonNull
  private ReplicantChangeRecorder getRecorder()
  {
    if ( null == _recorder )
    {
      final var beanManager = ReplicantEntityChangeListener.<BeanManager>lookup( "java:comp/BeanManager" );
      final var bean = beanManager.getBeans( ReplicantChangeRecorder.class ).iterator().next();
      final var creationalContext = beanManager.createCreationalContext( bean );
      _recorder = (ReplicantChangeRecorder) beanManager.getReference( bean,
                                                                      ReplicantChangeRecorder.class,
                                                                      creationalContext );
    }
    return _recorder;
  }
}
