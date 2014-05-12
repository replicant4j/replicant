package org.realityforge.replicant.client.json.gwt;

import org.realityforge.gwt.propertysource.client.PropertySource;
import org.realityforge.gwt.propertysource.client.annotations.Namespace;
import org.realityforge.gwt.propertysource.client.annotations.Property;

@Namespace("replicant")
public interface ReplicantConfig
  extends PropertySource
{
  @Property( "shouldValidateRepositoryOnLoad" )
  boolean shouldValidateRepositoryOnLoad();
}
