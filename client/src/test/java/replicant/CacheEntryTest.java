package replicant;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class CacheEntryTest
  extends AbstractReplicantTest
{
  @Test
  public void basicOperation()
  {
    final var address =
      new ChannelAddress( ValueUtil.randomInt(), ValueUtil.randomInt(), ValueUtil.randomInt() );
    final var eTag = ValueUtil.randomString();
    final var content = ValueUtil.randomString();
    final var entry = new CacheEntry( address, eTag, content );

    assertEquals( entry.getAddress(), address );
    assertEquals( entry.getETag(), eTag );
    assertEquals( entry.getContent(), content );
  }
}
