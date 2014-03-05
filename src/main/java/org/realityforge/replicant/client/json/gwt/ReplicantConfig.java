package org.realityforge.replicant.client.json.gwt;

import org.realityforge.gwt.propertysource.client.PropertySource;
import org.realityforge.gwt.propertysource.client.annotations.Namespace;
import org.realityforge.gwt.propertysource.client.annotations.Property;

@Namespace("foo")
public interface ReplicantConfig
  extends PropertySource
{
  @Property( "replicant.shouldValidateRepositoryOnLoad" )
  boolean shouldValidateRepositoryOnLoad();
}
