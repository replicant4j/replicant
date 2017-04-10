package org.realityforge.replicant.client.runtime;

import java.util.function.Consumer;
import org.realityforge.replicant.client.transport.DataLoaderService;
import org.testng.annotations.Test;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

public class DataLoaderEntryTest
{
  static class TestConsumer
    implements Consumer<DataLoaderService>
  {
    private DataLoaderService _dataLoaderService;

    @Override
    public void accept( final DataLoaderService dataLoaderService )
    {
      _dataLoaderService = dataLoaderService;
    }
  }

  @Test
  public void basicOperation()
  {
    final DataLoaderService service = mock( DataLoaderService.class );
    final DataLoaderEntry entry = new DataLoaderEntry( service, true );
    assertEquals( entry.getService(), service );
    assertEquals( entry.isRequired(), true );
    assertEquals( entry.getRateLimiter().getTokensPerSecond(), 10D );

    entry.getRateLimiter().setTokenCount( 0 );

    final TestConsumer action = new TestConsumer();
    assertFalse( entry.attemptAction( action ) );
    assertEquals( action._dataLoaderService, null );

    entry.getRateLimiter().fillBucket();

    assertTrue( entry.attemptAction( action ) );
    assertEquals( action._dataLoaderService, service );
  }

  @Test
  public void optionalService()
  {
    final DataLoaderEntry entry = new DataLoaderEntry( mock( DataLoaderService.class ), false );
    assertEquals( entry.isRequired(), false );
    assertEquals( entry.getRateLimiter().getTokensPerSecond(), 2D );
  }
}
