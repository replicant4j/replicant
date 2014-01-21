package org.realityforge.replicant.server.transport;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import org.realityforge.replicant.server.EntityMessage;

/**
 * A simple class that collects EntityMessage instances and then
 * on "completion" passes the messages to a client.
 */
public final class EntityMessageAccumulator
{
  private final Map<PacketQueue, LinkedList<EntityMessage>> _changeSets = new HashMap<>();

  /**
   * Add a message destined for a particular packet queue.
   *
   * @param queue the queue.
   * @param message the message.
   */
  public void addEntityMessage( final PacketQueue queue, final EntityMessage message )
  {
    getChangeSet( queue ).add( message );
  }

  /**
   * Complete the collection of messages and forward them to the client.
   */
  public void complete()
  {
    for ( final Entry<PacketQueue, LinkedList<EntityMessage>> entry : _changeSets.entrySet() )
    {
      entry.getKey().addPacket( entry.getValue() );
    }
    _changeSets.clear();
  }

  private LinkedList<EntityMessage> getChangeSet( final PacketQueue queue )
  {
    LinkedList<EntityMessage> clientChangeSet = _changeSets.get( queue );
    if ( null == clientChangeSet )
    {
      clientChangeSet = new LinkedList<>();
      _changeSets.put( queue, clientChangeSet );
    }
    return clientChangeSet;
  }
}
