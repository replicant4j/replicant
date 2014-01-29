package org.realityforge.replicant.server.ee;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A mechanism for passing context data from the servlet/ejb tier to the ejb tier.
 * <p/>
 * <p>The expectation is that an interceptor in the ejb tier will inspect and utilize
 * the context data. The implementation uses thread-locals as it assumes that at least
 * the first interceptor will be invoked in the thread that initiates the request.</p>
 */
public final class ReplicantContextHolder
{
  private static final ThreadLocal<Map<String, Serializable>> c_context = new ThreadLocal<>();

  private ReplicantContextHolder()
  {
  }

  /**
   * Specify some context data for a particular key.
   *
   * @param key  the key.
   * @param data the data.
   */
  public static void put( @Nonnull final String key, @Nonnull final Serializable data )
  {
    if ( null == c_context.get() )
    {
      c_context.set( new HashMap<String, Serializable>() );
    }
    c_context.get().put( key, data );
  }

  /**
   * Retrieve context data specified for key.
   *
   * @param key the key.
   * @return the context data if any, else null.
   */
  @Nullable
  public static Serializable get( @Nonnull final String key )
  {
    final Map<String, Serializable> map = c_context.get();
    if ( null == map )
    {
      return null;
    }
    else
    {
      return map.get( key );
    }
  }

  /**
   * Cleanup and remove any context data associated with the current request.
   * This should be invoked by the outer interceptor.
   */
  public static void clean()
  {
    c_context.remove();
  }
}