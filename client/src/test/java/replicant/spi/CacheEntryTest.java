package replicant.spi;

import org.realityforge.guiceyloops.shared.ValueUtil;
import org.testng.annotations.Test;
import replicant.AbstractReplicantTest;
import static org.testng.Assert.*;

public class CacheEntryTest
  extends AbstractReplicantTest
{
  @Test
  public void basicOperation()
  {
    final String key = ValueUtil.randomString();
    final String eTag = ValueUtil.randomString();
    final String content = ValueUtil.randomString();
    final CacheEntry entry = new CacheEntry( key, eTag, content );

    assertEquals( entry.getKey(), key );
    assertEquals( entry.getETag(), eTag );
    assertEquals( entry.getContent(), content );
  }
}
