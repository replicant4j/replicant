package replicant;

/**
 * Receives the optional application result of a completed Command.
 */
public interface CommandResultHandler {
    void onCommandResult(Object commandResult);
}
