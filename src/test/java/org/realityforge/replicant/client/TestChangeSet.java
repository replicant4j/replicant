package org.realityforge.replicant.client;

final class TestChangeSet
  implements ChangeSet
{
  private final int _sequence;
  private final Change[] _changes;

  TestChangeSet( final int sequence, final Change[] changes )
  {
    _sequence = sequence;
    _changes = changes;
  }

  @Override
  public int getSequence()
  {
    return _sequence;
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
