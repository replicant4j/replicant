package replicant;

import java.util.function.Consumer;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

public class ConnectorEntryTest
  extends AbstractReplicantTest
{
  static class TestConsumer
    implements Consumer<Connector>
  {
    private Connector _connector;

    @Override
    public void accept( final Connector connector )
    {
      _connector = connector;
    }
  }

  @Test
  public void basicOperation()
  {
    final TestConnector connector = TestConnector.create();
    final ConnectorEntry entry = new ConnectorEntry( connector, true );
    assertEquals( entry.getConnector(), connector );
    assertEquals( entry.isRequired(), true );
    assertEquals( entry.getRateLimiter().getTokensPerSecond(), 1D * ConnectorEntry.REQUIRED_REGEN_PER_MILLISECOND );

    entry.getRateLimiter().setTokenCount( 0 );

    final TestConsumer action = new TestConsumer();
    assertFalse( entry.attemptAction( action ) );
    assertEquals( action._connector, null );

    entry.getRateLimiter().fillBucket();

    assertTrue( entry.attemptAction( action ) );
    assertEquals( action._connector, connector );
  }

  @Test
  public void optionalService()
  {
    final ConnectorEntry entry = new ConnectorEntry( TestConnector.create(), false );
    assertEquals( entry.isRequired(), false );
    assertEquals( entry.getRateLimiter().getTokensPerSecond(), 1D * ConnectorEntry.OPTIONAL_REGEN_PER_MILLISECOND );
  }

  @Test
  public void flipRequiredState()
  {
    final ConnectorEntry entry = new ConnectorEntry( TestConnector.create(), true );
    assertEquals( entry.isRequired(), true );
    entry.setRequired( false );
    assertEquals( entry.isRequired(), false );
  }
}
