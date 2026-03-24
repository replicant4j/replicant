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
  static LinkOwner entity( final int typeId, final int entityId )
  {
    return new LinkOwner( new EntityReference( typeId, entityId ) );
  }

  boolean isGraphScoped()
  {
    return null == entityReference();
  }
}
