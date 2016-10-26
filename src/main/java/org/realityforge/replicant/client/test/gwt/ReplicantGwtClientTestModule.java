package org.realityforge.replicant.client.test.gwt;

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.event.shared.SimpleEventBus;
import org.realityforge.guiceyloops.shared.AbstractModule;

/**
 * Module containing gwt specific client services.
 */
public final class ReplicantGwtClientTestModule
  extends AbstractModule
{
  @Override
  protected void configure()
  {
    bindSingleton( EventBus.class, SimpleEventBus.class );
  }
}
