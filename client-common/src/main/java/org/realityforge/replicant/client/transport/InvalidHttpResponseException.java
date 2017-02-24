package org.realityforge.replicant.client.transport;

import javax.annotation.Nullable;

public class InvalidHttpResponseException
  extends Exception
{
  private final int _statusCode;
  private final String _statusLine;

  /**
   * Method only supplied for serialization. It should not be used.
   */
  @Deprecated
  public InvalidHttpResponseException()
  {
    this( 0, null );
  }

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
    return "InvalidHttpResponseException[StatusCode=" + _statusCode + ",StatusLine='" + _statusLine + '\'' + ']';
  }
}
