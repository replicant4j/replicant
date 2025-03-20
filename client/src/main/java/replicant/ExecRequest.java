package replicant;

import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

final class ExecRequest
{
  @Nonnull
  private final String _command;
  @Nullable
  private final Object _payload;
  private int _requestId;

  ExecRequest( @Nonnull final String command, @Nullable final Object payload )
  {
    _command = Objects.requireNonNull( command );
    _payload = payload;
    _requestId = -1;
  }

  @Nonnull
  String getCommand()
  {
    return _command;
  }

  @Nullable
  Object getPayload()
  {
    return _payload;
  }

  boolean isInProgress()
  {
    return -1 != _requestId;
  }

  int getRequestId()
  {
    return _requestId;
  }

  void markAsInProgress( final int requestId )
  {
    _requestId = requestId;
  }

  void markAsComplete()
  {
    _requestId = -1;
  }

  @Override
  public String toString()
  {
    if ( Replicant.areNamesEnabled() )
    {
      return "ExecRequest[" +
             "Command=" + _command +
             ( null == _payload ? "" : " Filter=" + FilterUtil.filterToString( _payload ) ) + "]" +
             ( -1 != _requestId ? "(InProgress)" : "" );
    }
    else
    {
      return super.toString();
    }
  }
}
