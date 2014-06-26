package org.realityforge.replicant.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

public class EntityRepositoryDebugger
{
  protected static final Logger LOG = Logger.getLogger( EntityRepositoryDebugger.class.getName() );

  public void outputRepository( final EntityRepository repository )
  {
    final ArrayList<Class> types = repository.getTypes();
    for ( final Class type : types )
    {
      outputType( repository, type );
    }
  }

  protected void outputType( final EntityRepository repository, final Class type )
  {
    final ArrayList entityIDs = repository.findAllIDs( type );
    outputTypeHeader( type, entityIDs.size() );
    Collections.sort( entityIDs );
    for ( final Object entityID : entityIDs )
    {
      final Object entity = repository.findByID( type, entityID );
      outputEntity( type, entityID, entity );
    }
  }

  protected void outputTypeHeader( final Class type, final int count )
  {
    LOG.log( Level.INFO, type.getSimpleName() + ": " + count + " entities\n\n" );
  }

  protected void outputEntity( final Class type, final Object entityID, final Object entity )
  {
    LOG.log( Level.INFO, type.getSimpleName() + "/" + entityID + ":   " + entity );
  }
}
