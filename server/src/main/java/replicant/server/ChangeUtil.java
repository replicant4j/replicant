package replicant.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

public final class ChangeUtil
{
  private ChangeUtil()
  {
  }

  @Nonnull
  public static List<Change> toChanges( @Nonnull final Collection<EntityMessage> messages,
                                        @Nonnull final ChannelAddress address )
  {
    return
      messages
        .stream()
        .map( message -> new Change( message, address ) )
        .collect( Collectors.toCollection( () -> new ArrayList<>( messages.size() ) ) );
  }
}
