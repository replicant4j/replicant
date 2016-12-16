package org.realityforge.replicant.server.transport;

import javax.annotation.Nonnull;
import org.realityforge.replicant.server.ChannelDescriptor;
import org.realityforge.ssf.SessionManager;

public interface ReplicantSessionManager<T extends ReplicantSession>
  extends SessionManager<T>
{
  @Nonnull
  ChannelMetaData getChannelMetaData( @Nonnull ChannelDescriptor descriptor );
}
