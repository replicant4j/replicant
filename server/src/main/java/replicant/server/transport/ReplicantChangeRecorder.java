package replicant.server.transport;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import replicant.server.EntityMessage;

public interface ReplicantChangeRecorder
{
  /**
   * Converts the given object into an appropriate {@link EntityMessage}.
   *
   * @param object   the source object to be converted; must not be null
   * @param isUpdate a boolean indicating if the conversion is for an update
   * @return the converted {@link EntityMessage}, or null if the conversion cannot be performed
   */
  @Nullable
  EntityMessage convertToEntityMessage( @Nonnull final Object object, final boolean isUpdate );
}
