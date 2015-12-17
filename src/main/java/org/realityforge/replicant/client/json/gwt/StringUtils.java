package org.realityforge.replicant.client.json.gwt;

import javax.annotation.Nonnull;

final class StringUtils
{
  private static char[] HEX_CHARS =
    new char[]{ '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F' };

  private StringUtils()
  {
  }

  static String toHexString( @Nonnull final byte[] bytes )
  {
    char[] hexString = new char[ 2 * bytes.length ];

    int i = 0;

    for ( final byte b : bytes )
    {
      hexString[ i++ ] = HEX_CHARS[ ( b & 240 ) >> 4 ];
      hexString[ i++ ] = HEX_CHARS[ b & 15 ];
    }

    return new String( hexString );
  }
}
