package replicant.server.ee;

import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A mechanism for passing context data from the servlet/ejb tier to the ejb tier.
 *
 * <p>The expectation is that an interceptor in the ejb tier will inspect and utilize
 * the context data. The implementation uses thread-locals as it assumes that at least
 * the first interceptor will be invoked in the thread that initiates the request.</p>
 */
public final class ReplicantContextHolder
{
  @Nonnull
  private static final ThreadLocal<Map<String, Object>> c_context = new ThreadLocal<>();

  private ReplicantContextHolder()
  {
  }

  /**
   * Specify some context data for a particular key.
   *
   * @param key  the key.
   * @param data the data.
   */
  public static void put( @Nonnull final String key, @Nullable final Object data )
  {
    if ( null == data )
    {
      getRawContext().remove( key );
    }
    else
    {
      getRawContext().put( key, data );
    }
  }

  /**
   * Put all specified context data.
   *
   * @param data the data.
   */
  static void putAll( @Nonnull final Map<String, Object> data )
  {
    getRawContext().putAll( data );
  }

  /**
   * Return a copy of the current ReplicantContext.
   */
  @Nonnull
  public static Map<String, Object> getContext()
  {
    return new HashMap<>( getRawContext() );
  }

  /**
   * Return the underlying ReplicantContext without copying.
   */
  private static Map<String, Object> getRawContext()
  {
    if ( null == c_context.get() )
    {
      c_context.set( new HashMap<>() );
    }

    return c_context.get();
  }

  /**
   * Remove some context data for a particular key.
   *
   * @param key the key.
   * @return the value that was removed if any.
   */
  @Nullable
  static Object remove( @Nonnull final String key )
  {
    final Map<String, Object> map = c_context.get();
    return null != map ? map.remove( key ) : null;
  }

  /**
   * Retrieve context data specified for key.
   *
   * @param key the key.
   * @return the context data if any, else null.
   */
  @Nullable
  public static Object get( @Nonnull final String key )
  {
    final Map<String, Object> map = c_context.get();
    return null == map ? null : map.get( key );
  }

  /**
   * Cleanup and remove any context data associated with the current request.
   * This should be invoked by the outer interceptor.
   */
  static void clean()
  {
    c_context.remove();
  }
}
