package org.realityforge.replicant.server.ee;

import javax.naming.Context;
import org.realityforge.guiceyloops.server.TestInitialContextFactory;
import org.realityforge.replicant.server.EntityMessageSet;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class EntityMessageCacheUtilTest
{
  @BeforeMethod
  public void setup()
    throws Exception
  {
    TestInitialContextFactory.reset();
    final Context context = TestInitialContextFactory.getContext().createSubcontext( "java:comp" );
    context.bind( "TransactionSynchronizationRegistry", new TestTransactionSynchronizationRegistry() );
  }

  @Test
  public void ensureCacheBehavesAsExpected()
  {
    final TestTransactionSynchronizationRegistry registry = new TestTransactionSynchronizationRegistry();
    assertNull( EntityMessageCacheUtil.lookupEntityMessageSet() );

    //Now we force the creation of EntityMessageSet
    final EntityMessageSet messageSet = EntityMessageCacheUtil.getEntityMessageSet();

    assertNotNull( messageSet );
    assertEquals( messageSet, EntityMessageCacheUtil.lookupEntityMessageSet() );
    assertEquals( messageSet, EntityMessageCacheUtil.getEntityMessageSet() );

    //Now we remove EntityMessageSet
    assertEquals( messageSet, EntityMessageCacheUtil.removeEntityMessageSet() );
    assertNull( EntityMessageCacheUtil.lookupEntityMessageSet() );

    //Duplicate remove returns null
    assertNull( EntityMessageCacheUtil.removeEntityMessageSet() );
  }

  @Test
  public void clientEntityMessageSet()
  {
    assertNull( EntityMessageCacheUtil.lookupSessionEntityMessageSet() );

    //Now we force the creation of EntityMessageSet
    final EntityMessageSet messageSet = EntityMessageCacheUtil.getSessionEntityMessageSet();

    assertNotNull( messageSet );
    assertEquals( messageSet, EntityMessageCacheUtil.lookupSessionEntityMessageSet() );
    assertEquals( messageSet, EntityMessageCacheUtil.getSessionEntityMessageSet() );

    //Now we remove EntityMessageSet
    assertEquals( messageSet, EntityMessageCacheUtil.removeSessionEntityMessageSet() );
    assertNull( EntityMessageCacheUtil.lookupSessionEntityMessageSet() );

    //Duplicate remove returns null
    assertNull( EntityMessageCacheUtil.removeSessionEntityMessageSet() );

    // Ensure that it works with regular changes
    assertNull( EntityMessageCacheUtil.removeEntityMessageSet() );
  }
}
