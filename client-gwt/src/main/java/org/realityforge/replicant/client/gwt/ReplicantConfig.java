package org.realityforge.replicant.client.gwt;

import org.realityforge.gwt.propertysource.client.PropertySource;
import org.realityforge.gwt.propertysource.client.annotations.Namespace;

@Namespace( "replicant" )
public interface ReplicantConfig
  extends PropertySource
{
  boolean shouldValidateRepositoryOnLoad();

  boolean repositoryDebugOutputEnabled();

  boolean subscriptionsDebugOutputEnabled();

  boolean requestDebugOutputEnabled();
}
