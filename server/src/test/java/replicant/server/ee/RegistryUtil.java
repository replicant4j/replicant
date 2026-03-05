package replicant.server.ee;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.TransactionSynchronizationRegistry;
import org.realityforge.spice.jndikit.DefaultNameParser;
import org.realityforge.spice.jndikit.DefaultNamespace;
import org.realityforge.spice.jndikit.memory.MemoryContext;
import replicant.server.ServerConstants;
import static org.testng.Assert.*;

public final class RegistryUtil
{
  private RegistryUtil()
  {
  }

  public static void bind()
  {
    try
    {
      TestInitialContextFactory.reset();
      final TestTransactionSynchronizationRegistry registry = new TestTransactionSynchronizationRegistry();
      registry.putResource( ServerConstants.REPLICATION_INVOCATION_KEY, "Ignored" );
      TestInitialContextFactory
        .getContext()
        .createSubcontext( "java:comp" )
        .bind( "TransactionSynchronizationRegistry", registry );
    }
    catch ( final NamingException e )
    {
      fail( "Unexpected exception", e );
      throw new IllegalStateException();
    }
  }

  public static void unbind()
  {
    TestInitialContextFactory.clear();
  }

  public static class TestTransactionSynchronizationRegistry
    implements TransactionSynchronizationRegistry
  {
    @Nonnull
    private final Map<Object, Object> _resources = new HashMap<>();
    private boolean _rollbackOnly;

    public Object getTransactionKey()
    {
      throw new UnsupportedOperationException();
    }

    public void putResource( final Object key, final Object value )
    {
      _resources.put( key, value );
    }

    public Object getResource( final Object key )
    {
      return _resources.get( key );
    }

    @Override
    public void registerInterposedSynchronization( final Synchronization sync )
    {
      throw new UnsupportedOperationException();
    }

    @Override
    public int getTransactionStatus()
    {
      return _rollbackOnly ? Status.STATUS_MARKED_ROLLBACK : Status.STATUS_ACTIVE;
    }

    @Override
    public void setRollbackOnly()
    {
      _rollbackOnly = true;
    }

    @Override
    public boolean getRollbackOnly()
    {
      return _rollbackOnly;
    }
  }

  /**
   * Utility in-memory JNDI context useful for testing.
   */
  public static final class TestInitialContextFactory
    implements InitialContextFactory
  {
    private static MemoryContext c_context;

    @SuppressWarnings( { "rawtypes", "RedundantSuppression" } )
    public Context getInitialContext( final Hashtable environment )
    {
      return getContext();
    }

    private static MemoryContext getContext()
    {
      return c_context;
    }

    private static void reset()
    {
      System.setProperty( "java.naming.factory.initial", TestInitialContextFactory.class.getName() );
      final DefaultNamespace namespace = new DefaultNamespace( new DefaultNameParser() );
      c_context = new MemoryContext( namespace, new Hashtable<>(), null );
    }

    private static void clear()
    {
      System.getProperties().remove( "java.naming.factory.initial" );
      c_context = null;
    }
  }
}
