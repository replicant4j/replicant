package org.realityforge.replicant.shared.gwt_json;

import com.google.gwt.json.client.JSONValue;

public interface JsonGwtChangeMapper
{
  /**
   * Apply the change set to the local client repository.
   *
   * @param changeSet the json representation of the client set
   * @return the id of the change set applied.
   */
  int apply( JSONValue changeSet );
}
