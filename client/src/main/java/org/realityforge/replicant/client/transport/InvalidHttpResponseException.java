package org.realityforge.replicant.client.transport;

import javax.annotation.Nullable;
import replicant.Replicant;

@SuppressWarnings( "GwtInconsistentSerializableClass" )
public class InvalidHttpResponseException
  extends Exception
{
  private final int _statusCode;
  private final String _statusLine;

  public InvalidHttpResponseException( final int statusCode, @Nullable final String statusLine )
  {
    _statusCode = statusCode;
    _statusLine = statusLine;
  }

  public int getStatusCode()
  {
    return _statusCode;
  }

  @Nullable
  public String getStatusLine()
  {
    return _statusLine;
  }

  @Override
  public String toString()
  {
    if ( Replicant.areNamesEnabled() )
    {
      return "InvalidHttpResponseException[StatusCode=" + _statusCode + ",StatusLine='" + _statusLine + '\'' + ']';
    }
    else
    {
      return super.toString();
    }
  }
}
