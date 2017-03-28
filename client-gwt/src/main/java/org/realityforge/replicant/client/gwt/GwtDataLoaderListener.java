package org.realityforge.replicant.client.gwt;

import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.user.client.rpc.InvocationException;
import com.google.web.bindery.event.shared.EventBus;
import java.util.Objects;
import javax.annotation.Nonnull;
import org.realityforge.replicant.client.transport.DataLoadStatus;
import org.realityforge.replicant.client.transport.DataLoaderListenerAdapter;

public class GwtDataLoaderListener<G extends Enum<G>>
  extends DataLoaderListenerAdapter<G>
{
  private final EventBus _eventBus;
  private final String _key;

  public GwtDataLoaderListener( @Nonnull final EventBus eventBus, @Nonnull final String key )
  {
    _eventBus = Objects.requireNonNull( eventBus );
    _key = Objects.requireNonNull( key );
  }

  @Override
  public void onConnect()
  {
    fireEvent( new ConnectEvent( _key ) );
  }

  @Override
  public void onInvalidConnect( @Nonnull final Throwable throwable )
  {
    fireEvent( new InvalidConnectEvent( _key, toCause( throwable ) ) );
  }

  @Override
  public void onDisconnect()
  {
    fireEvent( new DisconnectEvent( _key ) );
  }

  @Override
  public void onInvalidDisconnect( @Nonnull final Throwable throwable )
  {
    fireEvent( new InvalidDisconnectEvent( _key, toCause( throwable ) ) );
  }

  @Override
  public void onDataLoadComplete( @Nonnull final DataLoadStatus status )
  {
    fireEvent( new DataLoadCompleteEvent( status ) );
  }

  @Override
  public void onDataLoadFailure( @Nonnull final Throwable throwable )
  {
    fireEvent( new DataLoadFailureEvent(_key, throwable ) );
  }

  @Override
  public void onPollFailure( @Nonnull final Throwable throwable )
  {
    fireEvent( new PollFailureEvent(_key, throwable ) );
  }

  protected Throwable toCause( final @Nonnull Throwable caught )
  {
    return caught instanceof InvocationException ? caught.getCause() : caught;
  }

  protected void fireEvent( final GwtEvent<?> event )
  {
    _eventBus.fireEvent( event );
  }
}
