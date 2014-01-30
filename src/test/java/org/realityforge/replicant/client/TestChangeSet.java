package org.realityforge.replicant.client;

import javax.annotation.Nullable;

final class TestChangeSet
  implements ChangeSet
{
  private final int _sequence;
  private final String _jobID;
  private final Change[] _changes;

  TestChangeSet( final int sequence, @Nullable final String jobID, final Change[] changes )
  {
    _sequence = sequence;
    _jobID = jobID;
    _changes = changes;
  }

  @Override
  public int getSequence()
  {
    return _sequence;
  }

  @Override
  public String getJobID()
  {
    return _jobID;
  }

  @Override
  public int getChangeCount()
  {
    return _changes.length;
  }

  @Override
  public Change getChange( final int index )
  {
    return _changes[ index ];
  }

  @Override
    public String toString()
    {
      return "ChangeSet:" + System.identityHashCode( this );
    }
}
