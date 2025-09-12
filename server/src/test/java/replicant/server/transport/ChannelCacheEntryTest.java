package replicant.server.transport;

import org.testng.annotations.Test;
import replicant.server.ChangeSet;
import replicant.server.ChannelAddress;
import static org.testng.Assert.*;

public class ChannelCacheEntryTest
{
  @Test
  public void basicOperation()
  {
    final ChannelAddress descriptor = new ChannelAddress( 1, null );
    final ChannelCacheEntry entry = new ChannelCacheEntry( descriptor );
    assertEquals( entry.getDescriptor(), descriptor );

    assertNotNull( entry.getLock() );
    try
    {
      entry.getCacheKey();
      fail( "Should have raised exception as not initialized" );
    }
    catch ( final NullPointerException ignored )
    {
    }
    try
    {
      entry.getChangeSet();
      fail( "Should have raised exception as not initialized" );
    }
    catch ( final NullPointerException ignored )
    {
    }

    final ChangeSet changeSet = new ChangeSet();
    entry.init( "X", changeSet );

    assertEquals( entry.getCacheKey(), "X" );
    assertEquals( entry.getChangeSet(), changeSet );
  }
}
