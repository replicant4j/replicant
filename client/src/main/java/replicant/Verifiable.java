package replicant;

import javax.annotation.Nonnull;

/**
 * Interface implemented by imitations that can verify their state.
 */
public interface Verifiable
{
  /**
   * Check the state of the entity and raise an exception if invalid or any related entities are invalid.
   *
   * @throws Exception if entity invalid.
   */
  void verify()
    throws Exception;

  /**
   * Verify they supplied object if it is verifiable.
   *
   * @param object the object to verify.
   * @throws Exception if entity invalid.
   */
  static void verify( @Nonnull final Object object )
    throws Exception
  {
    if ( object instanceof Verifiable )
    {
      ( (Verifiable) object ).verify();
    }
  }
}
