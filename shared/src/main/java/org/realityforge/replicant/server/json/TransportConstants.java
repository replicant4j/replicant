package org.realityforge.replicant.server.json;

/**
 * Constants used to build up JSON payload transmitted to the client.
 */
public interface TransportConstants
{
  String LAST_CHANGE_SET_ID = "last_id";
  String REQUEST_ID = "request_id";
  String ETAG = "etag";
  String CHANGES = "changes";
  String ENTITY_ID = "id";
  String TYPE_ID = "type";
  String DATA = "data";
  String CHANNEL_ACTIONS = "channel_actions";
  String ACTION = "action";
  String ACTION_ADD = "add";
  String ACTION_REMOVE = "remove";
  String ACTION_UPDATE = "update";
  String CHANNELS = "channels";
  String CHANNEL_ID = "cid";
  String SUBCHANNEL_ID = "scid";
  String CHANNEL_FILTER = "filter";
}
