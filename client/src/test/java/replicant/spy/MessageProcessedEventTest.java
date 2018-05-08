package replicant.spy;

import arez.Arez;
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
    final String requestId = ValueUtil.randomString();
    final int channelAddCount = ValueUtil.getRandom().nextInt( 10 );
    final int channelUpdateCount = ValueUtil.getRandom().nextInt( 10 );
    final int channelRemoveCount = ValueUtil.getRandom().nextInt( 10 );
    final int entityUpdateCount = ValueUtil.getRandom().nextInt( 100 );
    final int entityRemoveCount = ValueUtil.getRandom().nextInt( 100 );
    final int entityLinkCount = ValueUtil.getRandom().nextInt( 10 );
    final DataLoadStatus dataLoadStatus =
      new DataLoadStatus( "G",
                          sequence,
                          requestId,
                          channelAddCount,
                          channelUpdateCount,
                          channelRemoveCount,
                          entityUpdateCount,
                          entityRemoveCount,
                          entityLinkCount );
    final MessageProcessedEvent event =
      new MessageProcessedEvent( G.class, dataLoadStatus );

    assertEquals( event.getSystemType(), G.class );
    assertEquals( event.getDataLoadStatus(), dataLoadStatus );

    final HashMap<String, Object> data = new HashMap<>();
    Arez.context().safeAction( () -> event.toMap( data ) );

    assertEquals( data.get( "type" ), "DataLoader.MessageProcess" );
    assertEquals( data.get( "systemType" ), "G" );
    assertEquals( data.get( "sequence" ), sequence );
    assertEquals( data.get( "requestId" ), requestId );
    assertEquals( data.get( "channelAddCount" ), channelAddCount );
    assertEquals( data.get( "channelUpdateCount" ), channelUpdateCount );
    assertEquals( data.get( "channelRemoveCount" ), channelRemoveCount );
    assertEquals( data.get( "entityUpdateCount" ), entityUpdateCount );
    assertEquals( data.get( "entityRemoveCount" ), entityRemoveCount );
    assertEquals( data.get( "entityLinkCount" ), entityLinkCount );
    assertEquals( data.size(), 10 );
  }

  enum G
  {
    G1
  }
}
