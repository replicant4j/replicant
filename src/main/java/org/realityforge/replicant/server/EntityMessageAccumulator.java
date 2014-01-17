package org.realityforge.replicant.server;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

/**
 * A simple class that collects EntityMessage instances and then
 * on "completion" passes the messages to a client.
 *
 * @param <T> the ReplicantClient instance.
 */
public final class EntityMessageAccumulator<T extends ReplicantClient>
{
  private final Map<T, LinkedList<EntityMessage>> _changeSets = new HashMap<>();

  /**
   * Add a message to a client.
   *
   * @param client the client/
   * @param message the message.
   */
  public void addEntityMessage( final T client, final EntityMessage message )
  {
    getChangeSet( client ).add( message );
  }

  /**
   * Complete the collection of messages and forward them to the client.
   */
  public void complete()
  {
    for ( final Entry<T, LinkedList<EntityMessage>> entry : _changeSets.entrySet() )
    {
      entry.getKey().addChangeSet( entry.getValue() );
    }
    _changeSets.clear();
  }

  private LinkedList<EntityMessage> getChangeSet( final T info )
  {
    LinkedList<EntityMessage> clientChangeSet = _changeSets.get( info );
    if ( null == clientChangeSet )
    {
      clientChangeSet = new LinkedList<>();
      _changeSets.put( info, clientChangeSet );
    }
    return clientChangeSet;
  }
}
