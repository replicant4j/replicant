package replicant;

/**
 * Functional interface for returning a value.
 *
 * @param <T> The type of the returned value.
 */
@FunctionalInterface
public interface Function<T>
{
  /**
   * Return a value.
   *
   * @return the value generated by function.
   * @throws Throwable if unable to return value.
   */
  T call()
    throws Throwable;
}
