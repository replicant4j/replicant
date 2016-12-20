package org.realityforge.replicant.server.transport;

import javax.annotation.Nonnull;
import org.realityforge.replicant.server.ChannelDescriptor;
import org.realityforge.ssf.SessionManager;

public interface ReplicantSessionManager
  extends SessionManager<ReplicantSession>
{
  @Nonnull
  ChannelMetaData getChannelMetaData( @Nonnull ChannelDescriptor descriptor );

  @Nonnull
  ChannelMetaData getChannelMetaData( int channelID );
}
