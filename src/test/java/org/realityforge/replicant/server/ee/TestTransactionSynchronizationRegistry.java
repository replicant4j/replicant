package org.realityforge.replicant.server.ee;

import java.util.HashMap;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.TransactionSynchronizationRegistry;

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
}
