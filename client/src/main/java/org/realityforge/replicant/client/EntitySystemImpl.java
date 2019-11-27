package org.realityforge.replicant.client;

import javax.annotation.Nonnull;

public class EntitySystemImpl
  implements EntitySystem
{
  private final EntityRepository _repository;
  private final EntityChangeBroker _changeBroker;
  private final EntitySubscriptionManager _subscriptionManager;

  public EntitySystemImpl()
  {
    this( new EntityRepositoryImpl(), new EntityChangeBrokerImpl(), new EntitySubscriptionManagerImpl() );
  }

  public EntitySystemImpl( @Nonnull final EntityRepository repository,
                           @Nonnull final EntityChangeBroker changeBroker,
                           @Nonnull final EntitySubscriptionManager subscriptionManager )
  {
    _repository = repository;
    _changeBroker = changeBroker;
    _subscriptionManager = subscriptionManager;
  }

  @Nonnull
  @Override
  public EntityRepository getRepository()
  {
    return _repository;
  }

  @Nonnull
  @Override
  public EntityChangeBroker getChangeBroker()
  {
    return _changeBroker;
  }

  @Nonnull
  @Override
  public EntitySubscriptionManager getSubscriptionManager()
  {
    return _subscriptionManager;
  }
}
