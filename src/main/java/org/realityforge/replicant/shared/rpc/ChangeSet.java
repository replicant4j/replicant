package org.realityforge.replicant.shared.rpc;

import java.io.Serializable;
import javax.annotation.Nonnull;

public final class ChangeSet
  implements Serializable
{
  private long _changeSetID;
  private Change[] _changes;

  @SuppressWarnings( { "UnusedDeclaration" } )
  private ChangeSet()
  {
  }

  public ChangeSet( final long changeSetID, @Nonnull final Change[] changes )
  {
    _changeSetID = changeSetID;
    _changes = changes;
  }

  public long getChangeSetID()
  {
    return _changeSetID;
  }

  @Nonnull
  public Change[] getChanges()
  {
    return _changes;
  }
}
