package org.realityforge.replicant.client.runtime;

import javax.annotation.Nonnull;
import javax.inject.Inject;

public class TestReplicantClientSystem
  extends ReplicantClientSystem
{
  @Inject
  TestReplicantClientSystem( @Nonnull final DataLoaderEntry[] dataLoaderEntries )
  {
    setDataLoaders( dataLoaderEntries );
  }
}
