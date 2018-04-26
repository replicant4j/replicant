package org.realityforge.replicant.client.ee;

import java.io.StringReader;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.json.Json;
import javax.json.JsonObject;
import org.realityforge.replicant.client.transport.Change;
import org.realityforge.replicant.client.transport.ChangeSet;
import org.realityforge.replicant.client.ChannelAction;
import org.realityforge.replicant.shared.json.TransportConstants;

public class ChangeSetDTO
  implements ChangeSet
{
  private final JsonObject _object;

  public ChangeSetDTO( @Nonnull final JsonObject object )
  {
    _object = object;
  }

  @Override
  public int getSequence()
  {
    return _object.getInt( TransportConstants.LAST_CHANGE_SET_ID );
  }

  @Override
  @Nullable
  public String getRequestID()
  {
    return _object.isNull( TransportConstants.REQUEST_ID ) ? null : _object.getString( TransportConstants.REQUEST_ID );
  }

  @Nullable
  @Override
  public String getETag()
  {
    return _object.isNull( TransportConstants.ETAG ) ? null : _object.getString( TransportConstants.ETAG );
  }

  @Override
  public int getChangeCount()
  {
    return _object.containsKey( TransportConstants.CHANGES ) ?
           _object.getJsonArray( TransportConstants.CHANGES ).size() :
           0;
  }

  @Override
  @Nonnull
  public final Change getChange( final int index )
  {
    return new ChangeDTO( _object.getJsonArray( TransportConstants.CHANGES ).getJsonObject( index ) );
  }

  @Override
  public int getChannelActionCount()
  {
    return _object.containsKey( TransportConstants.CHANNEL_ACTIONS ) ?
           _object.getJsonArray( TransportConstants.CHANNEL_ACTIONS ).size() :
           0;
  }

  @Override
  @Nonnull
  public ChannelAction getChannelAction( final int index )
  {
    return new ChannelActionDTO( _object.getJsonArray( TransportConstants.CHANNEL_ACTIONS ).getJsonObject( index ) );
  }

  @Nonnull
  public static ChangeSet asChangeSet( @Nonnull final String json )
  {
    return new ChangeSetDTO( Json.createReader( new StringReader( json ) ).readObject() );
  }
}
