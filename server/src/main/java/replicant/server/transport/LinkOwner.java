package replicant.server.transport;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

record LinkOwner(@Nullable EntityReference entityReference)
{
  @Nonnull
  private static final LinkOwner GRAPH = new LinkOwner( null );

  @Nonnull
  static LinkOwner graph()
  {
    return GRAPH;
  }

  @Nonnull
  static LinkOwner entity( final int entityTypeId, final int entityId )
  {
    return new LinkOwner( new EntityReference( entityTypeId, entityId ) );
  }

  boolean isGraphScoped()
  {
    return null == entityReference();
  }
}
