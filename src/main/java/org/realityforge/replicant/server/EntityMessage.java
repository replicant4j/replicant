package org.realityforge.replicant.server;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class EntityMessage
{
  private final Serializable _id;
  private final int _typeID;
  private final Map<String, Serializable> _routingKeys;
  private Set<ChannelLink> _links;
  private Map<String, Serializable> _attributeValues;
  private long _timestamp;

  public EntityMessage( @Nonnull final Serializable id,
                        final int typeID,
                        final long timestamp,
                        @Nonnull final Map<String, Serializable> routingKeys,
                        @Nullable final Map<String, Serializable> attributeValues,
                        @Nullable final Set<ChannelLink> links )
  {
    _id = id;
    _typeID = typeID;
    _timestamp = timestamp;
    _routingKeys = routingKeys;
    _attributeValues = attributeValues;
    _links = links;
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

  public long getTimestamp()
  {
    return _timestamp;
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

  @Nullable
  public Set<ChannelLink> getLinks()
  {
    return _links;
  }

  @Nonnull
  public EntityMessage duplicate()
  {
    final EntityMessage message =
      new EntityMessage( getID(),
                         getTypeID(),
                         getTimestamp(),
                         new HashMap<String, Serializable>(),
                         new HashMap<String, Serializable>(),
                         null );
    message.merge( this );
    return message;
  }

  @Override
  public String toString()
  {
    return ( isUpdate() ? "U" : "D" ) +
           "(Type=" + getTypeID() +
           ",ID=" + getID() +
           ",RoutingKeys=" + getRoutingKeys() +
           ( !isDelete() ? ",Data=" + getAttributeValues() : "" ) +
           ",Links=" + getLinks() +
           ")";
  }

  public void merge( final EntityMessage message )
  {
    mergeTimestamp( message );
    mergeRoutingKeys( message );
    mergeAttributeValues( message );
    mergeLinks( message );
  }

  private void mergeTimestamp( final EntityMessage message )
  {
    if ( message.getTimestamp() > getTimestamp() )
    {
      _timestamp = message.getTimestamp();
    }
  }

  private void mergeRoutingKeys( final EntityMessage message )
  {
    final Map<String, Serializable> routingKeys = message.getRoutingKeys();
    for ( final Map.Entry<String, Serializable> entry : routingKeys.entrySet() )
    {
      getRoutingKeys().put( entry.getKey(), entry.getValue() );
    }
  }

  private void mergeAttributeValues( final EntityMessage message )
  {
    final Map<String, Serializable> attributeValues = message.getAttributeValues();
    if ( null == attributeValues )
    {
      _attributeValues = null;
    }
    else
    {
      if ( null == _attributeValues )
      {
        _attributeValues = new HashMap<String, Serializable>();
      }
      for ( final Map.Entry<String, Serializable> entry : attributeValues.entrySet() )
      {
        _attributeValues.put( entry.getKey(), entry.getValue() );
      }
    }
  }

  private void mergeLinks( final EntityMessage message )
  {
    final Set<ChannelLink> links = message.getLinks();
    if ( null != links )
    {
      if ( null == _links )
      {
        _links = new HashSet<ChannelLink>();
      }
      _links.addAll( links );
    }
  }
}
