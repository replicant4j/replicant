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
    final Connector connector = createConnector();
    final ConnectorEntry entry = new ConnectorEntry( connector, true );
    assertEquals( entry.getConnector(), connector );
    assertTrue( entry.isRequired() );
    assertEquals( entry.getRateLimiter().getTokensPerSecond(), 1D * ConnectorEntry.REQUIRED_REGEN_PER_SECOND );

    entry.getRateLimiter().setTokenCount( 0 );

    final TestConsumer action = new TestConsumer();
    assertFalse( entry.attemptAction( action ) );
    assertNull( action._connector );

    entry.getRateLimiter().fillBucket();

    assertTrue( entry.attemptAction( action ) );
    assertEquals( action._connector, connector );
  }

  @Test
  public void optionalService()
  {
    final ConnectorEntry entry = new ConnectorEntry( createConnector(), false );
    assertFalse( entry.isRequired() );
    assertEquals( entry.getRateLimiter().getTokensPerSecond(), 1D * ConnectorEntry.OPTIONAL_REGEN_PER_SECOND );
  }

  @SuppressWarnings( "ConstantValue" )
  @Test
  public void flipRequiredState()
  {
    final ConnectorEntry entry = new ConnectorEntry( createConnector(), true );
    assertTrue( entry.isRequired() );
    entry.setRequired( false );
    assertFalse( entry.isRequired() );
  }
}
