package replicant;

import org.realityforge.guiceyloops.shared.ValueUtil;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class CacheEntryTest
  extends AbstractReplicantTest
{
  @Test
  public void basicOperation()
  {
    final ChannelAddress address =
      new ChannelAddress( ValueUtil.randomInt(), ValueUtil.randomInt(), ValueUtil.randomInt() );
    final String eTag = ValueUtil.randomString();
    final String content = ValueUtil.randomString();
    final CacheEntry entry = new CacheEntry( address, eTag, content );

    assertEquals( entry.getAddress(), address );
    assertEquals( entry.getETag(), eTag );
    assertEquals( entry.getContent(), content );
  }
}
