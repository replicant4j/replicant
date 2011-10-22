package org.realityforge.replicant.shared.rpc;

import java.io.Serializable;
import java.util.HashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class Change
  implements Serializable
{
  private Serializable _id;
  private int _typeID;
  private HashMap<String, Serializable> _values;

  @SuppressWarnings( { "UnusedDeclaration" } )
  private Change()
  {
  }

  public Change( @Nonnull final Serializable id, final int typeID, @Nullable final HashMap<String, Serializable> values )
  {
    _id = id;
    _typeID = typeID;
    _values = values;
  }

  public int getTypeID()
  {
    return _typeID;
  }

  @Nonnull
  public Object getID()
  {
    return _id;
  }

  public boolean isUpdate()
  {
    return null != _values;
  }

  public boolean isDelete()
  {
    return !isUpdate();
  }

  @Nullable
  public HashMap<String, Serializable> getAttributeValues()
  {
    return _values;
  }

  @Override
  public String toString()
  {
    return (isUpdate() ? "U" : "D") + "(Type=" + _typeID + ",ID=" + _id + (isUpdate() ? ",Data=" + _values : "") +")";
  }
}
