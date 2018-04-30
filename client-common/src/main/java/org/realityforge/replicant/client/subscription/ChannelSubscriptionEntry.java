package org.realityforge.replicant.client.subscription;

import arez.Disposable;
import arez.annotations.ArezComponent;
import arez.annotations.Feature;
import arez.annotations.Observable;
import arez.component.AbstractContainer;
import arez.component.RepositoryUtil;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;
import org.realityforge.replicant.client.Channel;

/**
 * Representation of a subscription to a channel.
 */
@ArezComponent
public abstract class ChannelSubscriptionEntry
  extends AbstractContainer<Class<?>, Map<Object, Entity>>
{
  @Nonnull
  private final Channel _channel;

  private final Map<Class<?>, Map<Object, Entity>> _entities = new HashMap<>();

  public static ChannelSubscriptionEntry create( @Nonnull final Channel channel, final boolean explicitSubscription )
  {
    return new Arez_ChannelSubscriptionEntry( channel, explicitSubscription );
  }

  ChannelSubscriptionEntry( @Nonnull final Channel channel )
  {
    _channel = Objects.requireNonNull( channel );
  }

  @Nonnull
  public Channel getChannel()
  {
    return _channel;
  }

  @Observable( initializer = Feature.ENABLE )
  public abstract boolean isExplicitSubscription();

  public abstract void setExplicitSubscription( boolean explicitSubscription );

  @Nonnull
  public List<Map<Object, Entity>> getEntitySubscriptionEntries()
  {
    return RepositoryUtil.asList( entities() );
  }

  public Map<Class<?>, Map<Object, Entity>> getEntities()
  {
    return _entities;
  }

  final Map<Class<?>, Map<Object, Entity>> getRwEntities()
  {
    return _entities;
  }

  @Override
  protected void preDispose()
  {
    disposeUnOwnedEntities();
    super.preDispose();
  }

  private void disposeUnOwnedEntities()
  {
    _entities.values().stream().flatMap( entitySet -> entitySet.values().stream() ).forEach( entityEntry -> {
      final ChannelSubscriptionEntry element = entityEntry.deregisterChannel( getChannel().getAddress() );
      assert null != element;
      if ( entityEntry.getChannelSubscriptions().isEmpty() )
      {
        final Object userObject = entityEntry.getUserObject();
        assert null != userObject;
        Disposable.dispose( userObject );
      }
    } );
  }
}
