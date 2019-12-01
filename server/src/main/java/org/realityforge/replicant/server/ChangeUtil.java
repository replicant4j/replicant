package org.realityforge.replicant.server;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class ChangeUtil
{
  private ChangeUtil()
  {
  }

  @Nonnull
  public static List<Change> toChanges( @Nonnull final Collection<EntityMessage> messages,
                                        @Nonnull final ChannelDescriptor descriptor )
  {
    return toChanges( messages, descriptor.getChannelId(), descriptor.getSubChannelId() );
  }

  @Nonnull
  public static List<Change> toChanges( @Nonnull final Collection<EntityMessage> messages,
                                        final int channelID,
                                        @Nullable final Serializable subChannelId )
  {
    final ArrayList<Change> changes = new ArrayList<>( messages.size() );
    for ( final EntityMessage message : messages )
    {
      changes.add( new Change( message, channelID, subChannelId ) );
    }
    return changes;
  }
}
