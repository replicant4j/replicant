package replicant.server;

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
                                        @Nonnull final ChannelAddress address )
  {
    return toChanges( messages, address.channelId(), address.rootId() );
  }

  @Nonnull
  public static List<Change> toChanges( @Nonnull final Collection<EntityMessage> messages,
                                        final int channelId,
                                        @Nullable final Integer rootId )
  {
    final List<Change> changes = new ArrayList<>( messages.size() );
    for ( final EntityMessage message : messages )
    {
      changes.add( new Change( message, channelId, rootId ) );
    }
    return changes;
  }
}
