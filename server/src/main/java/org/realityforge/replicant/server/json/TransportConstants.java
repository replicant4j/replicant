package org.realityforge.replicant.server.json;

/**
 * Constants used to build up JSON payload transmitted to the client.
 */
interface TransportConstants
{
  String LAST_CHANGE_SET_ID = "last_id";
  String REQUEST_ID = "requestId";
  String ETAG = "etag";
  String CHANGES = "changes";
  String ENTITY_ID = "id";
  String DATA = "data";
  String CHANNEL_ACTIONS = "channels";
  String FILTERED_CHANNEL_ACTIONS = "fchannels";
  String CHANNEL = "channel";
  String ACTION_ADD = "+";
  String ACTION_REMOVE = "-";
  String ACTION_UPDATE = "=";
  String CHANNELS = "channels";
  String CHANNEL_FILTER = "filter";
}
