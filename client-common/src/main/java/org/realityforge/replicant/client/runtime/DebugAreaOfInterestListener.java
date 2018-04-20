package org.realityforge.replicant.client.runtime;

import java.util.logging.Logger;
import javax.annotation.Nonnull;
import org.realityforge.replicant.client.Channel;

public class DebugAreaOfInterestListener
  implements AreaOfInterestListener
{
  private static final Logger LOG = Logger.getLogger( DebugAreaOfInterestListener.class.getName() );

  private final AreaOfInterestService _service;

  public DebugAreaOfInterestListener( final AreaOfInterestService service )
  {
    _service = service;
  }

  @Override
  public void channelCreated( @Nonnull final Channel channel )
  {
    LOG.warning( "channelCreated: " + channel );
    emitAreaOfInterest();
  }

  @Override
  public void channelUpdated( @Nonnull final Channel channel )
  {
    LOG.warning( "channelUpdated: " + channel );
    emitAreaOfInterest();
  }

  @Override
  public void channelDeleted( @Nonnull final Channel channel )
  {
    LOG.warning( "channelDeleted: " + channel );
    emitAreaOfInterest();
  }

  private void emitAreaOfInterest()
  {
    LOG.info( "\n\nAreaOfInterest Database\nSubscriptions: " );
    for ( final Channel s : _service.getSubscriptionsMap().values() )
    {
      LOG.info( "Channel: " + s );
    }
    LOG.info( "\n" );
  }
}
