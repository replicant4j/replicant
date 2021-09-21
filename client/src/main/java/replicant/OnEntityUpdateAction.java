package replicant;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Interface invoked after processing a MessageResponse.
 */
public interface OnEntityUpdateAction
{
  void onEntityUpdate( @Nonnull ReplicantContext context, @Nonnull Object entity );
}
