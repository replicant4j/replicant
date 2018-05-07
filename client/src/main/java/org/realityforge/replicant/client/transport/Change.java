package org.realityforge.replicant.client.transport;

import java.util.Date;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * A change to an entity.
 */
public interface Change
{
  /**
   * @return the id of the entity.
   */
  int getId();

  /**
   * @return a code indicating the type of the entity changed.
   */
  int getTypeId();

  /**
   * @return true if the change is an update, false if it is a remove.
   */
  boolean isUpdate();

  /**
   * @return true if the change is a remove, false if it is an update.
   */
  default boolean isRemove()
  {
    return !isUpdate();
  }

  /**
   * Return true if data for the attribute identified by the key is present in the change.
   *
   * @param key the attribute key.
   * @return true if the data is present.
   */
  boolean containsKey( @Nonnull String key );

  /**
   * Return true if data for the attribute identified by the key is null.
   *
   * @param key the attribute key.
   * @return true if the data is null.
   */
  boolean isNull( @Nonnull String key );

  int getIntegerValue( @Nonnull String key );

  @Nonnull
  Date getDateValue( @Nonnull String key );

  @Nonnull
  String getStringValue( @Nonnull String key );

  boolean getBooleanValue( @Nonnull String key );

  /**
   * @return the number of channels on which the change is sent. Must be &gt; 1.
   */
  int getChannelCount();

  /**
   * Return the channel id at specific index.
   *
   * @param index the index of the channel.
   * @return the channel id.
   */
  int getChannelId( int index );

  /**
   * @param index the index of the channel.
   * @return the sub-channel id.
   */
  @Nullable
  Integer getSubChannelId( int index );
}
