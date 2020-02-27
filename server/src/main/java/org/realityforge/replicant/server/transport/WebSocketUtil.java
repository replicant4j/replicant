package org.realityforge.replicant.server.transport;

import java.io.IOException;
import javax.annotation.Nonnull;
import javax.json.JsonObject;
import javax.websocket.Session;
import org.realityforge.replicant.server.json.JsonEncoder;

@SuppressWarnings( "WeakerAccess" )
public final class WebSocketUtil
{
  private WebSocketUtil()
  {
  }

  public static void sendJsonObject( @Nonnull final Session session, @Nonnull final JsonObject message )
  {
    sendText( session, JsonEncoder.asString( message ) );
  }

  public static boolean sendText( @Nonnull final Session session, @Nonnull final String message )
  {
    if ( session.isOpen() )
    {
      try
      {
        session.getBasicRemote().sendText( message );
        return true;
      }
      catch ( final IOException ignored )
      {
        // This typically means that either the buffer is full or the websocket is in a bad state
        // Try to close the connection to let session be reaped.
        try
        {
          session.close();
        }
        catch ( final IOException ignore )
        {
          //Ignore as well.
        }
      }
    }
    return false;
  }
}
