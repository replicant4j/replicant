package org.realityforge.replicant.client;

/**
 * A change to an entity.
 */
public interface Change
{
  /**
   * @return the unique discriminator or designator for the entity. Typically this is the primary key of the entity in the database.
   */
  int getDesignatorAsInt();

    /**
   * @return the unique discriminator or designator for the entity. Typically this is the primary key of the entity in the database.
   */
  String getDesignatorAsString();

  /**
   * @return a code indicating the type of the entity changed.
   */
  int getTypeID();

  /**
   * @return true if the change is an update, false if it is a remove.
   */
  boolean isUpdate();

  /**
   * Return true if data for the attribute identified by the key is present in the change.
   *
   * @param key the attribute key.
   * @return true if the data is present.
   */
  boolean containsKey( String key );

  /**
   * Return true if data for the attribute identified by the key is null.
   *
   * @param key the attribute key.
   * @return true if the data is null.
   */
  boolean isNull( String key );

  int getIntegerValue( String key );

  String getStringValue( String key );

  boolean getBooleanValue( String key );
}
