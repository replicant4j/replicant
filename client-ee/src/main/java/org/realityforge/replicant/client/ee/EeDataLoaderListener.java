package org.realityforge.replicant.client.ee;

import java.lang.annotation.Annotation;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.enterprise.inject.spi.BeanManager;
import org.realityforge.replicant.client.transport.DataLoadStatus;
import org.realityforge.replicant.client.transport.DataLoaderListenerAdapter;

public class EeDataLoaderListener<G extends Enum<G>>
  extends DataLoaderListenerAdapter<G>
{
  private final BeanManager _beanManager;
  private final Annotation _eventQualifier;
  private final String _key;

  public EeDataLoaderListener( @Nonnull final BeanManager beanManager,
                               @Nonnull final Annotation eventQualifier,
                               @Nonnull final String key )
  {
    _beanManager = Objects.requireNonNull( beanManager );
    _eventQualifier = Objects.requireNonNull( eventQualifier );
    _key = Objects.requireNonNull( key );
  }

  @Override
  public void onDataLoadComplete( @Nonnull final DataLoadStatus status )
  {
    fireEvent( new DataLoadCompleteEvent( status ) );
  }

  @Override
  public void onConnect()
  {
    fireEvent( new ConnectEvent( _key ) );
  }

  @Override
  public void onInvalidConnect( @Nonnull final Throwable exception )
  {
    fireEvent( new InvalidConnectEvent( _key, exception ) );
  }

  @Override
  public void onDisconnect()
  {
    fireEvent( new DisconnectEvent( _key ) );
  }

  @Override
  public void onInvalidDisconnect( @Nonnull final Throwable exception )
  {
    fireEvent( new InvalidDisconnectEvent( _key, exception ) );
  }

  @Override
  public void onPollFailure( @Nonnull final Throwable exception )
  {
    fireEvent( new PollErrorEvent( _key, exception ) );
  }

  @Override
  public void onDataLoadFailure( @Nonnull final Throwable e )
  {
    fireEvent( new DataLoadFailureEvent( _key, e ) );
  }

  protected void fireEvent( @Nonnull final Object event )
  {
    _beanManager.fireEvent( event, _eventQualifier );
  }
}
