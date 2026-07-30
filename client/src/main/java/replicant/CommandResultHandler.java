package replicant;

import org.jspecify.annotations.Nullable;

/**
 * Receives the optional application result of a completed Command.
 */
public interface CommandResultHandler {
    void onCommandResult(@Nullable Object commandResult);
}
