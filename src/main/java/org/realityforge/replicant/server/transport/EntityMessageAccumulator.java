package org.realityforge.replicant.server.transport;

import java.util.Collection;
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
  private final Map<ReplicantSession, LinkedList<EntityMessage>> _changeSets = new HashMap<ReplicantSession, LinkedList<EntityMessage>>();

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
   * Add messages destined for a particular session.
   *
   * @param session  the session.
   * @param messages the messages.
   */
  public void addEntityMessages( final ReplicantSession session, final Collection<EntityMessage> messages )
  {
    final LinkedList<EntityMessage> changeSet = getChangeSet( session );
    for ( final EntityMessage message : messages )
    {
      changeSet.add( message );
    }
  }

  /**
   * Complete the collection of messages and forward them to the clients.
   *
   * @param sessionID the session that initiated the changes.
   * @param requestID the opaque identifier indicating the request that caused the changes.
   * @return true if a change set was send to the originating session
   */
  public boolean complete( @Nullable final String sessionID, @Nullable final String requestID )
  {
    boolean impactsInitiator = false;
    for ( final Entry<ReplicantSession, LinkedList<EntityMessage>> entry : _changeSets.entrySet() )
    {
      final ReplicantSession session = entry.getKey();
      final boolean isInitiator = session.getSessionID().equals( sessionID );
      impactsInitiator |= isInitiator;
      session.getQueue().addPacket( isInitiator ? requestID : null, null, entry.getValue() );
    }
    _changeSets.clear();

    return impactsInitiator;
  }

  private LinkedList<EntityMessage> getChangeSet( @Nonnull final ReplicantSession session )
  {
    LinkedList<EntityMessage> clientChangeSet = _changeSets.get( session );
    if ( null == clientChangeSet )
    {
      clientChangeSet = new LinkedList<EntityMessage>();
      _changeSets.put( session, clientChangeSet );
    }
    return clientChangeSet;
  }
}
