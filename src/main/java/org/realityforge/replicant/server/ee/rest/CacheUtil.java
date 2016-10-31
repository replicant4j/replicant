package org.realityforge.replicant.server.ee.rest;

import java.util.Date;
import javax.annotation.Nonnull;
import javax.ws.rs.core.Response;

final class CacheUtil
{
  private CacheUtil()
  {
  }

  static void configureNoCacheHeaders( @Nonnull final Response.ResponseBuilder builder )
  {
    final Date now = new Date();
    // set create date to current timestamp
    builder.header( "Date", now.getTime() );
    // set modify date to current timestamp
    builder.header( "Last-Modified", now.getTime() );
    // set expiry to back in the past (makes us a bad candidate for caching)
    builder.header( "Expires", 0 );
    // HTTP 1.0 (disable caching)
    builder.header( "Pragma", "no-cache" );
    // HTTP 1.1 (disable caching of any kind)
    // HTTP 1.1 'pre-check=0, post-check=0' => (Internet Explorer should always check)
    builder.header( "Cache-control", "private, no-store, no-cache, must-revalidate, max-age=0, pre-check=0, post-check=0" );
  }
}
