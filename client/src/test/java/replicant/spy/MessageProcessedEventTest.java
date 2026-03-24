package replicant.spy;

import java.util.HashMap;
import org.testng.annotations.Test;
import replicant.AbstractReplicantTest;
import replicant.ValueUtil;
import static org.testng.Assert.*;

public class MessageProcessedEventTest
  extends AbstractReplicantTest
{
  @Test
  public void basicOperation()
  {
    final var requestId = ValueUtil.randomInt();
    final var channelAddCount = ValueUtil.getRandom().nextInt( 10 );
    final var channelUpdateCount = ValueUtil.getRandom().nextInt( 10 );
    final var channelRemoveCount = ValueUtil.getRandom().nextInt( 10 );
    final var entityUpdateCount = ValueUtil.getRandom().nextInt( 100 );
    final var entityRemoveCount = ValueUtil.getRandom().nextInt( 100 );
    final var entityLinkCount = ValueUtil.getRandom().nextInt( 10 );
    final var dataLoadStatus =
      new DataLoadStatus( requestId,
                          channelAddCount,
                          channelUpdateCount,
                          channelRemoveCount,
                          entityUpdateCount,
                          entityRemoveCount,
                          entityLinkCount );
    final var event =
      new MessageProcessedEvent( 23, "Rose", dataLoadStatus );

    assertEquals( event.getSchemaId(), 23 );
    assertEquals( event.getSchemaName(), "Rose" );
    assertEquals( event.getDataLoadStatus(), dataLoadStatus );

    final var data = new HashMap<String, Object>();
    event.toMap( data );

    assertEquals( data.get( "type" ), "Connector.MessageProcess" );
    assertEquals( data.get( "schema.id" ), 23 );
    assertEquals( data.get( "schema.name" ), "Rose" );
    assertEquals( data.get( "requestId" ), requestId );
    assertEquals( data.get( "channelAddCount" ), channelAddCount );
    assertEquals( data.get( "channelUpdateCount" ), channelUpdateCount );
    assertEquals( data.get( "channelRemoveCount" ), channelRemoveCount );
    assertEquals( data.get( "entityUpdateCount" ), entityUpdateCount );
    assertEquals( data.get( "entityRemoveCount" ), entityRemoveCount );
    assertEquals( data.get( "entityLinkCount" ), entityLinkCount );
    assertEquals( data.size(), 10 );
  }
}
