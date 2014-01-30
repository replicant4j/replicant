package org.realityforge.replicant.server.transport;

import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.realityforge.replicant.server.EntityMessage;

/**
 * The server-side representation of a ChangeSet.
 */
public final class Packet
  implements Comparable
{
  private final int _sequence;
  @Nullable
  private final String _requestID;
  @Nonnull
  private final List<EntityMessage> _changes;

  public Packet( final int sequence,
                 @Nullable final String requestID,
                 @Nonnull final List<EntityMessage> changes )
  {
    _sequence = sequence;
    _requestID = requestID;
    _changes = changes;
  }

  public int getSequence()
  {
    return _sequence;
  }

  @Nullable
  public String getRequestID()
  {
    return _requestID;
  }

  @Nonnull
  public List<EntityMessage> getChanges()
  {
    return _changes;
  }

  /**
   * {@inheritDoc}
   */
  public boolean equals( final Object other )
  {
    if ( !( other instanceof Packet ) )
    {
      return false;
    }
    else
    {
      final Packet packet = (Packet) other;
      return _sequence == packet.getSequence();
    }
  }

  /**
   * {@inheritDoc}
   */
  public int compareTo( @Nonnull final Object other )
  {
    if ( !( other instanceof Packet ) )
    {
      return -1;
    }

    final Packet packet = (Packet) other;
    final int sequence = packet.getSequence();
    if ( _sequence == sequence )
    {
      return 0;
    }
    else if ( isLessThan( sequence ) )
    {
      return -1;
    }
    else
    {
      return 1;
    }
  }

  /**
   * Return true if sequence is less than to this packets sequence
   * number accounting for wraparound.
   *
   * @param sequence a sequence
   */
  public boolean isLessThan( final int sequence )
  {
    return isLessThan( _sequence, sequence );
  }

  /**
   * Return true if sequence is less than or equal to this packets sequence
   * number accounting for wraparound.
   *
   * @param sequence a sequence
   */
  public boolean isLessThanOrEqual( final int sequence )
  {
    return _sequence == sequence || isLessThan( _sequence, sequence );
  }

  /**
   * Return true if seq1 is less than seq2.
   *
   * @param seq1 the first sequence.
   * @param seq2 the second sequence.
   * @return true if seq1 is less than seq2.
   */
  private boolean isLessThan( final int seq1, final int seq2 )
  {
    return seq1 < seq2;
  }

  /**
   * Return true if sequence is next after this packets sequence accounting for
   * wrapping.
   *
   * @param sequence the sequence.
   */
  public final boolean isNext( final int sequence )
  {
    final int next = _sequence + 1;
    return next == sequence;
  }

  /**
   * Return true if sequence is before after this packets sequence accounting for
   * wrapping.
   *
   * @param sequence the sequence.
   */
  public final boolean isPrevious( final int sequence )
  {
    return _sequence - 1 == sequence;
  }

  /**
   * {@inheritDoc}
   */
  public int hashCode()
  {
    return _sequence;
  }

  /**
   * {@inheritDoc}
   */
  public String toString()
  {
    return "PacketPacket[Sequence=" + _sequence + "]";
  }
}
