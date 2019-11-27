package org.realityforge.replicant.client.runtime;

import javax.annotation.Nonnull;
import javax.inject.Inject;

public class TestReplicantClientSystemImpl
  extends ReplicantClientSystemImpl
{
  @Inject
  public TestReplicantClientSystemImpl( @Nonnull final DataLoaderEntry[] dataLoaderEntries )
  {
    setDataLoaders( dataLoaderEntries );
  }
}
