package org.realityforge.replicant.server.transport;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.replicant.server.EntityMessage;

/**
 * A simple class that collects EntityMessage instances and then
 * on "completion" passes the messages to a client.
 */
public final class EntityMessageAccumulator
{
  private final Map<ReplicantSession, LinkedList<EntityMessage>> _changeSets = new HashMap<>();

  /**
   * Add a message destined for a particular packet queue.
   *
   * @param session the session.
   * @param message the message.
   */
  public void addEntityMessage( final ReplicantSession session, final EntityMessage message )
  {
    getChangeSet( session ).add( message );
  }

  /**
   * Complete the collection of messages and forward them to the clients.
   *
   * @param sessionID the session that initiated the changes.
   * @param requestID the opaque identifier indicating the request that caused the changes.
   */
  public void complete( @Nullable final String sessionID, @Nullable final String requestID )
  {
    for ( final Entry<ReplicantSession, LinkedList<EntityMessage>> entry : _changeSets.entrySet() )
    {
      final ReplicantSession session = entry.getKey();
      session.getQueue().addPacket( session.getSessionID().equals( sessionID ) ? requestID : null, entry.getValue() );
    }
    _changeSets.clear();
  }

  private LinkedList<EntityMessage> getChangeSet( @Nonnull final ReplicantSession session )
  {
    LinkedList<EntityMessage> clientChangeSet = _changeSets.get( session );
    if ( null == clientChangeSet )
    {
      clientChangeSet = new LinkedList<>();
      _changeSets.put( session, clientChangeSet );
    }
    return clientChangeSet;
  }
}
