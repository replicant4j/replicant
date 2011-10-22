package org.realityforge.replicant.server;

import java.io.Serializable;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class EntityMessage
{
  private final Serializable _id;
  private final int _typeID;
  private final Map<String, Serializable> _attributeValues;
  private final Map<String, Serializable> _routingKeys;

  public EntityMessage( @Nonnull final Serializable id,
                        final int typeID,
                        @Nonnull final Map<String, Serializable> routingKeys,
                        @Nullable final Map<String, Serializable> attributeValues )
  {
    _id = id;
    _typeID = typeID;
    _routingKeys = routingKeys;
    _attributeValues = attributeValues;
  }

  public int getTypeID()
  {
    return _typeID;
  }

  @Nonnull
  public Serializable getID()
  {
    return _id;
  }

  public boolean isUpdate()
  {
    return null != getAttributeValues();
  }

  public boolean isDelete()
  {
    return !isUpdate();
  }

  @Nullable
  public Map<String, Serializable> getAttributeValues()
  {
    return _attributeValues;
  }

  @Nonnull
  public Map<String, Serializable> getRoutingKeys()
  {
    return _routingKeys;
  }

  @Override
  public String toString()
  {
    return ( isUpdate() ? "U" : "D" ) +
           "(Type=" + getTypeID() +
           ",ID=" + getID() +
           ",RoutingKeys=" + getRoutingKeys() +
           ( !isDelete() ? ",Data=" + getAttributeValues() : "" ) +
           ")";
  }
}
