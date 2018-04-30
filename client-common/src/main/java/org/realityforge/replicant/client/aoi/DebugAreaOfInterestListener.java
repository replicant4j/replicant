package org.realityforge.replicant.client.aoi;

import java.util.logging.Logger;
import javax.annotation.Nonnull;

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
  public void areaOfInterestCreated( @Nonnull final AreaOfInterest areaOfInterest )
  {
    LOG.warning( "areaOfInterestCreated: " + areaOfInterest );
    emitAreaOfInterest();
  }

  @Override
  public void areaOfInterestUpdated( @Nonnull final AreaOfInterest areaOfInterest )
  {
    LOG.warning( "areaOfInterestUpdated: " + areaOfInterest );
    emitAreaOfInterest();
  }

  @Override
  public void areaOfInterestDeleted( @Nonnull final AreaOfInterest areaOfInterest )
  {
    LOG.warning( "areaOfInterestDeleted: " + areaOfInterest );
    emitAreaOfInterest();
  }

  private void emitAreaOfInterest()
  {
    LOG.info( "\n\nAreaOfInterest Database\nSubscriptions: " );
    for ( final AreaOfInterest s : _service.getAreasOfInterest() )
    {
      LOG.info( "Channel Subscription: " + s );
    }
    LOG.info( "\n" );
  }
}
