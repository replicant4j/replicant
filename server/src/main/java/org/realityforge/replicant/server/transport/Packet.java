package org.realityforge.replicant.server.transport;

import java.util.Collection;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.replicant.server.ChangeSet;
import org.realityforge.replicant.server.EntityMessage;

/**
 * Packet contains the data generated from the transaction that needs to be sent to a specific client.
 * This packet has not been fully resolved and is just used to pass the data to another thread that will perform
 * "expandLinks" operation.
 */
final class Packet
{
  /**
   * The request that resulted in this change when packet is being sent back to the initiator.
   */
  @Nullable
  private final Integer _requestId;
  /**
   * If request was a subscription and this subscription is a single graph that is cacheable then this
   * etag will contain the cache constant.
   */
  @Nullable
  private final String _etag;
  /**
   * The change messages that were collected during the transaction.
   */
  @Nonnull
  private final Collection<EntityMessage> _messages;
  /**
   * Empty ChangeSet unless packet is directed at the request initiator in which case it was
   * whatever was part of the session changes.
   */
  @Nonnull
  private final ChangeSet _changeSet;

  Packet( @Nullable final Integer requestId,
          @Nullable final String etag,
          @Nonnull final Collection<EntityMessage> messages,
          @Nonnull final ChangeSet changeSet )
  {
    assert null == etag || null != requestId;
    assert !changeSet.hasContent() || null != requestId;
    _requestId = requestId;
    _etag = etag;
    _messages = Objects.requireNonNull( messages );
    _changeSet = Objects.requireNonNull( changeSet );
  }

  @Nullable
  Integer getRequestId()
  {
    return _requestId;
  }

  @Nullable
  String getEtag()
  {
    return _etag;
  }

  @Nonnull
  Collection<EntityMessage> getMessages()
  {
    return _messages;
  }

  @Nonnull
  ChangeSet getChangeSet()
  {
    return _changeSet;
  }
}
