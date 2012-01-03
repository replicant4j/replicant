package org.realityforge.replicant.client.json.gwt;

import java.util.ArrayList;
import java.util.HashSet;
import javax.annotation.Nonnull;
import javax.inject.Inject;
import org.realityforge.replicant.client.Change;
import org.realityforge.replicant.client.ChangeMapper;
import org.realityforge.replicant.client.ChangeSet;
import org.realityforge.replicant.client.Linkable;

public class GwtJsonDecoder
{
  private final ChangeMapper _changeMapper;

  @Inject
  protected GwtJsonDecoder( @Nonnull final ChangeMapper changeMapper )
  {
    _changeMapper = changeMapper;
  }

  public final int apply( final ChangeSet changeSet )
  {
    final int size = changeSet.getChangeCount();
    final ArrayList<Linkable> updatedEntities = new ArrayList<Linkable>( size );
    final HashSet<Linkable> removedEntities = new HashSet<Linkable>( size );
    for ( int i = 0; i < size; i++ )
    {
      final Change change = changeSet.getChange( i );
      final Object entity = _changeMapper.applyChange( change );
      //Is the entity a update and is it linkable?
      if ( entity instanceof Linkable )
      {
        if ( change.isUpdate() )
        {
          updatedEntities.add( (Linkable) entity );
        }
        else
        {
          removedEntities.add( (Linkable) entity );
        }
      }
    }
    for ( final Linkable entity : updatedEntities )
    {
      // In some circumstances a create and remove can appear in same change set so guard against this
      if ( !removedEntities.contains( entity ) )
      {
        entity.link();
      }
    }
    return changeSet.getSequence();
  }
}
