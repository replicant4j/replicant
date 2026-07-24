package replicant;

import java.util.Objects;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

final class ExecRequest
{
  @NonNull
  private final String _command;
  @Nullable
  private final Object _payload;
  @Nullable
  private final ResponseHandler _responseHandler;
  private int _requestId;

  ExecRequest( @NonNull final String command,
               @Nullable final Object payload,
               @Nullable final ResponseHandler responseHandler )
  {
    _command = Objects.requireNonNull( command );
    _payload = payload;
    _responseHandler = responseHandler;
    _requestId = -1;
  }

  @NonNull
  String getCommand()
  {
    return _command;
  }

  @Nullable
  Object getPayload()
  {
    return _payload;
  }

  @Nullable
  ResponseHandler getResponseHandler()
  {
    return _responseHandler;
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
