package org.realityforge.replicant.server.transport;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import org.realityforge.replicant.server.EntityMessage;

public final class ChangeUtil
{
  private ChangeUtil()
  {
  }

  public static List<Change> toChanges( final Collection<EntityMessage> messages,
                                        final int channelID,
                                        @Nullable final Serializable subChannelID )
  {
    final ArrayList<Change> changes = new ArrayList<Change>( messages.size() );
    for ( final EntityMessage message : messages )
    {
      changes.add( new Change( message, channelID, subChannelID ) );
    }
    return changes;
  }
}
