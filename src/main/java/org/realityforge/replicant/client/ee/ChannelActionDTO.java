package org.realityforge.replicant.client.ee;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;
import org.realityforge.replicant.client.ChannelAction;
import org.realityforge.replicant.shared.json.TransportConstants;

public class ChannelActionDTO
  implements ChannelAction
{
  private final JsonObject _object;

  public ChannelActionDTO( @Nonnull final JsonObject object )
  {
    _object = object;
  }

  @Override
  public int getChannelID()
  {
    return _object.getInt( TransportConstants.CHANNEL_ID );
  }

  @Override
  @Nullable
  public Object getSubChannelID()
  {
    final JsonValue value = _object.get( TransportConstants.SUBCHANNEL_ID );
    return value.getValueType() == JsonValue.ValueType.NUMBER ?
           ( (JsonNumber) value ).intValue() :
           value.getValueType() == JsonValue.ValueType.STRING ?
           ( (JsonString) value ).getString() :
           null;
  }

  @Override
  @Nonnull
  public Action getAction()
  {
    return Action.valueOf( _object.getString( TransportConstants.ACTION ).toUpperCase() );
  }

  @Override
  public Object getChannelFilter()
  {
    final JsonValue value = _object.get( TransportConstants.CHANNEL_FILTER );
    return value.getValueType() == JsonValue.ValueType.OBJECT ? ( (JsonObject) value ) : null;
  }
}
