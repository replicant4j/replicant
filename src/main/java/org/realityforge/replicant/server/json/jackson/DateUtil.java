package org.realityforge.replicant.server.json.jackson;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Utility class for serialization and deserialization of dates.
 */
final class DateUtil
{
  private DateUtil()
  {
  }

  static Date parse( final DateFormat formatter, final String source )
    throws IOException
  {
    try
    {
      return formatter.parse( source );
    }
    catch ( final ParseException pe )
    {
      throw new IOException( pe.getMessage(), pe );
    }
  }

  static DateFormat newDateFormatter()
  {
    return new SimpleDateFormat( "yyyy-MM-dd" );
  }
}
