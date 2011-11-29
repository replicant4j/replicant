package org.realityforge.replicant.client;

import java.io.Serializable;
import javax.annotation.Nonnull;

/**
 * The client-side GWT representation of a date.
 */
public final class RDate
  implements Comparable<RDate>, Serializable
{
  private final int _year;
  private final int _month;
  private final int _day;
  @Nonnull
  private final DayOfWeek _dayOfWeek;

  @SuppressWarnings( { "ConstantConditions" } )
  public RDate( final int year, final int month, final int day, @Nonnull final DayOfWeek dayOfWeek )
  {
    assert ( year > 0 || year < 2050 );
    assert ( month > 0 || month <= 12 );
    assert ( day > 0 || day < 31 );
    assert ( null != dayOfWeek );
    _year = year;
    _month = month;
    _day = day;
    _dayOfWeek = dayOfWeek;
  }

  @Nonnull
  public DayOfWeek getDayOfWeek()
  {
    return _dayOfWeek;
  }

  public int getDay()
  {
    return _day;
  }

  public int getMonth()
  {
    return _month;
  }

  public int getYear()
  {
    return _year;
  }

  @Override
  public String toString()
  {
    return getDayOfWeek().name() + " " + getYear() + "-" + getMonth() + "-" + getDay();
  }

  @Override
  public int hashCode()
  {
    int h = getDay() * 31;
    h *= getMonth() * 13;
    h *= getYear() * 7;
    return h;
  }

  @Override
  public int compareTo( final RDate other )
  {
    if ( getYear() != other.getYear() )
    {
      return other.getYear() - getYear();
    }
    else if ( getMonth() != other.getMonth() )
    {
      return other.getMonth() - getMonth();
    }
    else if ( getDay() != other.getDay() )
    {
      return other.getDay() - getDay();
    }
    // Should never be an issue as it is bound by day but just in case ...
    else if ( getDayOfWeek() != other.getDayOfWeek() )
    {
      return other.getDayOfWeek().ordinal() - getDayOfWeek().ordinal();
    }
    else
    {
      return 0;
    }
  }

  @Override
  public boolean equals( final Object object )
  {
    if ( null == object || !( object instanceof RDate ) )
    {
      return false;
    }
    final RDate other = (RDate) object;
    return getYear() == other.getYear() &&
           getMonth() == other.getMonth() &&
           getDay() == other.getDay() &&
           getDayOfWeek() == other.getDayOfWeek();
  }

  public static RDate parse( final String text )
  {
    final int length = text.length();
    int i = 0;

    try
    {
      final StringBuilder sb = new StringBuilder();
      while ( i < length && !Character.isWhitespace( text.charAt( i ) ) )
      {
        sb.append( text.charAt( i ) );
        i++;
      }
      final DayOfWeek dayOfWeek = DayOfWeek.valueOf( sb.toString() );
      sb.setLength( 0 );

      //skip the space
      i++;

      while ( i < length && Character.isDigit( text.charAt( i ) ) )
      {
        sb.append( text.charAt( i ) );
        i++;
      }
      final int year = Integer.parseInt( sb.toString() );
      sb.setLength( 0 );

      //skip the -
      i++;

      while ( i < length && Character.isDigit( text.charAt( i ) ) )
      {
        sb.append( text.charAt( i ) );
        i++;
      }
      final int month = Integer.parseInt( sb.toString() );
      sb.setLength( 0 );

      //skip the -
      i++;

      while ( i < length && Character.isDigit( text.charAt( i ) ) )
      {
        sb.append( text.charAt( i ) );
        i++;
      }
      final int day = Integer.parseInt( sb.toString() );

      if ( i != length )
      {
        throw poorlyFormattedException( text );
      }

      return new RDate( year, month, day, dayOfWeek );
    }
    catch ( final NumberFormatException nfe )
    {
      throw poorlyFormattedException( text );
    }
  }

  private static IllegalArgumentException poorlyFormattedException( final String text )
  {
    final String message = "Poorly formatted date '" + text + "'";
    return new IllegalArgumentException( message );
  }
}
