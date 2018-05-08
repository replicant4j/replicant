package org.realityforge.replicant.client.test;

import org.realityforge.guiceyloops.shared.AbstractModule;
import org.realityforge.replicant.client.transport.ReplicantClientSystem;

/**
 * Module containing all the common client services.
 */
public class ReplicantClientTestModule
  extends AbstractModule
{
  @Override
  protected void configure()
  {
    bindMock( ReplicantClientSystem.class );
  }
}
