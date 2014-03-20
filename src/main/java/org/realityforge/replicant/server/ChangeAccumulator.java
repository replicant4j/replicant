package org.realityforge.replicant.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.replicant.server.transport.ReplicantSession;

/**
 * A simple class that collects Change instances and then on "completion" passes the messages to a client.
 */
public final class ChangeAccumulator
{
  private final Map<ReplicantSession, ChangeSet> _changeSets = new HashMap<ReplicantSession, ChangeSet>();

  /**
   * Add a message destined for a particular packet queue.
   *
   * @param session the session.
   * @param message the message.
   */
  public void addChange( final ReplicantSession session, final Change message )
  {
    getChangeSet( session ).merge( message );
  }

  /**
   * Add messages destined for a particular session.
   *
   * @param session  the session.
   * @param messages the messages.
   */
  public void addChanges( final ReplicantSession session, final Collection<Change> messages )
  {
    getChangeSet( session ).mergeAll( messages );
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
    for ( final Entry<ReplicantSession, ChangeSet> entry : _changeSets.entrySet() )
    {
      final ReplicantSession session = entry.getKey();
      final boolean isInitiator = session.getSessionID().equals( sessionID );
      impactsInitiator |= isInitiator;
      session.getQueue().addPacket( isInitiator ? requestID : null,
                                    null,
                                    new ArrayList<Change>( entry.getValue().getChanges() ) );
    }
    _changeSets.clear();

    return impactsInitiator;
  }

  private ChangeSet getChangeSet( @Nonnull final ReplicantSession session )
  {
    ChangeSet changeSet = _changeSets.get( session );
    if ( null == changeSet )
    {
      changeSet = new ChangeSet();
      _changeSets.put( session, changeSet );
    }
    return changeSet;
  }
}
