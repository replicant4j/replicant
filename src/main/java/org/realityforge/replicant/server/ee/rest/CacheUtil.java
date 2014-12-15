package org.realityforge.replicant.server.ee.rest;

import java.util.Date;
import javax.ws.rs.core.Response;

final class CacheUtil
{
  private CacheUtil()
  {
  }

  static void configureNoCacheHeaders( final Response.ResponseBuilder builder )
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
    //Note: no-store is not included here as it will disable offline application storage on Firefox
    builder.header( "Cache-control", "no-cache, must-revalidate, pre-check=0, post-check=0" );
  }
}
