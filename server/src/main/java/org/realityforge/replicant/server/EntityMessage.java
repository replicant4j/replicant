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
  private final int _id;
  private final int _typeId;
  /**
   * Routing keys contain two types of values.
   * For every graph that the entity type is contained within, the map will
   * contain "filter_in_graphs" attributes for this entity and any entity on
   * the path to the root of the instance graph. The map will also contain the
   * the id of any instance roots for graphs that graph_link to this entity.
   */
  private final Map<String, Serializable> _routingKeys;
  private Set<ChannelLink> _links;
  private Map<String, Serializable> _attributeValues;
  private long _timestamp;

  public EntityMessage( final int id,
                        final int typeId,
                        final long timestamp,
                        @Nonnull final Map<String, Serializable> routingKeys,
                        @Nullable final Map<String, Serializable> attributeValues,
                        @Nullable final Set<ChannelLink> links )
  {
    _id = id;
    _typeId = typeId;
    _timestamp = timestamp;
    _routingKeys = routingKeys;
    _attributeValues = attributeValues;
    _links = links;
  }

  public int getTypeId()
  {
    return _typeId;
  }

  public int getId()
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
      new EntityMessage( getId(), getTypeId(), getTimestamp(), new HashMap<>(), new HashMap<>(), null );
    message.merge( this );
    return message;
  }

  @Override
  public String toString()
  {
    return ( isUpdate() ? "U" : "D" ) +
           "(Type=" + getTypeId() +
           ",ID=" + getId() +
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
        _attributeValues = new HashMap<>();
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
        _links = new HashSet<>();
      }
      _links.addAll( links );
    }
  }
}
