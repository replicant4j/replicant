package org.realityforge.replicant.client;

import dagger.Module;
import dagger.Provides;
import javax.annotation.Nonnull;
import javax.inject.Singleton;

@Module( includes = { EntitySubscriptionManagerDaggerModule.class } )
public interface ReplicantClientDaggerModule
{
}
