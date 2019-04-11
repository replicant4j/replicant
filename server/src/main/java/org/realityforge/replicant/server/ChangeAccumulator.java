package org.realityforge.replicant.server;

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
  private final Map<ReplicantSession, ChangeSet> _changeSets = new HashMap<>();

  public void addActions( @Nonnull final ReplicantSession session, @Nonnull final Collection<ChannelAction> actions )
  {
    final ChangeSet changeSet = getChangeSet( session );
    for ( final ChannelAction action : actions )
    {
      changeSet.mergeAction( action );
    }
  }

  /**
   * Add a message destined for a particular packet queue.
   *
   * @param session the session.
   * @param message the message.
   */
  public void addChange( @Nonnull final ReplicantSession session, @Nonnull final Change message )
  {
    getChangeSet( session ).merge( message );
  }

  /**
   * Add messages destined for a particular session.
   *
   * @param session  the session.
   * @param messages the messages.
   */
  public void addChanges( @Nonnull final ReplicantSession session, @Nonnull final Collection<Change> messages )
  {
    getChangeSet( session ).mergeAll( messages );
  }

  /**
   * Complete the collection of messages and forward them to the clients.
   *
   * @param initiator the session that initiated the changes.
   * @param requestId the opaque identifier indicating the request that caused the changes.
   * @return true if a change set was send to the originating session
   */
  public boolean complete( @Nullable final ReplicantSession initiator, @Nullable final Integer requestId )
  {
    boolean impactsInitiator = false;
    for ( final Entry<ReplicantSession, ChangeSet> entry : _changeSets.entrySet() )
    {
      final ReplicantSession session = entry.getKey();
      final boolean isInitiator = session == initiator;
      final ChangeSet changeSet = entry.getValue();
      if ( !changeSet.getChannelActions().isEmpty() || !changeSet.getChanges().isEmpty() )
      {
        impactsInitiator |= isInitiator;
        session.sendPacket( isInitiator ? requestId : null, null, changeSet );
      }
    }
    _changeSets.clear();

    return impactsInitiator;
  }

  /**
   * Return a changeset for specified session, creating one if none yet exists.
   *
   * @param session the session.
   * @return the changeSet.
   */
  @Nonnull
  public final ChangeSet getChangeSet( @Nonnull final ReplicantSession session )
  {
    return _changeSets.computeIfAbsent( session, k -> new ChangeSet() );
  }
}
