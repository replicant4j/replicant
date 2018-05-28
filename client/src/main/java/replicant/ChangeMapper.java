package replicant;

import javax.annotation.Nonnull;
import replicant.messages.EntityChangeData;

public interface ChangeMapper
{
  @Nonnull
  Object createEntity( @Nonnull EntitySchema entitySchema, int id, @Nonnull EntityChangeData data );

  void updateEntity( @Nonnull EntitySchema entitySchema, @Nonnull Object entity, @Nonnull EntityChangeData data );
}
