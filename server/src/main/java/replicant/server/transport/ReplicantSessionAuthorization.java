package replicant.server.transport;

import java.io.IOException;
import javax.annotation.Nonnull;

public interface ReplicantSessionAuthorization
{
  boolean runIfValid( @Nonnull Action action )
    throws IOException;

  @Nonnull
  Object getPrincipal();

  void touchActivity();

  void close();

  @FunctionalInterface
  interface Action
  {
    void run()
      throws IOException;
  }
}
