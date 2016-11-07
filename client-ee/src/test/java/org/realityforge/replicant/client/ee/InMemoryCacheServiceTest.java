package org.realityforge.replicant.client.ee;

import org.realityforge.replicant.client.transport.CacheEntry;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class InMemoryCacheServiceTest
{
  @Test
  public void basicOperation()
  {
    final InMemoryCacheService cache = new InMemoryCacheService();
    final String key = "Foo";
    final String eTag = "Foo";
    final String content = "Foo";

    assertNull( cache.lookup( key ) );
    assertTrue( cache.store( key, eTag, content ) );

    final CacheEntry entry = cache.lookup( key );
    assertNotNull( entry );
    assertEquals( entry.getKey(), key );
    assertEquals( entry.getETag(), eTag );
    assertEquals( entry.getContent(), content );

    assertTrue( cache.invalidate( key ) );
    assertFalse( cache.invalidate( key ) );
    assertNull( cache.lookup( key ) );
  }
}
