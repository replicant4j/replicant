package org.realityforge.replicant.server;

import java.util.List;

public interface ReplicantClient
{
  void addChangeSet( List<EntityMessage> changeSet );
}
