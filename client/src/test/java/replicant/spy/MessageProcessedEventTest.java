package replicant.spy;

import java.util.HashMap;
import org.realityforge.guiceyloops.shared.ValueUtil;
import org.testng.annotations.Test;
import replicant.AbstractReplicantTest;
import static org.testng.Assert.*;

public class MessageProcessedEventTest
  extends AbstractReplicantTest
{
  @Test
  public void basicOperation()
  {
    final int sequence = ValueUtil.randomInt();
    final int requestId = ValueUtil.randomInt();
    final int channelAddCount = ValueUtil.getRandom().nextInt( 10 );
    final int channelUpdateCount = ValueUtil.getRandom().nextInt( 10 );
    final int channelRemoveCount = ValueUtil.getRandom().nextInt( 10 );
    final int entityUpdateCount = ValueUtil.getRandom().nextInt( 100 );
    final int entityRemoveCount = ValueUtil.getRandom().nextInt( 100 );
    final int entityLinkCount = ValueUtil.getRandom().nextInt( 10 );
    final DataLoadStatus dataLoadStatus =
      new DataLoadStatus( sequence,
                          requestId,
                          channelAddCount,
                          channelUpdateCount,
                          channelRemoveCount,
                          entityUpdateCount,
                          entityRemoveCount,
                          entityLinkCount );
    final MessageProcessedEvent event =
      new MessageProcessedEvent( 23, "Rose", dataLoadStatus );

    assertEquals( event.getSchemaId(), 23 );
    assertEquals( event.getSchemaName(), "Rose" );
    assertEquals( event.getDataLoadStatus(), dataLoadStatus );

    final HashMap<String, Object> data = new HashMap<>();
    event.toMap( data );

    assertEquals( data.get( "type" ), "Connector.MessageProcess" );
    assertEquals( data.get( "schema.id" ), 23 );
    assertEquals( data.get( "schema.name" ), "Rose" );
    assertEquals( data.get( "sequence" ), sequence );
    assertEquals( data.get( "requestId" ), requestId );
    assertEquals( data.get( "channelAddCount" ), channelAddCount );
    assertEquals( data.get( "channelUpdateCount" ), channelUpdateCount );
    assertEquals( data.get( "channelRemoveCount" ), channelRemoveCount );
    assertEquals( data.get( "entityUpdateCount" ), entityUpdateCount );
    assertEquals( data.get( "entityRemoveCount" ), entityRemoveCount );
    assertEquals( data.get( "entityLinkCount" ), entityLinkCount );
    assertEquals( data.size(), 11 );
  }
}
