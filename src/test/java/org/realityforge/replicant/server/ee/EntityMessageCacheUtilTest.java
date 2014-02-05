package org.realityforge.replicant.server.ee;

import org.realityforge.replicant.server.EntityMessageSet;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class EntityMessageCacheUtilTest
{
  @Test
  public void ensureCacheBehavesAsExpected()
  {
    final TestTransactionSynchronizationRegistry registry = new TestTransactionSynchronizationRegistry();
    assertNull( EntityMessageCacheUtil.lookupEntityMessageSet( registry ) );

    //Now we force the creation of EntityMessageSet
    final EntityMessageSet messageSet = EntityMessageCacheUtil.getEntityMessageSet( registry );

    assertNotNull( messageSet );
    assertEquals( messageSet, EntityMessageCacheUtil.lookupEntityMessageSet( registry ) );
    assertEquals( messageSet, EntityMessageCacheUtil.getEntityMessageSet( registry ) );

    //Now we remove EntityMessageSet
    assertEquals( messageSet, EntityMessageCacheUtil.removeEntityMessageSet( registry ) );
    assertNull( EntityMessageCacheUtil.lookupEntityMessageSet( registry ) );

    //Duplicate remove returns null
    assertNull( EntityMessageCacheUtil.removeEntityMessageSet( registry ) );
  }

  @Test
  public void clientEntityMessageSet()
  {
    final TestTransactionSynchronizationRegistry registry = new TestTransactionSynchronizationRegistry();
    assertNull( EntityMessageCacheUtil.lookupClientEntityMessageSet( registry ) );

    //Now we force the creation of EntityMessageSet
    final EntityMessageSet messageSet = EntityMessageCacheUtil.getClientEntityMessageSet( registry );

    assertNotNull( messageSet );
    assertEquals( messageSet, EntityMessageCacheUtil.lookupClientEntityMessageSet( registry ) );
    assertEquals( messageSet, EntityMessageCacheUtil.getClientEntityMessageSet( registry ) );

    //Now we remove EntityMessageSet
    assertEquals( messageSet, EntityMessageCacheUtil.removeClientEntityMessageSet( registry ) );
    assertNull( EntityMessageCacheUtil.lookupClientEntityMessageSet( registry ) );

    //Duplicate remove returns null
    assertNull( EntityMessageCacheUtil.removeClientEntityMessageSet( registry ) );

    // Ensure that it works with regular changes
    assertNull( EntityMessageCacheUtil.removeEntityMessageSet( registry ) );
  }
}
