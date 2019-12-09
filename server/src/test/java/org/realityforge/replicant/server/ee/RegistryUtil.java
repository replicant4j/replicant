package org.realityforge.replicant.server.ee;

import javax.annotation.Nonnull;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.spi.NamingManager;
import javax.transaction.TransactionSynchronizationRegistry;
import org.realityforge.guiceyloops.server.TestInitialContextFactory;
import org.realityforge.guiceyloops.server.TestTransactionSynchronizationRegistry;
import org.realityforge.replicant.server.ServerConstants;
import static org.testng.Assert.*;

public final class RegistryUtil
{
  private RegistryUtil()
  {
  }

  @Nonnull
  public static TransactionSynchronizationRegistry bind()
  {
    try
    {
      if ( !NamingManager.hasInitialContextFactoryBuilder() )
      {
        NamingManager.setInitialContextFactoryBuilder( environment -> e -> TestInitialContextFactory.getContext() );
      }
      TestInitialContextFactory.reset();
      final Context context = TestInitialContextFactory.getContext().createSubcontext( "java:comp" );
      final TestTransactionSynchronizationRegistry registry = new TestTransactionSynchronizationRegistry();
      registry.putResource( ServerConstants.REPLICATION_INVOCATION_KEY, "Ignored" );
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
