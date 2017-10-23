package org.realityforge.replicant.client.test.ee;

import java.lang.reflect.Field;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.persistence.EntityManager;
import org.eclipse.persistence.descriptors.ClassDescriptor;
import org.eclipse.persistence.descriptors.DescriptorEvent;
import org.eclipse.persistence.descriptors.DescriptorEventAdapter;
import org.eclipse.persistence.sessions.Session;
import org.realityforge.replicant.client.EntityLocator;

/**
 * A class to inject _$entityLocator into replicant entities.
 * Uses eclipselink specific listener infrastructure.
 */
public class ReplicantEntityCustomizer
  extends DescriptorEventAdapter
{
  private final EntityLocator _entityLocator;

  public ReplicantEntityCustomizer( @Nonnull final EntityLocator entityLocator )
  {
    _entityLocator = entityLocator;
  }

  public static void configure( @Nonnull final EntityManager em, @Nonnull final EntityLocator entityLocator )
  {
    final Session session = em.unwrap( Session.class );
    for ( final Map.Entry<Class, ClassDescriptor> entry : session.getDescriptors().entrySet() )
    {
      entry.getValue().getEventManager().
        addDefaultEventListener( new ReplicantEntityCustomizer( entityLocator ) );
    }
  }

  @Override
  public void postBuild( final DescriptorEvent event )
  {
    configureEntityLocator( event.getObject() );
  }

  @Override
  public void postMerge( final DescriptorEvent event )
  {
    configureEntityLocator( event.getObject() );
  }

  @Override
  public void postRefresh( final DescriptorEvent event )
  {
    configureEntityLocator( event.getObject() );
  }

  @Override
  public void preUpdate( final DescriptorEvent event )
  {
    configureEntityLocator( event.getObject() );
  }

  @Override
  public void prePersist( final DescriptorEvent event )
  {
    configureEntityLocator( event.getObject() );
  }

  private void configureEntityLocator( final Object object )
  {
    try
    {
      final Field field = object.getClass().getDeclaredField( "_$entityLocator" );
      field.setAccessible( true );
      field.set( object, _entityLocator );
    }
    catch ( final IllegalAccessException e )
    {
      throw new IllegalStateException( "Unable to set field _$entityLocator", e );
    }
    catch ( final NoSuchFieldException ignored )
    {
    }
  }
}
