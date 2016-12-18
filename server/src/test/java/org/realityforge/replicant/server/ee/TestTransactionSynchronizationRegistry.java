package org.realityforge.replicant.server.ee;

import java.util.HashMap;
import javax.annotation.Nonnull;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.TransactionSynchronizationRegistry;
import org.realityforge.guiceyloops.server.TestInitialContextFactory;
import org.realityforge.replicant.shared.transport.ReplicantContext;
import static org.testng.Assert.*;

public class TestTransactionSynchronizationRegistry
  implements TransactionSynchronizationRegistry
{
  private final HashMap<Object, Object> _resources = new HashMap<>();

  private boolean _rollBackOnly;

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
    return _rollBackOnly ? Status.STATUS_MARKED_ROLLBACK : Status.STATUS_ACTIVE;
  }

  @Override
  public void setRollbackOnly()
  {
    _rollBackOnly = true;
  }

  @Override
  public boolean getRollbackOnly()
  {
    return _rollBackOnly;
  }

  @Nonnull
  public static TransactionSynchronizationRegistry bind()
  {
    try
    {
      TestInitialContextFactory.reset();
      final Context context = TestInitialContextFactory.getContext().createSubcontext( "java:comp" );
      final TestTransactionSynchronizationRegistry registry = new TestTransactionSynchronizationRegistry();
      registry.putResource( ReplicantContext.REPLICATION_INVOCATION_KEY, "Ignored" );
      context.bind( "TransactionSynchronizationRegistry", registry );
      return registry;
    }
    catch ( final NamingException e )
    {
      fail( "Unexpected exception", e );
      throw new IllegalStateException();
    }
  }

}
