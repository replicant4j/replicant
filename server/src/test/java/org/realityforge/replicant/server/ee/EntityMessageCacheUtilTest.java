package org.realityforge.replicant.server.ee;

import org.realityforge.guiceyloops.server.TestInitialContextFactory;
import org.realityforge.replicant.server.ChangeSet;
import org.realityforge.replicant.server.EntityMessageSet;
import org.realityforge.replicant.server.ServerConstants;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class EntityMessageCacheUtilTest
{
  @BeforeMethod
  public void setup()
    throws Exception
  {
    RegistryUtil.bind();
  }

  @AfterMethod
  public void clearContext()
  {
    TestInitialContextFactory.reset();
  }

  @Test
  public void ensureCacheBehavesAsExpected()
  {
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
    assertNull( EntityMessageCacheUtil.lookupSessionChanges() );

    //Now we force the creation of EntityMessageSet
    final ChangeSet messageSet = EntityMessageCacheUtil.getSessionChanges();

    assertNotNull( messageSet );
    assertEquals( messageSet, EntityMessageCacheUtil.lookupSessionChanges() );
    assertEquals( messageSet, EntityMessageCacheUtil.getSessionChanges() );

    //Now we remove EntityMessageSet
    assertEquals( messageSet, EntityMessageCacheUtil.removeSessionChanges() );
    assertNull( EntityMessageCacheUtil.lookupSessionChanges() );

    //Duplicate remove returns null
    assertNull( EntityMessageCacheUtil.removeSessionChanges() );

    // Ensure that it works with regular changes
    assertNull( EntityMessageCacheUtil.removeEntityMessageSet() );
  }

  @Test( expectedExceptions = IllegalStateException.class )
  public void lookupOfResourceOutsideReplicationContext()
  {
    EntityMessageCacheUtil.lookupTransactionSynchronizationRegistry().
      putResource( ServerConstants.REPLICATION_INVOCATION_KEY, null );

    EntityMessageCacheUtil.lookupSessionChanges();
  }
}
