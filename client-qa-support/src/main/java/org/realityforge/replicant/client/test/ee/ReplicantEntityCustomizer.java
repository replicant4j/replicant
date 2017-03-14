package org.realityforge.replicant.client.test.ee;

import java.lang.reflect.Field;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.persistence.EntityManager;
import org.eclipse.persistence.descriptors.ClassDescriptor;
import org.eclipse.persistence.descriptors.DescriptorEvent;
import org.eclipse.persistence.descriptors.DescriptorEventAdapter;
import org.eclipse.persistence.sessions.Session;
import org.realityforge.replicant.client.EntitySystem;

/**
 * A class to inject _$entitySystem into replicant entities.
 * Uses eclipselink specific listener infrastructure.
 */
public class ReplicantEntityCustomizer
  extends DescriptorEventAdapter
{
  private final EntitySystem _entitySystem;

  public ReplicantEntityCustomizer( @Nonnull final EntitySystem entitySystem )
  {
    _entitySystem = entitySystem;
  }

  public static void configure( @Nonnull final EntityManager em, @Nonnull final EntitySystem entitySystem )
  {
    final Session session = em.unwrap( Session.class );
    for ( final Map.Entry<Class, ClassDescriptor> entry : session.getDescriptors().entrySet() )
    {
      entry.getValue().getEventManager().
        addDefaultEventListener( new ReplicantEntityCustomizer( entitySystem ) );
    }
  }

  @Override
  public void postBuild( final DescriptorEvent event )
  {
    configureEntitySystem( event.getObject() );
  }

  @Override
  public void postMerge( final DescriptorEvent event )
  {
    configureEntitySystem( event.getObject() );
  }

  @Override
  public void postRefresh( final DescriptorEvent event )
  {
    configureEntitySystem( event.getObject() );
  }

  @Override
  public void preUpdate( final DescriptorEvent event )
  {
    configureEntitySystem( event.getObject() );
  }

  @Override
  public void prePersist( final DescriptorEvent event )
  {
    configureEntitySystem( event.getObject() );
  }

  private void configureEntitySystem( final Object object )
  {
    try
    {
      final Field field = object.getClass().getDeclaredField( "_$entitySystem" );
      field.setAccessible( true );
      field.set( object, _entitySystem );
    }
    catch ( final IllegalAccessException e )
    {
      throw new IllegalStateException( "Unable to set field _$entitySystem", e );
    }
    catch ( final NoSuchFieldException ignored )
    {
    }
  }
}
