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
    final var descriptor = ChannelAddress.of( 1, null );
    final var entry = new ChannelCacheEntry( descriptor );
    assertEquals( entry.getDescriptor(), descriptor );

    assertNotNull( entry.getLock() );
    expectThrows( NullPointerException.class, entry::getCacheKey );
    expectThrows( NullPointerException.class, entry::getChangeSet );

    final var changeSet = new ChangeSet();
    entry.init( "X", changeSet );

    assertEquals( entry.getCacheKey(), "X" );
    assertEquals( entry.getChangeSet(), changeSet );
  }
}
